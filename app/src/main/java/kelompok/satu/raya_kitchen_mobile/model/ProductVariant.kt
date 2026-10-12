package kelompok.satu.raya_kitchen_mobile.model

// Pake data class biar otomatis dapet fungsi toString(), equals(), dll
data class ProductVariant(
    val id: Int,
    val productId: Int,
    val variantName: String,
    val price: Int,
    val imageUrl: String,
    val categoryName: String
)