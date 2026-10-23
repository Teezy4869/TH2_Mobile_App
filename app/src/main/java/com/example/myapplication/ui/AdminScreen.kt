@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myapplication.ui

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.DateUtils
import com.example.myapplication.data.Fruit
import com.example.myapplication.data.FruitShopDb
import java.util.Calendar
import java.util.Locale

private enum class ExpiryFilter(val label: String) { ALL("All"), FRESH("Fresh"), EXPIRED("Expired") }

@Composable
fun AdminScreen(db: FruitShopDb, username: String, onLogout: () -> Unit) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var refresh by remember { mutableIntStateOf(0) }
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Fruit?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin · $username") },
                actions = { TextButton(onClick = onLogout) { Text("Logout") } }
            )
        },
        floatingActionButton = {
            if (tab == 0) {
                ExtendedFloatingActionButton(onClick = { editing = null; showForm = true }) {
                    Text("+  Add fruit")
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Fruits") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Orders") })
            }
            if (tab == 0) {
                AdminFruitsTab(
                    db = db,
                    refresh = refresh,
                    onEdit = { editing = it; showForm = true },
                    onChanged = { refresh++ }
                )
            } else {
                EmptyMessage("Quản lý đơn hàng: làm ở đợt 3")
            }
        }
    }

    if (showForm) {
        FruitFormDialog(
            initial = editing,
            onDismiss = { showForm = false },
            onSave = { fruit ->
                if (fruit.id == 0L) db.insertFruit(fruit) else db.updateFruit(fruit)
                Toast.makeText(context, "Saved ${fruit.name}", Toast.LENGTH_SHORT).show()
                showForm = false
                refresh++
            }
        )
    }
}

@Composable
private fun AdminFruitsTab(db: FruitShopDb, refresh: Int, onEdit: (Fruit) -> Unit, onChanged: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(ExpiryFilter.ALL) }
    var toDelete by remember { mutableStateOf<Fruit?>(null) }
    val fruits = remember(query, refresh) { db.getFruits(query) }
    val expiredCount = fruits.count { it.isExpired }
    val shown = when (filter) {
        ExpiryFilter.ALL -> fruits
        ExpiryFilter.FRESH -> fruits.filterNot { it.isExpired }
        ExpiryFilter.EXPIRED -> fruits.filter { it.isExpired }
    }

    Column(Modifier.fillMaxSize()) {
        SearchField(query, { query = it }, "Search by name or category")
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExpiryFilter.entries.forEach { f ->
                val count = when (f) {
                    ExpiryFilter.ALL -> fruits.size
                    ExpiryFilter.FRESH -> fruits.size - expiredCount
                    ExpiryFilter.EXPIRED -> expiredCount
                }
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text("${f.label} ($count)") }
                )
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (shown.isEmpty()) item { EmptyMessage("No fruits found") }
            items(shown, key = { it.id }) { fruit ->
                FruitCard(fruit) {
                    TextButton(onClick = { onEdit(fruit) }) { Text("Edit") }
                    TextButton(
                        onClick = { toDelete = fruit },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                }
            }
        }
    }

    toDelete?.let { fruit ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete fruit") },
            text = { Text("Delete \"${fruit.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    db.deleteFruit(fruit.id)
                    toDelete = null
                    onChanged()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun FruitFormDialog(initial: Fruit?, onDismiss: () -> Unit, onSave: (Fruit) -> Unit) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var category by rememberSaveable { mutableStateOf(initial?.category.orEmpty()) }
    var price by rememberSaveable { mutableStateOf(initial?.price?.toString().orEmpty()) }
    var quantity by rememberSaveable { mutableStateOf(initial?.quantity?.toString().orEmpty()) }
    var expiry by rememberSaveable { mutableStateOf(initial?.expiryDate ?: DateUtils.offset(7)) }
    var description by rememberSaveable { mutableStateOf(initial?.description.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    fun pickDate() {
        val cal = Calendar.getInstance()
        DateUtils.parse(expiry)?.let { cal.time = it }
        DatePickerDialog(
            context,
            { _, y, m, d -> expiry = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d) },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun save() {
        val priceValue = price.trim().toDoubleOrNull()
        val quantityValue = quantity.trim().toIntOrNull()
        error = when {
            name.isBlank() -> "Name is required"
            category.isBlank() -> "Category is required"
            priceValue == null || priceValue <= 0 -> "Price must be a number greater than 0"
            quantityValue == null || quantityValue < 0 -> "Quantity must be a whole number ≥ 0"
            !DateUtils.isValid(expiry.trim()) -> "Expiry date must be in yyyy-MM-dd format"
            else -> null
        }
        if (error == null) {
            onSave(
                Fruit(
                    id = initial?.id ?: 0,
                    name = name.trim(),
                    category = category.trim(),
                    price = priceValue!!,
                    quantity = quantityValue!!,
                    expiryDate = expiry.trim(),
                    description = description.trim()
                )
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add fruit" else "Edit fruit") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FormField("Name", name) { name = it }
                FormField("Category", category) { category = it }
                FormField("Price", price, keyboardType = KeyboardType.Decimal) { price = it }
                FormField("Quantity", quantity, keyboardType = KeyboardType.Number) { quantity = it }
                FormField(
                    "Expiry date (yyyy-MM-dd)",
                    expiry,
                    trailingIcon = { TextButton(onClick = ::pickDate) { Text("Pick") } }
                ) { expiry = it }
                FormField("Description", description, singleLine = false) { description = it }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = ::save) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
