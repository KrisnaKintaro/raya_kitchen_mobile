package kelompok.satu.raya_kitchen_mobile.ui.pembeli.fragment.profil

import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kelompok.satu.raya_kitchen_mobile.databinding.FragmentTambahAlamatBinding
import kelompok.satu.raya_kitchen_mobile.model.Address
import kelompok.satu.raya_kitchen_mobile.utils.SessionManager
import mumayank.com.airlocationlibrary.AirLocation
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import java.util.Locale

class TambahAlamatFragment : Fragment() {

    private var _binding: FragmentTambahAlamatBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    // Variabel penanda: Kalau kosong berarti nambah baru, kalau ada isinya berarti mode edit
    private var editAddressId = ""
    private var airLocation: AirLocation? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Configuration.getInstance().userAgentValue = requireContext().packageName

        _binding = FragmentTambahAlamatBinding.inflate(inflater, container, false)

        setupMap()
        ambilGpsOtomatis()

        binding.btnAmbilLokasi.setOnClickListener {
            konversiTitikKeAlamat()
        }

        // Cek apakah ada data yang dilempar untuk di-edit
        arguments?.let {
            editAddressId = it.getString("EXTRA_ID", "")
            if (editAddressId.isNotEmpty()) {
                binding.btnSimpanAlamat.text = "Simpan Perubahan"
                binding.etLabel.setText(it.getString("EXTRA_LABEL"))
                binding.etNamaPenerima.setText(it.getString("EXTRA_NAME"))
                binding.etWaPenerima.setText(it.getString("EXTRA_WA"))
                binding.etProvinsi.setText(it.getString("EXTRA_PROVINCE"))
                binding.etKota.setText(it.getString("EXTRA_CITY"))
                binding.etKecamatan.setText(it.getString("EXTRA_DISTRICT"))
                binding.etKodePos.setText(it.getString("EXTRA_POSTAL"))
                binding.etJalan.setText(it.getString("EXTRA_STREET"))
                binding.etPatokan.setText(it.getString("EXTRA_LANDMARK"))
                binding.switchUtama.isChecked = it.getBoolean("EXTRA_IS_PRIMARY")
            }
        }

