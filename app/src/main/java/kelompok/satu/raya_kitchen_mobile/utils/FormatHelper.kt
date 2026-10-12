package kelompok.satu.raya_kitchen_mobile.utils

import java.text.NumberFormat
import java.util.Locale

object FormatHelper {
    fun formatRupiah(number: Int): String {
        val localeID = Locale("in", "ID")
        val formatRupiah = NumberFormat.getCurrencyInstance(localeID)
        return formatRupiah.format(number).replace("Rp", "Rp ").replace(",00", "")
    }
}