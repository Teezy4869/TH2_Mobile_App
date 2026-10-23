package com.example.myapplication.data

import android.content.Context
import androidx.core.content.edit

/** Stores the logged-in account in SharedPreferences. */
class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val isLoggedIn: Boolean get() = accountId != NO_ACCOUNT
    val accountId: Long get() = prefs.getLong(KEY_ACCOUNT_ID, NO_ACCOUNT)
    val username: String get() = prefs.getString(KEY_USERNAME, "").orEmpty()
    val role: String get() = prefs.getString(KEY_ROLE, "").orEmpty()
    val rememberMe: Boolean get() = prefs.getBoolean(KEY_REMEMBER, false)
    val rememberedUsername: String get() = prefs.getString(KEY_LAST_USERNAME, "").orEmpty()

    fun saveLogin(account: Account, remember: Boolean) = prefs.edit {
        putLong(KEY_ACCOUNT_ID, account.id)
        putString(KEY_USERNAME, account.username)
        putString(KEY_ROLE, account.role)
        putBoolean(KEY_REMEMBER, remember)
        if (remember) putString(KEY_LAST_USERNAME, account.username) else remove(KEY_LAST_USERNAME)
    }

    fun logout() = prefs.edit {
        remove(KEY_ACCOUNT_ID)
        remove(KEY_USERNAME)
        remove(KEY_ROLE)
    }

    /** Called on a fresh app start: only "Remember me" sessions survive a restart. */
    fun clearIfNotRemembered() {
        if (!rememberMe) logout()
    }

    private companion object {
        const val PREFS_NAME = "fruit_shop_session"
        const val KEY_ACCOUNT_ID = "account_id"
        const val KEY_USERNAME = "username"
        const val KEY_ROLE = "role"
        const val KEY_REMEMBER = "remember_me"
        const val KEY_LAST_USERNAME = "last_username"
        const val NO_ACCOUNT = -1L
    }
}
