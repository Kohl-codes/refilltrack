package com.orador.refilltrack.data

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Transaction
import com.orador.refilltrack.model.Order
import com.orador.refilltrack.model.OrderStatus
import com.orador.refilltrack.model.Role
import com.orador.refilltrack.model.User

object OrderRepository {

    private const val ORDERS = "orders"
    private val db = FirebaseFirestore.getInstance()

    // ============================================================ CREATE
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
            .addOnFailureListener { e -> onError(friendly(e)) }
    }

    // ============================================================ READ
    fun loadActive(onResult: (List<Order>) -> Unit, onError: (String) -> Unit) {
        loadByStatuses(OrderStatus.activeStatuses, onResult, onError)
    }

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
                    .sortedByDescending { it.createdAt }
                onResult(orders)
            }
            .addOnFailureListener { e -> onError(friendly(e)) }
    }


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
            .addOnFailureListener { e -> onError(friendly(e)) }
    }

    // ============================================================ UPDATE
    // onSuccess receives the new status.
    fun advanceStatus(
        orderId: String,
        expectedStatus: String,
        riderId: String?,
        actor: User,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val ref = db.collection(ORDERS).document(orderId)     // the exact record

        val task = db.runTransaction(Transaction.Function<Pair<Boolean, String>> { tx ->
            val snap = tx.get(ref)                            // read the CURRENT stored values
            val current = snap.getString("status") ?: ""
            val currentRider = snap.getString("riderId") ?: ""
            val next = OrderStatus.next(current) ?: ""
            val finalRider = if (riderId.isNullOrBlank()) currentRider else riderId

            when {
                !snap.exists() ->
                    Pair(false, "This order no longer exists")
                current != expectedStatus ->
                    Pair(false, "This order was already changed. Go back and refresh.")
                next.isEmpty() ->
                    Pair(false, "This order is locked and cannot be changed")      // BR-05
                (next == OrderStatus.PREPARING || next == OrderStatus.OUT_FOR_DELIVERY)
                        && actor.role != Role.STAFF ->
                    Pair(false, "Only Staff can do this")                           // BR-03
                next == OrderStatus.DELIVERED
                        && (actor.role != Role.RIDER || currentRider != actor.uid) ->
                    Pair(false, "Only the assigned rider can mark this order Delivered") // BR-03
                next == OrderStatus.OUT_FOR_DELIVERY && finalRider.isBlank() ->
                    Pair(false, "Please assign a rider first")                      // BR-02
                else -> {
                    tx.update(ref, mapOf("status" to next, "riderId" to finalRider))
                    Pair(true, next)
                }
            }
        })

        handle(task, onSuccess, onError)
    }

    // its status becomes Cancelled, and it moves to History (BR-04, BR-05).
    fun cancelOrder(
        orderId: String,
        expectedStatus: String,
        actor: User,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val ref = db.collection(ORDERS).document(orderId)

        val task = db.runTransaction(Transaction.Function<Pair<Boolean, String>> { tx ->
            val snap = tx.get(ref)
            val current = snap.getString("status") ?: ""

            when {
                !snap.exists() ->
                    Pair(false, "This order no longer exists")
                actor.role != Role.STAFF ->
                    Pair(false, "Only Staff can cancel an order")                   // BR-03
                current != OrderStatus.PENDING || expectedStatus != OrderStatus.PENDING ->
                    Pair(false, "Only Pending orders can be cancelled")             // BR-04
                else -> {
                    tx.update(ref, mapOf("status" to OrderStatus.CANCELLED))
                    Pair(true, OrderStatus.CANCELLED)
                }
            }
        })

        handle(task, { onSuccess() }, onError)
    }

    // ============================================================ HELPERS
    // Runs the success or error callback depending on the transaction result
    private fun handle(
        task: Task<Pair<Boolean, String>>,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        task
            .addOnSuccessListener { result ->
                if (result.first) onSuccess(result.second) else onError(result.second)
            }
            .addOnFailureListener { e -> onError(friendly(e)) }
    }

    // Converts a Firebase exception into a short message for the user
    private fun friendly(e: Exception): String {
        if (e is FirebaseFirestoreException) {
            return when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "You are not allowed to do this"
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "No internet connection"
                else -> e.message ?: "Something went wrong"
            }
        }
        return e.message ?: "Something went wrong"
    }

    // Converts one Firestore document into our Order data class.
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
