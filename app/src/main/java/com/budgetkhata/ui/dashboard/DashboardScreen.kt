package com.budgetkhata.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetkhata.R
import com.budgetkhata.core.Money
import com.budgetkhata.data.prefs.AppLanguage
import com.budgetkhata.di.AppContainer
import com.budgetkhata.ui.LocalAppSettings
import com.budgetkhata.ui.appViewModel
import com.budgetkhata.ui.components.AppTopBar
import com.budgetkhata.ui.components.LoadingBox
import com.budgetkhata.ui.findActivity
import com.budgetkhata.ui.settings.SettingsViewModel
import com.budgetkhata.ui.theme.LocalStatusPalette

@Composable
fun DashboardScreen(
    container: AppContainer,
    onSearchStudents: (String) -> Unit,
    onFilterStudents: (String?) -> Unit,
    onAddStudent: () -> Unit,
    onAddEvent: () -> Unit,
    onOpenEvent: (Long) -> Unit,
    onOpenEvents: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val viewModel = appViewModel(container) { app, _ -> DashboardViewModel(app) }
    val settingsViewModel = appViewModel(container) { app, _ -> SettingsViewModel(app) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = LocalAppSettings.current
    val context = LocalContext.current
    val palette = LocalStatusPalette.current

    BackHandler { context.findActivity()?.moveTaskToBack(true) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.app_name),
                actions = {
                    // Super easy language switch in top bar
                    TextButton(
                        onClick = {
                            val nextLang = if (settings.language == AppLanguage.MARATHI) AppLanguage.ENGLISH else AppLanguage.MARATHI
                            settingsViewModel.setLanguage(nextLang)
                        }
                    ) {
                        Text(
                            text = if (settings.language == AppLanguage.MARATHI) "English" else "मराठी",
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 16.sp
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings))
                    }
                }
            )
        }
    ) { padding ->
        val data = state
        if (data == null) {
            LoadingBox(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Large Collection Summary Box
                item {
                    MainCollectionCard(
                        collected = data.stats.collectedPaise,
                        expected = data.stats.expectedPaise,
                        palette = palette
                    )
                }

                // Grid of 4 Big Buttons
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            MenuActionTile(
                                title = stringResource(R.string.action_add_student),
                                icon = Icons.Rounded.PersonAdd,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                                onClick = onAddStudent
                            )
                            MenuActionTile(
                                title = stringResource(R.string.action_collect_payment),
                                icon = Icons.Rounded.Payments,
                                color = palette.paid,
                                modifier = Modifier.weight(1f),
                                onClick = { onFilterStudents(null) }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            MenuActionTile(
                                title = stringResource(R.string.action_pending_payments),
                                icon = Icons.Rounded.HourglassBottom,
                                color = palette.pending,
                                modifier = Modifier.weight(1f),
                                onClick = { onFilterStudents("Pending") }
                            )
                            MenuActionTile(
                                title = stringResource(R.string.action_view_accounts),
                                icon = Icons.Rounded.Assessment,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f),
                                onClick = onOpenReports
                            )
                        }
                    }
                }

                // Simple Stats Row
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            BottomStatItem(stringResource(R.string.students), data.stats.studentCount.toString())
                            BottomStatItem(stringResource(R.string.events), data.stats.eventCount.toString())
                            BottomStatItem(stringResource(R.string.paid), data.stats.paidCount.toString())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainCollectionCard(collected: Long, expected: Long, palette: com.budgetkhata.ui.theme.StatusPalette) {
    Surface(
        color = palette.paid.copy(alpha = 0.15f),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.collected),
                style = MaterialTheme.typography.titleLarge,
                color = palette.paid,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = Money.format(collected),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = palette.paid,
                fontSize = 44.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = palette.paid.copy(alpha = 0.1f),
                shape = CircleShape
            ) {
                Text(
                    text = "${stringResource(R.string.expected)}: ${Money.format(expected)}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Bold,
                    color = palette.paid
                )
            }
        }
    }
}

@Composable
private fun MenuActionTile(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = color
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
private fun BottomStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
