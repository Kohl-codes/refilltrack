package com.orador.refilltrack.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.orador.refilltrack.data.LocalAuth
import com.orador.refilltrack.data.Session
import com.orador.refilltrack.databinding.ActivityLoginBinding
import com.orador.refilltrack.model.Role

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener { attemptLogin() }
    }

    private fun attemptLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        // BR-09: both fields are required
        if (email.isEmpty() || password.isEmpty()) {
            binding.tvMessage.text = "Please fill in all fields"
            return
        }
        val user = LocalAuth.login(email, password)
        if (user == null) {
            binding.tvMessage.text = "Invalid email or password"
            return
        }

        Session.currentUser = user
        binding.tvMessage.text = ""


        val target = if (user.role == Role.STAFF) {
            DashboardActivity::class.java
        } else {
            MyDeliveriesActivity::class.java
        }
        startActivity(Intent(this, target))
        finish()     // remove Login from the back stack
    }
}


