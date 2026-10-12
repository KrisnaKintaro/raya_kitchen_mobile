package kelompok.satu.raya_kitchen_mobile.network.api

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URLDecoder

class VolleySingleton constructor(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: VolleySingleton? = null

        val cookieManager = CookieManager(null, CookiePolicy.ACCEPT_ALL)

        init {
            // Pasang toples ini jadi standar buat semua koneksi HTTP di aplikasi
            CookieHandler.setDefault(cookieManager)
        }

        fun getInstance(context: Context) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: VolleySingleton(context).also { INSTANCE = it }
            }
    }

    val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(context.applicationContext)
    }

    fun <T> addToRequestQueue(req: Request<T>) {
        requestQueue.add(req)
    }

    // 2. FUNGSI SATPAM: Ambil dan Decode XSRF-TOKEN dari dalam toples
    fun getCsrfToken(): String {
        val cookies = cookieManager.cookieStore.cookies
        for (cookie in cookies) {
            if (cookie.name == "XSRF-TOKEN") {
                // Decode token agar simbol seperti %3D kembali normal
                return URLDecoder.decode(cookie.value, "UTF-8")
            }
        }
        return ""
    }
}