package com.budgetkhata.ui.settings

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.budgetkhata.R
import com.budgetkhata.core.Validation
import com.budgetkhata.di.AppContainer
import com.budgetkhata.integrations.QrCodeGenerator
import com.budgetkhata.integrations.UpiLinkBuilder
import com.budgetkhata.ui.LocalSnackbarHostState
import com.budgetkhata.ui.appViewModel
import com.budgetkhata.ui.components.AppTextField
import com.budgetkhata.ui.components.AppTopBar
import com.budgetkhata.ui.components.InfoBanner
import com.budgetkhata.ui.components.SectionHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun UpiSettingsScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val viewModel = appViewModel(container) { app, _ -> UpiSettingsViewModel(app) }
    val form = viewModel.form
    val snackbar = LocalSnackbarHostState.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val qrPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    try {
                        val directory = File(context.filesDir, "qr").apply { mkdirs() }
                        val target = File(directory, "user_qr.png")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(target).use { output -> input.copyTo(output) }
                        }
                        target.absolutePath
                    } catch (_: Exception) {
                        null
                    }
                }
                if (saved != null) {
                    viewModel.updateUserQrPath(saved)
                } else {
                    snackbar?.showSnackbar(context.getString(R.string.err_image_read))
                }
            }
        }
    }

    LaunchedEffect(viewModel.message) {
        val messageRes = viewModel.message
        if (messageRes != null) {
            snackbar?.showSnackbar(context.getString(messageRes))
            viewModel.clearMessage()
        }
    }

    val upiId = form.upiId.trim()
    val payee = form.payeeName.trim()
    val link = remember(upiId, payee) {
        if (Validation.upiIdOptional(upiId) == null && payee.isNotBlank()) {
            UpiLinkBuilder.build(upiId, payee, note = "Contribution")
        } else {
            null
        }
    }

    val generatedQr = remember(link) {
        link?.let { QrCodeGenerator.generate(it, 768)?.asImageBitmap() }
    }
    val customQr = remember(viewModel.userQrPath) {
        viewModel.userQrPath?.let { path -> BitmapFactory.decodeFile(path)?.asImageBitmap() }
    }
    val qrBitmap = customQr ?: generatedQr

    fun shareQr() {
        scope.launch {
            val file = withContext(Dispatchers.IO) {
                val customPath = viewModel.userQrPath
                if (customPath != null && File(customPath).exists()) {
                    File(customPath)
                } else {
                    link?.let { QrCodeGenerator.saveToCache(context, it, "permanent-upi-qr.png") }
                }
            }
            if (file == null) {
                snackbar?.showSnackbar(context.getString(R.string.err_upi_config_required))
            } else {
                container.shareUtils.shareFile(
                    file = file,
                    mimeType = "image/png",
                    title = "Share payment QR",
                    text = "Pay using UPI\nUPI ID: $upiId\nPayee: $payee"
                )
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.upi_payment_qr),
                onBack = onBack,
                actions = {
                    TextButton(
                        onClick = { viewModel.save(onBack) },
                        enabled = !viewModel.saving
                    ) { Text(stringResource(R.string.save)) }
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Permanent receive QR",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = payee.ifBlank { "Set a payee name below" },
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = upiId.ifBlank { "No UPI ID set" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.size(14.dp))

                    if (qrBitmap != null) {
                        Box(
                            modifier = Modifier
                                .size(230.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(androidx.compose.ui.graphics.Color.White)
                                .padding(10.dp)
                        ) {
                            Image(
                                bitmap = qrBitmap,
                                contentDescription = "Permanent UPI payment QR code",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    } else {
                        Text(
                            text = "Add a valid UPI ID and payee name to generate the QR.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = "Scan with any UPI app \u00B7 Paytm, Google Pay, PhonePe, BHIM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.size(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(onClick = { shareQr() }) { Text(stringResource(R.string.share_qr)) }
                        Spacer(Modifier.size(8.dp))
                        OutlinedButton(
                            onClick = {
                                container.shareUtils.copyToClipboard("UPI ID", upiId)
                                scope.launch { snackbar?.showSnackbar(context.getString(R.string.upi_id_copied)) }
                            }
                        ) { Text(stringResource(R.string.copy) + " UPI ID") }
                    }

                    Spacer(Modifier.size(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(onClick = { qrPicker.launch(arrayOf("image/*")) }) {
                            Text(if (customQr == null) "Use own QR image" else "Replace QR image")
                        }
                        if (customQr != null) {
                            Spacer(Modifier.size(8.dp))
                            TextButton(onClick = { viewModel.updateUserQrPath(null) }) { Text("Use generated") }
                        }
                    }
                }
            }

            InfoBanner(
                text = "Opening a UPI app or scanning this QR does not confirm a payment. Record each " +
                    "payment in the student profile after you verify it. For automatic verification you " +
                    "need a payment gateway with server-side checks. The UPI PIN is always entered inside " +
                    "the payment app \u2014 it is never stored here.",
                centered = true
            )

            SectionHeader(
                title = stringResource(R.string.upi_payment_qr) + " " + stringResource(R.string.settings),
                subtitle = "These details are added to every WhatsApp reminder and payment QR.",
                centered = true
            )

            AppTextField(
                value = form.upiId,
                onValueChange = viewModel::updateUpiId,
                label = "UPI ID (VPA)",
                placeholder = "name@bank",
                isError = form.upiIdError != null,
                errorText = form.upiIdError
            )

            AppTextField(
                value = form.payeeName,
                onValueChange = viewModel::updatePayeeName,
                label = "Payee name",
                isError = form.payeeError != null,
                errorText = form.payeeError
            )

            AppTextField(
                value = form.instructions,
                onValueChange = viewModel::updateInstructions,
                label = "Payment instructions",
                singleLine = false,
                maxLines = 5
            )

            Button(
                onClick = { viewModel.save(onBack) },
                enabled = !viewModel.saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (viewModel.saving) stringResource(R.string.loading) else stringResource(R.string.save) + " UPI settings")
            }

            if (link != null) {
                TextButton(
                    onClick = {
                        val launched = container.upiLauncher.open(link)
                        scope.launch {
                            snackbar?.showSnackbar(
                                if (launched) {
                                    context.getString(R.string.upi_app_opened)
                                } else {
                                    context.getString(R.string.no_upi_app_found)
                                }
                            )
                        }
                    }
                ) { Text("Test UPI link") }

                TextButton(
                    onClick = {
                        container.shareUtils.copyToClipboard("UPI payment link", link)
                        scope.launch { snackbar?.showSnackbar(context.getString(R.string.upi_link_copied)) }
                    }
                ) { Text(stringResource(R.string.copy) + " UPI link") }

                Text(
                    text = link,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            Spacer(Modifier.size(24.dp))
        }
    }
}
