package kelompok.satu.raya_kitchen_mobile.ui.adapter.pembeli.profil

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kelompok.satu.raya_kitchen_mobile.databinding.ItemAlamatBinding
import kelompok.satu.raya_kitchen_mobile.model.Address

class AlamatAdapter(
    private var addresses: List<Address>,
    private val onSetPrimaryClick: (Address) -> Unit,
    private val onEditClick: (Address) -> Unit,
    private val onDeleteClick: (Address) -> Unit
) : RecyclerView.Adapter<AlamatAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAlamatBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlamatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val address = addresses[position]

        // Format teks sesuai desain XML
        holder.binding.tvLabelAlamat.text = address.label
        holder.binding.tvNamaPenerima.text = "${address.recipient_name} | ${address.recipient_whatsapp}"

        val fullAddress = "${address.street_address}, Kec. ${address.district}, ${address.city}, ${address.province} ${address.postal_code}"
        val landmarkText = if (address.landmark.isNotEmpty()) "\n(Patokan: ${address.landmark})" else ""
        holder.binding.tvAlamatLengkap.text = fullAddress + landmarkText

        // Logic Badge Utama & Tombol
        if (address.is_primary) {
            holder.binding.badgeUtama.visibility = View.VISIBLE
            holder.binding.btnJadikanUtama.visibility = View.GONE
        } else {
            holder.binding.badgeUtama.visibility = View.GONE
            holder.binding.btnJadikanUtama.visibility = View.VISIBLE
        }

        holder.binding.btnJadikanUtama.setOnClickListener {
            onSetPrimaryClick(address)
        }
        holder.binding.btnEditAlamat.setOnClickListener {
            onEditClick(address)
        }
        holder.binding.btnDeleteAlamat.setOnClickListener {
            onDeleteClick(address)
        }
    }

    override fun getItemCount() = addresses.size

    fun updateData(newAddresses: List<Address>) {
        addresses = newAddresses
        notifyDataSetChanged()
    }
}