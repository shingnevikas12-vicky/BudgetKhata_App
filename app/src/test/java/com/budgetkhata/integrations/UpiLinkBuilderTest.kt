package com.budgetkhata.integrations

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpiLinkBuilderTest {

    @Test
    fun `builds amount specific link`() {
        val link = UpiLinkBuilder.build(
            upiId = "organiser@okhdfcbank",
            payeeName = "Om Kadam",
            amountPaise = 30_050L,
            note = "College Jatra contribution"
        )
        assertTrue(link.startsWith("upi://pay?"))
        assertTrue(link.contains("pa=organiser%40okhdfcbank"))
        assertTrue(link.contains("pn=Om%20Kadam"))
        assertTrue(link.contains("am=300.50"))
        assertTrue(link.contains("cu=INR"))
        assertTrue(link.contains("tn=College%20Jatra%20contribution"))
    }

    @Test
    fun `omits amount when zero`() {
        val link = UpiLinkBuilder.build("a@bank", "Payee", 0L)
        assertFalse(link.contains("am="))
    }

    @Test
    fun `validates upi ids`() {
        assertTrue(UpiLinkBuilder.isValidUpiId("name.surname-1@okaxis"))
        assertFalse(UpiLinkBuilder.isValidUpiId("name@"))
        assertFalse(UpiLinkBuilder.isValidUpiId("no-at-sign"))
    }

    @Test
    fun `note describes the event`() {
        assertEquals("Contribution - NSS Camp", UpiLinkBuilder.note("NSS Camp"))
    }
}
