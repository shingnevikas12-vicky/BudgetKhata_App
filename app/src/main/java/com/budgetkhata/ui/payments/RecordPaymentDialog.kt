package com.budgetkhata.ui.payments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetkhata.R
import com.budgetkhata.core.Dates
import com.budgetkhata.core.Money
import com.budgetkhata.core.Validation
import com.budgetkhata.data.local.entity.PaymentMethod
import com.budgetkhata.ui.components.DateField
import com.budgetkhata.ui.components.MoneyTextField
import com.budgetkhata.ui.components.OptionDropdown

@Composable
fun RecordPaymentDialog(
    studentName: String,
    eventName: String,
    remainingPaise: Long,
    allowOverpayment: Boolean,
    saving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (
        amountPaise: Long,
        epochDay: Long,
        method: PaymentMethod,
        reference: String?,
        notes: String?
    ) -> Unit
) {
    val context = LocalContext.current
    var amount by remember {
        mutableStateOf(
            when {
                remainingPaise <= 0L -> ""
                remainingPaise % 100L == 0L -> (remainingPaise / 100L).toString()
                else -> Money.upiAmount(remainingPaise)
            }
        )
    }
    var epochDay by remember { mutableStateOf(Dates.todayEpochDay()) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var localErrorRes by remember { mutableStateOf<Int?>(null) }
    var showMore by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.record_a_payment),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$studentName \u00B7 $eventName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 450.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Amount Input
                MoneyTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        localErrorRes = null
                    },
                    label = stringResource(R.string.amount_received),
                    supportingText = stringResource(R.string.pending_balance_label, Money.format(remainingPaise)),
                    isError = localErrorRes != null,
                    errorText = localErrorRes
                )

                // Simplified Payment Method Selection
                OptionDropdown(
                    label = stringResource(R.string.payment_method),
                    options = PaymentMethod.entries.toList(),
                    selected = method,
                    labelOf = { context.getString(it.labelRes) },
                    onSelect = { method = it }
                )

                if (!showMore) {
                    TextButton(onClick = { showMore = true }) {
                        Text(stringResource(R.string.optional) + " (तारीख, नोंद)")
                    }
                } else {
                    DateField(
                        label = stringResource(R.string.payment_date),
                        epochDay = epochDay,
                        onPick = { epochDay = it }
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.payment_confirmation_msg),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val paise = Money.parseToPaise(amount)
                    val validationErrorRes = Validation.paymentAmount(paise, remainingPaise, allowOverpayment)
                    if (paise == null || validationErrorRes != null) {
                        localErrorRes = validationErrorRes ?: R.string.valid_amount_error
                        return@Button
                    }
                    onSave(
                        paise,
                        epochDay,
                        method,
                        null,
                        null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !saving
            ) {
                Text(
                    text = if (saving) stringResource(R.string.saving_dots) else stringResource(R.string.save_payment),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.cancel), fontSize = 16.sp)
            }
        }
    )
}
