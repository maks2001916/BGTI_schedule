package com.example.bgtischedule.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bgtischedule.data.auth.AuthStateManager
import com.example.bgtischedule.datebase.ScheduleRepository
import com.example.bgtischedule.parser.ScheduleParser
import com.example.bgtischedule.api.UniversityApi
import com.example.bgtischedule.data.model.SyncResult
import com.example.bgtischedule.model.ScheduleUiModel
import com.example.bgtischedule.model.StudentModel
import com.example.bgtischedule.service.Request
import com.example.bgtischedule.ui.mapper.LessonMapper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class MainViewModel(
    private val authManager: AuthStateManager,
    private val scheduleRepo: ScheduleRepository,
    private val api: UniversityApi,
    private val parser: ScheduleParser,
    private val request: Request
) : ViewModel() {

    private companion object { const val TAG = "MainViewModel" }

    sealed class UiState {
        object Loading : UiState()
        object Unauthorized : UiState()
        data class Authorized(val student: StudentModel) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()


    data class ScheduleState(
        val scheduleUi: List<ScheduleUiModel.DayGroupUi>? = null,
        val weekRange: String = "",
        val lastSyncTime: Long? = null,
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val weekOffset: Int = 0
    )

    private val _scheduleState = MutableStateFlow(ScheduleState())
    val scheduleState: StateFlow<ScheduleState> = _scheduleState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(replay = 0)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private suspend fun showMessage(msg: String) {
        _messages.emit(msg)
    }

    init {
        viewModelScope.launch {
            if (authManager.checkSavedCredentials()) {
                restoreSession()
            } else {
                _uiState.value = UiState.Unauthorized
            }
        }
    }


    fun loadSchedule(
        login: String?,
        password: String?,
        group: String?
    ) {

        if (login.isNullOrBlank() || password.isNullOrBlank() || group.isNullOrBlank()) return
        if (_scheduleState.value.isLoading) return


        viewModelScope.launch {

            val hadData = _scheduleState.value.scheduleUi != null
            _scheduleState.value = _scheduleState.value.copy(isLoading = true, errorMessage = null)

            val weekStart = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .plusWeeks(_scheduleState.value.weekOffset.toLong())

            val cached = try {
                request.loadCachedWeek(group, weekStart)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load from cache", e)
                null
            }

            if (cached is SyncResult.Cached) {
                //applyScheduleResult(cached)
                _scheduleState.value = applyResult(cached)
            }


            try {
                Log.d(TAG, "LoadSchedule: group=$group")
                when (val result = request.refreshWeek(
                    group,
                    login,
                    password,
                    _scheduleState.value.weekOffset
                )) {
                    is SyncResult.Success -> {
                        //applyScheduleResult(result)
                        _scheduleState.value = applyResult(result)
                        if (result.changes.isNotEmpty()) {
                            val added = result.changes.count {it.type == SyncResult.ChangeType.ADDED }
                            val modified = result.changes.count { it.type == SyncResult.ChangeType.MODIFIED }
                            showMessage("Обновлено: +$added новых, $modified изменено")
                        }
                    }
                    is SyncResult.Error -> {
                        _scheduleState.value = _scheduleState.value.copy(
                            isLoading = false,
                            errorMessage = if (!hadData && cached !is SyncResult.Cached) result.message else null
                        )
                    }

                    else -> _scheduleState.value = _scheduleState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadSchedule error", e)
                _scheduleState.value = _scheduleState.value.copy(
                    isLoading = false,
                    errorMessage = if (!hadData && cached !is SyncResult.Cached) "Нет подключения" else null
                )
            }
        }
    }

    private fun applyResult(result: SyncResult): ScheduleState {
        return when (result) {
            is SyncResult.Success, is SyncResult.Cached -> {
                val schedule = when (result) {
                    is SyncResult.Success -> result.schedule
                    is SyncResult.Cached -> result.schedule
                    else -> throw IllegalStateException()
                }
                _scheduleState.value.copy(
                    scheduleUi = LessonMapper.toDayGroups(schedule.lessons),
                    weekRange = schedule.weekRange,
                    lastSyncTime = System.currentTimeMillis(),
                    isLoading = false,
                    errorMessage = null
                )
            }
            is SyncResult.Error -> _scheduleState.value.copy(
                isLoading = true,
                errorMessage = result.message
            )
        }
    }

    /** Переключиться на следующую неделю */
    fun showNextWeek(login: String?, password: String?, group: String?) {
        _scheduleState.value = _scheduleState.value.copy(
            weekOffset = _scheduleState.value.weekOffset + 1
        )
        loadSchedule(login, password, group)
    }

    /** Переключиться на предыдущую неделю (не уходим в минус) */
    fun showPreviousWeek(login: String?, password: String?, group: String?) {
        val current = _scheduleState.value.weekOffset
        if (current > 0) {
            _scheduleState.value = _scheduleState.value.copy(weekOffset = current - 1)
            loadSchedule(login, password, group)
        }
    }

    /** Смена аккаунта — полный сброс состояния расписания и перезагрузка */
    fun onAccountChanged(login: String?, password: String?, group: String?) {
        _scheduleState.value = ScheduleState()
        loadSchedule(login, password, group)
    }

    /** Выход — очищаем всё */
    fun onLogout() {
        viewModelScope.launch {
            authManager.logout()
            _uiState.value = UiState.Unauthorized
            _scheduleState.value = ScheduleState()
        }
    }

    private suspend fun fetchStudentInfo(): StudentModel? {
        val html = api.getSchedulePage() ?: return null
        return parser.parse(html)?.studentFIO
    }

    suspend fun restoreSession() {
        _uiState.value = UiState.Loading

        authManager.switchToActiveAccount()
        val creds = authManager.getActiveCredentials() ?: run {
            _uiState.value = UiState.Unauthorized
            return
        }
        if (!api.login(creds.login, creds.password)) {
            _uiState.value = authManager.authState.value.student
                ?.let { UiState.Authorized(it) }
                ?: UiState.Unauthorized
            return
        }
        authManager.authenticateWithServer { fetchStudentInfo() }
            .onSuccess {
                _uiState.value = authManager.authState.value.student
                    ?.let { UiState.Authorized(it) }
                    ?: UiState.Error("Данные студента не загружены")
            }
            .onFailure {
                _uiState.value = UiState.Error(it.message ?: "Ошибка входа")
            }
    }

    fun onLogin(login: String, password: String) {
        viewModelScope.launch {
            if (!api.login(login, password)) {
                _uiState.value = UiState.Error("Не удалось авторизоваться на сервере")
                return@launch
            }
            authManager.loginNewAccount(login, password) { fetchStudentInfo() }
                .onSuccess {
                    _uiState.value = authManager.authState.value.student
                        ?.let { UiState.Authorized(it) }
                        ?: UiState.Error("Данные студента не загружены")
                }
                .onFailure {
                    _uiState.value = UiState.Error(it.message ?: "Ошибка входа")
                }
        }
    }

    /**
     * Переключение аккаунта: авторизация + обновление данных студента + загрузка расписания
     */
    fun onSwitchAccount() {
        //_uiState.value = UiState()


        viewModelScope.launch {
            authManager.switchToActiveAccount()
            val creds = authManager.getActiveCredentials() ?: return@launch

            // 1. Авторизация с новыми креденшнлами
            if (!api.login(creds.login, creds.password)) {
                // Сеть недоступна, но у нас есть данные из кэша
                _uiState.value = authManager.authState.value.student
                    ?.let { UiState.Authorized(it) }
                    ?: UiState.Error("Не удалось авторизоваться на сервере")

                // Загружаем расписание из кэша
                val group = authManager.authState.value.student?.group
                loadSchedule(creds.login, creds.password, group)
                return@launch
            }

            // 2. Получение данных студента (ФИО, группа)
            authManager.authenticateWithServer { fetchStudentInfo() }
                .onSuccess {
                    val student = authManager.authState.value.student
                    _uiState.value = student ?.let { UiState.Authorized(it) }
                        ?: UiState.Error("Не удалось получить данные студента")
                    student?.group?.let { group ->}
                }
                .onFailure {
                    _uiState.value = UiState.Error(it.message ?: "Ошибка авторизации")
                }

            // 3. Загрузка расписания для нового аккаунта
            val group = authManager.authState.value.student?.group
            loadSchedule(creds.login, creds.password, group)
        }
    }

    private fun parseWeekRange(weekRange: String): Pair<String, String> {
        val isoRegex = """(\d{4}-\d{2}-\d{2})""".toRegex()
        val isoDates = isoRegex.findAll(weekRange).map { it.value }.toList()
        if (isoDates.size >= 2) return isoDates[0] to isoDates[1]

        val dotRegex = """(\d{2})\.(\d{2})\.(\d{4})""".toRegex()
        val parsed = dotRegex.findAll(weekRange).map { match ->
            "${match.groupValues[3]}-${match.groupValues[2]}-${match.groupValues[1]}"
        }.toList()
        return if (parsed.size >= 2) parsed[0] to parsed[1] else "" to ""
    }
}
