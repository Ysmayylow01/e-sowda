package com.esowda.models

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("address") val address: String?,
    @SerializedName("is_admin") val isAdmin: Boolean
)

data class Category(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("name_tm") val nameTm: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("product_count") val productCount: Int?
)

data class Product(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("name_tm") val nameTm: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("price") val price: Double,
    @SerializedName("discount_price") val discountPrice: Double?,
    @SerializedName("stock") val stock: Int,
    @SerializedName("image") val image: String?,
    @SerializedName("images") val images: String?,
    @SerializedName("category_id") val categoryId: Int,
    @SerializedName("category_name") val categoryName: String?,
    @SerializedName("brand") val brand: String?,
    @SerializedName("rating") val rating: Double,
    @SerializedName("views") val views: Int,
    @SerializedName("sales") val sales: Int,
    @SerializedName("featured") val featured: Boolean
) {
    fun getDisplayPrice(): Double = discountPrice ?: price
    
    fun hasDiscount(): Boolean = discountPrice != null && discountPrice < price
    
    fun getDiscountPercentage(): Int {
        return if (hasDiscount()) {
            (((price - discountPrice!!) / price) * 100).toInt()
        } else 0
    }
}

data class CartItem(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("product_id") val productId: Int,
    @SerializedName("product") val product: Product?,
    @SerializedName("quantity") var quantity: Int
) {
    fun getTotalPrice(): Double {
        return (product?.getDisplayPrice() ?: 0.0) * quantity
    }
}

data class CartResponse(
    @SerializedName("items") val items: List<CartItem>,
    @SerializedName("total") val total: Double,
    @SerializedName("count") val count: Int
)

data class Order(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("user") val user: User?,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("status") val status: String,
    @SerializedName("shipping_address") val shippingAddress: String,
    @SerializedName("phone") val phone: String?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("items") val items: List<OrderItem>?,
    @SerializedName("created_at") val createdAt: String
)

data class OrderItem(
    @SerializedName("id") val id: Int,
    @SerializedName("order_id") val orderId: Int,
    @SerializedName("product_id") val productId: Int,
    @SerializedName("product") val product: Product?,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("price") val price: Double
)

data class ProductsResponse(
    @SerializedName("products") val products: List<Product>,
    @SerializedName("total") val total: Int,
    @SerializedName("pages") val pages: Int,
    @SerializedName("current_page") val currentPage: Int
)

data class AuthRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("address") val address: String?
)

data class AuthResponse(
    @SerializedName("message") val message: String,
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: User
)

data class AddToCartRequest(
    @SerializedName("product_id") val productId: Int,
    @SerializedName("quantity") val quantity: Int = 1
)

data class UpdateCartRequest(
    @SerializedName("quantity") val quantity: Int
)

data class CreateOrderRequest(
    @SerializedName("shipping_address") val shippingAddress: String,
    @SerializedName("phone") val phone: String?,
    @SerializedName("notes") val notes: String?
)

data class ApiResponse(
    @SerializedName("message") val message: String
)

data class ErrorResponse(
    @SerializedName("error") val error: String
)
