package com.budgetkhata.domain

import com.budgetkhata.core.Money

data class ReminderContext(
    val studentName: String,
    val eventName: String,
    val totalPaise: Long,
    val paidPaise: Long,
    val pendingPaise: Long,
    val upiId: String,
    val payeeName: String,
    val instructions: String,
    val upiLink: String?
)

object ReminderMessageBuilder {

    fun build(template: String, context: ReminderContext): String {
        val upiBlock = buildString {
            if (context.upiId.isNotBlank()) {
                append("UPI ID: ").append(context.upiId).append('\n')
                if (context.payeeName.isNotBlank()) {
                    append("Payee Name: ").append(context.payeeName).append('\n')
                }
                if (!context.upiLink.isNullOrBlank()) {
                    append("Payment Link: ").append(context.upiLink).append('\n')
                }
                if (context.instructions.isNotBlank()) {
                    append('\n').append(context.instructions).append('\n')
                }
                append('\n')
            }
        }

        return template
            .replace("{name}", context.studentName)
            .replace("{event}", context.eventName)
            .replace("{total}", Money.format(context.totalPaise))
            .replace("{paid}", Money.format(context.paidPaise))
            .replace("{pending}", Money.format(context.pendingPaise))
            .replace("{upi_id}", context.upiId)
            .replace("{payee}", context.payeeName)
            .replace("{upi_link}", context.upiLink.orEmpty())
            .replace("{instructions}", context.instructions)
            .replace("{upi_block}", upiBlock)
            .trim()
    }

    val placeholders: List<String> = listOf(
        "{name}",
        "{event}",
        "{total}",
        "{paid}",
        "{pending}",
        "{upi_id}",
        "{payee}",
        "{upi_link}",
        "{instructions}",
        "{upi_block}"
    )
}
