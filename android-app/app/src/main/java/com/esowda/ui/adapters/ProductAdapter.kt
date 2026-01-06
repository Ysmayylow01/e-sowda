package com.esowda.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.esowda.R
import com.esowda.models.Product

class ProductAdapter(
    private val onItemClick: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view, onItemClick)
    }
    
    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ProductViewHolder(
        itemView: View,
        private val onItemClick: (Product) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val productImage: ImageView = itemView.findViewById(R.id.productImage)
        private val productName: TextView = itemView.findViewById(R.id.productName)
        private val productCategory: TextView = itemView.findViewById(R.id.productCategory)
        private val productPrice: TextView = itemView.findViewById(R.id.productPrice)
        private val productOldPrice: TextView = itemView.findViewById(R.id.productOldPrice)
        private val discountBadge: TextView = itemView.findViewById(R.id.discountBadge)
        private val productRating: TextView = itemView.findViewById(R.id.productRating)
        private val productSales: TextView = itemView.findViewById(R.id.productSales)
        
        fun bind(product: Product) {
            productName.text = product.nameTm ?: product.name
            productCategory.text = product.categoryName ?: ""
            productPrice.text = "${product.getDisplayPrice()} TMT"
            productRating.text = product.rating.toString()
            productSales.text = "${product.sales} satyldy"
            
            // Load image
            Glide.with(itemView.context)
                .load(product.image ?: "https://via.placeholder.com/200")
                .placeholder(R.drawable.placeholder_product)
                .into(productImage)
            
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
            
            itemView.setOnClickListener {
                onItemClick(product)
            }
        }
    }
    
    private class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
}
