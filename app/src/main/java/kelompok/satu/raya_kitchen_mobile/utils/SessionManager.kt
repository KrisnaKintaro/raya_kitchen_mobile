package kelompok.satu.raya_kitchen_mobile.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private var prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = prefs.edit()

    companion object {
        private const val PREF_NAME = "RayaKitchenSession"
        private const val IS_LOGIN = "IsLoggedIn"
        private const val KEY_TOKEN = "AuthToken"
        private const val KEY_NAME = "UserName"
        private const val KEY_WA = "UserWa"
    }

    // Fungsi buat nyimpen data login setelah OTP bener / Login sukses
    fun createLoginSession(token: String, name: String, wa: String) {
        editor.putBoolean(IS_LOGIN, true)
        editor.putString(KEY_TOKEN, token)
        editor.putString(KEY_NAME, name)
        editor.putString(KEY_WA, wa)
        editor.apply() // Simpan secara asynchronous
    }

    // Ambil Token API (Dipakai buat header Authorization kalau mau nembak API lain)
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    // Cek apakah user sudah login atau belum
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(IS_LOGIN, false)
    }

    fun getUserName(): String {
        return prefs.getString(KEY_NAME, "User") ?: "User"
    }
    fun getUserWa(): String {
        return prefs.getString(KEY_WA, "") ?: ""
    }

    // Fungsi Logout (Hapus semua session)
    fun clearSession() {
        editor.clear()
        editor.apply()
    }
}