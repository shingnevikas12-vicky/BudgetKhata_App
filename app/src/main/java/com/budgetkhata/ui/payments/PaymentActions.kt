package com.budgetkhata.ui.payments

import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.budgetkhata.R
import com.budgetkhata.core.Money
import com.budgetkhata.data.local.model.EnrollmentRow
import com.budgetkhata.data.prefs.AppSettings
import com.budgetkhata.di.AppContainer
import com.budgetkhata.domain.ReminderContext
import com.budgetkhata.domain.ReminderMessageBuilder
import com.budgetkhata.integrations.QrCodeGenerator
import com.budgetkhata.integrations.UpiLinkBuilder
import com.budgetkhata.integrations.WhatsAppLaunchResult
import com.budgetkhata.ui.LocalAppSettings
import com.budgetkhata.ui.LocalSnackbarHostState
import com.budgetkhata.ui.components.InfoBanner
import kotlinx.coroutines.launch

class PaymentActions(
    private val container: AppContainer,
    private val settings: AppSettings
) {

    fun upiLink(row: EnrollmentRow): String? {
        if (settings.upiId.isBlank() || settings.payeeName.isBlank()) return null
        return UpiLinkBuilder.build(
            upiId = settings.upiId,
            payeeName = settings.payeeName,
            amountPaise = row.remainingPaise.takeIf { it > 0L },
            note = UpiLinkBuilder.note(row.eventName)
        )
    }

    fun buildReminder(row: EnrollmentRow): String = ReminderMessageBuilder.build(
        template = settings.reminderTemplate,
        context = ReminderContext(
            studentName = row.studentName,
            eventName = row.eventName,
            totalPaise = row.requiredPaise,
            paidPaise = row.paidPaise,
            pendingPaise = row.remainingPaise,
            upiId = settings.upiId,
            payeeName = settings.payeeName,
            instructions = settings.paymentInstructions,
            upiLink = upiLink(row)
        )
    )

    fun sendWhatsApp(context: Context, row: EnrollmentRow): WhatsAppLaunchResult =
        container.whatsAppLauncher.sendReminder(
            phone = row.studentPhone,
            message = buildReminder(row),
            countryCode = settings.defaultCountryCode
        )

    fun payWithUpi(context: Context, row: EnrollmentRow): Boolean {
        val link = upiLink(row) ?: return false
        return container.upiLauncher.open(link)
    }

    fun shareQr(context: Context, row: EnrollmentRow): Boolean {
        val link = upiLink(row) ?: return false
        val file = QrCodeGenerator.saveToCache(context, link, "upi-${row.enrollmentId}.png") ?: return false
        container.shareUtils.shareFile(
            file = file,
            mimeType = "image/png",
            title = context.getString(R.string.share_payment_qr),
            text = buildReminder(row)
        )
        return true
    }

    fun copyDetails(context: Context, row: EnrollmentRow) {
        val details = buildString {
            append("UPI ID: ").append(settings.upiId).append('\n')
            append("Payee: ").append(settings.payeeName).append('\n')
            append("Pending amount: ").append(Money.format(row.remainingPaise)).append('\n')
            upiLink(row)?.let { append("Payment link: ").append(it).append('\n') }
        }
        container.shareUtils.copyToClipboard(context.getString(R.string.upi_payment_qr), details)
    }
}

data class PaymentDialogContent(
    val title: String,
    val message: String,
    val primaryLabel: String? = null,
    val primaryAction: (() -> Unit)? = null
)

