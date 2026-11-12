package com.orador.refilltrack.ui

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.orador.refilltrack.data.LocalAuth
import com.orador.refilltrack.data.OrderRepository
import com.orador.refilltrack.data.Session
import com.orador.refilltrack.databinding.ActivityOrderDetailsBinding
import com.orador.refilltrack.model.Order
import com.orador.refilltrack.model.OrderStatus
import com.orador.refilltrack.model.Role

class OrderDetailsActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_ID = "extra_id"
        private const val EXTRA_NAME = "extra_name"
        private const val EXTRA_PHONE = "extra_phone"
        private const val EXTRA_ADDRESS = "extra_address"
        private const val EXTRA_GALLONS = "extra_gallons"
        private const val EXTRA_STATUS = "extra_status"
        private const val EXTRA_RIDER_ID = "extra_rider_id"

        // Data passing: build the Intent that carries the selected order
        fun newIntent(context: Context, order: Order): Intent {
            val intent = Intent(context, OrderDetailsActivity::class.java)
            intent.putExtra(EXTRA_ID, order.id)
            intent.putExtra(EXTRA_NAME, order.customerName)
            intent.putExtra(EXTRA_PHONE, order.phone)
            intent.putExtra(EXTRA_ADDRESS, order.address)
            intent.putExtra(EXTRA_GALLONS, order.gallons)
            intent.putExtra(EXTRA_STATUS, order.status)
            intent.putExtra(EXTRA_RIDER_ID, order.riderId)
            return intent
        }
    }

    private lateinit var binding: ActivityOrderDetailsBinding
    private val riders = LocalAuth.getRiders()
    private var orderId = ""
    private var status = ""
    private var riderId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // BR-10: only logged-in users may open this screen
        val user = Session.currentUser
        if (user == null) {
            Session.logout(this)
            return
        }

        binding = ActivityOrderDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Receive the data that was passed through the Intent extras
        orderId = intent.getStringExtra(EXTRA_ID) ?: ""
        val name = intent.getStringExtra(EXTRA_NAME) ?: ""
        val phone = intent.getStringExtra(EXTRA_PHONE) ?: ""
        val address = intent.getStringExtra(EXTRA_ADDRESS) ?: ""
        val gallons = intent.getIntExtra(EXTRA_GALLONS, 0)
        status = intent.getStringExtra(EXTRA_STATUS) ?: ""
        riderId = intent.getStringExtra(EXTRA_RIDER_ID) ?: ""

        showOrder(name, phone, address, gallons)
        setupRiderSpinner()
        setupButtons(user.role, user.uid)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnLogout.setOnClickListener { Session.logout(this) }
    }

    // FR-12: show the full details, current status and assigned rider
    private fun showOrder(name: String, phone: String, address: String, gallons: Int) {
        binding.tvOrderId.text = "Order $orderId"
        binding.tvCustomer.text = "Customer: $name"
        binding.tvPhone.text = "Phone: $phone"
        binding.tvAddress.text = "Address: $address"
        binding.tvGallons.text = "Gallons: $gallons"

        val riderName = if (riderId.isBlank()) "Not yet assigned" else LocalAuth.getRiderName(riderId)
        binding.tvRider.text = "Assigned rider: $riderName"

        binding.chipStatus.text = status
        binding.chipStatus.chipBackgroundColor = ColorStateList.valueOf(OrderStatus.colorOf(status))

        if (status == OrderStatus.CANCELLED) {
            binding.progressStatus.visibility = View.GONE
        } else {
            binding.progressStatus.max = OrderStatus.workflow.size
            binding.progressStatus.progress = OrderStatus.stepIndex(status) + 1
        }
    }

    private fun setupRiderSpinner() {
        val items = listOf("Select rider") + riders.map { it.name }
        binding.spRider.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
    }

    // BR-03, BR-04, BR-05: show only the buttons this role may use right now
    private fun setupButtons(role: String, uid: String) {
        binding.layoutAssign.visibility = View.GONE
        binding.btnAction.visibility = View.GONE
        binding.btnCancel.visibility = View.GONE
        binding.tvLocked.visibility = View.GONE

        // BR-05: Delivered and Cancelled orders are locked
        if (status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED) {
            binding.tvLocked.visibility = View.VISIBLE
            return
        }

        if (role == Role.STAFF) {
            if (status == OrderStatus.PENDING) {
                binding.btnAction.text = "START PREPARING"
                binding.btnAction.visibility = View.VISIBLE
                binding.btnCancel.visibility = View.VISIBLE      // BR-04: Pending only
            } else if (status == OrderStatus.PREPARING) {
                binding.btnAction.text = "SET OUT FOR DELIVERY"
                binding.btnAction.visibility = View.VISIBLE
                binding.layoutAssign.visibility = View.VISIBLE
            }
        } else if (role == Role.RIDER &&
            status == OrderStatus.OUT_FOR_DELIVERY && riderId == uid) {
            binding.btnAction.text = "MARK DELIVERED"
            binding.btnAction.visibility = View.VISIBLE
        }

        binding.btnAction.setOnClickListener { performAction(role) }
        binding.btnCancel.setOnClickListener { confirmCancel() }
    }

    // FR-06, FR-07, FR-09: move the order exactly one step forward
    private fun performAction(role: String) {
        var chosenRider: String? = null

// BR-02: a rider must be selected before Out for Delivery
        if (role == Role.STAFF && status == OrderStatus.PREPARING) {
            val position = binding.spRider.selectedItemPosition
            if (position <= 0) {
                Toast.makeText(this, "Please assign a rider first", Toast.LENGTH_SHORT).show()
                return
            }
            chosenRider = riders[position - 1].uid
        }

        val newStatus = OrderStatus.next(status)
        if (newStatus != null && OrderRepository.advanceStatus(orderId, chosenRider)) {
            Toast.makeText(this, "Order is now $newStatus", Toast.LENGTH_SHORT).show()
            finish()     // previous screen refreshes in onResume()
        } else {
            Toast.makeText(this, "This update is not allowed", Toast.LENGTH_SHORT).show()
        }
    }

    // FR-10: cancel (Pending only) after confirmation
    private fun confirmCancel() {
        AlertDialog.Builder(this)
            .setTitle("Cancel order")
            .setMessage("Cancel order $orderId? This cannot be undone.")
            .setPositiveButton("Yes, cancel") { _, _ ->
                if (OrderRepository.cancel(orderId)) {
                    Toast.makeText(this, "Order cancelled", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Only Pending orders can be cancelled", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}
