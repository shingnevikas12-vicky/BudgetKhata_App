package com.budgetkhata.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidationTest {

    @Test
    fun `name requires at least two characters`() {
        assertNotNull(Validation.personName(""))
        assertNotNull(Validation.personName("A"))
        assertNull(Validation.personName("Rahul"))
    }

    @Test
    fun `phone accepts local and international formats`() {
        assertNull(Validation.phone("9876543210", "91"))
        assertNull(Validation.phone("+91 98765 43210", "91"))
        assertNotNull(Validation.phone("123", "91"))
        assertNotNull(Validation.phone("", "91"))
    }

    @Test
    fun `contribution must be positive`() {
        assertNotNull(Validation.contribution(null))
        assertNotNull(Validation.contribution(0L))
        assertNull(Validation.contribution(50_000L))
    }

    @Test
    fun `payment cannot exceed pending balance unless allowed`() {
        assertNotNull(Validation.paymentAmount(60_000L, 50_000L, allowOverpayment = false))
        assertNull(Validation.paymentAmount(60_000L, 50_000L, allowOverpayment = true))
        assertNull(Validation.paymentAmount(50_000L, 50_000L, allowOverpayment = false))
        assertNotNull(Validation.paymentAmount(0L, 50_000L, allowOverpayment = false))
    }

    @Test
    fun `upi id can be blank to disable upi`() {
        assertNull(Validation.upiIdOptional(""))
        assertNull(Validation.upiIdOptional("organiser@okhdfcbank"))
        assertNotNull(Validation.upiIdOptional("organiser@"))
    }

    @Test
    fun `pin must be four to six digits`() {
        assertNotNull(Validation.pin("123"))
        assertNotNull(Validation.pin("1234567"))
        assertNotNull(Validation.pin("12a4"))
        assertNull(Validation.pin("1234"))
        assertNull(Validation.pin("123456"))
    }

    @Test
    fun `plural helper picks the right form`() {
        assertEquals("1 student", plural(1, "student"))
        assertEquals("2 students", plural(2, "student"))
        assertEquals("1 active event", plural(1L, "active event"))
    }
}
