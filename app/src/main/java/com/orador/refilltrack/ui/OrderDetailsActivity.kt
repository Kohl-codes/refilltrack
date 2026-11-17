package com.orador.refilltrack.ui

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.orador.refilltrack.data.AuthRepository
import com.orador.refilltrack.data.Session
import com.orador.refilltrack.databinding.ActivityOrderDetailsBinding
import com.orador.refilltrack.model.Order
import com.orador.refilltrack.model.OrderStatus

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Session.currentUser == null) {
            Session.logout(this)
            return
        }

        binding = ActivityOrderDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Receive the data that was passed through the Intent extras
        val orderId = intent.getStringExtra(EXTRA_ID) ?: ""
        val name = intent.getStringExtra(EXTRA_NAME) ?: ""
        val phone = intent.getStringExtra(EXTRA_PHONE) ?: ""
        val address = intent.getStringExtra(EXTRA_ADDRESS) ?: ""
        val gallons = intent.getIntExtra(EXTRA_GALLONS, 0)
        val status = intent.getStringExtra(EXTRA_STATUS) ?: ""
        val riderId = intent.getStringExtra(EXTRA_RIDER_ID) ?: ""

        binding.tvOrderId.text = "Order $orderId"
        binding.tvCustomer.text = "Customer: $name"
        binding.tvPhone.text = "Phone: $phone"
        binding.tvAddress.text = "Address: $address"
        binding.tvGallons.text = "Gallons: $gallons"

        // The rider's name is read from the users collection in Firestore
        if (riderId.isBlank()) {
            binding.tvRider.text = "Assigned rider: Not yet assigned"
        } else {
            binding.tvRider.text = "Assigned rider: Loading..."
            AuthRepository.getUserName(riderId) { riderName ->
                binding.tvRider.text = "Assigned rider: $riderName"
            }
        }

        binding.chipStatus.text = status
        binding.chipStatus.chipBackgroundColor = ColorStateList.valueOf(OrderStatus.colorOf(status))
        if (status == OrderStatus.CANCELLED) {
            binding.progressStatus.visibility = View.GONE
        } else {
            binding.progressStatus.max = OrderStatus.workflow.size
            binding.progressStatus.progress = OrderStatus.stepIndex(status) + 1
        }

        // Phase 2: no action buttons yet
        binding.layoutAssign.visibility = View.GONE
        binding.btnAction.visibility = View.GONE
        binding.btnCancel.visibility = View.GONE
        binding.tvLocked.visibility = View.VISIBLE
        binding.tvLocked.text =
            if (status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED) {
                "This order is finished and locked. It cannot be edited."
            } else {
                "Status updates (prepare, assign rider, deliver, cancel) arrive in Phase 3."
            }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnLogout.setOnClickListener { Session.logout(this) }
    }
}
