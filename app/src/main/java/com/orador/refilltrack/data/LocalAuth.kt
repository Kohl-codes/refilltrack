package com.orador.refilltrack.data

import com.orador.refilltrack.model.Role
import com.orador.refilltrack.model.User

//local hard coded accounts (no firebase)

object LocalAuth{
    private data class Account(val password: String, val user: User)

    private val accounts = listOf(
        Account("staff123",User("u_staff1","Ada Wung", "staff@refilltrack.com", Role.STAFF)),
        Account("rider123",User("u_rider1","Leo Kennedy", "rider1@refilltrack.com", Role.RIDER)),
        Account("rider123",User("u_rider2","Chris Bluefield", "rider2@refilltrack.com", Role.RIDER))
    )

    fun login(email: String, password: String): User? {
        return accounts.firstOrNull {
            it.user.email.equals(email.trim(), ignoreCase = true) && it.password == password
        }?.user
    }

    fun getRiders(): List<User> =
        accounts.map { it.user }.filter { it.role == Role.RIDER }

    fun getRiderName(uid: String): String =
        accounts.firstOrNull { it.user.uid == uid }?.user?.name ?: "Unassigned"

}