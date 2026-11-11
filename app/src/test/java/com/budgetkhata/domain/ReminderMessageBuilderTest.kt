package com.budgetkhata.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderMessageBuilderTest {

    private val context = ReminderContext(
        studentName = "Rahul",
        eventName = "College Jatra",
        totalPaise = 50_000L,
        paidPaise = 20_000L,
        pendingPaise = 30_000L,
        upiId = "organiser@okhdfcbank",
        payeeName = "Om Kadam",
        instructions = "Share the screenshot after paying.",
        upiLink = "upi://pay?pa=organiser@okhdfcbank&pn=Om%20Kadam&am=300.00&cu=INR"
    )

    @Test
    fun `replaces every placeholder`() {
        val message = ReminderMessageBuilder.build(
            template = "{name}|{event}|{total}|{paid}|{pending}",
            context = context
        )
        assertEquals("Rahul|College Jatra|\u20B9500|\u20B9200|\u20B9300", message)
    }

    @Test
    fun `upi block contains id, payee, link and instructions`() {
        val message = ReminderMessageBuilder.build("{upi_block}", context)
        assertTrue(message.contains("organiser@okhdfcbank"))
        assertTrue(message.contains("Om Kadam"))
        assertTrue(message.contains("upi://pay?"))
        assertTrue(message.contains("Share the screenshot"))
    }

    @Test
    fun `upi block is empty when upi is not configured`() {
        val message = ReminderMessageBuilder.build(
            template = "Hello {name}{upi_block}",
            context = context.copy(upiId = "", payeeName = "", upiLink = null)
        )
        assertEquals("Hello Rahul", message)
    }

    @Test
    fun `unknown placeholders stay untouched`() {
        val message = ReminderMessageBuilder.build("Hi {name} {unknown}", context)
        assertEquals("Hi Rahul {unknown}", message)
    }

    @Test
    fun `default template includes student event and amounts`() {
        val message = ReminderMessageBuilder.build(
            com.budgetkhata.data.prefs.DEFAULT_REMINDER_TEMPLATE,
            context
        )
        assertTrue(message.startsWith("Hello Rahul"))
        assertTrue(message.contains("College Jatra"))
        assertTrue(message.contains("\u20B9300"))
        assertTrue(message.contains("UPI ID"))
        assertFalse(message.contains("{name}"))
    }
}
