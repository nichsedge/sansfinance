package com.sans.finance.presentation.portfolio.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import com.sans.finance.domain.model.ValuedHolding
import com.sans.finance.presentation.components.GlassCard
import com.sans.finance.presentation.components.PrivacyText
import java.util.Locale

enum class PortfolioSortOption(val label: String) {
    VALUE_DESC("Highest Value"),
    VALUE_ASC("Lowest Value"),
    GAIN_DESC("Top Gainers"),
    GAIN_ASC("Top Losers"),
    YIELD_DESC("Highest Yield / APY"),
    NAME_ASC("Name (A-Z)")
}

enum class PortfolioViewMode(val label: String) {
    GROUPED("By Category"),
    FLAT("All Assets")
}

data class ParsedAssetDisplay(
    val symbol: String,
    val secondaryText: String?,
    val initials: String
)

fun parseAssetDisplay(asset: String, accountDisplay: String): ParsedAssetDisplay {
    if (asset.contains(" - ")) {
        val parts = asset.split(" - ", limit = 2)
        val symbol = parts[0].trim()
        val desc = parts.getOrNull(1)?.trim()
        val initials = if (symbol.length >= 2) {
            symbol.filter { it.isLetterOrDigit() }.take(3).uppercase()
        } else {
            symbol.take(2).uppercase()
        }
        return ParsedAssetDisplay(
            symbol = symbol,
            secondaryText = desc ?: accountDisplay,
            initials = initials.ifBlank { "AS" }
        )
    }

    val initials = asset.filter { it.isLetterOrDigit() }.take(3).uppercase().ifBlank { "AS" }
    return ParsedAssetDisplay(
        symbol = asset,
        secondaryText = accountDisplay,
        initials = initials
    )
}

fun getSourceAccentColor(source: String): Color {
    val s = source.lowercase()
    return when {
        s.contains("ksei") -> Color(0xFF1E88E5) // Blue
        s.contains("binance") -> Color(0xFFF59E0B) // Amber
        s.contains("debank") || s.contains("evm") || s.contains("eth") -> Color(0xFF8B5CF6) // Purple
        s.contains("alchemy") || s.contains("sol") -> Color(0xFF06B6D4) // Cyan
        s.contains("cash") || s.contains("bank") || s.contains("p2p") || s.contains("account") || s.contains("sans") -> Color(0xFF10B981) // Emerald
        else -> Color(0xFF64748B) // Slate
    }
}

fun getCategoryIconAndColor(category: String): Pair<ImageVector, Color> {
    val c = category.lowercase()
    return when {
        c.contains("equity") || c.contains("stock") -> Icons.AutoMirrored.Filled.ShowChart to Color(0xFF3B82F6)
        c.contains("crypto") -> Icons.Default.Bolt to Color(0xFFF59E0B)
        c.contains("defi") -> Icons.Default.AutoAwesome to Color(0xFF8B5CF6)
        c.contains("cash") || c.contains("bank") || c.contains("wallet") -> Icons.Default.AccountBalanceWallet to Color(0xFF10B981)
        c.contains("fixed") || c.contains("p2p") || c.contains("bond") || c.contains("sbn") -> Icons.Default.Shield to Color(0xFF06B6D4)
        else -> Icons.Default.PieChart to Color(0xFF6366F1)
    }
}

