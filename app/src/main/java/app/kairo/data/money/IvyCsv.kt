package app.kairo.data.money

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * One row of an Ivy Wallet CSV (Settings → Export to CSV in Ivy):
 * Date, Title, Category, Account, Amount, Currency, Type, Transfer Amount, Transfer Currency,
 * To Account, Receive Amount, Receive Currency, Description, Due Date, ID.
 * A planned payment has a Due Date but no Date.
 */
data class IvyRow(
    val date: LocalDate?, val time: LocalTime?, val title: String, val category: String, val account: String,
    val paise: Long, val type: String, val toAccount: String, val description: String, val due: LocalDate?,
) {
    val planned get() = date == null && due != null
}

/** Reading the file: bytes to text, text to rows. Nothing is written here. */
object IvyCsv {
    /**
     * File bytes to text. Handles the byte-order mark Ivy and spreadsheets put first (it made the
     * first header read as "﻿date", so "no date column") and UTF-16 "Unicode text" saves.
     */
    fun decode(b: ByteArray): String {
        val cs = when {
            b.size >= 2 && b[0] == 0xFF.toByte() && b[1] == 0xFE.toByte() -> Charsets.UTF_16LE
            b.size >= 2 && b[0] == 0xFE.toByte() && b[1] == 0xFF.toByte() -> Charsets.UTF_16BE
            // UTF-16 without a mark: every other byte of ASCII text is zero.
            b.size >= 4 && b[1] == 0.toByte() && b[3] == 0.toByte() -> Charsets.UTF_16LE
            else -> Charsets.UTF_8
        }
        return String(b, cs).trimStart('﻿')
    }

    /** The rows and the column count. Throws with a plain message when it isn't an Ivy CSV. */
    fun parse(text0: String): Pair<List<IvyRow>, Int> {
        val lines = text0.trimStart('﻿').lines().filter { it.isNotBlank() }
        require(lines.isNotEmpty()) { "The file is empty" }
        val delim = if (lines[0].count { it == '\t' } > lines[0].count { it == ',' }) '\t' else ','
        val header = split(lines[0], delim).map { it.replace("﻿", "").trim().trim('"').trim().lowercase() }
        fun col(name: String) = header.indexOf(name).also { require(it >= 0) { "Not an Ivy Wallet CSV (no \"$name\" column)" } }
        val iDate = col("date"); val iTitle = col("title"); val iCat = col("category"); val iAcc = col("account")
        val iAmt = col("amount"); val iType = col("type"); val iTo = header.indexOf("to account")
        val iDesc = header.indexOf("description"); val iDue = header.indexOf("due date")
        val rows = lines.drop(1).map { line ->
            val f = split(line, delim)
            fun at(i: Int) = if (i < 0) "" else f.getOrNull(i)?.trim().orEmpty()
            val (date, time) = parseDateTime(at(iDate))
            IvyRow(
                date, time, at(iTitle), at(iCat), at(iAcc),
                Money.parsePaise(at(iAmt).replace(",", "").removePrefix("-")),
                at(iType).uppercase().ifBlank { "EXPENSE" }, at(iTo), at(iDesc), parseDateTime(at(iDue)).first,
            )
        }
        return rows to header.size
    }

    private val formats = listOf("M/d/yyyy H:mm", "M/d/yyyy", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd", "d/M/yyyy H:mm")
        .map { DateTimeFormatter.ofPattern(it) }

    /** The date, and the time when the cell has one. */
    internal fun parseDateTime(s0: String): Pair<LocalDate?, LocalTime?> {
        val s = s0.trim()
        if (s.isBlank()) return null to null
        fun dt(d: LocalDateTime) = d.toLocalDate() to d.toLocalTime().withSecond(0).withNano(0)
        // Ivy's own export: ISO "2026-11-21T20:25:44.269" (fraction of any length, maybe a zone).
        runCatching { return dt(LocalDateTime.parse(s)) }
        runCatching { return dt(java.time.OffsetDateTime.parse(s).toLocalDateTime()) }
        runCatching { return dt(java.time.ZonedDateTime.parse(s).toLocalDateTime()) }
        for (f in formats) {
            runCatching { return dt(LocalDateTime.parse(s, f)) }
            runCatching { return LocalDate.parse(s, f) to null }
        }
        return null to null
    }

    internal fun parseDate(s: String): LocalDate? = parseDateTime(s).first

    /** One CSV line, honoring "quoted, fields" and "" escapes. */
    private fun split(line: String, delim: Char): List<String> {
        val out = mutableListOf<String>(); val cur = StringBuilder(); var q = false; var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && q && line.getOrNull(i + 1) == '"' -> { cur.append('"'); i++ }
                ch == '"' -> q = !q
                ch == delim && !q -> { out += cur.toString(); cur.clear() }
                else -> cur.append(ch)
            }
            i++
        }
        out += cur.toString()
        return out
    }
}
