package com.esowda.network

import com.esowda.models.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface ApiService {
    
    // Auth
    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>
    
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
    
    @GET("auth/me")
    suspend fun getCurrentUser(): Response<User>
    
    // Categories
    @GET("categories")
    suspend fun getCategories(): Response<List<Category>>
    
    @GET("categories/{id}")
    suspend fun getCategory(@Path("id") id: Int): Response<Category>
    
    // Products
    @GET("products")
    suspend fun getProducts(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("category_id") categoryId: Int? = null,
        @Query("search") search: String? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("featured") featured: Boolean? = null
    ): Response<ProductsResponse>
    
    @GET("products/{id}")
    suspend fun getProduct(@Path("id") id: Int): Response<Product>
    
    // Cart
    @GET("cart")
    suspend fun getCart(): Response<CartResponse>
    
    @POST("cart")
    suspend fun addToCart(@Body request: AddToCartRequest): Response<ApiResponse>
    
    @PUT("cart/{id}")
    suspend fun updateCartItem(
        @Path("id") id: Int,
        @Body request: UpdateCartRequest
    ): Response<ApiResponse>
    
    @DELETE("cart/{id}")
    suspend fun removeFromCart(@Path("id") id: Int): Response<ApiResponse>
    
    @DELETE("cart/clear")
    suspend fun clearCart(): Response<ApiResponse>
    
    // Orders
    @GET("orders")
    suspend fun getOrders(): Response<List<Order>>
    
    @GET("orders/{id}")
    suspend fun getOrder(@Path("id") id: Int): Response<Order>
    
    @POST("orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): Response<ApiResponse>
    
    // Recommendations
    @GET("recommendations/{id}")
    suspend fun getProductRecommendations(@Path("id") id: Int): Response<List<Product>>
    
    @GET("recommendations/user")
    suspend fun getUserRecommendations(): Response<List<Product>>
    
    // User
    @PUT("users/{id}")
    suspend fun updateUser(
        @Path("id") id: Int,
        @Body user: Map<String, String>
    ): Response<ApiResponse>
}

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:5000/api/" // Use 10.0.2.2 for emulator to access localhost
    
    private var token: String? = null
    
    fun setToken(newToken: String?) {
        token = newToken
    }
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val authInterceptor = okhttp3.Interceptor { chain ->
        val request = chain.request().newBuilder()
        token?.let {
            request.addHeader("Authorization", "Bearer $it")
        }
        chain.proceed(request.build())
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
