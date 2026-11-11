package com.orador.refilltrack.model

data class Order (
    val id: String,
    val customerName: String,
    val phone: String,
    val address: String,
    val gallons: Int,
    val status: String,
    val riderId: String = ""
)