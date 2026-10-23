@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myapplication.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.Customer
import com.example.myapplication.data.Fruit
import com.example.myapplication.data.FruitShopDb

@Composable
fun CustomerScreen(db: FruitShopDb, customer: Customer, onLogout: () -> Unit) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var refresh by remember { mutableIntStateOf(0) }
    var ordering by remember { mutableStateOf<Fruit?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Fruit Shop")
                        Text(
                            "Hi, ${customer.fullname}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = { TextButton(onClick = onLogout) { Text("Logout") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Shop") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("My orders") })
            }
            if (tab == 0) {
                ShopTab(db, refresh, onOrder = { ordering = it })
            } else {
                MyOrdersTab(db, customer.id, refresh)
            }
        }
    }

    ordering?.let { fruit ->
        OrderDialog(
            fruit = fruit,
            onDismiss = { ordering = null },
            onConfirm = { quantity ->
                val error = db.placeOrder(customer.id, fruit.id, quantity)
                if (error == null) {
                    Toast.makeText(context, "Order placed: ${fruit.name} × $quantity", Toast.LENGTH_SHORT).show()
                    ordering = null
                    refresh++
                }
                error
            }
        )
    }
}

@Composable
private fun ShopTab(db: FruitShopDb, refresh: Int, onOrder: (Fruit) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var hideExpired by rememberSaveable { mutableStateOf(false) }
    val fruits = remember(query, refresh) { db.getFruits(query, inStockOnly = true) }
    val shown = if (hideExpired) fruits.filterNot { it.isExpired } else fruits

    Column(Modifier.fillMaxSize()) {
        SearchField(query, { query = it }, "Search fruits or categories")
        Row(Modifier.padding(horizontal = 16.dp)) {
            FilterChip(
                selected = hideExpired,
                onClick = { hideExpired = !hideExpired },
                label = { Text("Hide expired") }
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (shown.isEmpty()) item { EmptyMessage("No fruits available") }
            items(shown, key = { it.id }) { fruit ->
                FruitCard(fruit) {
                    if (fruit.isExpired) {
                        Text(
                            "Expired — cannot be ordered",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        Button(onClick = { onOrder(fruit) }) { Text("Order") }
                    }
                }
            }
        }
    }
}

/** [onConfirm] returns an error message, or null when the order went through. */
@Composable
private fun OrderDialog(fruit: Fruit, onDismiss: () -> Unit, onConfirm: (Int) -> String?) {
    var quantityText by rememberSaveable { mutableStateOf("1") }
    var error by remember { mutableStateOf<String?>(null) }
    val quantity = quantityText.trim().toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Order ${fruit.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Unit price: ${formatPrice(fruit.price)} · In stock: ${fruit.quantity}")
                ExpiryPill(fruit.expiryDate)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { quantityText = ((quantity ?: 1) - 1).coerceAtLeast(1).toString() }) { Text("−") }
                    FormField(
                        "Quantity",
                        quantityText,
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Number
                    ) { quantityText = it; error = null }
                    OutlinedButton(onClick = { quantityText = ((quantity ?: 0) + 1).coerceAtMost(fruit.quantity).toString() }) { Text("+") }
                }
                Text(
                    "Total: ${formatPrice(fruit.price * (quantity ?: 0))}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    quantity == null || quantity < 1 -> "Enter a quantity of at least 1"
                    quantity > fruit.quantity -> "Only ${fruit.quantity} left in stock"
                    else -> onConfirm(quantity)
                }
            }) { Text("Place order") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun MyOrdersTab(db: FruitShopDb, customerId: Long, refresh: Int) {
    val orders = remember(customerId, refresh) { db.getOrdersByCustomer(customerId) }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (orders.isEmpty()) {
            item { EmptyMessage("You haven't placed any orders yet") }
        } else {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp)) {
                        Text("${orders.size} order(s)", modifier = Modifier.weight(1f))
                        Text(
                            "Spent: ${formatPrice(orders.sumOf { it.totalAmount })}",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        items(orders, key = { it.id }) { order -> OrderCard(order, showCustomer = false) }
    }
}
