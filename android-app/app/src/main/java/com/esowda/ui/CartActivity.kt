package com.esowda.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.esowda.R
import com.esowda.models.CartResponse
import com.esowda.models.CreateOrderRequest
import com.esowda.network.RetrofitClient
import com.esowda.ui.adapters.CartAdapter
import com.esowda.utils.PrefsManager
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CartActivity : AppCompatActivity() {
    
    private lateinit var cartRecyclerView: RecyclerView
    private lateinit var emptyCartText: TextView
    private lateinit var totalAmountText: TextView
    private lateinit var checkoutButton: Button
    private lateinit var clearCartButton: Button
    private lateinit var progressBar: ProgressBar
    
    private lateinit var cartAdapter: CartAdapter
    private lateinit var prefsManager: PrefsManager
    
    private var cartData: CartResponse? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Sebet"
        
        prefsManager = PrefsManager(this)
        
        initViews()
        setupRecyclerView()
        loadCart()
        
        checkoutButton.setOnClickListener {
            if (cartData != null && cartData!!.items.isNotEmpty()) {
                showCheckoutDialog()
            }
        }
        
        clearCartButton.setOnClickListener {
            showClearCartDialog()
        }
    }
    
    private fun initViews() {
        cartRecyclerView = findViewById(R.id.cartRecyclerView)
        emptyCartText = findViewById(R.id.emptyCartText)
        totalAmountText = findViewById(R.id.totalAmountText)
        checkoutButton = findViewById(R.id.checkoutButton)
        clearCartButton = findViewById(R.id.clearCartButton)
        progressBar = findViewById(R.id.progressBar)
    }
    
    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            onUpdateQuantity = { itemId, newQuantity ->
                updateCartItem(itemId, newQuantity)
            },
            onRemoveItem = { itemId ->
                removeCartItem(itemId)
            }
        )
        cartRecyclerView.layoutManager = LinearLayoutManager(this)
        cartRecyclerView.adapter = cartAdapter
    }
    
    private fun loadCart() {
        progressBar.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getCart()
                
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    
                    if (response.isSuccessful) {
                        response.body()?.let { cart ->
                            cartData = cart
                            displayCart(cart)
                        }
                    } else {
                        Toast.makeText(
                            this@CartActivity,
                            "Sebet ýüklenmedi",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@CartActivity,
                        "Baglanyşyk ýalňyşlygy: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun displayCart(cart: CartResponse) {
        if (cart.items.isEmpty()) {
            emptyCartText.visibility = View.VISIBLE
            cartRecyclerView.visibility = View.GONE
            totalAmountText.text = "0.00 TMT"
            checkoutButton.isEnabled = false
            clearCartButton.isEnabled = false
        } else {
            emptyCartText.visibility = View.GONE
            cartRecyclerView.visibility = View.VISIBLE
            cartAdapter.submitList(cart.items)
            totalAmountText.text = "${cart.total.format(2)} TMT"
            checkoutButton.isEnabled = true
            clearCartButton.isEnabled = true
        }
    }
    
    private fun updateCartItem(itemId: Int, newQuantity: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = com.esowda.models.UpdateCartRequest(newQuantity)
                val response = RetrofitClient.apiService.updateCartItem(itemId, request)
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        loadCart() // Reload to get updated totals
                    } else {
                        Toast.makeText(
                            this@CartActivity,
                            "Täzelemek şowsuz boldy",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CartActivity,
                        "Baglanyşyk ýalňyşlygy: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun removeCartItem(itemId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.removeFromCart(itemId)
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@CartActivity,
                            "Sebetden aýryldy",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadCart()
                    } else {
                        Toast.makeText(
                            this@CartActivity,
                            "Aýyrmak şowsuz boldy",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CartActivity,
                        "Baglanyşyk ýalňyşlygy: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun showClearCartDialog() {
        AlertDialog.Builder(this)
            .setTitle("Sebeti arassalamak")
            .setMessage("Sebetdäki ähli harytlary aýyrmak isleýärsiňizmi?")
            .setPositiveButton("Hawa") { _, _ ->
                clearCart()
            }
            .setNegativeButton("Ýok", null)
            .show()
    }
    
    private fun clearCart() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.clearCart()
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@CartActivity,
                            "Sebet arassalandy",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadCart()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CartActivity,
                        "Baglanyşyk ýalňyşlygy: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun showCheckoutDialog() {
        val user = prefsManager.getUser()
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_checkout, null)
        val addressInput = dialogView.findViewById<TextInputEditText>(R.id.addressInput)
        val phoneInput = dialogView.findViewById<TextInputEditText>(R.id.phoneInput)
        val notesInput = dialogView.findViewById<TextInputEditText>(R.id.notesInput)
        
        // Pre-fill with user data
        addressInput.setText(user?.address ?: "")
        phoneInput.setText(user?.phone ?: "")
        
        AlertDialog.Builder(this)
            .setTitle("Sargyt bermek")
            .setView(dialogView)
            .setPositiveButton("Tassyklamak") { _, _ ->
                val address = addressInput.text.toString()
                val phone = phoneInput.text.toString()
                val notes = notesInput.text.toString()
                
                if (address.isBlank()) {
                    Toast.makeText(this, "Salgy girizilmeli", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                placeOrder(address, phone, notes)
            }
            .setNegativeButton("Ýatyrmak", null)
            .show()
    }
    
    private fun placeOrder(address: String, phone: String?, notes: String?) {
        progressBar.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = CreateOrderRequest(address, phone, notes)
                val response = RetrofitClient.apiService.createOrder(request)
                
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@CartActivity,
                            "Sargyt üstünlikli berildi!",
                            Toast.LENGTH_LONG
                        ).show()
                        
                        // Navigate to orders
                        startActivity(Intent(this@CartActivity, OrdersActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(
                            this@CartActivity,
                            "Sargyt bermek şowsuz boldy",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@CartActivity,
                        "Baglanyşyk ýalňyşlygy: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun Double.format(digits: Int) = "%.${digits}f".format(this)
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
