package com.esowda.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.esowda.R
import com.esowda.models.Category
import com.esowda.models.Product
import com.esowda.network.RetrofitClient
import com.esowda.ui.adapters.CategoryAdapter
import com.esowda.ui.adapters.ProductAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {
    
    private lateinit var featuredRecyclerView: RecyclerView
    private lateinit var categoriesRecyclerView: RecyclerView
    private lateinit var recommendedRecyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    
    private lateinit var featuredAdapter: ProductAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var recommendedAdapter: ProductAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        
        initViews(view)
        setupRecyclerViews()
        loadData()
        
        swipeRefresh.setOnRefreshListener {
            loadData()
        }
        
        return view
    }
    
    private fun initViews(view: View) {
        featuredRecyclerView = view.findViewById(R.id.featuredRecyclerView)
        categoriesRecyclerView = view.findViewById(R.id.categoriesRecyclerView)
        recommendedRecyclerView = view.findViewById(R.id.recommendedRecyclerView)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
    }
    
    private fun setupRecyclerViews() {
        // Featured products
        featuredAdapter = ProductAdapter { product ->
            openProductDetail(product.id)
        }
        featuredRecyclerView.layoutManager = LinearLayoutManager(
            context, 
            LinearLayoutManager.HORIZONTAL, 
            false
        )
        featuredRecyclerView.adapter = featuredAdapter
        
        // Categories
        categoryAdapter = CategoryAdapter { category ->
            openCategoryProducts(category.id)
        }
        categoriesRecyclerView.layoutManager = GridLayoutManager(context, 2)
        categoriesRecyclerView.adapter = categoryAdapter
        
        // Recommended products
        recommendedAdapter = ProductAdapter { product ->
            openProductDetail(product.id)
        }
        recommendedRecyclerView.layoutManager = LinearLayoutManager(context)
        recommendedRecyclerView.adapter = recommendedAdapter
    }
    
    private fun loadData() {
        swipeRefresh.isRefreshing = true
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Load featured products
                val featuredResponse = RetrofitClient.apiService.getProducts(
                    featured = true,
                    perPage = 10
                )
                
                // Load categories
                val categoriesResponse = RetrofitClient.apiService.getCategories()
                
                // Load recommended products
                val recommendedResponse = RetrofitClient.apiService.getUserRecommendations()
                
                withContext(Dispatchers.Main) {
                    if (featuredResponse.isSuccessful) {
                        featuredResponse.body()?.let {
                            featuredAdapter.submitList(it.products)
                        }
                    }
                    
                    if (categoriesResponse.isSuccessful) {
                        categoriesResponse.body()?.let {
                            categoryAdapter.submitList(it)
                        }
                    }
                    
                    if (recommendedResponse.isSuccessful) {
                        recommendedResponse.body()?.let {
                            recommendedAdapter.submitList(it)
                        }
                    }
                    
                    swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    Toast.makeText(
                        context,
                        "Baglanyşyk ýalňyşlygy: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun openProductDetail(productId: Int) {
        val intent = Intent(context, ProductDetailActivity::class.java)
        intent.putExtra("product_id", productId)
        startActivity(intent)
    }
    
    private fun openCategoryProducts(categoryId: Int) {
        // Navigate to products filtered by category
        val fragment = SearchFragment.newInstance(categoryId = categoryId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
