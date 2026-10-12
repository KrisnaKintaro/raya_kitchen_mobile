package kelompok.satu.raya_kitchen_mobile.ui.pembeli.fragment.katalog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.google.firebase.firestore.FirebaseFirestore
import kelompok.satu.raya_kitchen_mobile.databinding.FragmentDetailProductBinding
import kelompok.satu.raya_kitchen_mobile.utils.FormatHelper
import kelompok.satu.raya_kitchen_mobile.utils.SessionManager

class DetailProductFragment : Fragment() {

    private var _binding: FragmentDetailProductBinding? = null
    private val binding get() = _binding!!

    private var quantity = 1

    // Jadikan semua data produk sebagai variabel global di dalam Fragment
    private var variantId = 0
    private var variantName = ""
    private var categoryName = ""
    private var variantPrice = 0
    private var productImageUrl = "" // Variabel yang tadi kelupaan

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailProductBinding.inflate(inflater, container, false)

        // 1. Tangkap Data dari Bundle (Arguments) yang dikirim Adapter
        arguments?.let {
            variantId = it.getInt("EXTRA_ID", 0)
            variantName = it.getString("EXTRA_NAME", "")
            categoryName = it.getString("EXTRA_CATEGORY", "")
            variantPrice = it.getInt("EXTRA_PRICE", 0)
            productImageUrl = it.getString("EXTRA_IMAGE", "")

            // 2. Tampilkan Teks
            binding.tvDetailNama.text = variantName
            binding.tvDetailKategori.text = categoryName
            binding.tvDetailHarga.text = FormatHelper.formatRupiah(variantPrice)
            binding.tvQuantity.text = quantity.toString()

            // 3. Muat Gambar pakai Glide
            loadProductImage(productImageUrl)
        }

        // 4. Logika Plus Minus Kuantitas Keranjang
        binding.btnPlus.setOnClickListener {
            quantity++
            binding.tvQuantity.text = quantity.toString()
        }

        binding.btnMinus.setOnClickListener {
            if (quantity > 1) {
                quantity--
                binding.tvQuantity.text = quantity.toString()
            }
        }

        // 5. Kembali ke Halaman Katalog
        binding.btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // 6. Tombol Add to Cart (Simpan ke Firestore)
        binding.btnAddToCart.setOnClickListener {
            binding.btnAddToCart.isEnabled = false
            binding.btnAddToCart.text = "Menyimpan..."

            val db = FirebaseFirestore.getInstance()
            val userWa = SessionManager(requireContext()).getUserWa()

            // Kita bikin ID Dokumen gabungan WA + ID Roti biar unik dan gampang dicari
            val docId = "${userWa}_${variantId}"

            val cartRef = db.collection("carts").document(docId)

            // Cek dulu, rotinya udah ada di keranjang belum?
            cartRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    // Kalau udah ada, update kuantitasnya (ditambah)
                    val currentQty = document.getLong("quantity") ?: 0
                    cartRef.update("quantity", currentQty + quantity)
                        .addOnSuccessListener { suksesMasukKeranjang() }
                        .addOnFailureListener { gagalMasukKeranjang() }
                } else {
                    // Kalau belum ada, bikin data baru
                    val cartItem = hashMapOf(
                        "user_wa" to userWa,
                        "variant_id" to variantId,
                        "variant_name" to variantName,
                        "category_name" to categoryName,
                        "price" to variantPrice,
                        "image_url" to productImageUrl,
                        "quantity" to quantity
                    )
                    cartRef.set(cartItem)
                        .addOnSuccessListener { suksesMasukKeranjang() }
                        .addOnFailureListener { gagalMasukKeranjang() }
                }
            }.addOnFailureListener {
                gagalMasukKeranjang()
            }
        }

        return binding.root
    }

    private fun loadProductImage(rawUrl: String) {
        if (rawUrl.isEmpty()) return

        val glideUrl = GlideUrl(
            rawUrl,
            LazyHeaders.Builder()
                .addHeader("ngrok-skip-browser-warning", "69420")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()
        )

        Glide.with(this)
            .load(glideUrl)
            .timeout(30000)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_delete)
            .centerCrop()
            .into(binding.ivDetailGambar)
    }

    private fun suksesMasukKeranjang() {
        Toast.makeText(requireContext(), "$quantity $variantName berhasil masuk keranjang!", Toast.LENGTH_SHORT).show()
        requireActivity().supportFragmentManager.popBackStack()
    }

    private fun gagalMasukKeranjang() {
        binding.btnAddToCart.isEnabled = true
        binding.btnAddToCart.text = "Masuk Keranjang"
        Toast.makeText(requireContext(), "Gagal menyimpan ke keranjang", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}