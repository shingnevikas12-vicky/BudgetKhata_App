package com.budgetkhata.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.budgetkhata.R
import com.budgetkhata.data.local.entity.EventType
import com.budgetkhata.di.AppContainer
import com.budgetkhata.ui.appViewModel
import com.budgetkhata.ui.components.AppTextField
import com.budgetkhata.ui.components.AppTopBar
import com.budgetkhata.ui.components.DateField
import com.budgetkhata.ui.components.ErrorBanner
import com.budgetkhata.ui.components.LoadingBox
import com.budgetkhata.ui.components.OptionDropdown
import com.budgetkhata.ui.components.SectionHeader

@Composable
fun EventEditScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit
) {
    val viewModel = appViewModel(container) { app, handle -> EventEditViewModel(app, handle) }
    val form = viewModel.form
    val context = LocalContext.current

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (viewModel.isNew) stringResource(R.string.new_event) else stringResource(R.string.edit_event),
                onBack = onBack
            )
        }
    ) { padding ->
        if (viewModel.loading) {
            LoadingBox(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            viewModel.message?.let { message ->
                ErrorBanner(message = message, onRetry = viewModel::dismissMessage)
            }

            SectionHeader(
                title = stringResource(R.string.event_details),
                subtitle = stringResource(R.string.event_details_msg)
            )

            AppTextField(
                value = form.name,
                onValueChange = viewModel::updateName,
                label = stringResource(R.string.event_name),
                placeholder = "College Jatra 2026",
                isError = form.nameError != null,
                errorText = form.nameError
            )

            OptionDropdown(
                label = stringResource(R.string.event_type),
                options = EventType.entries.toList(),
                selected = form.type,
                labelOf = { context.getString(it.labelRes) },
                onSelect = viewModel::updateType
            )

            DateField(
                label = stringResource(R.string.event_date),
                epochDay = form.startEpochDay,
                onPick = viewModel::updateStartDate
            )

            AppTextField(
                value = form.description,
                onValueChange = viewModel::updateDescription,
                label = stringResource(R.string.description_optional),
                singleLine = false,
                maxLines = 4
            )

            Button(
                onClick = { viewModel.save(onSaved) },
                enabled = !viewModel.saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (viewModel.saving) stringResource(R.string.loading) else stringResource(R.string.save_event))
            }

            TextButton(onClick = onBack) { Text(stringResource(R.string.cancel)) }

            Text(
                text = stringResource(R.string.archiving_msg),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
