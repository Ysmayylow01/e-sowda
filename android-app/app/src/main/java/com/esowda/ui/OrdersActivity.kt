package com.esowda.ui

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.esowda.R
import com.esowda.network.RetrofitClient
import com.esowda.ui.adapters.OrderAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OrdersActivity : AppCompatActivity() {
    
    private lateinit var ordersRecyclerView: RecyclerView
    private lateinit var emptyOrdersText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var orderAdapter: OrderAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_orders)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Meniň sargytlarym"
        
        ordersRecyclerView = findViewById(R.id.ordersRecyclerView)
        emptyOrdersText = findViewById(R.id.emptyOrdersText)
        progressBar = findViewById(R.id.progressBar)
        
        setupRecyclerView()
        loadOrders()
    }
    
    private fun setupRecyclerView() {
        orderAdapter = OrderAdapter()
        ordersRecyclerView.layoutManager = LinearLayoutManager(this)
        ordersRecyclerView.adapter = orderAdapter
    }
    
    private fun loadOrders() {
        progressBar.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getOrders()
                
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    
                    if (response.isSuccessful) {
                        response.body()?.let { orders ->
                            if (orders.isEmpty()) {
                                emptyOrdersText.visibility = View.VISIBLE
                                ordersRecyclerView.visibility = View.GONE
                            } else {
                                emptyOrdersText.visibility = View.GONE
                                ordersRecyclerView.visibility = View.VISIBLE
                                orderAdapter.submitList(orders)
                            }
                        }
                    } else {
                        Toast.makeText(
                            this@OrdersActivity,
                            "Sargytlar ýüklenmedi",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@OrdersActivity,
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
