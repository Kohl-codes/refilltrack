package com.orador.refilltrack.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
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

    // The two lists loaded from Firestore
    private var activeOrders: List<Order> = emptyList()
    private var historyOrders: List<Order> = emptyList()
    private var loadFailed = false

    // FR-11: the status chosen in the filter (null = all statuses)
    private var selectedStatus: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // BR-10 + BR-03: only a logged-in Staff member may open this screen
        val user = Session.currentUser
        if (user == null || user.role != Role.STAFF) {
            Session.logout(this)
            return
        }

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvWelcome.text = "RefillTrack - " + user.name

        // RecyclerView + adapter
        adapter = OrderAdapter(emptyList()) { order -> openDetails(order) }
        binding.rvOrders.layoutManager = LinearLayoutManager(this)
        binding.rvOrders.adapter = adapter

        // Status filter (FR-11)
        binding.spStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val options = currentStatuses()
                selectedStatus = if (position == 0) null else options[position - 1]
                refreshList()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        setupStatusSpinner()

        // Active | History tabs
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Active"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("History"))
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                showingActive = (tab?.position == 0)
                setupStatusSpinner()      // each tab has its own statuses
                refreshList()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Search as the staff types (filters the lists already loaded)
        binding.etSearch.addTextChangedListener { refreshList() }

        binding.btnLogout.setOnClickListener { Session.logout(this) }
        binding.btnAddOrder.setOnClickListener {
            startActivity(Intent(this, AddOrderActivity::class.java))
        }
    }

    // Reload from Firebase whenever we come back from Add Order / Order Details,
    // so every change made there appears here (refresh after changes).
    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) loadOrders()
    }

    private fun currentStatuses(): List<String> =
        if (showingActive) OrderStatus.activeStatuses else OrderStatus.historyStatuses

    private fun setupStatusSpinner() {
        val items = listOf("All statuses") + currentStatuses()
        binding.spStatus.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
        selectedStatus = null
    }

    // READ: two Firestore queries, one for Active and one for History
    private fun loadOrders() {
        loadFailed = false
        binding.tvEmpty.text = "Loading orders..."
        binding.tvEmpty.visibility = View.VISIBLE

        OrderRepository.loadActive(
            onResult = { orders ->
                activeOrders = orders
                refreshList()
            },
            onError = { message -> showLoadError(message) }
        )
        OrderRepository.loadHistory(
            onResult = { orders ->
                historyOrders = orders
                refreshList()
            },
            onError = { message -> showLoadError(message) }
        )
    }

    private fun showLoadError(message: String) {
        loadFailed = true
        Toast.makeText(this, "Could not load orders: " + message, Toast.LENGTH_LONG).show()
        refreshList()
    }

    // DISPLAY: tab list, then the status filter, then the name search
    private fun refreshList() {
        val base = if (showingActive) activeOrders else historyOrders
        val byStatus = selectedStatus?.let { s -> base.filter { it.status == s } } ?: base

        val query = binding.etSearch.text.toString().trim()
        val shown = if (query.isEmpty()) {
            byStatus
        } else {
            byStatus.filter { it.customerName.contains(query, ignoreCase = true) }
        }

        adapter.updateList(shown)
        binding.tvEmpty.text = when {
            loadFailed -> "Could not load orders. Check your connection."
            query.isNotEmpty() || selectedStatus != null -> "No orders match your search or filter."
            else -> "No orders to show."
        }
        binding.tvEmpty.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
        updateSummary()
    }

    private fun updateSummary() {
        binding.tvCountActive.text = activeOrders.size.toString()
        binding.tvCountPreparing.text =
            activeOrders.count { it.status == OrderStatus.PREPARING }.toString()
        binding.tvCountOnRoad.text =
            activeOrders.count { it.status == OrderStatus.OUT_FOR_DELIVERY }.toString()
        binding.tvCountDone.text =
            historyOrders.count { it.status == OrderStatus.DELIVERED }.toString()
    }

    // Data passing: the selected order (including its document ID) travels in the Intent
    private fun openDetails(order: Order) {
        startActivity(OrderDetailsActivity.newIntent(this, order))
    }
}
