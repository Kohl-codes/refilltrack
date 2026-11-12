package com.orador.refilltrack.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.orador.refilltrack.adapter.OrderAdapter
import com.orador.refilltrack.data.OrderRepository
import com.orador.refilltrack.data.Session
import com.orador.refilltrack.databinding.ActivityDashboardBinding
import com.orador.refilltrack.model.Order
import com.orador.refilltrack.model.OrderStatus
import com.orador.refilltrack.model.Role

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var adapter: OrderAdapter
    private var showingActive = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = Session.currentUser
        if (user == null || user.role != Role.STAFF) {
            Session.logout(this)
            return
        }

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvWelcome.text = "RefillTrack - " + user.name

        adapter = OrderAdapter(emptyList()) { order -> openDetails(order) }
        binding.rvOrders.layoutManager = LinearLayoutManager(this)
        binding.rvOrders.adapter = adapter

        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Active"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("History"))
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                showingActive = (tab?.position == 0)
                refreshList()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Search as the staff types
        binding.etSearch.addTextChangedListener { refreshList() }

        binding.btnLogout.setOnClickListener { Session.logout(this) }
        binding.btnAddOrder.setOnClickListener {
            startActivity(Intent(this, AddOrderActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) refreshList()
    }

    private fun refreshList() {
        val base = if (showingActive) OrderRepository.getActive() else OrderRepository.getHistory()
        val query = binding.etSearch.text.toString().trim()
        val shown = if (query.isEmpty()) {
            base
        } else {
            base.filter { it.customerName.contains(query, ignoreCase = true) }
        }

        adapter.updateList(shown)
        binding.tvEmpty.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
        updateSummary()
    }

    private fun updateSummary() {
        binding.tvCountActive.text = OrderRepository.getActive().size.toString()
        binding.tvCountPreparing.text =
            OrderRepository.countByStatus(OrderStatus.PREPARING).toString()
        binding.tvCountOnRoad.text =
            OrderRepository.countByStatus(OrderStatus.OUT_FOR_DELIVERY).toString()
        binding.tvCountDone.text =
            OrderRepository.countByStatus(OrderStatus.DELIVERED).toString()
    }

    private fun openDetails(order: Order) {
        startActivity(OrderDetailsActivity.newIntent(this, order))
    }
}
