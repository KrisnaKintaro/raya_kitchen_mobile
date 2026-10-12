package kelompok.satu.raya_kitchen_mobile.ui.adapter.pembeli.katalog

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import kelompok.satu.raya_kitchen_mobile.R
import kelompok.satu.raya_kitchen_mobile.databinding.ItemProdukBinding
import kelompok.satu.raya_kitchen_mobile.model.ProductVariant
import kelompok.satu.raya_kitchen_mobile.ui.pembeli.fragment.katalog.DetailProductFragment
import kelompok.satu.raya_kitchen_mobile.utils.Constant
import kelompok.satu.raya_kitchen_mobile.utils.FormatHelper

class ProductVariantAdapter(
    private var variants: List<ProductVariant>
) : RecyclerView.Adapter<ProductVariantAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemProdukBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProdukBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val variant = variants[position]

        holder.binding.tvNamaProduk.text = variant.variantName
        holder.binding.tvKategori.text = variant.categoryName
        holder.binding.tvHarga.text = FormatHelper.formatRupiah(variant.price)

        // Bersihkan Path Gambar dari API
        val imagePath = variant.imageUrl
        val domain = Constant.BASE_URL.replace("api/", "")
        val cleanImagePath = if (imagePath.startsWith("/")) imagePath.substring(1) else imagePath
        val rawUrl = domain + cleanImagePath

        Log.d("CEK_URL_GAMBAR", rawUrl)

        // Bungkus dengan Header Anti-Ngrok
        val glideUrl = GlideUrl(
            rawUrl,
            LazyHeaders.Builder()
                .addHeader("ngrok-skip-browser-warning", "69420")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()
        )

        // Load gambar dengan Timeout panjang
        Glide.with(holder.itemView.context)
            .load(glideUrl)
            .timeout(30000) // Waktu tunggu 30 detik
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_delete)
            .centerCrop()
            .into(holder.binding.ivGambarProduk)

        // Handler klik langsung di Adapter
        holder.itemView.setOnClickListener {
            val detailFragment = DetailProductFragment().apply {
                arguments = Bundle().apply {
                    putInt("EXTRA_ID", variant.id)
                    putString("EXTRA_NAME", variant.variantName)
                    putString("EXTRA_CATEGORY", variant.categoryName)
                    putInt("EXTRA_PRICE", variant.price)
                    putString("EXTRA_IMAGE", rawUrl)
                }
            }

            // 2. Ambil activity dari context item view, lalu jalankan transisi Fragment
            val activity = holder.itemView.context as AppCompatActivity
            activity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, detailFragment)
                .addToBackStack(null) // Masukin ke history biar bisa di-back
                .commit()
        }
    }

    override fun getItemCount() = variants.size

    fun updateData(newVariants: List<ProductVariant>) {
        variants = newVariants
        notifyDataSetChanged()
    }
}