package kelompok.satu.raya_kitchen_mobile.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import kelompok.satu.raya_kitchen_mobile.databinding.ActivityOtpBinding
import kelompok.satu.raya_kitchen_mobile.network.api.VolleySingleton
import kelompok.satu.raya_kitchen_mobile.ui.pembeli.MasterActivity
import kelompok.satu.raya_kitchen_mobile.utils.Constant
import kelompok.satu.raya_kitchen_mobile.utils.SessionManager
import org.json.JSONObject

class OtpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOtpBinding
    private var waNumber = ""
    private var countDownTimer: android.os.CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        waNumber = intent.getStringExtra("EXTRA_WA") ?: ""
        binding.tvInfoWa.text = "Masukkan kode OTP yang dikirim ke\n$waNumber"

        setupOtpInputs()
        startResendTimer()

        binding.btnVerify.setOnClickListener {
            val otpCode = getOtpCode()
            if (otpCode.length == 6) {
                prosesVerifikasi(otpCode)
            } else {
                Toast.makeText(this, "Kode OTP harus diisi semua (6 digit)!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvResend.setOnClickListener {
            reqResendOtp()
        }
    }

    private fun getOtpCode(): String {
        return binding.otp1.text.toString() +
                binding.otp2.text.toString() +
                binding.otp3.text.toString() +
                binding.otp4.text.toString() +
                binding.otp5.text.toString() +
                binding.otp6.text.toString()
    }

    // Fungsi buat ngatur kursor pindah otomatis
    private fun setupOtpInputs() {
        val otps = listOf(
            binding.otp1, binding.otp2, binding.otp3,
            binding.otp4, binding.otp5, binding.otp6
        )

        for (i in otps.indices) {
            otps[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    // Kalau diisi 1 angka, otomatis lompat ke kotak kanannya
                    if (s?.length == 1 && i < otps.size - 1) {
                        otps[i + 1].requestFocus()
                    }
                }
            })

            // Logic khusus buat tombol Backspace/Delete (mundur ke kotak kiri)
            otps[i].setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (otps[i].text.isEmpty() && i > 0) {
                        otps[i - 1].requestFocus()
                        otps[i - 1].text.clear()
                    }
                }
                false
            }
        }
    }

    private fun reqResendOtp() {
        binding.tvResend.isEnabled = false
        binding.tvResend.text = "Mengirim..."

        val url = Constant.BASE_URL + "auth/resend-otp"
        val stringRequest = object : StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                try {
                    val jsonObject = JSONObject(response)
                    if (jsonObject.getString("status") == "success") {
                        Toast.makeText(this, jsonObject.getString("message"), Toast.LENGTH_SHORT).show()
                        startResendTimer() // Mulai hitung mundur 60 detik
                    }
                } catch (e: Exception) {
                    binding.tvResend.isEnabled = true
                    binding.tvResend.text = "Kirim Ulang"
                }
            },
            Response.ErrorListener { error ->
                binding.tvResend.isEnabled = true
                binding.tvResend.text = "Kirim Ulang"

                val networkResponse = error.networkResponse
                if (networkResponse != null && networkResponse.data != null) {
                    try {
                        val errorJson = JSONObject(String(networkResponse.data))
                        Toast.makeText(this, errorJson.getString("message"), Toast.LENGTH_LONG).show()

                        // Jika sesi habis (400), tendang kembali ke Register
                        if (networkResponse.statusCode == 400) {
                            val intent = Intent(this, RegisterActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                        // Jika kena cooldown API (429), paksa jalankan timer di UI
                        else if (networkResponse.statusCode == 429) {
                            startResendTimer()
                        }
                    } catch (e: Exception) {}
                }
            }) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["whatsapp_number"] = waNumber
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

    private fun startResendTimer() {
        binding.tvResend.isEnabled = false
        // Ubah warna jadi abu-abu biar kelihatan nonaktif
        binding.tvResend.setTextColor(android.graphics.Color.parseColor("#A1A09A"))

        countDownTimer = object : android.os.CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.tvResend.text = "Tunggu ${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                binding.tvResend.isEnabled = true
                binding.tvResend.text = "Kirim Ulang"
                // Kembalikan ke warna ungu primer
                binding.tvResend.setTextColor(resources.getColor(kelompok.satu.raya_kitchen_mobile.R.color.buyer_primary))
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cegah memory leak jika user keluar halaman saat timer masih jalan
        countDownTimer?.cancel()
    }

    private fun prosesVerifikasi(otpCode: String) {
        // Kunci tombol biar nggak di-spam klik
        binding.btnVerify.isEnabled = false
        binding.btnVerify.text = "Memverifikasi..."

        val url = Constant.BASE_URL + "auth/verify-otp"

        val stringRequest = object : StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                binding.btnVerify.isEnabled = true
                binding.btnVerify.text = "Verifikasi"

                try {
                    val jsonObject = JSONObject(response)
                    if (jsonObject.getString("status") == "success") {
                        Toast.makeText(this, "Verifikasi Sukses!", Toast.LENGTH_SHORT).show()

                        // 1. Ambil data dari JSON kembalian Laravel
                        val token = jsonObject.getString("token")
                        val dataUser = jsonObject.getJSONObject("data")
                        val nama = dataUser.getString("name")
                        val wa = dataUser.getString("whatsapp_number")

                        // 2. Simpan Sesi (Token & Data User) pakai SessionManager
                        val sessionManager = SessionManager(this)
                        sessionManager.createLoginSession(token, nama, wa)

                        // 3. Lempar ke MasterActivity (Halaman Utama)
                        val intent = Intent(this, MasterActivity::class.java)
                        // Bersihin history halaman biar user gak bisa mencet "Back" ke halaman OTP lagi
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, jsonObject.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Terjadi kesalahan parsing data", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                binding.btnVerify.isEnabled = true
                binding.btnVerify.text = "Verifikasi"

                val networkResponse = error.networkResponse
                if (networkResponse != null && networkResponse.data != null) {
                    try {
                        val errorJson = JSONObject(String(networkResponse.data))
                        Toast.makeText(this, errorJson.getString("message"), Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "OTP Salah atau Kadaluarsa", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Gagal konek ke server", Toast.LENGTH_SHORT).show()
                }
            }) {

            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["whatsapp_number"] = waNumber
                params["otp"] = otpCode
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

        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest)
    }
}