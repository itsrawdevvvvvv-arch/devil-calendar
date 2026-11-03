package com.devil.calendar.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeySanitizerTest {

    @Test
    fun `trims whitespace and newlines`() {
        assertEquals("sk_abc123", KeySanitizer.sanitize("  sk_abc123 \n"))
    }

    @Test
    fun `strips a bearer prefix in any case`() {
        assertEquals("sk_abc123", KeySanitizer.sanitize("Bearer sk_abc123"))
        assertEquals("sk_abc123", KeySanitizer.sanitize("bearer sk_abc123"))
    }

    @Test
    fun `strips wrapping quotes and backticks`() {
        assertEquals("sk_abc123", KeySanitizer.sanitize("\"sk_abc123\""))
        assertEquals("sk_abc123", KeySanitizer.sanitize("`sk_abc123`"))
    }

    @Test
    fun `removes zero width and non-breaking characters`() {
        val dirty = "sk_ab\u200Bc\u00A0123"
        assertEquals("sk_abc123", KeySanitizer.sanitize(dirty))
    }

    @Test
    fun `warns rather than rejects a non standard key`() {
        assertFalse(KeySanitizer.looksStandard("abc123"))
        assertTrue(KeySanitizer.looksStandard("sk_abc123"))
    }

    @Test
    fun `rejects obviously unusable input`() {
        assertFalse(KeySanitizer.isUsable(""))
        assertFalse(KeySanitizer.isUsable("abc"))
        assertTrue(KeySanitizer.isUsable("sk_abcdefgh"))
    }
}
