package com.esowda.ui

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.esowda.R
import com.esowda.models.AddToCartRequest
import com.esowda.models.Product
import com.esowda.network.RetrofitClient
import com.esowda.ui.adapters.ProductAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductDetailActivity : AppCompatActivity() {
    
    private lateinit var productImage: ImageView
    private lateinit var productName: TextView
    private lateinit var productCategory: TextView
    private lateinit var productPrice: TextView
    private lateinit var productOldPrice: TextView
    private lateinit var discountBadge: TextView
    private lateinit var productRating: TextView
    private lateinit var productSales: TextView
    private lateinit var productStock: TextView
    private lateinit var productDescription: TextView
    private lateinit var quantityText: TextView
    private lateinit var minusButton: ImageButton
    private lateinit var plusButton: ImageButton
    private lateinit var addToCartButton: Button
    private lateinit var recommendedRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    
    private var productId: Int = 0
    private var quantity: Int = 1
    private var currentProduct: Product? = null
    
    private lateinit var recommendedAdapter: ProductAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Haryt maglumatlary"
        
        productId = intent.getIntExtra("product_id", 0)
        
        initViews()
        setupQuantityControls()
        setupRecommendedRecyclerView()
        loadProductDetails()
    }
    
    private fun initViews() {
        productImage = findViewById(R.id.productImage)
        productName = findViewById(R.id.productName)
        productCategory = findViewById(R.id.productCategory)
        productPrice = findViewById(R.id.productPrice)
        productOldPrice = findViewById(R.id.productOldPrice)
        discountBadge = findViewById(R.id.discountBadge)
        productRating = findViewById(R.id.productRating)
        productSales = findViewById(R.id.productSales)
        productStock = findViewById(R.id.productStock)
        productDescription = findViewById(R.id.productDescription)
        quantityText = findViewById(R.id.quantityText)
        minusButton = findViewById(R.id.minusButton)
        plusButton = findViewById(R.id.plusButton)
        addToCartButton = findViewById(R.id.addToCartButton)
        recommendedRecyclerView = findViewById(R.id.recommendedRecyclerView)
        progressBar = findViewById(R.id.progressBar)
    }
    
    private fun setupQuantityControls() {
        minusButton.setOnClickListener {
            if (quantity > 1) {
                quantity--
                quantityText.text = quantity.toString()
            }
        }
        
        plusButton.setOnClickListener {
            currentProduct?.let { product ->
                if (quantity < product.stock) {
                    quantity++
                    quantityText.text = quantity.toString()
                } else {
                    Toast.makeText(this, "Haryt ýeterlik däl", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        addToCartButton.setOnClickListener {
            addToCart()
        }
    }
    
    private fun setupRecommendedRecyclerView() {
        recommendedAdapter = ProductAdapter { product ->
            // Open another product detail
            productId = product.id
            quantity = 1
            quantityText.text = "1"
            loadProductDetails()
            window.decorView.findViewById<View>(android.R.id.content).scrollTo(0, 0)
        }
        recommendedRecyclerView.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        recommendedRecyclerView.adapter = recommendedAdapter
    }
    
    private fun loadProductDetails() {
        progressBar.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getProduct(productId)
                
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    
                    if (response.isSuccessful) {
                        response.body()?.let { product ->
                            currentProduct = product
                            displayProduct(product)
                            loadRecommendations(product.id)
                        }
                    } else {
                        Toast.makeText(
                            this@ProductDetailActivity,
                            "Haryt ýüklenmedi",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@ProductDetailActivity,
                        "Baglanyşyk ýalňyşlygy: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun displayProduct(product: Product) {
        // Load image
        Glide.with(this)
            .load(product.image ?: "https://via.placeholder.com/400")
            .placeholder(R.drawable.placeholder_product)
            .into(productImage)
        
        // Set text fields
        productName.text = product.nameTm ?: product.name
        productCategory.text = product.categoryName ?: ""
        productPrice.text = "${product.getDisplayPrice()} TMT"
        
        // Show discount if available
        if (product.hasDiscount()) {
            productOldPrice.visibility = View.VISIBLE
            productOldPrice.text = "${product.price} TMT"
            productOldPrice.paintFlags = productOldPrice.paintFlags or 
                android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            
            discountBadge.visibility = View.VISIBLE
            discountBadge.text = "-${product.getDiscountPercentage()}%"
        } else {
            productOldPrice.visibility = View.GONE
            discountBadge.visibility = View.GONE
        }
        
        productRating.text = product.rating.toString()
        productSales.text = "${product.sales} satyldy"
        
        // Stock status
        if (product.stock > 0) {
            productStock.text = "Mukdar: ${product.stock}"
            productStock.setTextColor(resources.getColor(R.color.success, null))
            addToCartButton.isEnabled = true
        } else {
            productStock.text = "Haryt ýok"
            productStock.setTextColor(resources.getColor(R.color.error, null))
            addToCartButton.isEnabled = false
        }
        
        productDescription.text = product.description ?: "Düşündiriş ýok"
    }
    
    private fun loadRecommendations(productId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getProductRecommendations(productId)
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        response.body()?.let { products ->
                            if (products.isNotEmpty()) {
                                recommendedAdapter.submitList(products)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Silently fail for recommendations
            }
        }
    }
    
    private fun addToCart() {
        addToCartButton.isEnabled = false
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = AddToCartRequest(productId, quantity)
                val response = RetrofitClient.apiService.addToCart(request)
                
                withContext(Dispatchers.Main) {
                    addToCartButton.isEnabled = true
                    
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@ProductDetailActivity,
                            "Sebede goşuldy!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@ProductDetailActivity,
                            "Sebede goşmak şowsuz boldy",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    addToCartButton.isEnabled = true
                    Toast.makeText(
                        this@ProductDetailActivity,
                        "Baglanyşyk ýalňyşlygy: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
