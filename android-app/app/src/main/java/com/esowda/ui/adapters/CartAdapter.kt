package com.esowda.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.esowda.R
import com.esowda.models.CartItem

class CartAdapter(
    private val onUpdateQuantity: (Int, Int) -> Unit,
    private val onRemoveItem: (Int) -> Unit
) : ListAdapter<CartItem, CartAdapter.CartViewHolder>(CartDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view, onUpdateQuantity, onRemoveItem)
    }
    
    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class CartViewHolder(
        itemView: View,
        private val onUpdateQuantity: (Int, Int) -> Unit,
        private val onRemoveItem: (Int) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val productImage: ImageView = itemView.findViewById(R.id.productImage)
        private val productName: TextView = itemView.findViewById(R.id.productName)
        private val productPrice: TextView = itemView.findViewById(R.id.productPrice)
        private val quantityText: TextView = itemView.findViewById(R.id.quantityText)
        private val totalPrice: TextView = itemView.findViewById(R.id.totalPrice)
        private val minusButton: ImageButton = itemView.findViewById(R.id.minusButton)
        private val plusButton: ImageButton = itemView.findViewById(R.id.plusButton)
        private val removeButton: ImageButton = itemView.findViewById(R.id.removeButton)
        
        fun bind(cartItem: CartItem) {
            val product = cartItem.product ?: return
            
            productName.text = product.nameTm ?: product.name
            productPrice.text = "${product.getDisplayPrice()} TMT"
            quantityText.text = cartItem.quantity.toString()
            totalPrice.text = "${cartItem.getTotalPrice().format(2)} TMT"
            
            // Load image
            Glide.with(itemView.context)
                .load(product.image ?: "https://via.placeholder.com/100")
                .placeholder(R.drawable.placeholder_product)
                .into(productImage)
            
            // Quantity controls
            minusButton.setOnClickListener {
                if (cartItem.quantity > 1) {
                    onUpdateQuantity(cartItem.id, cartItem.quantity - 1)
                }
            }
            
            plusButton.setOnClickListener {
                if (cartItem.quantity < product.stock) {
                    onUpdateQuantity(cartItem.id, cartItem.quantity + 1)
                }
            }
            
            removeButton.setOnClickListener {
                onRemoveItem(cartItem.id)
            }
        }
        
        private fun Double.format(digits: Int) = "%.${digits}f".format(this)
    }
    
    private class CartDiffCallback : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem == newItem
        }
    }
}
