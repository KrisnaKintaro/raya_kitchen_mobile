package kelompok.satu.raya_kitchen_mobile.model

data class Address(
    var id: String = "", // ID Dokumen Firestore
    var user_wa: String = "",
    var label: String = "",
    var recipient_name: String = "",
    var recipient_whatsapp: String = "",
    var province: String = "",
    var city: String = "",
    var district: String = "",
    var postal_code: String = "",
    var street_address: String = "",
    var landmark: String = "",
    var is_primary: Boolean = false
)