package com.esowda.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.esowda.R
import com.esowda.network.RetrofitClient
import com.esowda.ui.adapters.CategoryAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoriesFragment : Fragment() {
    
    private lateinit var categoriesRecyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var categoryAdapter: CategoryAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_categories, container, false)
        
        categoriesRecyclerView = view.findViewById(R.id.categoriesRecyclerView)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        
        setupRecyclerView()
        loadCategories()
        
        swipeRefresh.setOnRefreshListener {
            loadCategories()
        }
        
        return view
    }
    
    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter { category ->
            openCategoryProducts(category.id)
        }
        categoriesRecyclerView.layoutManager = GridLayoutManager(context, 2)
        categoriesRecyclerView.adapter = categoryAdapter
    }
    
    private fun loadCategories() {
        swipeRefresh.isRefreshing = true
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getCategories()
                
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    
                    if (response.isSuccessful) {
                        response.body()?.let { categories ->
                            categoryAdapter.submitList(categories)
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Kategoriýalar ýüklenmedi",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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
    
    private fun openCategoryProducts(categoryId: Int) {
        val fragment = SearchFragment.newInstance(categoryId = categoryId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
