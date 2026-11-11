package com.budgetkhata.ui.settings

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.budgetkhata.R
import com.budgetkhata.di.AppContainer
import com.budgetkhata.domain.ReminderContext
import com.budgetkhata.domain.ReminderMessageBuilder
import com.budgetkhata.integrations.UpiLinkBuilder
import com.budgetkhata.ui.LocalAppSettings
import com.budgetkhata.ui.LocalSnackbarHostState
import com.budgetkhata.ui.appViewModel
import com.budgetkhata.ui.components.AppTopBar
import com.budgetkhata.ui.components.InfoBanner
import com.budgetkhata.ui.components.SectionHeader

@Composable
fun ReminderTemplateScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val viewModel = appViewModel(container) { app, _ -> ReminderTemplateViewModel(app) }
    val settings = LocalAppSettings.current
    val snackbar = LocalSnackbarHostState.current

    LaunchedEffect(viewModel.message) {
        val message = viewModel.message
        if (message != null) {
            snackbar?.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    val preview = remember(viewModel.template, settings, viewModel.sampleRow) {
        val row = viewModel.sampleRow
        val upiLink = if (settings.isUpiConfigured) {
            UpiLinkBuilder.build(
                upiId = settings.upiId,
                payeeName = settings.payeeName,
                amountPaise = row?.remainingPaise ?: 30_000L,
                note = "Contribution"
            )
        } else {
            null
        }
        ReminderMessageBuilder.build(
            template = viewModel.template,
            context = ReminderContext(
                studentName = row?.studentName ?: "Rahul",
                eventName = row?.eventName ?: "College Jatra",
                totalPaise = row?.requiredPaise ?: 50_000L,
                paidPaise = row?.paidPaise ?: 20_000L,
                pendingPaise = row?.remainingPaise ?: 30_000L,
                upiId = settings.upiId.ifBlank { "yourname@okhdfcbank" },
                payeeName = settings.payeeName.ifBlank { "Organiser" },
                instructions = settings.paymentInstructions,
                upiLink = upiLink
            )
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.reminder_message),
                onBack = onBack,
                actions = {
                    TextButton(onClick = viewModel::resetToDefault) { Text(stringResource(R.string.reset)) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(
                title = stringResource(R.string.whatsapp_reminder_title),
                subtitle = stringResource(R.string.whatsapp_reminder_msg)
            )

            InfoBanner(
                text = stringResource(R.string.placeholder_hint)
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ReminderMessageBuilder.placeholders) { placeholder ->
                    AssistChip(
                        onClick = { viewModel.appendPlaceholder(placeholder) },
                        label = { Text(placeholder) }
                    )
                }
            }

            OutlinedTextField(
                value = viewModel.template,
                onValueChange = viewModel::updateTemplate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                label = { Text(stringResource(R.string.message_template)) },
                textStyle = MaterialTheme.typography.bodySmall
            )

            Button(
                onClick = { viewModel.save(onBack) },
                enabled = !viewModel.saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (viewModel.saving) stringResource(R.string.loading) else stringResource(R.string.save_message))
            }

            TextButton(onClick = onBack) { Text(stringResource(R.string.cancel)) }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(R.string.live_preview),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = viewModel.sampleRow?.let { stringResource(R.string.using_sample_msg, it.studentName, it.eventName) }
                            ?: stringResource(R.string.sample_data_msg),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.size(8.dp))
                    SelectionContainer {
                        Text(text = preview, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.whatsapp_paid_warning),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.size(12.dp))
        }
    }
}
