package kelompok.satu.raya_kitchen_mobile.ui.adapter.pembeli.keranjang

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import kelompok.satu.raya_kitchen_mobile.databinding.ItemKeranjangBinding
import kelompok.satu.raya_kitchen_mobile.model.CartItem
import kelompok.satu.raya_kitchen_mobile.utils.FormatHelper

class KeranjangAdapter(
    private var cartItems: List<CartItem>,
    private val onUpdateQty: (CartItem, Int) -> Unit,
    private val onDelete: (CartItem) -> Unit
) : RecyclerView.Adapter<KeranjangAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemKeranjangBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemKeranjangBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = cartItems[position]

        holder.binding.tvNamaCart.text = item.variant_name
        holder.binding.tvHargaCart.text = FormatHelper.formatRupiah(item.price)
        holder.binding.tvQtyCart.text = item.quantity.toString()

        // Load gambar dengan Glide + Header
        val glideUrl = GlideUrl(
            item.image_url,
            LazyHeaders.Builder()
                .addHeader("ngrok-skip-browser-warning", "69420")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()
        )

        Glide.with(holder.itemView.context)
            .load(glideUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .centerCrop()
            .into(holder.binding.ivGambarCart)

        // Logika Plus Minus
        holder.binding.btnPlusCart.setOnClickListener {
            onUpdateQty(item, item.quantity + 1)
        }

        holder.binding.btnMinusCart.setOnClickListener {
            if (item.quantity > 1) {
                onUpdateQty(item, item.quantity - 1)
            }
        }

        // Logika Delete
        holder.binding.btnDeleteCart.setOnClickListener {
            onDelete(item)
        }
    }

    override fun getItemCount() = cartItems.size

    fun updateData(newItems: List<CartItem>) {
        cartItems = newItems
        notifyDataSetChanged()
    }
}