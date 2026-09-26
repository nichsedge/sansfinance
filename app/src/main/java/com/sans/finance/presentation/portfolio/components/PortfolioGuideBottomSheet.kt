package com.sans.finance.presentation.portfolio.components

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioGuideBottomSheet(
    onDismiss: () -> Unit,
    onImportFile: () -> Unit,
    snackbarHostState: SnackbarHostState? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    fun copyToClipboard(label: String, text: String, feedbackMessage: String) {
        scope.launch {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(label, text)))
            snackbarHostState?.showSnackbar(feedbackMessage)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
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
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Panduan Data Portofolio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Format snapshot & setup otomatisasi",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Tab Selector: Pemula vs Expert
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "🔰 Pemula (Manual)",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "⚡ Expert (Automasi)",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedTab == 0) {
                    BeginnerGuideSection(
                        onCopyText = { label, content, feedback ->
                            copyToClipboard(label, content, feedback)
                        },
                        onImportFile = onImportFile
                    )
                } else {
                    ExpertGuideSection(
                        onCopyCommand = { cmd, msg ->
                            copyToClipboard("CLI Command", cmd, msg)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BeginnerGuideSection(
    onCopyText: (String, String, String) -> Unit,
    onImportFile: () -> Unit
) {
    var formatType by rememberSaveable { mutableIntStateOf(0) } // 0: CSV, 1: JSON

    val sampleCsv = remember {
        """date,source,category,asset,currency,quantity,price,value_idr,asset_class,account,details,cost_basis,yield_rate
2026-09-26,manual,equity,BBCA,IDR,1000,10250,10250000,Equities,Stockbit,,9500,0.038
2026-09-26,manual,crypto,BTC,USD,0.05,95000,76000000,Crypto,Binance,,65000,0.0
2026-09-26,manual,cash,IDR,IDR,25000000,1,25000000,Cash,BCA Rekening,,1,0.015"""
    }
    val sampleJson = remember {
        """{
  "metadata": {
    "date": "2026-09-26",
    "exchange_rate": 16000.0
  },
  "holdings": [
    {
      "source": "manual",
      "category": "equity",
      "asset": "BBCA",
      "currency": "IDR",
      "quantity": 1000.0,
      "price": 10250.0,
      "value_idr": 10250000.0,
      "value_usd": 640.6,
      "asset_class": "Equities",
      "account": "Stockbit",
      "cost_basis": 9500.0,
      "yield_rate": 0.038
    },
    {
      "source": "manual",
      "category": "crypto",
      "asset": "BTC",
      "currency": "USD",
      "quantity": 0.05,
      "price": 95000.0,
      "value_idr": 76000000.0,
      "value_usd": 4750.0,
      "asset_class": "Crypto",
      "account": "Binance / Cold Wallet",
      "cost_basis": 65000.0,
      "yield_rate": 0.0
    },
    {
      "source": "manual",
      "category": "cash",
      "asset": "IDR",
      "currency": "IDR",
      "quantity": 25000000.0,
      "price": 1.0,
      "value_idr": 25000000.0,
      "value_usd": 1562.5,
      "asset_class": "Cash",
      "account": "BCA Rekening Utama",
      "cost_basis": 1.0,
      "yield_rate": 0.015
    }
  ]
}"""
    }

    // Concept Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Bisa pakai CSV atau JSON?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "Ya! Aplikasi mendukung file CSV (paling cocok untuk Excel & Google Sheets) maupun file JSON (snapshot terstruktur). Sans Finance menerapkan prinsip zero-credential: tidak pernah menyimpan password broker atau token rahasia perbankan Anda di HP demi privasi mutlak.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }

    // How to Upload Options
    Text(
        text = "Cara Mengunggah Data",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        UploadMethodItem(
            stepNumber = "1",
            title = "Pilih File CSV / JSON Lokal (Paling Cepat)",
            description = "Simpan file .csv atau .json di memori HP / Google Drive, lalu tekan tombol pilih file di bawah atau via menu titik tiga di layar Portfolio.",
            icon = Icons.Default.FileOpen,
            actionButton = {
                Button(
                    onClick = onImportFile,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pilih File Sekarang (CSV/JSON)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        )

        UploadMethodItem(
            stepNumber = "2",
            title = "Menu Settings > Data Management",
            description = "Anda juga bisa mengimpor dan mengekspor file portofolio (CSV atau JSON) kapan saja melalui menu Settings > Data Management > Portfolio.",
            icon = Icons.Default.Storage
        )

        UploadMethodItem(
            stepNumber = "3",
            title = "Cloud Storage Sync (R2 / GCS)",
            description = "Unggah file snapshot ke Cloudflare R2 atau GCS Anda sebagai 'snapshots/latest.json'. Konfigurasi di Settings > Cloud Storage, lalu tekan tombol 'Sync from Cloud'.",
            icon = Icons.Default.CloudDownload
        )
    }

    // Format Selector (CSV vs JSON)
    Text(
        text = "Pilih Contoh Format Template",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = formatType == 0,
            onClick = { formatType = 0 },
            label = { Text("CSV (Excel / Spreadsheet)") },
            leadingIcon = if (formatType == 0) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
            } else null
        )
        FilterChip(
            selected = formatType == 1,
            onClick = { formatType = 1 },
            label = { Text("JSON (Hierarki Lengkap)") },
            leadingIcon = if (formatType == 1) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
            } else null
        )
    }

    // Format Code Box
    val currentSnippet = if (formatType == 0) sampleCsv else sampleJson
    val snippetLabel = if (formatType == 0) "Template CSV Portofolio" else "Format JSON Snapshot"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(snippetLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = {
                val label = if (formatType == 0) "Portfolio CSV" else "Portfolio JSON"
                val feedback = if (formatType == 0) "Template CSV berhasil disalin!" else "Format JSON berhasil disalin!"
                onCopyText(label, currentSnippet, feedback)
            },
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("Salin Template", fontSize = 12.sp)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = currentSnippet,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 16.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    // Field Dictionary
    Text(
        text = "Penjelasan Kolom & Field (CSV & JSON)",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FieldExplanationRow("date", "String (YYYY-MM-DD)", "Tanggal snapshot diambil", true)
        FieldExplanationRow("asset", "String", "Kode saham/koin (cth: BBCA, BTC, SBN)", true)
        FieldExplanationRow("category", "String", "equity, crypto, cash, p2p, fixed_income", true)
        FieldExplanationRow("asset_class", "String", "Equities, Crypto, Cash, Fixed Income", true)
        FieldExplanationRow("value_idr", "Double", "Nilai total dalam Rupiah (SSOT kalkulasi)", true)
        FieldExplanationRow("value_usd", "Double", "Nilai dalam USD (opsional, untuk cross-currency)", false)
        FieldExplanationRow("quantity", "Double", "Jumlah lembar/unit/koin yang dimiliki", false)
        FieldExplanationRow("price", "Double", "Harga saat ini per unit", false)
        FieldExplanationRow("cost_basis", "Double", "Modal beli rata-rata (untuk hitung untung/rugi)", false)
        FieldExplanationRow("yield_rate", "Double", "Persentase dividen/bunga (cth: 0.045 = 4.5% p.a.)", false)
        FieldExplanationRow("account", "String", "Nama sekuritas/dompet (cth: Stockbit, Binance)", false)
        FieldExplanationRow("source", "String", "manual, ksei, debank, binance", false)
    }
}

@Composable
private fun ExpertGuideSection(
    onCopyCommand: (String, String) -> Unit
) {
    val quickStartCommand = remember {
        """git clone https://github.com/nichsedge/portfolio-integration.git
cd portfolio-integration
uv sync
uv run run-all"""
    }

    val envTemplate = remember {
        """# Direktori data snapshot harian
PORTFOLIO_DATA_DIR=data/

# 🏛️ KSEI (Saham Indonesia & RDN)
KSEI_USERNAME=username_ksei_anda
KSEI_PASSWORD=password_ksei_anda

# 🌐 DeBank (EVM DeFi & On-chain Wallets)
ETH_ADDRESS=0xYourEvmAddress...

# 🟡 Binance (Spot, Earn, Futures via CCXT)
BINANCE_API_KEY=your_read_only_api_key
BINANCE_API_SECRET=your_read_only_api_secret

# 🟣 Alchemy (Solana SPL Tokens & SOL)
SOL_ADDRESS=your_solana_address
ALCHEMY_API_KEY=your_alchemy_api_key

# ☁️ Cloudflare R2 (Sinkronisasi Otomatis ke Sans Finance)
R2_ACCOUNT_ID=your_cloudflare_account_id
R2_ACCESS_KEY_ID=your_r2_access_key
R2_SECRET_ACCESS_KEY=your_r2_secret_key
R2_BUCKET_NAME=your_r2_bucket_name"""
    }

    // Architecture Overview Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Arsitektur portfolio-integration",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "portfolio-integration adalah Python monorepo (uv) yang berfungsi sebagai Canonical ETL Engine. Engine ini menarik data secara paralel dari KSEI, DeBank, Binance, Alchemy, dan Sans Finance, menyelaraskan yield, membuat SQLite SSOT, dan mengunggah snapshots/latest.json ke Cloudflare R2.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }

    // Supported Sources Grid
    Text(
        text = "Sumber Data yang Didukung",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SourceCard("🏛️ KSEI Akses", "Saham IDX, Reksadana & RDN via ksei dump", "uv run ksei dump")
        SourceCard("🌐 DeBank Scraper", "Multi-chain EVM DeFi, LP, staking via debank-scrape", "uv run debank-scrape")
        SourceCard("🟡 Binance CEX", "Saldo Spot, Earn, & Futures via CCXT API", "uv run binance-fetch")
        SourceCard("🟣 Solana / Alchemy", "Native SOL & SPL tokens via Alchemy RPC", "uv run alchemy-fetch")
        SourceCard("📱 Sans Finance", "Kas bank, dompet, & P2P lending dari database R2", "uv run sansfinance-fetch")
        SourceCard("🦙 DefiLlama", "Token price feeds, yield pools & protocol audits", "uv run llama-fetch")
    }

    // Step by Step Setup
    Text(
        text = "Langkah Setup Workstation",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )

    // Step 1: Quickstart CLI
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "1. Clone & Jalankan Pipeline",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { onCopyCommand(quickStartCommand, "Perintah CLI berhasil disalin!") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy CLI", modifier = Modifier.size(16.dp))
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(10.dp)
            ) {
                Text(
                    text = quickStartCommand,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    // Step 2: Environment Config
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "2. Konfigurasi File .env",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = { onCopyCommand(envTemplate, "Template .env berhasil disalin!") },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Salin .env", fontSize = 12.sp)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(10.dp)
            ) {
                Text(
                    text = envTemplate,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.5.sp,
                        lineHeight = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    // Step 3: Link to Sans Finance App
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("3. Sinkronkan ke Sans Finance", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "1. Buka Settings > Cloud Storage di aplikasi ini.\n2. Masukkan R2 Account ID, Bucket, Access Key, dan Secret Key yang sama dengan .env workstation.\n3. Kembali ke Portfolio screen dan tekan tombol 'Sync from Cloud'. Data snapshot dan Sovereign Advisor AI akan otomatis tersinkronisasi!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun UploadMethodItem(
    stepNumber: String,
    title: String,
    description: String,
    icon: ImageVector,
    actionButton: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(10.dp))
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )
            if (actionButton != null) {
                actionButton()
            }
        }
    }
}

@Composable
private fun FieldExplanationRow(
    fieldName: String,
    type: String,
    description: String,
    isRequired: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = fieldName,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(90.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = type,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (isRequired) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "Wajib",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 9.sp
                        )
                    }
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.5.sp
            )
        }
    }
}

@Composable
private fun SourceCard(
    title: String,
    description: String,
    command: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.5.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = command,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp
            )
        }
    }
}
