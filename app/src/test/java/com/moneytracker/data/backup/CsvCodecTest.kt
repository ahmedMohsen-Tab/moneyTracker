package com.moneytracker.data.backup

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvCodecTest {

    @Test
    fun `plain comma-separated line splits into fields`() {
        assertEquals(
            listOf("a", "b", "c"),
            CsvCodec.parseLine("a,b,c")
        )
    }

    @Test
    fun `empty fields are preserved`() {
        assertEquals(
            listOf("a", "", "c"),
            CsvCodec.parseLine("a,,c")
        )
    }

    @Test
    fun `quoted field can contain commas`() {
        assertEquals(
            listOf("type", "id", "amount", "categoryId", "Hello, world", "2026-08-26"),
            CsvCodec.parseLine("type,id,amount,categoryId,\"Hello, world\",2026-08-26")
        )
    }

    @Test
    fun `escaped double quotes inside a quoted field become a single quote`() {
        // Field value is:  She said "hi"
        assertEquals(
            listOf("note", "She said \"hi\""),
            CsvCodec.parseLine("note,\"She said \"\"hi\"\"\"")
        )
    }

    @Test
    fun `unquoted field with bare double quote still toggles quote state`() {
        // The original bespoke parser had this behaviour — keep it stable.
        assertEquals(
            listOf("a", "b"),
            CsvCodec.parseLine("a,\"b")
        )
    }

    @Test
    fun `escape doubles every literal quote`() {
        // Source string contains two literal `"` characters. Each becomes `""`.
        assertEquals("She said \"\"hi\"\"", CsvCodec.escape("She said \"hi\""))
        assertEquals("plain text", CsvCodec.escape("plain text"))
    }

    @Test
    fun `round trip of an escaped field`() {
        val original = "Lunch, with \"special\" sauce"
        val encoded = CsvCodec.escape(original)
        val parsed = CsvCodec.parseLine("\"$encoded\"")
        assertEquals(listOf(original), parsed)
    }
}
