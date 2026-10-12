package kelompok.satu.raya_kitchen_mobile.ui.pembeli

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kelompok.satu.raya_kitchen_mobile.R
import kelompok.satu.raya_kitchen_mobile.databinding.ActivityMasterBinding
import kelompok.satu.raya_kitchen_mobile.ui.pembeli.fragment.katalog.KatalogFragment
import kelompok.satu.raya_kitchen_mobile.ui.pembeli.fragment.keranjang.KeranjangFragment
import kelompok.satu.raya_kitchen_mobile.ui.pembeli.fragment.pesanan.RiwayatPesananFragment
import kelompok.satu.raya_kitchen_mobile.ui.pembeli.fragment.profil.ProfilFragment
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.app.AlertDialog
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import kelompok.satu.raya_kitchen_mobile.databinding.DialogPromoBinding
import android.view.View

class MasterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMasterBinding

    private val promoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val title = intent?.getStringExtra("promo_title") ?: "Promo Spesial!"
            val body = intent?.getStringExtra("promo_body") ?: "Dapatkan diskon menarik hari ini."
            val imageUrl = intent?.getStringExtra("promo_image")

            tampilkanModalPromo(title, body, imageUrl)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMasterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tampilkan KatalogFragment saat Activity pertama kali dibuka
        if (savedInstanceState == null) {
            loadFragment(KatalogFragment())
        }

        binding.btnCartTop.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, KeranjangFragment())
                .addToBackStack(null) // Biar bisa pencet tombol Back untuk kembali ke Katalog
                .commit()
        }

        // Logic perpindahan antar Fragment lewat Bottom Navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->

            if (binding.bottomNavigation.selectedItemId == item.itemId) {
                return@setOnItemSelectedListener false
            }

            when (item.itemId) {
                R.id.nav_katalog -> {
                    loadFragment(KatalogFragment())
                    true
                }
                R.id.nav_pesanan -> {
                    loadFragment(RiwayatPesananFragment())
                    true
                }
                R.id.nav_profil -> {
                    loadFragment(ProfilFragment())
                    true
                }
                else -> false
            }
        }

        // Minta Izin Notifikasi (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val tokenFcm = task.result
                val userWa = kelompok.satu.raya_kitchen_mobile.utils.SessionManager(this).getUserWa()

                if (userWa.isNotEmpty()) {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    // Simpan ke koleksi "users", jadikan nomor WA sebagai ID Dokumen
                    db.collection("users").document(userWa).set(
                        mapOf(
                            "whatsapp" to userWa,
                            "fcm_token" to tokenFcm
                        ),
                        com.google.firebase.firestore.SetOptions.merge() // Merge biar nggak nimpa data lain kalau ada
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Aktifkan penangkap sinyal promo pas aplikasi dibuka
        LocalBroadcastManager.getInstance(this).registerReceiver(
            promoReceiver, IntentFilter("EVENT_PROMO_NOTIF")
        )
    }

    override fun onStop() {
        super.onStop()
        // Matikan penangkap sinyal pas aplikasi diminimize
        LocalBroadcastManager.getInstance(this).unregisterReceiver(promoReceiver)
    }

    private fun tampilkanModalPromo(title: String, body: String, imageUrl: String?) {
        val dialogBinding = DialogPromoBinding.inflate(layoutInflater)

        dialogBinding.tvPromoTitle.text = title
        dialogBinding.tvPromoBody.text = body

        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this).load(imageUrl).into(dialogBinding.ivPromoGambar)
            dialogBinding.ivPromoGambar.visibility = View.VISIBLE
        } else {
            dialogBinding.ivPromoGambar.visibility = View.GONE
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root) // Set view pakai root dari binding
            .setCancelable(true)
            .create()

        // Bikin background dialog transparan biar round corner dari XML kelihatan
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        dialogBinding.btnTutupPromo.setOnClickListener {
            dialog.dismiss()
        }
    }

    // Fungsi helper buat gonta-ganti isi <FragmentContainerView>
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}