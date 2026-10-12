package kelompok.satu.raya_kitchen_mobile.ui.adapter.pembeli.checkout

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import kelompok.satu.raya_kitchen_mobile.databinding.ItemCheckoutBinding
import kelompok.satu.raya_kitchen_mobile.model.CartItem
import kelompok.satu.raya_kitchen_mobile.utils.FormatHelper

class CheckoutAdapter(
    private var cartItems: List<CartItem>
) : RecyclerView.Adapter<CheckoutAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemCheckoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCheckoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = cartItems[position]

        holder.binding.tvNamaCo.text = item.variant_name
        holder.binding.tvHargaCo.text = FormatHelper.formatRupiah(item.price)
        holder.binding.tvQtyCo.text = "${item.quantity}x"

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
            .into(holder.binding.ivGambarCo)
    }

    override fun getItemCount() = cartItems.size

    fun updateData(newItems: List<CartItem>) {
        cartItems = newItems
        notifyDataSetChanged()
    }
}