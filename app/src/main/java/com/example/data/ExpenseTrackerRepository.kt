package com.example.data

import com.example.data.dao.BudgetDao
import com.example.data.dao.TransactionDao
import com.example.data.dao.UserDao
import com.example.data.model.Budget
import com.example.data.model.Transaction
import com.example.data.model.User
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ExpenseTrackerRepository(
    private val userDao: UserDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao
) {
    // Authentication Operations
    suspend fun getActiveSessionUser(): User? = userDao.getActiveSessionUser()
    
    suspend fun getUserByEmailOrId(emailOrId: String): User? = userDao.getUserByEmail(emailOrId)
    
    suspend fun signup(emailOrId: String, passwordHash: String, fullName: String): Boolean {
        val existing = userDao.getUserByEmail(emailOrId)
        if (existing != null) return false // Email / User ID already registered
        
        val newUser = User(
            emailOrUserId = emailOrId,
            passwordHash = passwordHash,
            fullName = fullName,
            currentSessionToken = UUID.randomUUID().toString()
        )
        userDao.insertUser(newUser)
        return true
    }
    
    suspend fun login(emailOrId: String, passwordHash: String): User? {
        val user = userDao.getUserByEmail(emailOrId) ?: return null
        if (user.passwordHash == passwordHash) {
            val updated = user.copy(currentSessionToken = UUID.randomUUID().toString())
            userDao.insertUser(updated)
            return updated
        }
        return null
    }
    
    suspend fun logout() {
        userDao.clearAllSessions()
    }
    
    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }

    // Transaction Management Operations
    fun getTransactions(userId: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsForUser(userId)
        
    suspend fun saveTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }
    
    suspend fun deleteTransaction(transactionId: Long) {
        transactionDao.deleteTransactionById(transactionId)
    }

    // Budget Rule Operations
    fun getBudgetForMonth(monthYear: String, userId: String): Flow<Budget?> =
        budgetDao.getBudgetForMonth(monthYear, userId)
        
    fun getAllBudgets(userId: String): Flow<List<Budget>> =
        budgetDao.getAllBudgets(userId)
        
    suspend fun saveBudget(budget: Budget) {
        budgetDao.insertBudget(budget)
    }
}
