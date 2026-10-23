package com.example.myapplication.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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

        is Screen.Admin -> AdminScreen(db = db, username = s.username, onLogout = logout)
        is Screen.CustomerHome -> CustomerScreen(db = db, customer = s.customer, onLogout = logout)
    }
}
