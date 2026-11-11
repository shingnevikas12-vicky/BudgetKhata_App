package com.budgetkhata.ui.payments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.budgetkhata.R
import com.budgetkhata.core.Money
import com.budgetkhata.core.Validation
import com.budgetkhata.data.local.model.EnrollmentRow
import com.budgetkhata.ui.components.MoneyTextField

@Composable
fun EditContributionDialog(
    row: EnrollmentRow,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    var amount by remember {
        mutableStateOf(
            if (row.requiredPaise % 100L == 0L) {
                (row.requiredPaise / 100L).toString()
            } else {
                Money.upiAmount(row.requiredPaise)
            }
        )
    }
    var localError by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_contribution)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${row.studentName} \u00B7 ${row.eventName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MoneyTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        localError = null
                    },
                    label = stringResource(R.string.required_contribution),
                    isError = localError != null,
                    errorText = localError,
                    supportingText = stringResource(R.string.amount_paid) + ": ${Money.format(row.paidPaise)}"
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val paise = Money.parseToPaise(amount)
                    val error = Validation.contribution(paise)
                    if (paise == null || error != null) {
                        localError = error ?: R.string.valid_amount_error
                        return@TextButton
                    }
                    onSave(paise)
                }
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
