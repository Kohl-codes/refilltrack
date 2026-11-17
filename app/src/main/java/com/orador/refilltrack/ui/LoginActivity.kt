package com.orador.refilltrack.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.orador.refilltrack.data.AuthRepository
import com.orador.refilltrack.data.Session
import com.orador.refilltrack.databinding.ActivityLoginBinding
import com.orador.refilltrack.model.Role
import com.orador.refilltrack.model.User

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AuthRepository.logout()
        Session.currentUser = null

        binding.btnLogin.setOnClickListener { attemptLogin() }
    }

    private fun attemptLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            binding.tvMessage.text = "Please fill in all fields"
            return
        }

        binding.tvMessage.text = ""
        setLoading(true)

        AuthRepository.login(
            email,
            password,
            onSuccess = { user -> openHome(user) },
            onError = { message ->
                setLoading(false)
                binding.tvMessage.text = message
            }
        )
    }

private fun openHome(user: User) {
    Session.currentUser = user

    val target = if (user.role == Role.STAFF) {
        DashboardActivity::class.java
    } else {
        MyDeliveriesActivity::class.java
    }
    startActivity(Intent(this, target))
    finish()
}

    private fun setLoading(loading: Boolean) {
        binding.btnLogin.isEnabled = !loading
        binding.btnLogin.text = if (loading) "Signing in..." else "Log in"
    }
}
