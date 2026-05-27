package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ExpenseTrackerRepository
import com.example.data.model.Budget
import com.example.data.model.Transaction
import com.example.data.model.User
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class AppScreen {
    AUTH,
    DASHBOARD,
    TRANSACTIONS_LIST,
    ADD_TRANSACTION,
    PROFILE
}

enum class DateRangeFilter {
    ALL,
    TODAY,
    THIS_WEEK,
    THIS_MONTH
}

class ExpenseTrackerViewModel(private val repository: ExpenseTrackerRepository) : ViewModel() {

    // Global navigation & routing state
    private val _currentScreen = MutableStateFlow(AppScreen.AUTH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Transaction detail view helper (null implies "Add Mode")
    private val _editingTransaction = MutableStateFlow<Transaction?>(null)
    val editingTransaction: StateFlow<Transaction?> = _editingTransaction.asStateFlow()

    // Auth screen sub-states
    private val _isLoginMode = MutableStateFlow(true)
    val isLoginMode: StateFlow<Boolean> = _isLoginMode.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authSuccessMessage = MutableStateFlow<String?>(null)
    val authSuccessMessage: StateFlow<String?> = _authSuccessMessage.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    // Active logged-in user
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Filter states
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<String?>(null)
    val selectedDateRange = MutableStateFlow(DateRangeFilter.ALL)
    val filterTypeIsIncome = MutableStateFlow<Boolean?>(null) // null = All, true = Income, false = Expense

    // Initial session checklist
    init {
        checkActiveSession()
    }

    private fun checkActiveSession() {
        viewModelScope.launch {
            _authLoading.value = true
            val activeUser = repository.getActiveSessionUser()
            if (activeUser != null) {
                _currentUser.value = activeUser
                _currentScreen.value = AppScreen.DASHBOARD
            } else {
                _currentScreen.value = AppScreen.AUTH
            }
            _authLoading.value = false
        }
    }

    // Reactive Transactions Source (responds immediately to active user updates)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<Transaction>> = currentUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.getTransactions(user.emailOrUserId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All registered budgets for active user
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val budgets: StateFlow<List<Budget>> = currentUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.getAllBudgets(user.emailOrUserId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reactive Filtered Transactions Source
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        transactions,
        searchQuery,
        selectedCategory,
        selectedDateRange,
        filterTypeIsIncome
    ) { txList, query, category, dateRange, isIncomeFilter ->
        txList.filter { tx ->
            val matchesQuery = query.isBlank() || 
                    tx.title.contains(query, ignoreCase = true) || 
                    tx.notes.contains(query, ignoreCase = true)
            
            val matchesCategory = category == null || tx.category == category
            val matchesType = isIncomeFilter == null || tx.isIncome == isIncomeFilter
            
            val matchesDate = when (dateRange) {
                DateRangeFilter.ALL -> true
                DateRangeFilter.TODAY -> isTimestampToday(tx.dateTimestamp)
                DateRangeFilter.THIS_WEEK -> isTimestampThisWeek(tx.dateTimestamp)
                DateRangeFilter.THIS_MONTH -> isTimestampThisMonth(tx.dateTimestamp)
            }
            
            matchesQuery && matchesCategory && matchesType && matchesDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Analytics details computed reactively
    val totalIncome: StateFlow<Double> = transactions
        .map { txList -> txList.filter { it.isIncome }.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = transactions
        .map { txList -> txList.filter { !it.isIncome }.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val remainingBalance: StateFlow<Double> = combine(totalIncome, totalExpense) { income, expense ->
        income - expense
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Current Month Budget Goal details
    val currentMonthYearString: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
            return sdf.format(Date())
        }

    val currentMonthBudgetGoal: StateFlow<Double> = combine(budgets, currentUser) { budgetList, user ->
        if (user == null) return@combine 0.0
        val monthStr = currentMonthYearString
        budgetList.find { it.monthYear == monthStr }?.amount ?: 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentMonthExpenses: StateFlow<Double> = transactions
        .map { txList ->
            txList.filter { !it.isIncome && isTimestampThisMonth(it.dateTimestamp) }.sumOf { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val budgetStatusMessage: StateFlow<String?> = combine(
        currentMonthBudgetGoal,
        currentMonthExpenses
    ) { budget, expenses ->
        if (budget > 0.0) {
            when {
                expenses > budget -> "Alert: You have exceeded your monthly budget goal of $${String.format(Locale.US, "%.2f", budget)}! (Spent: $${String.format(Locale.US, "%.2f", expenses)})"
                expenses >= budget * 0.85 -> "Warning: You have used up ${String.format(Locale.US, "%.0f", (expenses/budget)*100)}% of your monthly budget of $${String.format(Locale.US, "%.2f", budget)}."
                else -> null
            }
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Actions & Route operations
    fun navigateTo(screen: AppScreen) {
        if (screen != AppScreen.ADD_TRANSACTION) {
            _editingTransaction.value = null
        }
        _currentScreen.value = screen
    }

    fun navigateToEdit(transaction: Transaction) {
        _editingTransaction.value = transaction
        _currentScreen.value = AppScreen.ADD_TRANSACTION
    }

    fun toggleLoginSignupMode() {
        _isLoginMode.value = !_isLoginMode.value
        _authError.value = null
        _authSuccessMessage.value = null
    }

    // Authentications
    fun performAuth(emailOrId: String, pass: String, fullName: String = "") {
        if (emailOrId.isBlank() || pass.isBlank()) {
            _authError.value = "Username or password cannot be blank."
            return
        }
        if (!_isLoginMode.value && fullName.isBlank()) {
            _authError.value = "Please provide your full name."
            return
        }

        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            _authSuccessMessage.value = null

            // Clean simple hash logic for simulation
            val passHash = hashPassword(pass)

            if (_isLoginMode.value) {
                // Log in
                val user = repository.login(emailOrId, passHash)
                if (user != null) {
                    _currentUser.value = user
                    _currentScreen.value = AppScreen.DASHBOARD
                } else {
                    _authError.value = "Invalid username/email or password."
                }
            } else {
                // Sign up
                val success = repository.signup(emailOrId, passHash, fullName)
                if (success) {
                    _authSuccessMessage.value = "Registration successful! You may now login."
                    _isLoginMode.value = true
                } else {
                    _authError.value = "User ID / Email is already registered."
                }
            }
            _authLoading.value = false
        }
    }

    fun performResetPassword(emailOrId: String, newPass: String) {
        if (emailOrId.isBlank() || newPass.isBlank()) {
            _authError.value = "Please complete all fields to reset password."
            return
        }
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            val existing = repository.getUserByEmailOrId(emailOrId)
            if (existing != null) {
                val updated = existing.copy(passwordHash = hashPassword(newPass))
                repository.updateUser(updated)
                _authSuccessMessage.value = "Password reset successfully! Please login."
                _isLoginMode.value = true
            } else {
                _authError.value = "User ID / Email not found in our matching records."
            }
            _authLoading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _currentUser.value = null
            _currentScreen.value = AppScreen.AUTH
        }
    }

    fun toggleDarkModeOverride() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updatedUser = user.copy(isDarkMode = !user.isDarkMode)
            repository.updateUser(updatedUser)
            _currentUser.value = updatedUser
        }
    }

    // Budget setting
    fun updateMonthlyBudget(amount: Double) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.saveBudget(
                Budget(
                    monthYear = currentMonthYearString,
                    userId = user.emailOrUserId,
                    amount = amount
                )
            )
        }
    }

    // Transactions edits / additions / deletions
    fun saveTransaction(title: String, amount: Double, category: String, isIncome: Boolean, dateOffsetDays: Int, notes: String) {
        val user = _currentUser.value ?: return
        if (title.isBlank() || amount <= 0.0) return

        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -dateOffsetDays)
            val timestamp = calendar.timeInMillis

            val currentEdit = _editingTransaction.value
            val tx = if (currentEdit != null) {
                currentEdit.copy(
                    title = title,
                    amount = amount,
                    category = category,
                    isIncome = isIncome,
                    dateTimestamp = timestamp,
                    notes = notes
                )
            } else {
                Transaction(
                    userId = user.emailOrUserId,
                    title = title,
                    amount = amount,
                    category = category,
                    dateTimestamp = timestamp,
                    isIncome = isIncome,
                    notes = notes
                )
            }

            repository.saveTransaction(tx)
            _editingTransaction.value = null
            _currentScreen.value = AppScreen.DASHBOARD
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction.id)
            if (_editingTransaction.value?.id == transaction.id) {
                _editingTransaction.value = null
            }
        }
    }

    // Export Logic (returns beautiful clean CSV content)
    fun getTransactionsCsvString(): String {
        val list = transactions.value
        val sb = StringBuilder()
        sb.append("ID,Type,Title,Amount,Category,Date,Notes\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        for (tx in list) {
            val typeStr = if (tx.isIncome) "Income" else "Expense"
            val formattedDate = sdf.format(Date(tx.dateTimestamp))
            val safeTitle = tx.title.replace("\"", "\"\"")
            val safeNotes = tx.notes.replace("\"", "\"\"")
            sb.append("${tx.id},\"$typeStr\",\"$safeTitle\",${tx.amount},\"${tx.category}\",\"$formattedDate\",\"$safeNotes\"\n")
        }
        return sb.toString()
    }

    // Helper secure password hashing simulation
    private fun hashPassword(raw: String): String {
        return raw.fold(0L) { acc, c -> acc * 31 + c.code }.toString(16)
    }

    // Calendar checks for date filters
    private fun isTimestampToday(ts: Long): Boolean {
        val today = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = ts }
        return today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
               today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    }

    private fun isTimestampThisWeek(ts: Long): Boolean {
        val today = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = ts }
        return today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
               today.get(Calendar.WEEK_OF_YEAR) == target.get(Calendar.WEEK_OF_YEAR)
    }

    private fun isTimestampThisMonth(ts: Long): Boolean {
        val today = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = ts }
        return today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
               today.get(Calendar.MONTH) == target.get(Calendar.MONTH)
    }
}

class ExpenseTrackerViewModelFactory(private val repository: ExpenseTrackerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseTrackerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseTrackerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
