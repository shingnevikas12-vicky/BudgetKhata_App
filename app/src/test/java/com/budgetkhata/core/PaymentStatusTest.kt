package com.budgetkhata.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentStatusTest {

    @Test
    fun `fully paid when collected equals required`() {
        assertEquals(PaymentStatus.PAID, PaymentStatus.of(50_000L, 50_000L))
    }

    @Test
    fun `paid when collected exceeds required`() {
        assertEquals(PaymentStatus.PAID, PaymentStatus.of(50_000L, 60_000L))
    }

    @Test
    fun `partial when something is paid`() {
        assertEquals(PaymentStatus.PARTIAL, PaymentStatus.of(50_000L, 20_000L))
    }

    @Test
    fun `pending when nothing is paid`() {
        assertEquals(PaymentStatus.PENDING, PaymentStatus.of(50_000L, 0L))
    }

    @Test
    fun `zero contribution is treated as paid`() {
        assertEquals(PaymentStatus.PAID, PaymentStatus.of(0L, 0L))
    }
}