@Composable
fun PortfolioFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    totalHoldingCount: Int,
    sourceCounts: Map<String, Int>,
    selectedSource: String?,
    onSourceSelect: (String?) -> Unit,
    sortOption: PortfolioSortOption,
    onSortOptionSelect: (PortfolioSortOption) -> Unit,
    viewMode: PortfolioViewMode,
    onToggleViewMode: () -> Unit,
    isAllExpanded: Boolean,
    onToggleExpandAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    "Search ticker, asset, or account...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            shape = CircleShape,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        )

        // Filter and Sort Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // View Mode Toggle (Grouped vs Flat)
            FilterChip(
                selected = viewMode == PortfolioViewMode.FLAT,
                onClick = onToggleViewMode,
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (viewMode == PortfolioViewMode.FLAT) Icons.AutoMirrored.Filled.List else Icons.Default.Category,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(viewMode.label, style = MaterialTheme.typography.labelSmall)
                    }
                },
                shape = CircleShape
            )

            // Sort Menu Chip
            Box {
                FilterChip(
                    selected = sortOption != PortfolioSortOption.VALUE_DESC,
                    onClick = { showSortMenu = true },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.SwapVert,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(sortOption.label, style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    shape = CircleShape
                )

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    PortfolioSortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    option.label,
                                    fontWeight = if (option == sortOption) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            trailingIcon = {
                                if (option == sortOption) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            onClick = {
                                onSortOptionSelect(option)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }

            // All Sources chip with count
            FilterChip(
                selected = selectedSource == null,
                onClick = { onSourceSelect(null) },
                label = {
                    Text(
                        "All ($totalHoldingCount)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selectedSource == null) FontWeight.Bold else FontWeight.Normal
                    )
                },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )

            // Dynamic Sources chips with counts
            sourceCounts.forEach { (source, count) ->
                val sourceColor = getSourceAccentColor(source)
                FilterChip(
                    selected = selectedSource == source,
                    onClick = {
                        onSourceSelect(if (selectedSource == source) null else source)
                    },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(sourceColor)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "${source.uppercase()} ($count)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selectedSource == source) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = sourceColor.copy(alpha = 0.2f),
                        selectedLabelColor = sourceColor
                    )
                )
            }

            // Expand / Collapse All Toggle Chip (Only in Grouped mode)
            if (viewMode == PortfolioViewMode.GROUPED) {
                FilterChip(
                    selected = false,
                    onClick = onToggleExpandAll,
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isAllExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (isAllExpanded) "Collapse All" else "Expand All",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    shape = CircleShape
                )
            }
        }
    }
}

