package com.budgetkhata.ui.reports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetkhata.R
import com.budgetkhata.data.prefs.AppSettings
import com.budgetkhata.di.AppContainer
import com.budgetkhata.domain.ReportTable
import com.budgetkhata.domain.ReportType
import com.budgetkhata.ui.LocalAppSettings
import com.budgetkhata.ui.LocalSnackbarHostState
import com.budgetkhata.ui.appViewModel
import com.budgetkhata.ui.components.AppTopBar
import com.budgetkhata.ui.components.EmptyState
import com.budgetkhata.ui.components.InfoBanner
import com.budgetkhata.ui.components.LoadingBox
import com.budgetkhata.ui.components.SectionHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ReportsScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val viewModel = appViewModel(container) { app, handle -> ReportsViewModel(app, handle) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = LocalAppSettings.current
    val snackbar = LocalSnackbarHostState.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val table = remember(state) { state.buildTable(context) }
    val currentTable by rememberUpdatedState(table)
    val allTables = remember(state) { state.buildAllTables(context) }

    val csvSavedMsg = stringResource(R.string.csv_saved)
    val csvSaveFailedMsg = stringResource(R.string.csv_save_failed)
    val pdfCreateFailedMsg = stringResource(R.string.pdf_create_failed)
    val sharePdfLabel = stringResource(R.string.share_pdf_report)

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val success = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(currentTable.toCsv().toByteArray(Charsets.UTF_8))
                        }
                        true
                    } catch (error: Exception) {
                        false
                    }
                }
                snackbar?.showSnackbar(if (success) csvSavedMsg else csvSaveFailedMsg)
            }
        }
    }

    fun sharePdf(tables: List<ReportTable>, title: String) {
        scope.launch {
            val file = withContext(Dispatchers.IO) {
                try {
                    container.pdfReportWriter.write(
                        tables = tables,
                        documentTitle = title,
                        fileName = container.pdfReportWriter.fileName("budget-khata-report")
                    )
                } catch (error: Exception) {
                    null
                }
            }
            if (file == null) {
                snackbar?.showSnackbar(pdfCreateFailedMsg)
            } else {
                container.shareUtils.shareFile(file, "application/pdf", sharePdfLabel)
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.reports),
                onBack = onBack,
                actions = {
                    TextButton(onClick = { sharePdf(allTables, "Budget Khata report") }) {
                        Text(stringResource(R.string.full_pdf))
                    }
                }
            )
        }
    ) { padding ->
        if (state.loading) {
            LoadingBox(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SelectionInfo(state = state, settings = settings)
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.eventFilter == null,
                            onClick = { viewModel.selectEvent(null) },
                            label = { Text(stringResource(R.string.all_events)) }
                        )
                    }
                    items(state.events, key = { it.id }) { event ->
                        FilterChip(
                            selected = state.eventFilter == event.id,
                            onClick = {
                                viewModel.selectEvent(if (state.eventFilter == event.id) null else event.id)
                            },
                            label = { Text(event.name, maxLines = 1) }
                        )
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ReportType.entries.toList(), key = { it.name }) { type ->
                        FilterChip(
                            selected = state.selectedReport == type,
                            onClick = { viewModel.selectReport(type) },
                            label = { Text(stringResource(type.labelRes)) }
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            csvLauncher.launch(
                                "budget-khata-${state.selectedReport.name.lowercase()}.csv"
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.FileDownload, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("CSV")
                    }
                    OutlinedButton(
                        onClick = { sharePdf(listOf(currentTable), currentTable.title) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("PDF")
                    }
                }
            }

            item { ReportPreview(table = table) }
        }
    }
}

@Composable
private fun SelectionInfo(state: ReportsUiState, settings: AppSettings) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionHeader(
            title = stringResource(state.selectedReport.labelRes),
            subtitle = stringResource(state.selectedReport.descRes)
        )
        InfoBanner(
            text = stringResource(R.string.reports_banner_msg, state.getFilterLabel(context).lowercase())
        )
        if (!settings.isUpiConfigured) {
            Text(
                text = stringResource(R.string.reports_upi_tip),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReportPreview(table: ReportTable) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = table.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = table.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(10.dp))

            if (table.totals.isNotEmpty()) {
                table.totals.forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = value,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            if (table.rows.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.nothing_to_show),
                    message = table.emptyMessage,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = stringResource(R.string.preview_stats, table.rows.size, table.columns.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                table.rows.take(40).forEachIndexed { index, row ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    }
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Text(
                            text = row.firstOrNull().orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        val details = row.drop(1).mapIndexedNotNull { columnIndex, value ->
                            val header = table.columns.getOrNull(columnIndex + 1)?.header ?: return@mapIndexedNotNull null
                            "$header: $value"
                        }
                        if (details.isNotEmpty()) {
                            Text(
                                text = details.joinToString(" \u00B7 "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (table.rows.size > 40) {
                    Text(
                        text = stringResource(R.string.showing_first_rows),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}
