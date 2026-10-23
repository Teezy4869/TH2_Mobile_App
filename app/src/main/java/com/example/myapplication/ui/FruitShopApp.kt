package com.example.myapplication.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.Customer
import com.example.myapplication.data.FruitShopDb
import com.example.myapplication.data.Role
import com.example.myapplication.data.SessionManager

private sealed interface Screen {
    data object Login : Screen
    data object Register : Screen
    data class Admin(val username: String) : Screen
    data class CustomerHome(val customer: Customer) : Screen
}

@Composable
fun FruitShopApp() {
    val context = LocalContext.current
    val db = remember { FruitShopDb.getInstance(context) }
    val session = remember { SessionManager(context) }

    fun homeScreen(): Screen {
        if (!session.isLoggedIn) return Screen.Login
        return when (session.role) {
            Role.ADMIN -> Screen.Admin(session.username)
            else -> db.getCustomerByAccountId(session.accountId)?.let { Screen.CustomerHome(it) } ?: Screen.Login
        }
    }

    var screen by remember { mutableStateOf(homeScreen()) }
    val logout = {
        session.logout()
        screen = Screen.Login
    }

    when (val s = screen) {
        Screen.Login -> LoginScreen(
            db = db,
            session = session,
            onLoggedIn = { screen = homeScreen() },
            onRegisterClick = { screen = Screen.Register }
        )

        Screen.Register -> {
            BackHandler { screen = Screen.Login }
            RegisterScreen(db = db, onDone = { screen = Screen.Login })
        }

        is Screen.Admin -> PlaceholderHome("Admin · ${s.username}", logout)
        is Screen.CustomerHome -> PlaceholderHome("Hi, ${s.customer.fullname}", logout)
    }
}

/** Temporary home screen until the Admin (round 2) and Customer (round 3) screens exist. */
@Composable
private fun PlaceholderHome(title: String, onLogout: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text("Chức năng sẽ có ở đợt sau")
            Button(onClick = onLogout) { Text("Logout") }
        }
    }
}
