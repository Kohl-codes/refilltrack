package com.orador.refilltrack.data

import android.app.Activity
import android.content.Intent
import com.orador.refilltrack.model.User
import com.orador.refilltrack.ui.LoginActivity


object Session {

    var currentUser: User? = null

    fun logout(activity: Activity) {
        currentUser = null
        val intent = Intent(activity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity.startActivity(intent)
        activity.finish()
    }
}
