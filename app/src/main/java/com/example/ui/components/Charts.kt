package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

// Unified color resolver for Category items
fun getCategoryColor(category: String): Color {
    return when (category) {
        "Food" -> Color(0xFFF97316)          // Orange
        "Utilities" -> Color(0xFFF59E0B)     // Amber
        "Entertainment" -> Color(0xFFA855F7) // Purple
        "Transport" -> Color(0xFF3B82F6)     // Blue
        "Shopping" -> Color(0xFFEC4899)      // Pink
        "Health" -> Color(0xFFF43F5E)        // Rose
        "Education" -> Color(0xFF6366F1)     // Indigo
        "Salary" -> Color(0xFF10B981)        // Emerald
        "Business" -> Color(0xFF0D9488)      // Teal
        "Investment" -> Color(0xFF0891B2)    // Cyan
        "Gift" -> Color(0xFF4F46E5)          // Indigo-Violet
        else -> Color(0xFF6B7280)            // Cool Slate Gray for Others
    }
}

@Composable
fun TransactionDonutChart(
    categoryAmounts: Map<String, Double>,
    modifier: Modifier = Modifier,
    thickness: Dp = 22.dp
) {
    val total = categoryAmounts.values.sum()
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF1A1C1E)

    // Smoothly animate completion of the chart arc
    val sweepAngleModifier by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000),
        label = "sweep_angle"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (total == 0.0) {
            // Placeholder empty state chart
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = if (isDark) Color(0xFF1F2937) else Color(0xFFE5E7EB),
                    radius = size.minDimension / 2 - thickness.toPx(),
                    style = Stroke(width = thickness.toPx())
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$0.00",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "No Expenses",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            // Draw actual multi-slice donut chart
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartSize = size.minDimension - thickness.toPx() * 2
                val chartOffset = (size.minDimension - chartSize) / 2
                val topLeft = Offset(
                    x = (size.width - chartSize) / 2,
                    y = (size.height - chartSize) / 2
                )
                val rectSize = Size(chartSize, chartSize)

                var startAngle = -90f

                categoryAmounts.forEach { (category, amount) ->
                    val percentage = amount / total
                    val sweepAngle = (percentage * 360f).toFloat() * sweepAngleModifier

                    drawArc(
                        color = getCategoryColor(category),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = rectSize,
                        style = Stroke(width = thickness.toPx(), cap = StrokeCap.Round)
                    )
                    startAngle += sweepAngle
                }
            }

            // Beautiful center label to display sum
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(thickness + 4.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "$%.2f", total),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp
                )
                Text(
                    text = "Total Active",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun MonthlyProgressIndicator(
    expenses: Double,
    budget: Double,
    modifier: Modifier = Modifier
) {
    val percentage = if (budget > 0.0) {
        (expenses / budget).coerceIn(0.0, 1.0).toFloat()
    } else {
        0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(durationMillis = 800),
        label = "progress"
    )

    val progressColor = when {
        percentage >= 1.0f -> MaterialTheme.colorScheme.error
        percentage >= 0.85f -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }

    val trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Monthly Spending Cap Limit",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${String.format(Locale.US, "%.0f", percentage * 100)}%",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = progressColor
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
        ) {
            val strokeWidth = size.height
            val radius = strokeWidth / 2

            // Draw Background Track
            drawLine(
                color = trackColor,
                start = Offset(radius, radius),
                end = Offset(size.width - radius, radius),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Draw Real-time Progress Bar Fill
            if (animatedProgress > 0f) {
                val fillWidth = (size.width - radius * 2) * animatedProgress + radius
                drawLine(
                    color = progressColor,
                    start = Offset(radius, radius),
                    end = Offset(fillWidth, radius),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = String.format(Locale.US, "$%.2f Spent", expenses),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (budget > 0.0) {
                    String.format(Locale.US, "Cap: $%.2f", budget)
                } else {
                    "Cap: Not Set"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Compact Legend Composable for Expense breakdown details
@Composable
fun ColorIndicatorLegend(
    category: String,
    amount: Double,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(getCategoryColor(category), CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = category,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = String.format(Locale.US, "$%.2f", amount),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color(0xFFE5E7EB) else Color(0xFF374151)
        )
    }
}
