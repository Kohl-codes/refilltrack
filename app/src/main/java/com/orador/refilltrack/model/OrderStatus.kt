package com.orador.refilltrack.model

import android.graphics.Color

object OrderStatus {
    const val PENDING = "Pending"
    const val PREPARING = "Preparing"
    const val OUT_FOR_DELIVERY = "Out for Delivery"
    const val DELIVERED = "Delivered"
    const val CANCELLED = "Cancelled"

    val workflow = listOf(PENDING, PREPARING, OUT_FOR_DELIVERY, DELIVERED)
    val activeStatuses = listOf(PENDING, PREPARING, OUT_FOR_DELIVERY)
    val historyStatuses = listOf(DELIVERED, CANCELLED)

    fun next(current: String): String? {
        val index = workflow.indexOf(current)
        return if (index in 0 until workflow.lastIndex) workflow[index + 1] else null
    }

    fun stepIndex(status: String): Int = workflow.indexOf(status)
    fun colorOf(status: String): Int = when (status) {
        PENDING -> Color.parseColor("#D97706")
        PREPARING -> Color.parseColor("#2563EB")
        OUT_FOR_DELIVERY -> Color.parseColor("#7C3AED")
        DELIVERED -> Color.parseColor("#16A34A")
        CANCELLED -> Color.parseColor("#6B7280")
        else -> Color.GRAY
    }
}