        binding.btnBackAddAlamat.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        binding.btnSimpanAlamat.setOnClickListener {
            prosesSimpanAlamat()
        }

        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        airLocation?.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        airLocation?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // OSMDroid butuh di-detach saat Fragment hancur
        binding.mapView.onDetach()
        _binding = null
    }

    private fun setupMap() {
        binding.mapView.setMultiTouchControls(true)
        val mapController = binding.mapView.controller
        mapController.setZoom(18.0) // Level zoom layaknya Google Maps

        // Default lokasi awal (misal: Jakarta / Kediri) sebelum GPS dapat
        val defaultPoint = GeoPoint(-7.8480, 112.0178) // Ganti koordinat kota lu
        mapController.setCenter(defaultPoint)
    }

    private fun ambilGpsOtomatis() {
        airLocation = AirLocation(requireActivity(), object : AirLocation.Callback {
            override fun onSuccess(locations: ArrayList<Location>) {
                // Pindahkan peta ke titik GPS User
                val lat = locations.first().latitude
                val lon = locations.first().longitude
                val point = GeoPoint(lat, lon)

                binding.mapView.controller.animateTo(point)
                Toast.makeText(requireContext(), "Lokasi ditemukan!", Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(locationFailedEnum: AirLocation.LocationFailedEnum) {
                Toast.makeText(requireContext(), "Gagal mencari titik GPS. Geser peta manual aja.", Toast.LENGTH_SHORT).show()
            }
        }, true) // true = cukup 1 kali deteksi saja
    }

    private fun konversiTitikKeAlamat() {
        binding.btnAmbilLokasi.text = "Mencari..."
        binding.btnAmbilLokasi.isEnabled = false

        val center = binding.mapView.mapCenter
        val lat = center.latitude
        val lon = center.longitude

        try {
            val geocoder = Geocoder(requireContext(), Locale("id", "ID"))
            val addresses = geocoder.getFromLocation(lat, lon, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]

                // Isi otomatis ke dalam EditText
                binding.etProvinsi.setText(address.adminArea ?: "")
                binding.etKota.setText(address.subAdminArea ?: address.locality ?: "")
                binding.etKecamatan.setText(address.locality ?: address.subLocality ?: "")
                binding.etKodePos.setText(address.postalCode ?: "")

                var jalan = address.thoroughfare ?: address.featureName ?: ""

                // Kalau jalannya ternyata Plus Code (mengandung tanda '+'), kosongin aja!
                if (jalan.contains("+")) {
                    jalan = ""
                }

                binding.etJalan.setText(jalan)

                Toast.makeText(requireContext(), "Berhasil mengisi alamat!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Alamat tidak dikenali, silakan ketik manual", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Layanan peta tidak tersedia di HP ini", Toast.LENGTH_SHORT).show()
        } finally {
            binding.btnAmbilLokasi.text = "Gunakan Lokasi Ini"
            binding.btnAmbilLokasi.isEnabled = true
        }
    }

    private fun prosesSimpanAlamat() {
        val label = binding.etLabel.text.toString().trim()
        val nama = binding.etNamaPenerima.text.toString().trim()
        val wa = binding.etWaPenerima.text.toString().trim()
        val provinsi = binding.etProvinsi.text.toString().trim()
        val kota = binding.etKota.text.toString().trim()
        val kecamatan = binding.etKecamatan.text.toString().trim()
        val kodePos = binding.etKodePos.text.toString().trim()
        val jalan = binding.etJalan.text.toString().trim()
        val isPrimary = binding.switchUtama.isChecked

        // Validasi Anti Kosong
        if (label.isEmpty() || nama.isEmpty() || wa.isEmpty() || provinsi.isEmpty() || kota.isEmpty() || kecamatan.isEmpty() || jalan.isEmpty()) {
            Toast.makeText(requireContext(), "Lengkapi data wajib dulu cuy!", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSimpanAlamat.isEnabled = false
        binding.btnSimpanAlamat.text = "Menyimpan..."

        val userWa = SessionManager(requireContext()).getUserWa()

        if (editAddressId.isNotEmpty()) {
            // ================= MODE EDIT =================
            val addressRef = db.collection("addresses").document(editAddressId)
            val updatedAddress = Address(
                id = editAddressId, user_wa = userWa, label = label,
                recipient_name = nama, recipient_whatsapp = wa,
                province = provinsi, city = kota, district = kecamatan,
                postal_code = kodePos, street_address = jalan,
                landmark = binding.etPatokan.text.toString().trim(),
                is_primary = isPrimary
            )
            eksekusiSimpanKeFirestore(addressRef, updatedAddress, isPrimary, userWa)

        } else {
            // ================= MODE TAMBAH BARU =================
            db.collection("addresses")
                .whereEqualTo("user_wa", userWa)
                .get()
                .addOnSuccessListener { documents ->
                    val countAlamatBaru = documents.size() + 1
                    val customDocId = "${userWa}_alamat_ke_${countAlamatBaru}"

                    val addressRef = db.collection("addresses").document(customDocId)
                    val newAddress = Address(
                        id = customDocId, user_wa = userWa, label = label,
                        recipient_name = nama, recipient_whatsapp = wa,
                        province = provinsi, city = kota, district = kecamatan,
                        postal_code = kodePos, street_address = jalan,
                        landmark = binding.etPatokan.text.toString().trim(),
                        is_primary = isPrimary
                    )
                    eksekusiSimpanKeFirestore(addressRef, newAddress, isPrimary, userWa)
                }.addOnFailureListener {
                    tombolGagal()
                }
        }
    }

    private fun eksekusiSimpanKeFirestore(addressRef: DocumentReference, data: Address, isPrimary: Boolean, userWa: String) {
        if (isPrimary) {
            // Kalau dicentang utama, matikan alamat utama yang lain dulu pakai Batch
            db.collection("addresses")
                .whereEqualTo("user_wa", userWa)
                .whereEqualTo("is_primary", true)
                .get()
                .addOnSuccessListener { documents ->
                    val batch = db.batch()
                    for (doc in documents) {
                        // Pastikan jangan mematikan dokumen yang sedang kita edit sendiri
                        if (doc.id != addressRef.id) {
                            batch.update(doc.reference, "is_primary", false)
                        }
                    }
                    batch.set(addressRef, data) // Simpan data baru/editannya

                    batch.commit().addOnSuccessListener { suksesSimpan() }.addOnFailureListener { tombolGagal() }
                }
        } else {
            // Kalau nggak dicentang utama, langsung timpa/simpan datanya
            addressRef.set(data).addOnSuccessListener { suksesSimpan() }.addOnFailureListener { tombolGagal() }
        }
    }

    private fun suksesSimpan() {
        Toast.makeText(requireContext(), "Sip, alamat berhasil disimpan!", Toast.LENGTH_SHORT).show()
        requireActivity().supportFragmentManager.popBackStack()
    }

    private fun tombolGagal() {
        binding.btnSimpanAlamat.isEnabled = true
        binding.btnSimpanAlamat.text = if (editAddressId.isNotEmpty()) "Simpan Perubahan" else "Simpan Alamat"
        Toast.makeText(requireContext(), "Gagal menyimpan ke server", Toast.LENGTH_SHORT).show()
    }
}