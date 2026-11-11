package com.budgetkhata.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetkhata.R
import com.budgetkhata.core.Money
import com.budgetkhata.data.local.entity.EventType
import com.budgetkhata.data.local.model.EventSummary
import com.budgetkhata.di.AppContainer
import com.budgetkhata.ui.appViewModel
import com.budgetkhata.ui.components.AppTopBar
import com.budgetkhata.ui.components.EmptyState
import com.budgetkhata.ui.components.LoadingBox
import com.budgetkhata.ui.theme.LocalStatusPalette

@Composable
fun EventsScreen(
    container: AppContainer,
    onOpenEvent: (Long) -> Unit,
    onAddEvent: () -> Unit,
    onEditEvent: (Long) -> Unit,
    onOpenReports: () -> Unit
) {
    val viewModel = appViewModel(container) { app, _ -> EventsViewModel(app) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val palette = LocalStatusPalette.current

    Scaffold(
        topBar = {
            AppTopBar(title = stringResource(R.string.events))
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddEvent,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(32.dp))
            }
        }
    ) { padding ->
        if (state.loading) {
            LoadingBox(modifier = Modifier.padding(padding))
        } else if (state.active.isEmpty() && state.archived.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.no_events_yet),
                message = stringResource(R.string.no_events_msg),
                icon = Icons.Rounded.Event,
                actionLabel = stringResource(R.string.create_event),
                onAction = onAddEvent,
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.active, key = { it.eventId }) { summary ->
                    SimpleEventCard(
                        summary = summary,
                        palette = palette,
                        onClick = { onOpenEvent(summary.eventId) }
                    )
                }

                if (state.archived.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.archived_events),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(state.archived, key = { it.eventId }) { summary ->
                        SimpleEventCard(
                            summary = summary,
                            palette = palette,
                            onClick = { onOpenEvent(summary.eventId) }
                        )
                    }
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun SimpleEventCard(
    summary: EventSummary,
    palette: com.budgetkhata.ui.theme.StatusPalette,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = eventEmoji(summary.eventType), fontSize = 24.sp)
                }
                Spacer(Modifier.size(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.eventName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = summary.eventType.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                EventStat(label = stringResource(R.string.students), value = summary.studentCount.toString())
                EventStat(label = stringResource(R.string.collected), value = Money.format(summary.collectedPaise), color = palette.paid)
                EventStat(
                    label = stringResource(R.string.pending),
                    value = Money.format(summary.pendingPaise),
                    color = if (summary.pendingPaise > 0L) palette.pending else palette.paid
                )
            }
            
            if (summary.expensePaise > 0L) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = stringResource(R.string.expenses) + ":", style = MaterialTheme.typography.labelSmall)
                    Text(text = Money.format(summary.expensePaise), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EventStat(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

private fun eventEmoji(type: EventType): String = when (type) {
    EventType.JATRA -> "\uD83D\uDE8C"
    EventType.NSS -> "\uD83E\uDD1D"
    EventType.FESTIVAL -> "\uD83C\uDF89"
    EventType.TOUR -> "\uD83D\uDDFA\uFE0F"
    EventType.PARTY -> "\uD83C\uDF7D\uFE0F"
    EventType.OTHER -> "\uD83D\uDCCB"
}
