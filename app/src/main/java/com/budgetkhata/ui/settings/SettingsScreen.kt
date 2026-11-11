package com.budgetkhata.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetkhata.BuildConfig
import com.budgetkhata.R
import com.budgetkhata.data.prefs.AppLanguage
import com.budgetkhata.data.prefs.ThemeMode
import com.budgetkhata.di.AppContainer
import com.budgetkhata.ui.LocalAppSettings
import com.budgetkhata.ui.appViewModel
import com.budgetkhata.ui.components.AppTopBar
import com.budgetkhata.ui.components.InfoBanner
import com.budgetkhata.ui.components.SectionHeader

@Composable
fun SettingsScreen(
    container: AppContainer,
    onOpenUpi: () -> Unit,
    onOpenReminder: () -> Unit,
    onOpenData: () -> Unit,
    onOpenReports: () -> Unit
) {
    val viewModel = appViewModel(container) { app, _ -> SettingsViewModel(app) }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val liveSettings = LocalAppSettings.current

    Scaffold(topBar = { AppTopBar(title = stringResource(R.string.settings)) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionHeader(stringResource(R.string.collections)) }

            item {
                SettingsTile(
                    icon = Icons.Rounded.QrCode2,
                    title = stringResource(R.string.upi_payment_qr),
                    subtitle = if (liveSettings.isUpiConfigured) {
                        "${liveSettings.upiId} \u00B7 ${liveSettings.payeeName}"
                    } else {
                        stringResource(R.string.not_configured_yet)
                    },
                    onClick = onOpenUpi
                )
            }

            item {
                SettingsTile(
                    icon = Icons.AutoMirrored.Rounded.Chat,
                    title = stringResource(R.string.whatsapp_reminder),
                    subtitle = stringResource(R.string.whatsapp_reminder_subtitle),
                    onClick = onOpenReminder
                )
            }

            item {
                SettingsTile(
                    icon = Icons.Rounded.Assessment,
                    title = stringResource(R.string.reports_exports),
                    subtitle = stringResource(R.string.reports_exports_subtitle),
                    onClick = onOpenReports
                )
            }

            item { SectionHeader(stringResource(R.string.appearance)) }

            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Palette, contentDescription = null)
                            Spacer(Modifier.size(10.dp))
                            Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(Modifier.size(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = settings.themeMode == mode,
                                    onClick = { viewModel.setTheme(mode) },
                                    label = { Text(stringResource(mode.labelRes)) }
                                )
                            }
                        }
                        
                        Spacer(Modifier.size(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Language, contentDescription = null)
                            Spacer(Modifier.size(10.dp))
                            Text(stringResource(R.string.language), style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(Modifier.size(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppLanguage.entries.forEach { lang ->
                                FilterChip(
                                    selected = settings.language == lang,
                                    onClick = { viewModel.setLanguage(lang) },
                                    label = { Text(lang.label) }
                                )
                            }
                        }

                        Spacer(Modifier.size(8.dp))
                        SwitchRow(
                            title = stringResource(R.string.dynamic_colors),
                            subtitle = stringResource(R.string.dynamic_colors_subtitle),
                            checked = settings.dynamicColor,
                            onCheckedChange = viewModel::setDynamicColor
                        )
                    }
                }
            }

            item { SectionHeader(stringResource(R.string.privacy)) }

            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SwitchRow(
                            title = stringResource(R.string.block_screenshots),
                            subtitle = stringResource(R.string.block_screenshots_subtitle),
                            checked = settings.screenshotProtection,
                            onCheckedChange = viewModel::setScreenshotProtection
                        )
                    }
                }
            }

            item { SectionHeader(stringResource(R.string.data)) }

            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsTile(
                            icon = Icons.Rounded.Backup,
                            title = stringResource(R.string.backup_restore_delete),
                            subtitle = stringResource(R.string.backup_restore_subtitle),
                            onClick = onOpenData
                        )
                        SwitchRow(
                            title = stringResource(R.string.allow_overpayment),
                            subtitle = stringResource(R.string.allow_overpayment_subtitle),
                            checked = settings.allowOverpayment,
                            onCheckedChange = viewModel::setAllowOverpayment
                        )
                    }
                }
            }

            item {
                InfoBanner(
                    text = stringResource(R.string.privacy_info_banner)
                )
            }

            item {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Column {
                        Text(
                            text = "Budget Khata ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.manual_payment_warning),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
