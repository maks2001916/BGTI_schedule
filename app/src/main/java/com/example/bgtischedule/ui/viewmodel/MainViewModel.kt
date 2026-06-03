package com.example.bgtischedule.ui.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bgtischedule.data.auth.AuthStateManager
import com.example.bgtischedule.datebase.ScheduleRepository
import com.example.bgtischedule.parser.ScheduleParser
import com.example.bgtischedule.api.UniversityApi
import com.example.bgtischedule.datebase.LessonEntity
import com.example.bgtischedule.model.StudentModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val authManager: AuthStateManager,
    private val scheduleRepo: ScheduleRepository,
    private val api: UniversityApi,
    private val parser: ScheduleParser
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        object Unauthorized : UiState()
        data class Authorized(val student: StudentModel) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (authManager.checkSavedCredentials()) {
                restoreSession()
            } else {
                _uiState.value = UiState.Unauthorized
            }
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

    fun onLogout() {
        viewModelScope.launch {
            authManager.logout()
            _uiState.value = UiState.Unauthorized
        }
    }

    fun syncSchedule() {
        viewModelScope.launch {
            try {
                val group = authManager.getCurrentGroup() ?: return@launch
                val html = api.getSchedulePage() ?: throw Exception("Нет ответа от сервера")
                val schedule = parser.parse(html) ?: throw Exception("Ошибка парсинга")
                val lessons = schedule.lessons.map { it.toLessonDbEntity().copy(group = group) }
                val (weekStart, weekEnd) = parseWeekRange(schedule.weekRange)
                if (weekStart.isNotBlank() && weekEnd.isNotBlank()) {
                    scheduleRepo.setWeek(weekStart, weekEnd, lessons)
                }
            } catch (_: Exception) {
            }
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
