package kelompok.satu.raya_kitchen_mobile.ui.pembeli.fragment.profil

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import kelompok.satu.raya_kitchen_mobile.databinding.FragmentProfilBinding
import kelompok.satu.raya_kitchen_mobile.network.api.VolleySingleton
import kelompok.satu.raya_kitchen_mobile.ui.auth.LoginActivity
import kelompok.satu.raya_kitchen_mobile.ui.pembeli.fragment.pesanan.RiwayatPesananFragment
import kelompok.satu.raya_kitchen_mobile.utils.Constant
import kelompok.satu.raya_kitchen_mobile.utils.SessionManager

class ProfilFragment : Fragment() {

    private var _binding: FragmentProfilBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilBinding.inflate(inflater, container, false)

        val sessionManager = SessionManager(requireContext())

        // 1. Tampilkan Data User
        binding.tvNamaProfil.text = sessionManager.getUserName()
        binding.tvWaProfil.text = sessionManager.getUserWa()

        binding.menuAlamat.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(kelompok.satu.raya_kitchen_mobile.R.id.fragment_container, DaftarAlamatFragment())
                .addToBackStack(null)
                .commit()
        }

        // 3. Aksi Klik Menu Riwayat Pesanan
        binding.menuPesanan.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(kelompok.satu.raya_kitchen_mobile.R.id.fragment_container,
                    RiwayatPesananFragment()
                )
                .addToBackStack(null)
                .commit()
        }

        // 4. Aksi Tombol Logout
        binding.btnLogout.setOnClickListener {
            binding.btnLogout.isEnabled = false
            binding.btnLogout.text = "Proses Keluar..."

            val token = sessionManager.getToken() ?: ""
            val url = Constant.BASE_URL + "auth/logout"

            val stringRequest = object : StringRequest(Method.GET, url,
                Response.Listener { response ->
                    sessionManager.clearSession()
                    Toast.makeText(requireContext(), "Berhasil Logout!", Toast.LENGTH_SHORT).show()
                    navigateToLogin()
                },
                Response.ErrorListener { error ->
                    // Tetap paksa logout di aplikasi meskipun API gagal/timeout
                    sessionManager.clearSession()
                    navigateToLogin()
                }
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers["ngrok-skip-browser-warning"] = "69420"
                    headers["Accept"] = "application/json"
                    headers["Authorization"] = "Bearer $token"

                    val csrfToken = VolleySingleton.getInstance(requireContext()).getCsrfToken()
                    if (csrfToken.isNotEmpty()) headers["X-XSRF-TOKEN"] = csrfToken

                    return headers
                }
            }

            VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
        }

        return binding.root
    }

    private fun navigateToLogin() {
        val intent = Intent(requireActivity(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}