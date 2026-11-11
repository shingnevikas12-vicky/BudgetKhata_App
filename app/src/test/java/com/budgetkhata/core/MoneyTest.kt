package com.budgetkhata.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun `parses plain rupees`() {
        assertEquals(50_000L, Money.parseToPaise("500"))
    }

    @Test
    fun `parses decimals with commas and symbol`() {
        assertEquals(150_050L, Money.parseToPaise("\u20B91,500.50"))
    }

    @Test
    fun `parses trailing slash notation`() {
        assertEquals(30_000L, Money.parseToPaise("300/-"))
    }

    @Test
    fun `rejects letters and too many decimals`() {
        assertNull(Money.parseToPaise("abc"))
        assertNull(Money.parseToPaise("10.999"))
        assertNull(Money.parseToPaise(""))
    }

    @Test
    fun `formats with indian grouping`() {
        assertEquals("\u20B91,23,456", Money.format(12_345_600L))
        assertEquals("\u20B91,23,45,678", Money.format(1_234_567_800L))
        assertEquals("\u20B9500", Money.format(50_000L))
        assertEquals("\u20B9500.25", Money.format(50_025L))
    }

    @Test
    fun `formats negative amounts`() {
        assertEquals("-\u20B925", Money.format(-2_500L))
    }

    @Test
    fun `upi amount always has two decimals`() {
        assertEquals("300.00", Money.upiAmount(30_000L))
        assertEquals("300.50", Money.upiAmount(30_050L))
    }

    @Test
    fun `percent is clamped`() {
        assertEquals(50f, Money.percent(50, 100), 0.01f)
        assertEquals(100f, Money.percent(200, 100), 0.01f)
        assertEquals(0f, Money.percent(10, 0), 0.01f)
    }
}
