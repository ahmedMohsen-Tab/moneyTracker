package com.moneytracker.data.backup

/**
 * Tiny RFC-4180-ish CSV codec used by [com.moneytracker.data.repository.BackupRepository].
 *
 * One record per line — embedded newlines are not supported. Quoted fields
 * honour the `""` escape for a literal double quote.
 */
internal object CsvCodec {

    fun escape(field: String): String = field.replace("\"", "\"\"")

    fun parseLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val char = line[i]
            when {
                char == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(char)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
