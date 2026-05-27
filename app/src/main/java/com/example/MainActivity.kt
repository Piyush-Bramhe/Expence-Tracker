package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ExpenseTrackerDatabase
import com.example.data.ExpenseTrackerRepository
import com.example.ui.screens.AddEditTransactionScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.TransactionsListScreen
import com.example.ui.theme.SpendWiseTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ExpenseTrackerViewModel
import com.example.ui.viewmodel.ExpenseTrackerViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize local Room persistence layer and repositories
        val database = ExpenseTrackerDatabase.getDatabase(applicationContext)
        val repository = ExpenseTrackerRepository(
            userDao = database.userDao(),
            transactionDao = database.transactionDao(),
            budgetDao = database.budgetDao()
        )
        val viewModelFactory = ExpenseTrackerViewModelFactory(repository)

        setContent {
            val viewModel: ExpenseTrackerViewModel = viewModel(factory = viewModelFactory)
            val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

            // Dynamic light/dark theme preference binding
            val useDarkTheme = currentUser?.isDarkMode ?: isSystemInDarkTheme()

            SpendWiseTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentScreen) {
                        AppScreen.AUTH -> AuthScreen(viewModel = viewModel)
                        AppScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                        AppScreen.TRANSACTIONS_LIST -> TransactionsListScreen(viewModel = viewModel)
                        AppScreen.ADD_TRANSACTION -> AddEditTransactionScreen(viewModel = viewModel)
                        AppScreen.PROFILE -> ProfileScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
