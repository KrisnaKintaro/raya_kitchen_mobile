package kelompok.satu.raya_kitchen_mobile.ui.pembeli.fragment.checkout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import kelompok.satu.raya_kitchen_mobile.databinding.FragmentCheckoutBinding
import kelompok.satu.raya_kitchen_mobile.model.Address
import kelompok.satu.raya_kitchen_mobile.model.CartItem
import kelompok.satu.raya_kitchen_mobile.ui.adapter.pembeli.checkout.CheckoutAdapter
import kelompok.satu.raya_kitchen_mobile.utils.FormatHelper
import kelompok.satu.raya_kitchen_mobile.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CheckoutFragment : Fragment() {

    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private lateinit var checkoutAdapter: CheckoutAdapter

    private var cartItems = mutableListOf<CartItem>()
    private var addresses = mutableListOf<Address>()
    private var selectedAddress: Address? = null
    private var totalTagihan = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckoutBinding.inflate(inflater, container, false)

        setupRecyclerView()
        loadCartData()
        loadAddresses()

        binding.btnBackCheckout.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        binding.btnBuatPesanan.setOnClickListener {
            prosesBuatPesanan()
        }

        return binding.root
    }

    private fun setupRecyclerView() {
        checkoutAdapter = CheckoutAdapter(cartItems)
        binding.rvCheckoutItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCheckoutItems.adapter = checkoutAdapter
    }

    private fun loadCartData() {
        val userWa = SessionManager(requireContext()).getUserWa()
        db.collection("carts")
            .whereEqualTo("user_wa", userWa)
            .get()
            .addOnSuccessListener { documents ->
                cartItems.clear()
                totalTagihan = 0
                for (doc in documents) {
                    val item = doc.toObject(CartItem::class.java)
                    item.id = doc.id
                    cartItems.add(item)
                    totalTagihan += (item.price * item.quantity)
                }
                checkoutAdapter.updateData(cartItems)

                // Set text tagihan
                binding.tvSubtotalCo.text = FormatHelper.formatRupiah(totalTagihan)
                binding.tvTotalAkhirCo.text = FormatHelper.formatRupiah(totalTagihan)
            }
    }

    private fun loadAddresses() {
        val userWa = SessionManager(requireContext()).getUserWa()
        db.collection("addresses")
            .whereEqualTo("user_wa", userWa)
            .get()
            .addOnSuccessListener { documents ->
                addresses.clear()
                var primaryIndex = 0

                for ((index, doc) in documents.withIndex()) {
                    val address = doc.toObject(Address::class.java)
                    address.id = doc.id
                    addresses.add(address)
                    if (address.is_primary) {
                        primaryIndex = index
                    }
                }

                if (addresses.isEmpty()) {
                    binding.tvDetailAlamatCo.text = "Belum ada alamat. Silakan tambah di menu Profil."
                    binding.btnBuatPesanan.isEnabled = false
                    return@addOnSuccessListener
                }

                // Pasang ke Spinner (Dropdown)
                val addressLabels = addresses.map { it.label }
                val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, addressLabels)
                binding.spinnerAlamat.adapter = spinnerAdapter

                // Set default pilihan ke alamat utama
                binding.spinnerAlamat.setSelection(primaryIndex)

                // Listener kalau user milih alamat lain di dropdown
                binding.spinnerAlamat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        selectedAddress = addresses[position]
                        tampilkanDetailAlamat(selectedAddress!!)
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
    }

    private fun tampilkanDetailAlamat(address: Address) {
        val fullAddress = "${address.recipient_name} (${address.recipient_whatsapp})\n" +
                "${address.street_address}, Kec. ${address.district}, ${address.city}, ${address.province} ${address.postal_code}"
        val landmarkText = if (address.landmark.isNotEmpty()) "\nPatokan: ${address.landmark}" else ""
        binding.tvDetailAlamatCo.text = fullAddress + landmarkText
    }

    private fun prosesBuatPesanan() {
        if (cartItems.isEmpty() || selectedAddress == null) {
            Toast.makeText(requireContext(), "Keranjang atau alamat tidak valid!", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnBuatPesanan.isEnabled = false
        binding.btnBuatPesanan.text = "Memproses..."

        val sessionManager = SessionManager(requireContext())
        val userWa = sessionManager.getUserWa()

        // Bikin Format Waktu (Contoh: 20261011171109)
        val timeStamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())

        // 🌟 BIKIN CUSTOM ID RAPI DI SINI 🌟
        val customOrderId = "ORD-$timeStamp-$userWa"
        val customTransId = "TRX-$timeStamp-$userWa"

        // Pilihan Metode Pembayaran
        val isTransfer = binding.rbTransfer.isChecked
        val paymentMethod = if (isTransfer) "Transfer" else "COD"

        val batch = db.batch()

        // 1. BUAT KEPALA PESANAN (ORDERS)
        // Masukkan customOrderId ke dalam kurung document()
        val orderRef = db.collection("orders").document(customOrderId)
        val orderData = hashMapOf(
            "id" to customOrderId,
            "user_wa" to userWa,
            "order_number" to customOrderId, // Kita samain aja ID sama Order Number biar gampang
            "recipient_name" to selectedAddress!!.recipient_name,
            "recipient_whatsapp" to selectedAddress!!.recipient_whatsapp,
            "shipping_address" to binding.tvDetailAlamatCo.text.toString(),
            "total_amount" to totalTagihan,
            "status" to "Menunggu Pembayaran",
            "order_source" to "android",
            "created_at" to System.currentTimeMillis()
        )
        batch.set(orderRef, orderData)

        // 2. BUAT RINCIAN PESANAN (ORDER_ITEMS)
        // Pakai .withIndex() biar kita dapat nomor urut (index) itemnya
        for ((index, item) in cartItems.withIndex()) {
            val customItemId = "ITEM-$timeStamp-$userWa-$index"
            val itemRef = db.collection("order_items").document(customItemId)
            val itemData = hashMapOf(
                "order_id" to customOrderId,
                "variant_id" to item.variant_id,
                "variant_name" to item.variant_name,
                "quantity" to item.quantity,
                "price_at_order" to item.price,
                "subtotal" to (item.price * item.quantity),
                "image_url" to item.image_url
            )
            batch.set(itemRef, itemData)

            // 4. HAPUS KERANJANG (CARTS)
            val cartRef = db.collection("carts").document(item.id)
            batch.delete(cartRef)
        }

        // 3. BUAT TRANSAKSI (TRANSACTIONS)
        val transRef = db.collection("transactions").document(customTransId)
        val transData = hashMapOf(
            "id" to customTransId,
            "order_id" to customOrderId,
            "amount" to totalTagihan,
            "payment_method" to paymentMethod,
            "payment_status" to "Pending",
            "payment_proof" to "",
            "paid_at" to null
        )
        batch.set(transRef, transData)

        // EKSEKUSI SEMUANYA BERSAMAAN
        // EKSEKUSI SEMUANYA BERSAMAAN
        batch.commit().addOnSuccessListener {
            if (isTransfer) {
                val payFragment = PembayaranFragment().apply {
                    arguments = Bundle().apply {
                        putString("EXTRA_TRANS_ID", customTransId)
                        putString("EXTRA_ORDER_ID", customOrderId)
                        putInt("EXTRA_TOTAL", totalTagihan)
                    }
                }
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(kelompok.satu.raya_kitchen_mobile.R.id.fragment_container, payFragment)
                    .commit()
            } else {
                Toast.makeText(requireContext(), "Pesanan COD Berhasil Dibuat!", Toast.LENGTH_SHORT).show()
                requireActivity().supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
        }.addOnFailureListener {
            binding.btnBuatPesanan.isEnabled = true
            binding.btnBuatPesanan.text = "Buat Pesanan"
            Toast.makeText(requireContext(), "Gagal membuat pesanan", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}