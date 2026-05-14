package com.sans.finance.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sans.finance.R
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar

@Composable
fun MonthSelector(
    startDate: Long,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthYear = remember(startDate) {
        if (startDate <= 0L) "All Time"
        else {
            val cal = com.sans.finance.core.util.CalendarUtils.getInstance().apply {
                timeInMillis = startDate
            }
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Previous Month",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Surface(
                onClick = onMonthClick,
                color = Color.Transparent,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = monthYear,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            IconButton(onClick = onNextMonth) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Next Month",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun MonthYearPickerDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (Int, Int) -> Unit, // month 0-11, year
    initialMonth: Int,
    initialYear: Int
) {
    var selectedMonth by remember { mutableStateOf(initialMonth) }
    var selectedYear by remember { mutableStateOf(initialYear) }

    val months = remember {
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.width(320.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Month & Year",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Year selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedYear-- }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = null)
                    }
                    Text(
                        text = selectedYear.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                    IconButton(onClick = { selectedYear++ }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Month Grid (4x3)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0 until 4) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (col in 0 until 3) {
                                val index = row * 3 + col
                                val month = months[index]
                                val isSelected = selectedMonth == index
                                Surface(
                                    onClick = { selectedMonth = index },
                                    modifier = Modifier.weight(1f),
                                    shape = MaterialTheme.shapes.medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = 0.3f
                                    ),
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = month,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            selectedMonth = cal.get(Calendar.MONTH)
                            selectedYear = cal.get(Calendar.YEAR)
                        }
                    ) {
                        Text("Current Month")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    androidx.compose.material3.TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                    androidx.compose.material3.TextButton(onClick = {
                        onDateSelected(
                            selectedMonth,
                            selectedYear
                        )
                    }) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@Composable
fun TodaySeparator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
            thickness = 1.dp
        )
        Surface(
            color = MaterialTheme.colorScheme.error,
            shape = androidx.compose.foundation.shape.CircleShape,
            tonalElevation = 4.dp
        ) {
            Text(
                text = stringResource(R.string.today).uppercase(),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onError,
                letterSpacing = 1.sp
            )
        }
    }
}
