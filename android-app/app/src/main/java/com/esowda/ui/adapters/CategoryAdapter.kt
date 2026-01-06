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
import com.esowda.models.Category

class CategoryAdapter(
    private val onItemClick: (Category) -> Unit
) : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view, onItemClick)
    }
    
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class CategoryViewHolder(
        itemView: View,
        private val onItemClick: (Category) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val categoryImage: ImageView = itemView.findViewById(R.id.categoryImage)
        private val categoryName: TextView = itemView.findViewById(R.id.categoryName)
        private val productCount: TextView = itemView.findViewById(R.id.productCount)
        
        fun bind(category: Category) {
            categoryName.text = category.nameTm ?: category.name
            productCount.text = "${category.productCount ?: 0} haryt"
            
            // Load image
            if (!category.image.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(category.image)
                    .placeholder(R.drawable.placeholder_category)
                    .into(categoryImage)
            } else {
                categoryImage.setImageResource(R.drawable.placeholder_category)
            }
            
            itemView.setOnClickListener {
                onItemClick(category)
            }
        }
    }
    
    private class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }
}
