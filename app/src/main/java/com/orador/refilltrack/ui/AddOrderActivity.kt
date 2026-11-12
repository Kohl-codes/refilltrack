package com.orador.refilltrack.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.orador.refilltrack.data.OrderRepository
import com.orador.refilltrack.data.Session
import com.orador.refilltrack.databinding.ActivityAddOrderBinding
import com.orador.refilltrack.model.Role

class AddOrderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddOrderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = Session.currentUser
        if (user == null || user.role != Role.STAFF) {
            Session.logout(this)
            return
        }

        binding = ActivityAddOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnLogout.setOnClickListener { Session.logout(this) }
        binding.btnSave.setOnClickListener { saveOrder() }
    }

    private fun saveOrder() {
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val gallonsText = binding.etGallons.text.toString().trim()
        val gallons = gallonsText.toIntOrNull()
        var isValid = true

        binding.tilName.error = null
        binding.tilPhone.error = null
        binding.tilAddress.error = null
        binding.tilGallons.error = null

        if (name.isEmpty()) {
            binding.tilName.error = "Customer name is required"
            isValid = false
        }

        if (phone.isEmpty()) {
            binding.tilPhone.error = "Phone number is required"
            isValid = false
        } else if (phone.length != 11 || !phone.all { it.isDigit() }) {
            binding.tilPhone.error = "Phone must be 11 digits"
            isValid = false
        }

        if (address.isEmpty()) {
            binding.tilAddress.error = "Address is required"
            isValid = false
        }

        if (gallonsText.isEmpty()) {
            binding.tilGallons.error = "Number of gallons is required"
            isValid = false
        } else if (gallons == null || gallons !in 1..50) {
            binding.tilGallons.error = "Gallons must be a whole number from 1 to 50"
            isValid = false
        }

        if (!isValid || gallons == null) {
            binding.tvMessage.text = "Please correct the highlighted fields."
            return
        }

        binding.tvMessage.text = ""
        val order = OrderRepository.add(name, phone, address, gallons)
        Toast.makeText(this, "Order " + order.id + " saved as Pending", Toast.LENGTH_SHORT).show()
        finish()
    }
}
