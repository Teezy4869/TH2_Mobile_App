package com.example.myapplication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.DateUtils
import com.example.myapplication.data.Fruit
import java.util.Locale

private val Warning = Color(0xFFE65100)
private val Info = Color(0xFF1565C0)
private val Success = Color(0xFF2E7D32)

fun formatPrice(value: Double): String = String.format(Locale.US, "$%,.2f", value)

@Composable
fun SearchField(query: String, onQueryChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Text("🔍") },
        trailingIcon = if (query.isNotEmpty()) {
            { TextButton(onClick = { onQueryChange("") }) { Text("Clear") } }
        } else null,
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun Pill(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(50)) {
        Text(
            text,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** Shows the expiry date and flags expired / soon-to-expire fruits. */
@Composable
fun ExpiryPill(expiryDate: String) {
    val days = DateUtils.daysUntil(expiryDate)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val (text, color) = when {
        days == null -> "Expiry: $expiryDate" to muted
        days < 0 -> "EXPIRED · $expiryDate" to MaterialTheme.colorScheme.error
        days == 0L -> "Expires today · $expiryDate" to Warning
        days <= 3 -> "Expires in $days day${if (days == 1L) "" else "s"} · $expiryDate" to Warning
        else -> "Expiry: $expiryDate" to muted
    }
    Pill(text, color)
}

@Composable
fun FruitCard(fruit: Fruit, actions: @Composable RowScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    fruit.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    formatPrice(fruit.price),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "${fruit.category} · ${if (fruit.quantity > 0) "In stock: ${fruit.quantity}" else "Out of stock"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            ExpiryPill(fruit.expiryDate)
            if (fruit.description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(fruit.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
    }
}

@Composable
fun EmptyMessage(text: String) {
    Text(
        text,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
    )
}
