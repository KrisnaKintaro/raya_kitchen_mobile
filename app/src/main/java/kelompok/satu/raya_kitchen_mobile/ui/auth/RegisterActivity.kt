package kelompok.satu.raya_kitchen_mobile.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import kelompok.satu.raya_kitchen_mobile.databinding.ActivityRegisterBinding
import kelompok.satu.raya_kitchen_mobile.network.api.VolleySingleton
import kelompok.satu.raya_kitchen_mobile.utils.Constant
import org.json.JSONObject

class RegisterActivity : AppCompatActivity() {

    // Siapin variabel binding buat manggil elemen XML tanpa findViewById
    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tembak GET kosong agar Laravel mengirimkan laravel_session dan XSRF-TOKEN
        pancingCsrfCookie()

        // State awal password tersembunyi
        var isPasswordVisible = false
        var isPasswordConfVisible = false

        binding.ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility(binding.etPassword, binding.ivTogglePassword, isPasswordVisible)
        }

        binding.ivTogglePasswordConf.setOnClickListener {
            isPasswordConfVisible = !isPasswordConfVisible
            togglePasswordVisibility(binding.etPasswordConf, binding.ivTogglePasswordConf, isPasswordConfVisible)
        }

        binding.btnRegister.setOnClickListener {
            prosesRegister()
        }

        binding.tvToLogin.setOnClickListener {
            finish()
        }
    }

    private fun togglePasswordVisibility(editText: android.widget.EditText, icon: android.widget.ImageView, isVisible: Boolean) {
        if (isVisible) {
            // Tampilkan teks
            editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            icon.alpha = 1.0f // Bikin icon lebih gelap saat password terlihat
        } else {
            // Sembunyikan teks
            editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            icon.alpha = 0.5f // Bikin icon agak transparan saat password disembunyikan
        }
        // Pindahkan kursor ke ujung teks
        editText.setSelection(editText.text.length)
    }

    private fun pancingCsrfCookie() {
        // Tembak langsung ke halaman utama web Laravel (tanpa /api/)
        val url = Constant.BASE_URL.replace("api/", "")
        val request = object : StringRequest(Method.GET, url,
            { /* Diam saja, CookieManager otomatis menangkap cookie-nya */ },
            { /* Abaikan error */ }) {}

        VolleySingleton.getInstance(this).addToRequestQueue(request)
    }

    private fun prosesRegister() {
        // 1. Tangkap semua inputan yang diketik user
        val name = binding.etName.text.toString().trim()
        val wa = binding.etWhatsapp.text.toString().trim()
        val pass = binding.etPassword.text.toString()
        val passConf = binding.etPasswordConf.text.toString()

        // 2. Validasi Anti-Kosong
        if (name.isEmpty() || wa.isEmpty() || pass.isEmpty() || passConf.isEmpty()) {
            Toast.makeText(this, "Isi dulu semua kolomnya cuy!", Toast.LENGTH_SHORT).show()
            return
        }

        // 3. Validasi Typo Password
        if (pass != passConf) {
            Toast.makeText(this, "Password sama konfirmasi nggak cocok bjir!", Toast.LENGTH_SHORT).show()
            return
        }

        // 4. Ubah status tombol biar user nggak nge-klik 2x (mencegah spam request)
        binding.btnRegister.isEnabled = false
        binding.btnRegister.text = "Memproses..."

        // 5. Sambungin BASE_URL dengan endpoint API Laravel kita
        val url = Constant.BASE_URL + "auth/register"

        // 6. Siapin surat tugas (Request) buat si kurir Volley
        val stringRequest = object : StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                // Kalau Laravel balas dengan HTTP 200/201 (Sukses)
                binding.btnRegister.isEnabled = true
                binding.btnRegister.text = "Daftar Sekarang"

                try {
                    // Bedah paket JSON dari Laravel
                    val jsonObject = JSONObject(response)
                    if (jsonObject.getString("status") == "success") {
                        Toast.makeText(this@RegisterActivity, "Sukses! Cek WA lu buat nengok kode OTP", Toast.LENGTH_SHORT).show()

                        val formattedWa = jsonObject.getString("whatsapp_number")

                        // Pindah ke halaman OTP, dan bawa nomor WA yang udah di-format
                        val intent = Intent(this@RegisterActivity, OtpActivity::class.java)
                        intent.putExtra("EXTRA_WA", formattedWa) // Pake yang diformat!
                        startActivity(intent)

                        finish()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@RegisterActivity, "Gagal membedah respon server", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                // Kalau Laravel balas dengan Error (Misal: 422 Nomor WA udah kepake)
                binding.btnRegister.isEnabled = true
                binding.btnRegister.text = "Daftar Sekarang"

                // Coba tangkap pesan error spesifik yang dilempar dari Controller Laravel lu
                val networkResponse = error.networkResponse
                if (networkResponse != null && networkResponse.data != null) {
                    try {
                        val errorJson = JSONObject(String(networkResponse.data))
                        val pesanError = errorJson.getString("message")
                        Toast.makeText(this, pesanError, Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Gagal daftar. Server lagi ngambek.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Gagal konek ke server. Cek internet lu.", Toast.LENGTH_SHORT).show()
                }
            }) {

            // 7. Masukin data yang mau dikirim ke dalam body request (POST)
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["name"] = name
                params["whatsapp_number"] = wa
                params["password"] = pass
                params["password_confirmation"] = passConf
                return params
            }

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["ngrok-skip-browser-warning"] = "69420"
                headers["Accept"] = "application/json"

                // Panggil token dari VolleySingleton
                val csrfToken = VolleySingleton.getInstance(applicationContext).getCsrfToken()
                if (csrfToken.isNotEmpty()) {
                    headers["X-XSRF-TOKEN"] = csrfToken
                }
                return headers
            }
        }

        // 9. Kasih surat tugasnya ke kurir utama (VolleySingleton) biar langsung diantar ke server
        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest)
    }
}