class PaymentActionHandler(
    private val context: Context,
    private val actions: PaymentActions,
    private val container: AppContainer,
    private val showMessage: (String) -> Unit
) {

    var dialog by mutableStateOf<PaymentDialogContent?>(null)
        private set

    fun dismissDialog() {
        dialog = null
    }

    fun remind(row: EnrollmentRow) {
        when (val result = actions.sendWhatsApp(context, row)) {
            WhatsAppLaunchResult.Launched ->
                showMessage(context.getString(R.string.whatsapp_opened_msg))

            WhatsAppLaunchResult.WhatsAppNotInstalled -> dialog = PaymentDialogContent(
                title = context.getString(R.string.whatsapp_not_installed_title),
                message = context.getString(R.string.whatsapp_not_installed_msg),
                primaryLabel = context.getString(R.string.copy_message),
                primaryAction = {
                    container.shareUtils.copyToClipboard(context.getString(R.string.whatsapp_reminder), actions.buildReminder(row))
                    showMessage(context.getString(R.string.reminder_message_copied))
                }
            )

            WhatsAppLaunchResult.InvalidNumber -> dialog = PaymentDialogContent(
                title = context.getString(R.string.mobile_number_needed_title),
                message = context.getString(R.string.mobile_number_needed_msg, row.studentName)
            )

            is WhatsAppLaunchResult.Failed -> dialog = PaymentDialogContent(
                title = context.getString(R.string.could_not_open_whatsapp),
                message = result.message,
                primaryLabel = context.getString(R.string.copy_message),
                primaryAction = {
                    container.shareUtils.copyToClipboard(context.getString(R.string.whatsapp_reminder), actions.buildReminder(row))
                    showMessage(context.getString(R.string.reminder_message_copied))
                }
            )
        }
    }

    fun payWithUpi(row: EnrollmentRow) {
        if (actions.upiLink(row) == null) {
            dialog = PaymentDialogContent(
                title = context.getString(R.string.upi_not_configured_title),
                message = context.getString(R.string.upi_not_configured_msg)
            )
            return
        }
        if (actions.payWithUpi(context, row)) {
            showMessage(
                context.getString(R.string.upi_app_opened_msg, Money.format(row.remainingPaise))
            )
        } else {
            dialog = PaymentDialogContent(
                title = context.getString(R.string.no_upi_app_found_title),
                message = context.getString(R.string.no_upi_app_found_msg),
                primaryLabel = context.getString(R.string.copy_details),
                primaryAction = {
                    actions.copyDetails(context, row)
                    showMessage(context.getString(R.string.upi_details_copied))
                }
            )
        }
    }

    fun shareQr(row: EnrollmentRow) {
        if (actions.upiLink(row) == null) {
            dialog = PaymentDialogContent(
                title = context.getString(R.string.upi_not_configured_title),
                message = context.getString(R.string.upi_not_configured_msg)
            )
            return
        }
        if (!actions.shareQr(context, row)) {
            dialog = PaymentDialogContent(
                title = context.getString(R.string.could_not_create_qr_title),
                message = context.getString(R.string.could_not_create_qr_msg),
                primaryLabel = context.getString(R.string.copy_details),
                primaryAction = {
                    actions.copyDetails(context, row)
                    showMessage(context.getString(R.string.upi_details_copied))
                }
            )
        }
    }

    fun copyUpiDetails(row: EnrollmentRow) {
        actions.copyDetails(context, row)
        showMessage(context.getString(R.string.upi_payment_details_copied))
    }

    fun copyMessage(row: EnrollmentRow) {
        container.shareUtils.copyToClipboard(context.getString(R.string.whatsapp_reminder), actions.buildReminder(row))
        showMessage(context.getString(R.string.reminder_message_copied))
    }
}

val LocalPaymentActions = staticCompositionLocalOf<PaymentActionHandler?> { null }

@Composable
fun PaymentActionsHost(container: AppContainer, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val settings = LocalAppSettings.current
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    val handler = remember(container, settings) {
        PaymentActionHandler(
            context = context,
            actions = PaymentActions(container, settings),
            container = container,
            showMessage = { message ->
                scope.launch { snackbarHostState?.showSnackbar(message) }
            }
        )
    }

    CompositionLocalProvider(LocalPaymentActions provides handler) {
        content()

        val dialog = handler.dialog
        if (dialog != null) {
            AlertDialog(
                onDismissRequest = { handler.dismissDialog() },
                title = { Text(dialog.title) },
                text = { Text(dialog.message) },
                confirmButton = {
                    if (dialog.primaryLabel != null) {
                        TextButton(
                            onClick = {
                                dialog.primaryAction?.invoke()
                                handler.dismissDialog()
                            }
                        ) { Text(dialog.primaryLabel) }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { handler.dismissDialog() }) { Text(stringResource(R.string.close)) }
                }
            )
        }
    }
}

@Composable
fun UpiConfirmationInfo(modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    InfoBanner(
        text = stringResource(R.string.record_payment_banner),
        modifier = modifier
    )
}
