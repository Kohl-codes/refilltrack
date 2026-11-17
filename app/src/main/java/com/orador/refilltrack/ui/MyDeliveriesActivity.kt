package com.orador.refilltrack.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.orador.refilltrack.adapter.OrderAdapter
import com.orador.refilltrack.data.OrderRepository
import com.orador.refilltrack.data.Session
import com.orador.refilltrack.databinding.ActivityMyDeliveriesBinding
import com.orador.refilltrack.model.Role

class MyDeliveriesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyDeliveriesBinding
    private lateinit var adapter: OrderAdapter
    private var riderId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = Session.currentUser
        if (user == null || user.role != Role.RIDER) {
            Session.logout(this)
            return
        }
        riderId = user.uid

        binding = ActivityMyDeliveriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvWelcome.text = "My Deliveries - " + user.name

        adapter = OrderAdapter(emptyList()) { order ->
            startActivity(OrderDetailsActivity.newIntent(this, order))
        }
        binding.rvOrders.layoutManager = LinearLayoutManager(this)
        binding.rvOrders.adapter = adapter

        binding.btnLogout.setOnClickListener { Session.logout(this) }
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) loadDeliveries()
    }

    // READ: only this rider's orders that are Out for Delivery
    private fun loadDeliveries() {
        binding.tvEmpty.text = "Loading deliveries..."
        binding.tvEmpty.visibility = View.VISIBLE

        OrderRepository.loadForRider(
            riderId,
            onResult = { orders ->
                adapter.updateList(orders)
                binding.tvEmpty.text = "No deliveries assigned to you."
                binding.tvEmpty.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
            },
            onError = { message ->
                binding.tvEmpty.text = "Could not load deliveries. Check your connection."
                binding.tvEmpty.visibility = View.VISIBLE
                Toast.makeText(this, "Could not load deliveries: " + message, Toast.LENGTH_LONG).show()
            }
        )
    }
}
