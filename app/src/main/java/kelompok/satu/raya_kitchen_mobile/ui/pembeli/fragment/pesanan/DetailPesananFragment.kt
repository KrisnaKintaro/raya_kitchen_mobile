package kelompok.satu.raya_kitchen_mobile.ui.pembeli.fragment.pesanan

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import kelompok.satu.raya_kitchen_mobile.databinding.FragmentDetailPesananBinding
import kelompok.satu.raya_kitchen_mobile.model.OrderItem
import kelompok.satu.raya_kitchen_mobile.ui.adapter.pembeli.pesanan.DetailPesananAdapter
import kelompok.satu.raya_kitchen_mobile.utils.FormatHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetailPesananFragment : Fragment() {

    private var _binding: FragmentDetailPesananBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: DetailPesananAdapter
    private var orderId = ""
    private var totalTagihan = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailPesananBinding.inflate(inflater, container, false)

        // 1. Terima data dari RiwayatPesanan (PAKAI PENGAMAN ?:)
        arguments?.let {
            orderId = it.getString("EXTRA_ID") ?: ""
            val status = it.getString("EXTRA_STATUS") ?: ""
            val orderNumber = it.getString("EXTRA_ORDER_NUM") ?: ""
            val recipientName = it.getString("EXTRA_NAME") ?: ""
            val address = it.getString("EXTRA_ADDRESS") ?: ""
            val total = it.getInt("EXTRA_TOTAL", 0)
            val createdAt = it.getLong("EXTRA_DATE", 0L)
            totalTagihan = it.getInt("EXTRA_TOTAL", 0)

            // 2. Tampilkan Info Dasar ke Layar
            binding.tvDetailOrderId.text = orderNumber
            binding.tvDetailStatus.text = status
            binding.tvDetailNamaWa.text = recipientName
            binding.tvDetailAlamatLengkap.text = address
            binding.tvDetailTotal.text = FormatHelper.formatRupiah(total)
            binding.btnUploadUlang.visibility = View.GONE

            // Format tanggal pakai locale default HP biar ga crash
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            binding.tvDetailTanggal.text = sdf.format(Date(createdAt))

            // Set Warna Status
            when (status) {
                "Menunggu Pembayaran", "Pembayaran Ditolak" -> {
                    binding.tvDetailStatus.setTextColor(Color.parseColor("#EF4444")) // Merah
                    if (status == "Pembayaran Ditolak") {
                        binding.btnUploadUlang.visibility = View.VISIBLE // Munculkan tombol
                    }
                }
                "Menunggu Verifikasi" -> binding.tvDetailStatus.setTextColor(Color.parseColor("#8B5CF6"))
                "Diproses", "Dipanggang" -> binding.tvDetailStatus.setTextColor(Color.parseColor("#F59E0B"))
                "Dikirim" -> binding.tvDetailStatus.setTextColor(Color.parseColor("#3B82F6"))
                "Selesai" -> binding.tvDetailStatus.setTextColor(Color.parseColor("#10B981"))
                else -> binding.tvDetailStatus.setTextColor(Color.GRAY)
            }
        }

        binding.btnUploadUlang.setOnClickListener {
            val transId = orderId.replace("ORD", "TRX")

            val payFragment = kelompok.satu.raya_kitchen_mobile.ui.pembeli.fragment.checkout.PembayaranFragment().apply {
                arguments = Bundle().apply {
                    putString("EXTRA_TRANS_ID", transId)
                    putString("EXTRA_ORDER_ID", orderId)
                    putInt("EXTRA_TOTAL", totalTagihan)
                }
            }

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(kelompok.satu.raya_kitchen_mobile.R.id.fragment_container, payFragment)
                .addToBackStack(null)
                .commit()
        }

        binding.btnBackDetail.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        setupRecyclerView()
        loadOrderItems()

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = DetailPesananAdapter(emptyList())
        binding.rvDetailItem.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDetailItem.adapter = adapter
    }

    private fun loadOrderItems() {
        if (orderId.isEmpty()) return

        db.collection("order_items")
            .whereEqualTo("order_id", orderId)
            .get()
            .addOnSuccessListener { documents ->
                val items = mutableListOf<OrderItem>()
                for (doc in documents) {
                    val item = doc.toObject(OrderItem::class.java)
                    item.id = doc.id
                    items.add(item)
                }
                adapter.updateData(items)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal memuat rincian roti", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}