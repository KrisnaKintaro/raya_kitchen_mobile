package kelompok.satu.raya_kitchen_mobile.model

data class OrderItem(
    var id: String = "",
    var order_id: String = "",
    var variant_id: Int = 0,
    var variant_name: String? = "",
    var quantity: Int = 0,
    var price_at_order: Int = 0,
    var subtotal: Int = 0,
    var image_url: String? = ""
)