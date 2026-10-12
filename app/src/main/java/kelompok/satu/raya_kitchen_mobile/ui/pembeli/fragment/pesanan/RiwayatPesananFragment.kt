package kelompok.satu.raya_kitchen_mobile.ui.pembeli.fragment.pesanan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kelompok.satu.raya_kitchen_mobile.databinding.FragmentRiwayatPesananBinding
import kelompok.satu.raya_kitchen_mobile.model.Order
import kelompok.satu.raya_kitchen_mobile.ui.adapter.pembeli.pesanan.RiwayatPesananAdapter
import kelompok.satu.raya_kitchen_mobile.utils.SessionManager

class RiwayatPesananFragment : Fragment() {

    private var _binding: FragmentRiwayatPesananBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: RiwayatPesananAdapter

    // Master data = semua data asli dari database
    private var masterOrderList = mutableListOf<Order>()
    private var firestoreListener: ListenerRegistration? = null
    private var currentFilter = "Semua" // Filter default

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiwayatPesananBinding.inflate(inflater, container, false)

        setupRecyclerView()
        setupFilters()
        loadRealtimeOrders()

        binding.swipeRefresh.setOnRefreshListener {
            loadRealtimeOrders()
        }

        binding.btnBackRiwayat.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = RiwayatPesananAdapter(emptyList()) { order ->
            val detailFrag = DetailPesananFragment().apply {
                arguments = Bundle().apply {
                    putString("EXTRA_ID", order.id)
                    putString("EXTRA_STATUS", order.status)
                    putString("EXTRA_ORDER_NUM", order.order_number)
                    putString("EXTRA_NAME", "${order.recipient_name} (${order.recipient_whatsapp ?: ""})")
                    putString("EXTRA_ADDRESS", order.shipping_address)
                    putInt("EXTRA_TOTAL", order.total_amount)
                    putLong("EXTRA_DATE", order.created_at)
                }
            }

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(kelompok.satu.raya_kitchen_mobile.R.id.fragment_container, detailFrag)
                .addToBackStack(null)
                .commit()
        }
        binding.rvRiwayatPesanan.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRiwayatPesanan.adapter = adapter
    }

    private fun loadRealtimeOrders() {
        firestoreListener?.remove()

        binding.swipeRefresh.isRefreshing = true

        val userWa = SessionManager(requireContext()).getUserWa()

        firestoreListener = db.collection("orders")
            .whereEqualTo("user_wa", userWa)
            .addSnapshotListener { snapshots, error ->
                if (_binding == null) return@addSnapshotListener
                binding.swipeRefresh.isRefreshing = false

                if (error != null) {
                    Toast.makeText(requireContext(), "Gagal memuat pesanan", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                masterOrderList.clear()

                if (snapshots != null && !snapshots.isEmpty) {
                    for (doc in snapshots) {
                        val order = doc.toObject(Order::class.java)
                        order.id = doc.id
                        masterOrderList.add(order)
                    }
                }

                // Urutkan data berdasarkan waktu terbaru (descending)
                masterOrderList.sortByDescending { it.created_at }

                applyFilter(currentFilter)
            }
    }

    private fun setupFilters() {
        binding.filterSemua.setOnClickListener { applyFilter("Semua") }
        binding.filterPending.setOnClickListener { applyFilter("Menunggu Pembayaran") }
        binding.filterVerifikasi.setOnClickListener { applyFilter("Menunggu Verifikasi") }
        binding.filterDitolak.setOnClickListener { applyFilter("Pembayaran Ditolak") }
        binding.filterProses.setOnClickListener { applyFilter("Diproses") }
        binding.filterDikirim.setOnClickListener { applyFilter("Dikirim") }
        binding.filterSelesai.setOnClickListener { applyFilter("Selesai") }
    }

    private fun applyFilter(status: String) {
        currentFilter = status

        val filteredList = if (status == "Semua") {
            masterOrderList
        } else {
            masterOrderList.filter { it.status == status }
        }

        adapter.updateData(filteredList)

        // Show/Hide teks kosong
        if (filteredList.isEmpty()) {
            binding.tvKosongRiwayat.visibility = View.VISIBLE
            binding.rvRiwayatPesanan.visibility = View.GONE
        } else {
            binding.tvKosongRiwayat.visibility = View.GONE
            binding.rvRiwayatPesanan.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        firestoreListener?.remove()
        _binding = null
    }
}