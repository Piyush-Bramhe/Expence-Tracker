package com.example.data.dao

import androidx.room.*
import com.example.data.model.User
import com.example.data.model.Transaction
import com.example.data.model.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE emailOrUserId = :emailOrUserId LIMIT 1")
    suspend fun getUserByEmail(emailOrUserId: String): User?

    @Query("SELECT * FROM users WHERE currentSessionToken IS NOT NULL LIMIT 1")
    suspend fun getActiveSessionUser(): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("UPDATE users SET currentSessionToken = NULL")
    suspend fun clearAllSessions()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY dateTimestamp DESC")
    fun getTransactionsForUser(userId: String): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun deleteAllTransactionsForUser(userId: String)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE monthYear = :monthYear AND userId = :userId LIMIT 1")
    fun getBudgetForMonth(monthYear: String, userId: String): Flow<Budget?>

    @Query("SELECT * FROM budgets WHERE userId = :userId")
    fun getAllBudgets(userId: String): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget)
}
