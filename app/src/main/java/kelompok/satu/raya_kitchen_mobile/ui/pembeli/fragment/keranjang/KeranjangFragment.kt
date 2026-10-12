package kelompok.satu.raya_kitchen_mobile.ui.pembeli.fragment.keranjang

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kelompok.satu.raya_kitchen_mobile.databinding.FragmentKeranjangBinding
import kelompok.satu.raya_kitchen_mobile.model.CartItem
import kelompok.satu.raya_kitchen_mobile.ui.adapter.pembeli.keranjang.KeranjangAdapter
import kelompok.satu.raya_kitchen_mobile.utils.FormatHelper
import kelompok.satu.raya_kitchen_mobile.utils.SessionManager

class KeranjangFragment : Fragment() {

    private var _binding: FragmentKeranjangBinding? = null
    private val binding get() = _binding!!

    private lateinit var keranjangAdapter: KeranjangAdapter
    private var cartItems = mutableListOf<CartItem>()
    private val db = FirebaseFirestore.getInstance()
    private var firestoreListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKeranjangBinding.inflate(inflater, container, false)

        setupRecyclerView()
        loadRealtimeCart()

        binding.btnCheckout.setOnClickListener {
            if (cartItems.isEmpty()) {
                Toast.makeText(requireContext(), "Keranjang kosong cuy!", Toast.LENGTH_SHORT).show()
            } else {
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(kelompok.satu.raya_kitchen_mobile.R.id.fragment_container, kelompok.satu.raya_kitchen_mobile.ui.pembeli.fragment.checkout.CheckoutFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }

        return binding.root
    }

    private fun setupRecyclerView() {
        keranjangAdapter = KeranjangAdapter(cartItems,
            onUpdateQty = { item, newQty ->
                // Update ke Firestore
                db.collection("carts").document(item.id)
                    .update("quantity", newQty)
            },
            onDelete = { item ->
                // Hapus dari Firestore
                db.collection("carts").document(item.id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Dihapus dari keranjang", Toast.LENGTH_SHORT).show()
                    }
            }
        )
        binding.rvKeranjang.layoutManager = LinearLayoutManager(requireContext())
        binding.rvKeranjang.adapter = keranjangAdapter
    }

    private fun loadRealtimeCart() {
        val userWa = SessionManager(requireContext()).getUserWa()

        // Pasang pendengar (listener) yang bereaksi otomatis kalau data di server berubah
        firestoreListener = db.collection("carts")
            .whereEqualTo("user_wa", userWa)
            .addSnapshotListener { snapshots, error ->

                if (_binding == null) return@addSnapshotListener
                if (error != null) {
                    Toast.makeText(requireContext(), "Gagal memuat keranjang", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                cartItems.clear()
                var totalHarga = 0

                for (doc in snapshots!!) {
                    val item = doc.toObject(CartItem::class.java)
                    item.id = doc.id // Masukkan ID Dokumen
                    cartItems.add(item)
                    totalHarga += (item.price * item.quantity)
                }

                // Perbarui Adapter & Teks Total
                keranjangAdapter.updateData(cartItems)
                binding.tvTotalHarga.text = FormatHelper.formatRupiah(totalHarga)

                // Tampilkan atau sembunyikan gambar kosong
                if (cartItems.isEmpty()) {
                    binding.tvKeranjangKosong.visibility = View.VISIBLE
                    binding.rvKeranjang.visibility = View.GONE
                } else {
                    binding.tvKeranjangKosong.visibility = View.GONE
                    binding.rvKeranjang.visibility = View.VISIBLE
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Matikan pendengar kalau keluar layar biar hemat RAM
        firestoreListener?.remove()
        _binding = null
    }
}