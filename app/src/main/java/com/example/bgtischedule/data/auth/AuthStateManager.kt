package com.example.bgtischedule.data.auth

import android.util.Log
import com.example.bgtischedule.data.Credentials
import com.example.bgtischedule.data.SecureCredentialsStore
import com.example.bgtischedule.model.StudentModel
import kotlinx.coroutines.flow.*

private val TAG = "AuthStateManager"

class AuthStateManager(
    private val credentialsStore: SecureCredentialsStore
) {

    data class AuthState(
        val isAuthenticated: Boolean = false,
        val student: StudentModel? = null,
        val error: String? = null,
        val isLoading: Boolean = false
    )

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /** Добавить новый аккаунт и авторизоваться на сервере */
    suspend fun loginNewAccount(
        login: String,
        password: String,
        fetchStudentInfo: suspend () -> StudentModel?
    ): Result<Unit> {
        credentialsStore.addAccount(Credentials(login = login, password = password))
        return authenticateActiveAccount(fetchStudentInfo)
    }

    /** Авторизоваться по уже сохранённому активному аккаунту (старт приложения / переключение) */
    suspend fun authenticateActiveAccount(
        fetchStudentInfo: suspend () -> StudentModel?
    ): Result<Unit> {
        val creds = credentialsStore.getActiveAccount()
            ?: return Result.failure(Exception("Нет активного аккаунта"))
        return authenticateWithServer(fetchStudentInfo)
    }

    private suspend fun authenticateWithServer(
        fetchStudentInfo: suspend () -> StudentModel?
    ): Result<Unit> {
        return try {
            _authState.value = _authState.value.copy(isLoading = true, error = null)

            val student = fetchStudentInfo()
            Log.d(TAG, "authenticate: student=$student, group=${student?.group}")

            if (student != null) {
                _authState.value = AuthState(
                    isAuthenticated = true,
                    student = student,
                    isLoading = false
                )
                Result.success(Unit)
            } else {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = "Не удалось загрузить данные студента"
                )
                Result.failure(Exception("Student info not found"))
            }
        } catch (e: Exception) {
            _authState.value = _authState.value.copy(
                isLoading = false,
                error = e.message ?: "Ошибка авторизации"
            )
            Result.failure(e)
        }
    }

    suspend fun logout() {
        credentialsStore.clearAll()
        _authState.value = AuthState()
    }

    suspend fun checkSavedCredentials(): Boolean {
        val creds = credentialsStore.getActiveAccount()
        return !creds?.login.isNullOrBlank() && !creds?.password.isNullOrBlank()
    }

    fun getCurrentGroup(): String? = _authState.value.student?.group

    fun getActiveCredentials(): Credentials? = credentialsStore.getActiveAccount()

    fun clearSession() {
        _authState.value = AuthState()
    }

    fun setAuthError(message: String) {
        _authState.value = AuthState(
            isAuthenticated = false,
            isLoading = false,
            error = message
        )
    }

    fun setAuthLoading() {
        _authState.value = _authState.value.copy(isLoading = true, error = null)
    }
}
