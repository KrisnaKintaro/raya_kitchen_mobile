package kelompok.satu.raya_kitchen_mobile.ui.adapter.pembeli.pesanan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import kelompok.satu.raya_kitchen_mobile.databinding.ItemDetailPesananBinding
import kelompok.satu.raya_kitchen_mobile.model.OrderItem
import kelompok.satu.raya_kitchen_mobile.utils.FormatHelper

class DetailPesananAdapter(
    private var itemList: List<OrderItem>
) : RecyclerView.Adapter<DetailPesananAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDetailPesananBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDetailPesananBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList[position]
        holder.binding.tvNamaRotiDetail.text = item.variant_name ?: "Produk Tidak Diketahui"
        holder.binding.tvHargaDetail.text = "${FormatHelper.formatRupiah(item.price_at_order)} x ${item.quantity}"
        holder.binding.tvSubtotalDetail.text = FormatHelper.formatRupiah(item.subtotal)

        if (!item.image_url.isNullOrEmpty()) {
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
                .into(holder.binding.ivGambarDetail)
        } else {
            // Kalau data lamanya nggak punya link gambar, setel gambar abu-abu bawaan
            holder.binding.ivGambarDetail.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }

    override fun getItemCount() = itemList.size

    fun updateData(newList: List<OrderItem>) {
        itemList = newList
        notifyDataSetChanged()
    }
}