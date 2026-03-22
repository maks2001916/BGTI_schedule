package com.example.bgtischedule.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class Credentials(
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

    fun save(credentials: Credentials) {
        prefs.edit()
            .putString(KEY_LOGIN, credentials.login)
            .putString(KEY_PASSWORD, credentials.password)
            .apply()
    }

    fun load(): Credentials {
        return Credentials(
            login = prefs.getString(KEY_LOGIN, "") ?: "",
            password = prefs.getString(KEY_PASSWORD, "") ?: ""
        )
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_LOGIN)
            .remove(KEY_PASSWORD)
            .apply()
    }

    private companion object {
        const val FILE_NAME = "secure_credentials"
        const val KEY_LOGIN = "login"
        const val KEY_PASSWORD = "password"
    }
}
