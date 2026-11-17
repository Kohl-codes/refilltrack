package com.orador.refilltrack.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.orador.refilltrack.model.Order
import com.orador.refilltrack.model.OrderStatus

// Phase 2: orders are stored in Cloud Firestore, collection "orders".
// (Phase 1 used a local list here. Update and cancel are added in Phase 3.)
object OrderRepository {

    private const val ORDERS = "orders"
    private val db = FirebaseFirestore.getInstance()

    // ------------------------------------------------------------ CREATE
    // FR-03: save a new order. The status always starts as Pending.
    fun addOrder(
        customerName: String,
        phone: String,
        address: String,
        gallons: Int,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf(
            "customerName" to customerName,
            "phone" to phone,
            "address" to address,
            "gallons" to gallons,
            "status" to OrderStatus.PENDING,
            "riderId" to "",
            "createdAt" to System.currentTimeMillis()
        )

        db.collection(ORDERS)
            .add(data)                                   // Firestore creates the document ID
            .addOnSuccessListener { ref -> onSuccess(ref.id) }
            .addOnFailureListener { e -> onError(e.message ?: "Could not save the order") }
    }

    // ------------------------------------------------------------ READ
    // FR-04: Active tab = Pending, Preparing, Out for Delivery
    fun loadActive(onResult: (List<Order>) -> Unit, onError: (String) -> Unit) {
        loadByStatuses(OrderStatus.activeStatuses, onResult, onError)
    }

    // FR-05: History tab = Delivered, Cancelled
    fun loadHistory(onResult: (List<Order>) -> Unit, onError: (String) -> Unit) {
        loadByStatuses(OrderStatus.historyStatuses, onResult, onError)
    }

    private fun loadByStatuses(
        statuses: List<String>,
        onResult: (List<Order>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection(ORDERS)
            .whereIn("status", statuses)
            .get()
            .addOnSuccessListener { snapshot ->
                val orders = snapshot.documents
                    .mapNotNull { doc -> toOrder(doc) }
                    .sortedByDescending { it.createdAt }     // newest first
                onResult(orders)
            }
            .addOnFailureListener { e -> onError(e.message ?: "Could not load orders") }
    }

    // FR-08: a rider sees only their own orders that are Out for Delivery
    fun loadForRider(
        riderId: String,
        onResult: (List<Order>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection(ORDERS)
            .whereEqualTo("riderId", riderId)
            .whereEqualTo("status", OrderStatus.OUT_FOR_DELIVERY)
            .get()
            .addOnSuccessListener { snapshot ->
                val orders = snapshot.documents
                    .mapNotNull { doc -> toOrder(doc) }
                    .sortedByDescending { it.createdAt }
                onResult(orders)
            }
            .addOnFailureListener { e -> onError(e.message ?: "Could not load deliveries") }
    }

    // Converts one Firestore document into our Order data class
    private fun toOrder(doc: DocumentSnapshot): Order? {
        val name = doc.getString("customerName") ?: return null
        return Order(
            id = doc.id,
            customerName = name,
            phone = doc.getString("phone") ?: "",
            address = doc.getString("address") ?: "",
            gallons = (doc.getLong("gallons") ?: 0L).toInt(),
            status = doc.getString("status") ?: OrderStatus.PENDING,
            riderId = doc.getString("riderId") ?: "",
            createdAt = doc.getLong("createdAt") ?: 0L
        )
    }
}
