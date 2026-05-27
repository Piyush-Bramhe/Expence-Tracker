package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.getCategoryColor
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ExpenseTrackerViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    viewModel: ExpenseTrackerViewModel,
    modifier: Modifier = Modifier
) {
    val editingTx by viewModel.editingTransaction.collectAsState()

    var isIncome by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var dateOffsetDays by remember { mutableStateOf(0) } // 0 = Today, 1 = Yesterday, etc.

    val expenseCategories = listOf("Food", "Utilities", "Entertainment", "Transport", "Shopping", "Health", "Education", "Others")
    val incomeCategories = listOf("Salary", "Business", "Investment", "Gift", "Others")

    // Populate values if we are in Edit mode
    LaunchedEffect(editingTx) {
        val tx = editingTx
        if (tx != null) {
            isIncome = tx.isIncome
            title = tx.title
            amountStr = tx.amount.toString()
            notes = tx.notes
            selectedCategory = tx.category
            
            // Calculate date offset days back
            val diffMs = System.currentTimeMillis() - tx.dateTimestamp
            val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
            dateOffsetDays = diffDays.coerceIn(0, 7)
        } else {
            isIncome = false
            title = ""
            amountStr = ""
            notes = ""
            selectedCategory = "Food"
            dateOffsetDays = 0
        }
    }

    // Dynamic categories list responding immediately to active state changes
    val activeCategories = if (isIncome) incomeCategories else expenseCategories

    // Auto-adjust default category when type flips
    LaunchedEffect(isIncome) {
        if (!activeCategories.contains(selectedCategory)) {
            selectedCategory = activeCategories.first()
        }
    }

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = if (editingTx != null) "Edit Transaction" else "Add Transaction",
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                        modifier = Modifier.testTag("add_tx_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate back to core dashboard"
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
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Transaction Type Selector Block
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Expense toggle button
                    Button(
                        onClick = { isIncome = false },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("select_expense_type"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isIncome) MaterialTheme.colorScheme.error else Color.Transparent,
                            contentColor = if (!isIncome) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Expense", fontWeight = FontWeight.Bold)
                    }

                    // Income toggle button
                    Button(
                        onClick = { isIncome = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("select_income_type"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isIncome) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (isIncome) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Income", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Input Fields Card panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Numeric entry for Amount
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Amount") },
                        placeholder = { Text("0.00") },
                        prefix = { Text("$ ", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("amount_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            focusedLabelColor = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    )

                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Transaction Title") },
                        placeholder = { Text("e.g. Weekly Groceries") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("title_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Optional Notes
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Optional Notes") },
                        placeholder = { Text("Add payment details, stores, etc.") },
                        leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("notes_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Date picker pills section
            Text(
                text = "Transaction Date",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Today Pill
                    FilterChip(
                        selected = dateOffsetDays == 0,
                        onClick = { dateOffsetDays = 0 },
                        label = { Text("Today") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Yesterday Pill
                    FilterChip(
                        selected = dateOffsetDays == 1,
                        onClick = { dateOffsetDays = 1 },
                        label = { Text("Yesterday") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // 2 Days Ago
                    FilterChip(
                        selected = dateOffsetDays == 2,
                        onClick = { dateOffsetDays = 2 },
                        label = { Text("2 Days Ago") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Categories visual Grid Layout
            Text(
                text = "Select Category",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Custom category selector (uses high performance vertical scroll grid inside scroll block gracefully)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Standard 2-column grid layout for ergonomic tap targets
                    activeCategories.chunked(2).forEach { rowCategories ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowCategories.forEach { category ->
                                val isSelected = selectedCategory == category
                                val themeAccentColor = getCategoryColor(category)
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) themeAccentColor.copy(alpha = 0.15f) else Color.Transparent
                                        )
                                        .clickable { selectedCategory = category }
                                        .testTag("category_pill_$category")
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isSelected) themeAccentColor else themeAccentColor.copy(alpha = 0.1f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = getCategoryIconVector(category),
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else themeAccentColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = category,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) themeAccentColor else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Save Transaction button trigger
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0.0 && selectedCategory.isNotBlank()) {
                        viewModel.saveTransaction(title, amt, selectedCategory, isIncome, dateOffsetDays, notes)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(52.dp)
                    .testTag("save_transaction_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                ),
                enabled = title.isNotBlank() && (amountStr.toDoubleOrNull() ?: 0.0) > 0.0
            ) {
                Text(
                    text = if (editingTx != null) "Update transaction Entry" else "Confirm & Save Entry",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
