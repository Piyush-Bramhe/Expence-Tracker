package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import com.example.ui.components.getCategoryColor
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.DateRangeFilter
import com.example.ui.viewmodel.ExpenseTrackerViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsListScreen(
    viewModel: ExpenseTrackerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val transactions by viewModel.filteredTransactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedDateRange by viewModel.selectedDateRange.collectAsState()
    val filterTypeIsIncome by viewModel.filterTypeIsIncome.collectAsState()

    // Helper statistics for filtered logs
    val filteredIncomeSum = remember(transactions) {
        transactions.filter { it.isIncome }.sumOf { it.amount }
    }
    val filteredExpenseSum = remember(transactions) {
        transactions.filter { !it.isIncome }.sumOf { it.amount }
    }

    val expenseCategories = listOf("Food", "Utilities", "Entertainment", "Transport", "Shopping", "Health", "Education", "Others")
    val incomeCategories = listOf("Salary", "Business", "Investment", "Gift", "Others")
    val allCategories = remember { (expenseCategories + incomeCategories).distinct() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = "Transactions Ledger",
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                        modifier = Modifier.testTag("ledger_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate back to main dashboard"
                        )
                    }
                },
                actions = {
                    // Export CSV Button
                    IconButton(
                        onClick = {
                            val csvData = viewModel.getTransactionsCsvString()
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, csvData)
                                type = "text/csv"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Export SpendWise Rows"))
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("ledger_export_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share or export transactions as CSV spreadsheet format"
                        )
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Search Input text field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                label = { Text("Search transactions...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Input matching tag"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search query")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("ledger_search_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // Income / Expense/ Both type selectors row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ALL option
                FilterChip(
                    selected = filterTypeIsIncome == null,
                    onClick = { viewModel.filterTypeIsIncome.value = null },
                    label = { Text("All Ledger") },
                    shape = RoundedCornerShape(10.dp)
                )

                // ONLY EXPENSES option
                FilterChip(
                    selected = filterTypeIsIncome == false,
                    onClick = { viewModel.filterTypeIsIncome.value = false },
                    label = { Text("Expenses") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Expenses filter icon indicator",
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    shape = RoundedCornerShape(10.dp)
                )

                // ONLY INCOMES option
                FilterChip(
                    selected = filterTypeIsIncome == true,
                    onClick = { viewModel.filterTypeIsIncome.value = true },
                    label = { Text("Income") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Income filter icon indicator",
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Date Filters Selection horizontal list
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedDateRange == DateRangeFilter.ALL,
                        onClick = { viewModel.selectedDateRange.value = DateRangeFilter.ALL },
                        label = { Text("All Dates") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = selectedDateRange == DateRangeFilter.TODAY,
                        onClick = { viewModel.selectedDateRange.value = DateRangeFilter.TODAY },
                        label = { Text("Today") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = selectedDateRange == DateRangeFilter.THIS_WEEK,
                        onClick = { viewModel.selectedDateRange.value = DateRangeFilter.THIS_WEEK },
                        label = { Text("This Week") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = selectedDateRange == DateRangeFilter.THIS_MONTH,
                        onClick = { viewModel.selectedDateRange.value = DateRangeFilter.THIS_MONTH },
                        label = { Text("This Month") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Category Filter pills horizontal scroll row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { viewModel.selectedCategory.value = null },
                        label = { Text("All Categories") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                items(allCategories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { viewModel.selectedCategory.value = category },
                        label = { Text(category) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Computed Filter indicators block
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${transactions.size} matching records found",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "+ $%.2f", filteredIncomeSum),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = String.format(Locale.US, "- $%.2f", filteredExpenseSum),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Reset Filters shortcut textbutton
                    if (searchQuery.isNotEmpty() || selectedCategory != null || selectedDateRange != DateRangeFilter.ALL || filterTypeIsIncome != null) {
                        TextButton(
                            onClick = {
                                viewModel.searchQuery.value = ""
                                viewModel.selectedCategory.value = null
                                viewModel.selectedDateRange.value = DateRangeFilter.ALL
                                viewModel.filterTypeIsIncome.value = null
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset filter terms", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Ledger Rows List view section
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterListOff,
                            contentDescription = "Search filters did not match any local storage rows graphic",
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No matching transactions found",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Adjust your search terms, date intervals or category criteria to discover your data registry.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(transactions) { transaction ->
                        LedgerItemRow(
                            transaction = transaction,
                            onEdit = { viewModel.navigateToEdit(transaction) },
                            onDelete = { viewModel.deleteTransaction(transaction) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

// Compact row specifically for the scroll ledger list
@Composable
fun LedgerItemRow(
    transaction: Transaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(getCategoryColor(transaction.category).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIconVector(transaction.category),
                    contentDescription = null,
                    tint = getCategoryColor(transaction.category),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = transaction.category,
                    fontSize = 11.sp,
                    color = getCategoryColor(transaction.category),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = String.format(Locale.US, "${if (transaction.isIncome) "+" else "-"} $%.2f", transaction.amount),
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = if (transaction.isIncome) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete entry direct shortcut",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
