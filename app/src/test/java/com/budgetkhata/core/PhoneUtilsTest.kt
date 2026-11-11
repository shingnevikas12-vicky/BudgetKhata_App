package com.budgetkhata.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneUtilsTest {

    @Test
    fun `adds default country code to ten digit numbers`() {
        assertEquals("919876543210", PhoneUtils.normalize("98765 43210"))
    }

    @Test
    fun `keeps numbers that already have the country code`() {
        assertEquals("919876543210", PhoneUtils.normalize("+91 98765 43210"))
    }

    @Test
    fun `drops international prefix and leading zero`() {
        assertEquals("919876543210", PhoneUtils.normalize("0091 9876543210"))
        assertEquals("919876543210", PhoneUtils.normalize("09876543210"))
    }

    @Test
    fun `rejects empty or too short numbers`() {
        assertNull(PhoneUtils.normalize(""))
        assertNull(PhoneUtils.normalize("123"))
    }

    @Test
    fun `display adds plus and spacing`() {
        assertEquals("+91 98765 43210", PhoneUtils.display("919876543210"))
    }
}
