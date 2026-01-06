package com.esowda.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.esowda.R
import com.esowda.models.Order
import java.text.SimpleDateFormat
import java.util.*

class OrderAdapter : ListAdapter<Order, OrderAdapter.OrderViewHolder>(OrderDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        
        private val orderNumber: TextView = itemView.findViewById(R.id.orderNumber)
        private val orderDate: TextView = itemView.findViewById(R.id.orderDate)
        private val orderStatus: TextView = itemView.findViewById(R.id.orderStatus)
        private val orderTotal: TextView = itemView.findViewById(R.id.orderTotal)
        private val orderItems: TextView = itemView.findViewById(R.id.orderItems)
        
        fun bind(order: Order) {
            orderNumber.text = "Sargyt #${order.id}"
            orderTotal.text = "${order.totalAmount.format(2)} TMT"
            
            // Format date
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val date = inputFormat.parse(order.createdAt)
                orderDate.text = date?.let { outputFormat.format(it) } ?: order.createdAt
            } catch (e: Exception) {
                orderDate.text = order.createdAt
            }
            
            // Status with styling
            val statusInfo = getStatusInfo(order.status)
            orderStatus.text = statusInfo.first
            orderStatus.setBackgroundResource(statusInfo.second)
            
            // Show items count
            val itemsCount = order.items?.size ?: 0
            orderItems.text = "$itemsCount haryt"
        }
        
        private fun getStatusInfo(status: String): Pair<String, Int> {
            return when (status) {
                "pending" -> Pair("Garaşylýar", R.drawable.status_bg_pending)
                "processing" -> Pair("Işlenýär", R.drawable.status_bg_processing)
                "shipped" -> Pair("Ugradyldy", R.drawable.status_bg_shipped)
                "delivered" -> Pair("Gowşuryldy", R.drawable.status_bg_delivered)
                "cancelled" -> Pair("Ýatyryldy", R.drawable.status_bg_cancelled)
                else -> Pair(status, R.drawable.status_bg_pending)
            }
        }
        
        private fun Double.format(digits: Int) = "%.${digits}f".format(this)
    }
    
    private class OrderDiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem == newItem
        }
    }
}
