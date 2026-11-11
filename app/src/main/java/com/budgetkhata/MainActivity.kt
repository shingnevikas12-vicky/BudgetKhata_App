package com.budgetkhata

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.budgetkhata.ui.BudgetKhataRoot

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as BudgetKhataApp).container
        setContent {
            BudgetKhataRoot(container = container)
        }
    }
}
