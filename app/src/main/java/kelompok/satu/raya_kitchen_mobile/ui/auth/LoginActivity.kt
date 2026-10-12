package kelompok.satu.raya_kitchen_mobile.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import kelompok.satu.raya_kitchen_mobile.databinding.ActivityLoginBinding
import kelompok.satu.raya_kitchen_mobile.network.api.VolleySingleton
import kelompok.satu.raya_kitchen_mobile.ui.pembeli.MasterActivity
import kelompok.satu.raya_kitchen_mobile.utils.Constant
import kelompok.satu.raya_kitchen_mobile.utils.SessionManager
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. CEK SESSION: Kalau sudah login, langsung gas ke MasterActivity
        val sessionManager = SessionManager(this)
        if (sessionManager.isLoggedIn()) {
            val intent = Intent(this, MasterActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. Pancing Cookie CSRF ke Laravel (karena pake middleware web)
        pancingCsrfCookie()

        // 3. Pindah ke halaman Register
        binding.tvToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // 4. Aksi Show/Hide Password
        binding.ivTogglePasswordLogin.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility(binding.etPasswordLogin, binding.ivTogglePasswordLogin, isPasswordVisible)
        }

        // 5. Aksi Klik Login
        binding.btnLogin.setOnClickListener {
            reqLoginApi()
        }
    }

    private fun pancingCsrfCookie() {
        val url = Constant.BASE_URL.replace("api/", "")
        val request = object : StringRequest(Method.GET, url, {}, {}) {}
        VolleySingleton.getInstance(this).addToRequestQueue(request)
    }

    private fun togglePasswordVisibility(editText: android.widget.EditText, icon: android.widget.ImageView, isVisible: Boolean) {
        if (isVisible) {
            editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            icon.alpha = 1.0f
        } else {
            editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            icon.alpha = 0.5f
        }
        editText.setSelection(editText.text.length)
    }

    private fun reqLoginApi() {
        val wa = binding.etWhatsappLogin.text.toString().trim()
        val pass = binding.etPasswordLogin.text.toString()

        if (wa.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Nomor WA dan Password wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "Mengecek..."

        val url = Constant.BASE_URL + "auth/login"
        val stringRequest = object : StringRequest(Method.POST, url,
            Response.Listener { response ->
                binding.btnLogin.isEnabled = true
                binding.btnLogin.text = "Masuk"
                try {
                    val jsonObject = JSONObject(response)
                    if (jsonObject.getString("status") == "success") {
                        val token = jsonObject.getString("token")
                        val dataUser = jsonObject.getJSONObject("data")
                        val nama = dataUser.getString("name")
                        val waNum = dataUser.getString("whatsapp_number")

                        // Simpan sesi login
                        SessionManager(this).createLoginSession(token, nama, waNum)
                        Toast.makeText(this, "Berhasil login!", Toast.LENGTH_SHORT).show()

                        // Pindah ke halaman utama
                        val intent = Intent(this, MasterActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error memproses data server", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                binding.btnLogin.isEnabled = true
                binding.btnLogin.text = "Masuk"
                val networkResponse = error.networkResponse
                if (networkResponse != null && networkResponse.data != null) {
                    try {
                        val errorJson = JSONObject(String(networkResponse.data))
                        Toast.makeText(this, errorJson.getString("message"), Toast.LENGTH_SHORT).show()
                    } catch(e: Exception) {
                        Toast.makeText(this, "Gagal Login", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Gagal konek ke server", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["whatsapp_number"] = wa
                params["password"] = pass
                return params
            }

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["ngrok-skip-browser-warning"] = "69420"
                headers["Accept"] = "application/json"
                val csrfToken = VolleySingleton.getInstance(applicationContext).getCsrfToken()
                if (csrfToken.isNotEmpty()) headers["X-XSRF-TOKEN"] = csrfToken
                return headers
            }
        }
        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest)
    }
}