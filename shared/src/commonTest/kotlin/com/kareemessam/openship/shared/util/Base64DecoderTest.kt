package com.kareemessam.openship.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals

class Base64DecoderTest {

    @Test
    fun testValidBase64Decode() {
        // "Running build command: ./gradlew assemble" encoded in base64
        val encoded = "UnVubmluZyBidWlsZCBjb21tYW5kOiAuL2dyYWRsZXcgYXNzZW1ibGU="
        val decoded = Base64Decoder.decodeToString(encoded)
        assertEquals("Running build command: ./gradlew assemble", decoded)
    }

    @Test
    fun testEmptyAndBlankStrings() {
        assertEquals("", Base64Decoder.decodeToString(""))
        assertEquals("", Base64Decoder.decodeToString("   "))
    }

    @Test
    fun testMultilineBase64String() {
        // Base64 with carriage returns/newlines should be cleanly decoded
        val multiline = "SGVsbG8g\nV29ybGQ="
        val decoded = Base64Decoder.decodeToString(multiline)
        assertEquals("Hello World", decoded)
    }

    @Test
    fun testPlainTextFallback() {
        // If the server accidentally sends plain text, it returns the raw text gracefully
        val plain = "This is not base64 !!! @@@"
        val decoded = Base64Decoder.decodeToString(plain)
        assertEquals("This is not base64 !!! @@@", decoded)
    }
}
