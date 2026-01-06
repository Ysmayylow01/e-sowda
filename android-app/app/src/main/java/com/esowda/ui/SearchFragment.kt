package com.esowda.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.esowda.R
import com.esowda.network.RetrofitClient
import com.esowda.ui.adapters.ProductAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchFragment : Fragment() {
    
    private lateinit var searchView: SearchView
    private lateinit var sortSpinner: Spinner
    private lateinit var productsRecyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var noResultsText: TextView
    
    private lateinit var productAdapter: ProductAdapter
    
    private var categoryId: Int? = null
    private var searchQuery: String = ""
    private var sortBy: String = "created_at"
    
    companion object {
        fun newInstance(categoryId: Int? = null): SearchFragment {
            val fragment = SearchFragment()
            fragment.arguments = bundleOf("category_id" to categoryId)
            return fragment
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        categoryId = arguments?.getInt("category_id")
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        
        initViews(view)
        setupSearchView()
        setupSortSpinner()
        setupRecyclerView()
        loadProducts()
        
        swipeRefresh.setOnRefreshListener {
            loadProducts()
        }
        
        return view
    }
    
    private fun initViews(view: View) {
        searchView = view.findViewById(R.id.searchView)
        sortSpinner = view.findViewById(R.id.sortSpinner)
        productsRecyclerView = view.findViewById(R.id.productsRecyclerView)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        noResultsText = view.findViewById(R.id.noResultsText)
    }
    
    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchQuery = query ?: ""
                loadProducts()
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    searchQuery = ""
                    loadProducts()
                }
                return true
            }
        })
    }
    
    private fun setupSortSpinner() {
        val sortOptions = arrayOf(
            "Täze",
            "Baha: Pes → Ýokary",
            "Baha: Ýokary → Pes",
            "Reýting",
            "Iň köp satylan"
        )
        
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            sortOptions
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortSpinner.adapter = adapter
        
        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                sortBy = when (position) {
                    0 -> "created_at"
                    1 -> "price_asc"
                    2 -> "price_desc"
                    3 -> "rating"
                    4 -> "sales"
                    else -> "created_at"
                }
                loadProducts()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupRecyclerView() {
        productAdapter = ProductAdapter { product ->
            openProductDetail(product.id)
        }
        productsRecyclerView.layoutManager = GridLayoutManager(context, 2)
        productsRecyclerView.adapter = productAdapter
    }
    
    private fun loadProducts() {
        swipeRefresh.isRefreshing = true
        noResultsText.visibility = View.GONE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getProducts(
                    categoryId = categoryId,
                    search = searchQuery.ifEmpty { null },
                    sortBy = sortBy,
                    perPage = 50
                )
                
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    
                    if (response.isSuccessful) {
                        response.body()?.let { data ->
                            if (data.products.isEmpty()) {
                                noResultsText.visibility = View.VISIBLE
                                productsRecyclerView.visibility = View.GONE
                            } else {
                                noResultsText.visibility = View.GONE
                                productsRecyclerView.visibility = View.VISIBLE
                                productAdapter.submitList(data.products)
                            }
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Harytlar ýüklenmedi",
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
    
    private fun openProductDetail(productId: Int) {
        val intent = Intent(context, ProductDetailActivity::class.java)
        intent.putExtra("product_id", productId)
        startActivity(intent)
    }
}
