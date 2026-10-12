package kelompok.satu.raya_kitchen_mobile.ui.pembeli.fragment.profil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kelompok.satu.raya_kitchen_mobile.databinding.FragmentDaftarAlamatBinding
import kelompok.satu.raya_kitchen_mobile.model.Address
import kelompok.satu.raya_kitchen_mobile.ui.adapter.pembeli.profil.AlamatAdapter
import kelompok.satu.raya_kitchen_mobile.utils.SessionManager

class DaftarAlamatFragment : Fragment() {

    private var _binding: FragmentDaftarAlamatBinding? = null
    private val binding get() = _binding!!

    private lateinit var alamatAdapter: AlamatAdapter
    private var addressList = mutableListOf<Address>()
    private val db = FirebaseFirestore.getInstance()
    private var firestoreListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDaftarAlamatBinding.inflate(inflater, container, false)

        setupRecyclerView()
        loadRealtimeAddresses()

        // Tombol Back
        binding.btnBackAlamat.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Tombol Tambah Alamat
        binding.btnTambahAlamat.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(kelompok.satu.raya_kitchen_mobile.R.id.fragment_container, TambahAlamatFragment())
                .addToBackStack(null)
                .commit()
        }

        return binding.root
    }

    private fun setupRecyclerView() {
        alamatAdapter = AlamatAdapter(addressList,
            onSetPrimaryClick = { selectedAddress ->
                setPrimaryAddress(selectedAddress)
            },
            onEditClick = { selectedAddress ->
                // Lempar data alamat lama ke form menggunakan Bundle
                val formFragment = TambahAlamatFragment().apply {
                    arguments = Bundle().apply {
                        putString("EXTRA_ID", selectedAddress.id)
                        putString("EXTRA_LABEL", selectedAddress.label)
                        putString("EXTRA_NAME", selectedAddress.recipient_name)
                        putString("EXTRA_WA", selectedAddress.recipient_whatsapp)
                        putString("EXTRA_PROVINCE", selectedAddress.province)
                        putString("EXTRA_CITY", selectedAddress.city)
                        putString("EXTRA_DISTRICT", selectedAddress.district)
                        putString("EXTRA_POSTAL", selectedAddress.postal_code)
                        putString("EXTRA_STREET", selectedAddress.street_address)
                        putString("EXTRA_LANDMARK", selectedAddress.landmark)
                        putBoolean("EXTRA_IS_PRIMARY", selectedAddress.is_primary)
                    }
                }

                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(kelompok.satu.raya_kitchen_mobile.R.id.fragment_container, formFragment)
                    .addToBackStack(null)
                    .commit()
            },
            onDeleteClick = { selectedAddress ->
                // Munculkan dialog konfirmasi sebelum menghapus
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Hapus Alamat")
                    .setMessage("Yakin mau menghapus alamat '${selectedAddress.label}'?")
                    .setPositiveButton("Hapus") { _, _ ->
                        db.collection("addresses").document(selectedAddress.id)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Alamat berhasil dihapus", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        )
        binding.rvAlamat.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.rvAlamat.adapter = alamatAdapter
    }

    private fun loadRealtimeAddresses() {
        val userWa = SessionManager(requireContext()).getUserWa()

        firestoreListener = db.collection("addresses")
            .whereEqualTo("user_wa", userWa)
            .addSnapshotListener { snapshots, error ->
                if (_binding == null) return@addSnapshotListener
                if (error != null) {
                    Toast.makeText(requireContext(), "Gagal memuat alamat", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                addressList.clear()
                for (doc in snapshots!!) {
                    val address = doc.toObject(Address::class.java)
                    address.id = doc.id

                    // JURUS AMPUH: Paksa baca manual dari Firestore ke memori aplikasi
                    address.is_primary = doc.getBoolean("is_primary") ?: false

                    addressList.add(address)
                }

                // Urutkan list: yang is_primary = true wajib di atas!
                addressList.sortByDescending { it.is_primary }

                alamatAdapter.updateData(addressList)
            }
    }

    private fun setPrimaryAddress(selectedAddress: Address) {
        val batch = db.batch()

        for (address in addressList) {
            if (address.id != selectedAddress.id) {
                val refLain = db.collection("addresses").document(address.id)
                batch.update(refLain, "is_primary", false)
            }
        }

        val refBaru = db.collection("addresses").document(selectedAddress.id)
        batch.update(refBaru, "is_primary", true)

        batch.commit().addOnSuccessListener {
            Toast.makeText(requireContext(), "Alamat utama dipindah ke ${selectedAddress.label}", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Gagal mengubah alamat utama", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        firestoreListener?.remove()
        _binding = null
    }
}