@Composable
fun ExpandableCategoryGroup(
    category: String,
    total: Double,
    categoryWeightPct: Double,
    holdings: List<PortfolioHoldingEntity>,
    valuedHoldings: List<ValuedHolding> = emptyList(),
    currentCurrency: String,
    isPrivacyModeEnabled: Boolean,
    accountAliases: Map<String, String>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onHoldingClick: (PortfolioHoldingEntity, ValuedHolding?) -> Unit,
    modifier: Modifier = Modifier
) {
    val valuedMap = remember(valuedHoldings) {
        valuedHoldings.associateBy {
            if (it.holding.id != 0L) it.holding.id.toString() else "${it.holding.source}_${it.holding.asset}_${it.holding.accountKey}"
        }
    }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chevron_rotation"
    )

    val (categoryIcon, categoryColor) = remember(category) {
        getCategoryIconAndColor(category)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(categoryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                category.uppercase(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    "${holdings.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        if (categoryWeightPct > 0.0) {
                            Text(
                                "${String.format(Locale.US, "%.1f%%", categoryWeightPct)} of portfolio",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    PrivacyText(
                        amount = (total * 100).toLong(),
                        currencyCode = currentCurrency,
                        isVisible = !isPrivacyModeEnabled,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(rotationAngle),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Thin weight allocation progress bar
            if (categoryWeightPct > 0.0) {
                LinearProgressIndicator(
                    progress = { (categoryWeightPct / 100.0).coerceIn(0.0, 1.0).toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = categoryColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                )
            }

            // Expandable Holdings List
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    holdings.forEachIndexed { index, holding ->
                        val lookupKey = if (holding.id != 0L) holding.id.toString() else "${holding.source}_${holding.asset}_${holding.accountKey}"
                        val valued = valuedMap[lookupKey]
                        EnhancedHoldingItem(
                            holding = holding,
                            valuedHolding = valued,
                            categoryTotal = total,
                            currentCurrency = currentCurrency,
                            isPrivacyModeEnabled = isPrivacyModeEnabled,
                            accountAliases = accountAliases,
                            onClick = { onHoldingClick(holding, valued) }
                        )

                        if (index < holdings.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * High-density 2-line financial holding row.
 * Line 1: [Avatar] Symbol + Source Badge  ---  Value
 * Line 2: [Indent] Subtitle (Account/Desc/Qty) --- PnL / Yield Badge
 */
@Composable
fun EnhancedHoldingItem(
    holding: PortfolioHoldingEntity,
    valuedHolding: ValuedHolding? = null,
    categoryTotal: Double,
    currentCurrency: String,
    isPrivacyModeEnabled: Boolean,
    accountAliases: Map<String, String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayAccountName = accountAliases[holding.accountKey]
        ?: holding.accountName?.takeIf { it.isNotBlank() }
        ?: holding.account

    val nominalInBase = valuedHolding?.currentValueInBase ?: holding.valueIdr
    val holdingWeightPct = if (categoryTotal > 0.0) (nominalInBase / categoryTotal) * 100.0 else 0.0
    val sourceColor = getSourceAccentColor(holding.source)

    val parsed = remember(holding.asset, displayAccountName) {
        parseAssetDisplay(holding.asset, displayAccountName)
    }

    val compactQtyPrice = remember(holding.quantity, holding.price, holding.currency) {
        if (holding.quantity > 0 && holding.price != null) {
            val q = holding.quantity
            val p = holding.price
            val qStr = when {
                q >= 1_000_000 -> String.format(Locale.US, "%,.1fM", q / 1_000_000.0)
                q >= 1_000 -> String.format(Locale.US, "%,.0f", q)
                q >= 1 -> String.format(Locale.US, "%,.2f", q)
                else -> String.format(Locale.US, "%.4f", q).trimEnd('0').trimEnd('.')
            }
            val pStr = when {
                p >= 1_000_000 -> String.format(Locale.US, "%,.1fM", p / 1_000_000.0)
                p >= 1_000 -> String.format(Locale.US, "%,.0f", p)
                p >= 1 -> String.format(Locale.US, "%,.2f", p)
                else -> String.format(Locale.US, "%,.4f", p)
            }
            "$qStr @ $pStr ${holding.currency}"
        } else null
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Avatar + Symbol & Subtitle
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact 38.dp Avatar
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                sourceColor.copy(alpha = 0.22f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = parsed.initials,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = sourceColor,
                    fontSize = 11.5.sp
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f, fill = false)) {
                // Line 1: Symbol + Source Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = parsed.symbol,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(sourceColor.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = holding.source.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = sourceColor,
                            fontSize = 8.5.sp
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))

                // Line 2: Subtitle (Account / Description / Qty)
                val subtitleText = remember(parsed.secondaryText, displayAccountName, compactQtyPrice, isPrivacyModeEnabled) {
                    buildString {
                        append(parsed.secondaryText ?: displayAccountName)
                        if (compactQtyPrice != null && !isPrivacyModeEnabled) {
                            append(" • ")
                            append(compactQtyPrice)
                        }
                    }
                }

                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Right Column: Strict 2-line structure (Value on Top, Return/Yield Chip on Bottom)
        Column(horizontalAlignment = Alignment.End) {
            // Line 1: Primary Value
            PrivacyText(
                amount = (nominalInBase * 100).toLong(),
                currencyCode = currentCurrency,
                isVisible = !isPrivacyModeEnabled,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(3.dp))

            // Line 2: Compact Return or Yield Chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                // Yield Pill
                if (holding.yieldRate != null && holding.yieldRate > 0.0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.15f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "⚡${String.format(Locale.US, "%.1f%%", holding.yieldRate * 100)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            fontSize = 9.5.sp
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }

                // Return / PnL Pill
                if (valuedHolding != null && valuedHolding.hasCostBasis && valuedHolding.priceGainInBase != 0.0) {
                    val priceGain = valuedHolding.priceGainInBase
                    val isPositive = priceGain >= 0
                    val gainColor = if (isPositive) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                    val gainSign = if (isPositive) "+" else ""

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(gainColor.copy(alpha = 0.12f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "$gainSign${String.format(Locale.US, "%.1f%%", valuedHolding.totalGainPercentage)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = gainColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                } else if (valuedHolding != null && holding.currency != currentCurrency && valuedHolding.fxGainInBase != 0.0) {
                    val fxGain = valuedHolding.fxGainInBase
                    val isPositive = fxGain >= 0
                    val fxColor = if (isPositive) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                    val fxSign = if (isPositive) "+" else ""

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(fxColor.copy(alpha = 0.12f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "FX $fxSign${CurrencyFormatter.formatAmountCompact((fxGain * 100).toLong(), currentCurrency)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = fxColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                } else if (holdingWeightPct > 0.0) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f%%", holdingWeightPct)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HoldingDetailBottomSheet(
    holding: PortfolioHoldingEntity,
    valuedHolding: ValuedHolding? = null,
    currentCurrency: String,
    isPrivacyModeEnabled: Boolean,
    accountAliases: Map<String, String>,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val displayAccountName = accountAliases[holding.accountKey]
        ?: holding.accountName?.takeIf { it.isNotBlank() }
        ?: holding.account

    val nominalInBase = valuedHolding?.currentValueInBase ?: holding.valueIdr
    val sourceColor = getSourceAccentColor(holding.source)
    val (categoryIcon, categoryColor) = getCategoryIconAndColor(holding.category)

    val parsed = remember(holding.asset, displayAccountName) {
        parseAssetDisplay(holding.asset, displayAccountName)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        sourceColor.copy(alpha = 0.25f),
                                        categoryColor.copy(alpha = 0.15f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = parsed.initials,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = sourceColor
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = parsed.symbol,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(sourceColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    holding.source.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = sourceColor,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(
                                holding.assetClass,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Full Asset Description (if asset had long title)
            if (holding.asset != parsed.symbol) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = holding.asset,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Valuation Bento Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                alpha = 0.15f
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "CURRENT VALUATION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    PrivacyText(
                        amount = (nominalInBase * 100).toLong(),
                        currencyCode = currentCurrency,
                        isVisible = !isPrivacyModeEnabled,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (holding.currency != currentCurrency) {
                        val originalValue = holding.quantity * (holding.price ?: 1.0)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (isPrivacyModeEnabled) "••••" else "${CurrencyFormatter.formatAmountCompact((originalValue * 100).toLong(), holding.currency)} (${holding.currency})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Performance & Returns Grid (if cost basis or valuation available)
            if (valuedHolding != null && valuedHolding.hasCostBasis) {
                val totalGain = valuedHolding.totalGainInBase
                val isPositive = totalGain >= 0
                val pnlColor = if (isPositive) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                val pnlSign = if (isPositive) "+" else ""

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "RETURN BREAKDOWN",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Total Return", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = null,
                                        tint = pnlColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "$pnlSign${CurrencyFormatter.formatAmountCompact((totalGain * 100).toLong(), currentCurrency)} (${String.format(Locale.US, "%+.2f%%", valuedHolding.totalGainPercentage)})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Black,
                                        color = pnlColor
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Cost Basis", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                PrivacyText(
                                    amount = (valuedHolding.historicalValueInBase * 100).toLong(),
                                    currencyCode = currentCurrency,
                                    isVisible = !isPrivacyModeEnabled,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (valuedHolding.priceGainInBase != 0.0 || valuedHolding.fxGainInBase != 0.0) {
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (valuedHolding.priceGainInBase != 0.0) {
                                    val priceSign = if (valuedHolding.priceGainInBase >= 0) "+" else ""
                                    Text(
                                        "Price Gain: $priceSign${CurrencyFormatter.formatAmountCompact((valuedHolding.priceGainInBase * 100).toLong(), currentCurrency)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (valuedHolding.priceGainInBase >= 0) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (valuedHolding.fxGainInBase != 0.0) {
                                    val fxSign = if (valuedHolding.fxGainInBase >= 0) "+" else ""
                                    Text(
                                        "FX Gain: $fxSign${CurrencyFormatter.formatAmountCompact((valuedHolding.fxGainInBase * 100).toLong(), currentCurrency)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (valuedHolding.fxGainInBase >= 0) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Forward Passive Income / Yield Card
            if (holding.yieldRate != null && holding.yieldRate > 0.0) {
                val annualIncome = nominalInBase * holding.yieldRate
                val monthlyIncome = annualIncome / 12.0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.08f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "PASSIVE YIELD PROJECTION",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF10B981),
                                letterSpacing = 0.5.sp
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${String.format(Locale.US, "%.2f%%", holding.yieldRate * 100)} APY",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF10B981)
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Annual Payout", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                PrivacyText(
                                    amount = (annualIncome * 100).toLong(),
                                    currencyCode = currentCurrency,
                                    isVisible = !isPrivacyModeEnabled,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Monthly Equivalent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                PrivacyText(
                                    amount = (monthlyIncome * 100).toLong(),
                                    currencyCode = currentCurrency,
                                    isVisible = !isPrivacyModeEnabled,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }
            }

            // Position & Custody Specs
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "POSITION & CUSTODY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Account / Custodian", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Text(displayAccountName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }

                    if (holding.quantity > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Quantity", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            Text(
                                if (isPrivacyModeEnabled) "••••" else String.format(Locale.US, "%,.4f", holding.quantity).trimEnd('0').trimEnd('.'),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (holding.price != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Market Price", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            Text(
                                if (isPrivacyModeEnabled) "••••" else "${String.format(Locale.US, "%,.2f", holding.price)} ${holding.currency}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Category", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Text(holding.category, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }

                    if (!holding.details.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Details", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            Text(
                                holding.details,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    "Close",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
