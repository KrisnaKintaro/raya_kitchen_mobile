package kelompok.satu.raya_kitchen_mobile.model

data class Order(
    var id: String = "",
    var user_wa: String = "",
    var order_number: String = "",
    var recipient_name: String = "",
    var recipient_whatsapp: String = "",
    var shipping_address: String = "",
    var total_amount: Int = 0,
    var status: String = "",
    var created_at: Long = 0L
)