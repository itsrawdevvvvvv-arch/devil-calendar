package com.devil.calendar.data.ai

/**
 * Pasting a key from a chat window or a password manager routinely brings
 * invisible characters, quotes or a whole `Bearer ` prefix along with it. All of
 * that is stripped here rather than rejected, because the user cannot see it.
 */
object KeySanitizer {

    fun sanitize(raw: String): String {
        // 1. Drop anything outside printable ASCII: zero-width spaces, NBSP,
        //    bidi marks and stray control characters all break an HTTP header.
        val printable = raw.filter { it.code in 32..126 }
        // 2. Quotes and backticks from copy-paste.
        var key = printable.trim().trim('"', '\'', '`').trim()
        // 3. A pasted `Bearer <key>` (any case).
        if (key.startsWith("Bearer ", ignoreCase = true)) {
            key = key.substring(7).trim()
        }
        // 4. Strip any quotes that were inside the Bearer prefix.
        return key.trim('"', '\'', '`').trim()
    }

    /** Keys normally start with `sk_`; we warn rather than reject. */
    fun looksStandard(key: String): Boolean = key.startsWith("sk_")

    fun isUsable(key: String): Boolean = key.length in 8..200
}
