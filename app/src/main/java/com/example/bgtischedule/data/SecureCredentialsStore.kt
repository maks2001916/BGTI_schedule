package com.example.bgtischedule.data

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

private val TAG = "SecureCredentialsStore"
@Serializable
data class Credentials(
    val id: String = java.util.UUID.randomUUID().toString(),
    val login: String,
    val password: String
)


class SecureCredentialsStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val json = Json { encodeDefaults = true }

    // === Ключи ===
    companion object {
        private const val FILE_NAME = "secure_credentials"
        private const val KEY_ACCOUNTS = "accounts_list"
        private const val KEY_ACTIVE_ID = "active_account_id"
    }
    /** Получить все аккаунты */
    fun getAllAccounts(): List<Credentials> {
        val jsonStr = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        return try { json.decodeFromString(jsonStr) } catch (e: Exception) { emptyList() }
    }

    /** Получить активный аккаунт */
    fun getActiveAccount(): Credentials? {
        val activeId = prefs.getString(KEY_ACTIVE_ID, null) ?: return null
        val account = getAllAccounts().find { it.id == activeId }
        Log.i(TAG, "$account")
        return account
    }

    /** Добавить аккаунт (и сделать активным) */
    fun addAccount(account: Credentials) {
        val accounts = getAllAccounts().toMutableList()
        accounts.add(account)
        saveAccounts(accounts)
        prefs.edit().putString(KEY_ACTIVE_ID, account.id).apply()
    }

    /** Удалить аккаунт */
    fun removeAccount(accountId: String) {
        val accounts = getAllAccounts().toMutableList()
        accounts.removeAll { it.id == accountId }
        saveAccounts(accounts)

        // Если удалили активный — активируем первый оставшийся
        if (accounts.isNotEmpty() && getActiveAccount()?.id == accountId) {
            prefs.edit().putString(KEY_ACTIVE_ID, accounts.first().id).apply()
        } else if (accounts.isEmpty()) {
            prefs.edit().remove(KEY_ACTIVE_ID).apply()
        }
    }

    /** Переключиться на другой аккаунт */
    fun switchAccount(accountId: String) {
        prefs.edit().putString(KEY_ACTIVE_ID, accountId).apply()
    }

    /** Очистить всё */
    fun clearAll() {
        prefs.edit()
            .remove(KEY_ACCOUNTS)
            .remove(KEY_ACTIVE_ID)
            .apply()
    }

    private fun saveAccounts(accounts: List<Credentials>) {
        val jsonStr = json.encodeToString(accounts)
        prefs.edit().putString(KEY_ACCOUNTS, jsonStr).apply()
    }
}
