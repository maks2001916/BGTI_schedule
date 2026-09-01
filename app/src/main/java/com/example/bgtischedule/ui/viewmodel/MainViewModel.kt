package com.example.bgtischedule.ui.viewmodel

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
        val dayGroups: List<ScheduleUiModel.DayGroupUi>? = null,
        val weekRange: String = "",
        val lastSyncTime: Long? = null,
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val weekOffset: Int = 0
    )

    private val _scheduleState = MutableStateFlow(ScheduleState())
    val scheduleState: StateFlow<ScheduleState> = _scheduleState.asStateFlow()

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

            val badData = _scheduleState.value.dayGroups != null
            _scheduleState.value = _scheduleState.value.copy(isLoading = true, errorMessage = null)

            val weekStart = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .plusWeeks(_scheduleState.value.weekOffset.toLong())

            val cached = request.loadCachedWeek(group, weekStart)
            if (cached is SyncResult.Cached) {
                _scheduleState.value = _scheduleState.value.copy(
                    dayGroups = LessonMapper.toDayGroups(cached.schedule.lessons),
                    weekRange = cached.schedule.weekRange,
                    isLoading = false
                )
            }


            try {
                when (val fresh = request.refreshWeek(group, login, password, _scheduleState.value.weekOffset)) {
                    is SyncResult.Success -> {
                        val newGroup = fresh.schedule.studentFIO.group.ifBlank {group}
                        _scheduleState.value = _scheduleState.value.copy(
                            dayGroups = LessonMapper.toDayGroups(fresh.schedule.lessons),
                            weekRange = fresh.schedule.weekRange,
                            lastSyncTime = System.currentTimeMillis(),
                            isLoading = false

                        )
                    }
                    is SyncResult.Error -> {
                        _scheduleState.value = _scheduleState.value.copy(
                            isLoading = false,
                            errorMessage = if (!badData) fresh.message else null
                        )
                    }

                    else -> _scheduleState.value = _scheduleState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadSchedule error", e)
                _scheduleState.value = _scheduleState.value.copy(
                    isLoading = false,
                    errorMessage = if (!badData) e.message else null
                )
            }
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

    private suspend fun restoreSession() {
        _uiState.value = UiState.Loading
        val creds = authManager.getActiveCredentials() ?: run {
            _uiState.value = UiState.Unauthorized
            return
        }
        if (!api.login(creds.login, creds.password)) {
            _uiState.value = UiState.Unauthorized
            return
        }
        authManager.authenticateActiveAccount { fetchStudentInfo() }
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
        viewModelScope.launch {
            val creds = authManager.getActiveCredentials() ?: return@launch

            // 1. Авторизация с новыми креденшнлами
            if (!api.login(creds.login, creds.password)) {
                _uiState.value = UiState.Error("Не удалось авторизоваться на сервере")
                return@launch
            }

            // 2. Получение данных студента (ФИО, группа)
            authManager.authenticateActiveAccount { fetchStudentInfo() }
                .onSuccess {
                    _uiState.value = authManager.authState.value.student
                        ?.let { UiState.Authorized(it) }
                        ?: UiState.Error("Не удалось получить данные студента")
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
