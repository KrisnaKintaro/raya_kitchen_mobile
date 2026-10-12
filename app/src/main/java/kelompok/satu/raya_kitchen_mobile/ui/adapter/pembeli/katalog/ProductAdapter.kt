package kelompok.satu.raya_kitchen_mobile.ui.adapter.pembeli.katalog

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kelompok.satu.raya_kitchen_mobile.databinding.ItemKategoriBinding
import kelompok.satu.raya_kitchen_mobile.model.Product

class ProductAdapter(
    private val categories: List<Product>,
    private val onCategoryClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    private var selectedPosition = 0

    class ViewHolder(val binding: ItemKategoriBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemKategoriBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        holder.binding.tvNamaKategori.text = category.name

        // Ganti warna saat dipilih
        if (position == selectedPosition) {
            holder.binding.tvNamaKategori.setTextColor(Color.parseColor("#C099CE"))
        } else {
            holder.binding.tvNamaKategori.setTextColor(Color.parseColor("#716E77"))
        }

        holder.itemView.setOnClickListener {
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener

            val oldPosition = selectedPosition
            selectedPosition = currentPosition

            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)

            // Lempar object Product-nya ke Fragment
            onCategoryClick(categories[currentPosition])
        }
    }

    override fun getItemCount() = categories.size
}