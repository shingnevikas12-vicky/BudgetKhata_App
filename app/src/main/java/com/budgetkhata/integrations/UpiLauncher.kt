package com.budgetkhata.integrations

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Launches a UPI deep link. Opening a UPI app never confirms a payment in this app —
 * the administrator must record the received amount manually.
 */
class UpiLauncher(private val context: Context) {

    fun open(link: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(Intent.createChooser(intent, "Pay with UPI").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (error: ActivityNotFoundException) {
            false
        } catch (error: Exception) {
            false
        }
    }
}
