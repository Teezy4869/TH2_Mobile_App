package com.example.myapplication.data

object Role {
    const val ADMIN = "ADMIN"
    const val CUSTOMER = "CUSTOMER"
}

object OrderStatus {
    const val PENDING = "Pending"
    const val CONFIRMED = "Confirmed"
    const val COMPLETED = "Completed"
    val ALL = listOf(PENDING, CONFIRMED, COMPLETED)
}

data class Account(
    val id: Long,
    val username: String,
    val password: String,
    val role: String
)

data class Fruit(
    val id: Long = 0,
    val name: String,
    val category: String,
    val price: Double,
    val quantity: Int,
    val expiryDate: String, // yyyy-MM-dd
    val description: String
) {
    val isExpired: Boolean get() = DateUtils.isExpired(expiryDate)
}

data class Customer(
    val id: Long,
    val fullname: String,
    val phone: String,
    val email: String,
    val accountId: Long
)

/**
 * fruitId / quantity are extra columns so an order records what was bought.
 * fruitName / customerName are filled from joins for display only.
 */
data class Order(
    val id: Long,
    val customerId: Long,
    val orderDate: String,
    val totalAmount: Double,
    val status: String,
    val fruitId: Long?,
    val quantity: Int,
    val fruitName: String = "",
    val customerName: String = ""
)
