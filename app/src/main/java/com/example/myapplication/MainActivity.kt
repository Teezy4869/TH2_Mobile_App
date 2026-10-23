package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.myapplication.data.SessionManager
import com.example.myapplication.ui.FruitShopApp
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fresh launch (not a rotation): drop the session unless "Remember me" was ticked.
        if (savedInstanceState == null) SessionManager(this).clearIfNotRemembered()
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FruitShopApp()
            }
        }
    }
}
