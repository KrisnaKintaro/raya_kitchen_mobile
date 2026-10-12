package kelompok.satu.raya_kitchen_mobile.model

data class CartItem(
    var id: String = "",
    var user_wa: String = "",
    var variant_id: Int = 0,
    var variant_name: String = "",
    var category_name: String = "",
    var price: Int = 0,
    var image_url: String = "",
    var quantity: Int = 0
)