package com.orador.refilltrack.model

object Role {
    const val STAFF = "staff"
    const val RIDER = "rider"
}

//User Roles and login
data class User (
    val uid: String,
    val name:String,
    val email:String,
    val role: String
)