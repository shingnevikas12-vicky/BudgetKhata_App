package com.budgetkhata.integrations

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.budgetkhata.core.PhoneUtils

sealed interface WhatsAppLaunchResult {
    data object Launched : WhatsAppLaunchResult
    data object WhatsAppNotInstalled : WhatsAppLaunchResult
    data object InvalidNumber : WhatsAppLaunchResult
    data class Failed(val message: String) : WhatsAppLaunchResult
}

/**
 * Opens WhatsApp with the recipient and a pre-filled message. The administrator always
 * sends the message manually — nothing is sent automatically by the app.
 */
class WhatsAppLauncher(private val context: Context) {

    fun isInstalled(): Boolean =
        selectedPackage() != null

    fun sendReminder(
        phone: String?,
        message: String,
        countryCode: String
    ): WhatsAppLaunchResult {
        val digits = PhoneUtils.normalize(phone, countryCode)
            ?: return WhatsAppLaunchResult.InvalidNumber

        val targetPackage = selectedPackage() ?: return WhatsAppLaunchResult.WhatsAppNotInstalled
        val encodedMessage = Uri.encode(message)

        val candidates = listOf(
            Uri.parse("whatsapp://send?phone=$digits&text=$encodedMessage"),
            Uri.parse("https://wa.me/$digits?text=$encodedMessage")
        )

        for (uri in candidates) {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(targetPackage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                return WhatsAppLaunchResult.Launched
            } catch (error: Exception) {
                // Try the next supported URI shape.
            }
        }

        return WhatsAppLaunchResult.Failed(
            "WhatsApp could not open on this device. Copy the message instead."
        )
    }

    private fun selectedPackage(): String? {
        if (isPackageInstalled("com.whatsapp")) return "com.whatsapp"
        if (isPackageInstalled("com.whatsapp.w4b")) return "com.whatsapp.w4b"
        return null
    }

    private fun isPackageInstalled(packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (error: PackageManager.NameNotFoundException) {
        false
    }
}
