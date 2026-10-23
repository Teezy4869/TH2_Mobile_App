package com.example.myapplication.ui

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.Account
import com.example.myapplication.data.FruitShopDb
import com.example.myapplication.data.SessionManager

@Composable
fun LoginScreen(
    db: FruitShopDb,
    session: SessionManager,
    onLoggedIn: (Account) -> Unit,
    onRegisterClick: () -> Unit
) {
    var username by rememberSaveable { mutableStateOf(session.rememberedUsername) }
    var password by rememberSaveable { mutableStateOf("") }
    var rememberMe by rememberSaveable { mutableStateOf(session.rememberMe) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (username.isBlank() || password.isEmpty()) {
            error = "Please enter username and password"
            return
        }
        val account = db.login(username.trim(), password)
        if (account == null) {
            error = "Invalid username or password"
        } else {
            session.saveLogin(account, rememberMe)
            onLoggedIn(account)
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🍊", style = MaterialTheme.typography.displayMedium)
            Text("Fruit Shop", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Sign in to continue", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it; error = null },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                Text("Remember me")
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = ::submit, modifier = Modifier.fillMaxWidth()) { Text("Login") }
            TextButton(onClick = onRegisterClick) { Text("New customer? Create an account") }
            Spacer(Modifier.height(16.dp))
            Text(
                "Demo accounts — Admin: admin / admin123 · Customer: customer / 123456",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RegisterScreen(db: FruitShopDb, onDone: () -> Unit) {
    val context = LocalContext.current
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var fullname by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        error = when {
            username.trim().length < 3 -> "Username must have at least 3 characters"
            password.length < 6 -> "Password must have at least 6 characters"
            password != confirm -> "Passwords do not match"
            fullname.isBlank() -> "Full name is required"
            !phone.trim().matches(Regex("\\d{9,11}")) -> "Phone must be 9–11 digits"
            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Email is not valid"
            else -> db.registerCustomer(username.trim(), password, fullname.trim(), phone.trim(), email.trim())
        }
        if (error == null) {
            Toast.makeText(context, "Account created. Please log in.", Toast.LENGTH_SHORT).show()
            onDone()
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Create customer account", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            FormField("Username", username) { username = it }
            FormField("Password", password, password = true) { password = it }
            FormField("Confirm password", confirm, password = true) { confirm = it }
            FormField("Full name", fullname) { fullname = it }
            FormField("Phone", phone, keyboardType = KeyboardType.Phone) { phone = it }
            FormField("Email", email, keyboardType = KeyboardType.Email) { email = it }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = ::submit, modifier = Modifier.fillMaxWidth()) { Text("Register") }
            TextButton(onClick = onDone, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Back to login")
            }
        }
    }
}

@Composable
fun FormField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    keyboardType: KeyboardType = if (password) KeyboardType.Password else KeyboardType.Text,
    singleLine: Boolean = true,
    trailingIcon: (@Composable () -> Unit)? = null,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = trailingIcon,
        modifier = modifier.fillMaxWidth()
    )
}
