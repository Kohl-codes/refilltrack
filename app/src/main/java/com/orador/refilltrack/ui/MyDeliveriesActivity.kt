package com.orador.refilltrack.ui

import android.os.Bundle
import android.view.View
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
        if (::binding.isInitialized) refreshList()
    }

    private fun refreshList() {
        val myOrders = OrderRepository.getForRider(riderId)
        adapter.updateList(myOrders)
        binding.tvEmpty.visibility = if (myOrders.isEmpty()) View.VISIBLE else View.GONE
    }
}
