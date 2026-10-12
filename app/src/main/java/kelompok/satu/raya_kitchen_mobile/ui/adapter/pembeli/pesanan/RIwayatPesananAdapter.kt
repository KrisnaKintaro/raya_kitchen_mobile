package kelompok.satu.raya_kitchen_mobile.ui.adapter.pembeli.pesanan

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kelompok.satu.raya_kitchen_mobile.databinding.ItemRiwayatPesananBinding
import kelompok.satu.raya_kitchen_mobile.model.Order
import kelompok.satu.raya_kitchen_mobile.utils.FormatHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RiwayatPesananAdapter(
    private var orderList: List<Order>,
    private val onClick: (Order) -> Unit
) : RecyclerView.Adapter<RiwayatPesananAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemRiwayatPesananBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRiwayatPesananBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orderList[position]

        holder.binding.tvOrderNumber.text = order.order_number
        holder.binding.tvOrderTotal.text = FormatHelper.formatRupiah(order.total_amount)
        holder.binding.tvStatusBadge.text = order.status

        // Format Waktu Milliseconds ke Tanggal Cantik
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        holder.binding.tvOrderDate.text = sdf.format(Date(order.created_at))

        // Logic Warna Badge
        when (order.status) {
            "Menunggu Pembayaran", "Pembayaran Ditolak" -> holder.binding.tvStatusBadge.setTextColor(Color.parseColor("#EF4444")) // Merah
            "Menunggu Verifikasi" -> holder.binding.tvStatusBadge.setTextColor(Color.parseColor("#8B5CF6")) // Ungu
            "Diproses", "Dipanggang" -> holder.binding.tvStatusBadge.setTextColor(Color.parseColor("#F59E0B")) // Oren
            "Dikirim" -> holder.binding.tvStatusBadge.setTextColor(Color.parseColor("#3B82F6")) // Biru
            "Selesai" -> holder.binding.tvStatusBadge.setTextColor(Color.parseColor("#10B981")) // Hijau
            else -> holder.binding.tvStatusBadge.setTextColor(Color.GRAY)
        }

        holder.itemView.setOnClickListener {
            onClick(order)
        }
    }

    override fun getItemCount() = orderList.size

    fun updateData(newList: List<Order>) {
        orderList = newList
        notifyDataSetChanged()
    }
}