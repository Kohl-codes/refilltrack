package com.orador.refilltrack.data

import com.orador.refilltrack.model.Order
import com.orador.refilltrack.model.OrderStatus

object OrderRepository {

    private val orders = mutableListOf(
        Order("ORD-001", "Ana Reyes", "09171234567",
            "Blk 5 Lot 2, Lahug, Cebu City", 3, OrderStatus.PENDING),
        Order("ORD-002", "Carlos Lim", "09281112233",
            "12 Mango Ave, Cebu City", 2, OrderStatus.PENDING),
        Order("ORD-003", "Dina Flores", "09393334455",
            "88 Colon St, Cebu City", 5, OrderStatus.PREPARING),
        Order("ORD-004", "Eddie Tan", "09175556677",
            "Banilad Town Centre, Cebu City", 4, OrderStatus.PREPARING),
        Order("ORD-005", "Faye Garcia", "09284445566",
            "21 Archbishop Reyes Ave, Cebu City", 2, OrderStatus.OUT_FOR_DELIVERY, "u_rider1"),
        Order("ORD-006", "Gino Ramos", "09177778899",
            "7 Gorordo Ave, Cebu City", 6, OrderStatus.OUT_FOR_DELIVERY, "u_rider2"),
        Order("ORD-007", "Hanna Cruz", "09391112244",
            "45 Escario St, Cebu City", 3, OrderStatus.DELIVERED, "u_rider1"),
        Order("ORD-008", "Ian Uy", "09283334411",
            "19 Salinas Dr, Cebu City", 1, OrderStatus.CANCELLED)
    )

    private var counter = orders.size

    fun getActive(): List<Order> = orders.filter { it.status in OrderStatus.activeStatuses }
    fun getHistory(): List<Order> = orders.filter { it.status in OrderStatus.historyStatuses }
    fun getForRider(riderId: String): List<Order> = orders.filter { it.riderId == riderId && it.status == OrderStatus.OUT_FOR_DELIVERY }
    fun countByStatus(status: String): Int = orders.count { it.status == status }

    fun add(customerName: String, phone: String, address: String, gallons: Int): Order {
        counter++
        val order = Order(
            id = "ORD-%03d".format(counter),
            customerName = customerName,
            phone = phone,
            address = address,
            gallons = gallons,
            status = OrderStatus.PENDING
        )
        orders.add(0, order)      // newest order on top
        return order
    }


    fun advanceStatus(orderId: String, riderId: String? = null): Boolean {
        val index = orders.indexOfFirst { it.id == orderId }
        if (index == -1) return false
        val current = orders[index]
        val nextStatus = OrderStatus.next(current.status) ?: return false
        val finalRider = riderId ?: current.riderId
        if (nextStatus == OrderStatus.OUT_FOR_DELIVERY && finalRider.isBlank()) return false

        orders[index] = current.copy(status = nextStatus, riderId = finalRider)
        return true
    }

    fun cancel(orderId: String): Boolean {
        val index = orders.indexOfFirst { it.id == orderId }
        if (index == -1) return false

        val current = orders[index]
        if (current.status != OrderStatus.PENDING) return false

        orders[index] = current.copy(status = OrderStatus.CANCELLED)
        return true
    }
}
