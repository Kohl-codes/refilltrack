package com.orador.refilltrack.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.orador.refilltrack.databinding.ItemOrderBinding
import com.orador.refilltrack.model.Order
import com.orador.refilltrack.model.OrderStatus

class OrderAdapter(
    private var items: List<Order>,
    private val onItemClick: (Order) -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    class OrderViewHolder(val binding: ItemOrderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = items[position]
        val b = holder.binding

        b.tvCustomerName.text = order.customerName
        b.tvGallons.text = order.gallons.toString() + " gallon(s)"
        b.tvAddress.text = order.address
        b.chipStatus.text = order.status
        b.chipStatus.chipBackgroundColor = ColorStateList.valueOf(OrderStatus.colorOf(order.status))

        b.root.setOnClickListener { onItemClick(order) }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<Order>) {
        items = newItems
        notifyDataSetChanged()
    }
}
