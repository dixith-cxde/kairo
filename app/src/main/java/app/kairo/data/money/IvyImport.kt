package app.kairo.data.money

import app.kairo.data.db.AccountType
import app.kairo.data.db.TxKind
import app.kairo.data.model.ListColor
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Imports (and exports) an Ivy Wallet CSV (Settings → Export to CSV in Ivy):
 * Date, Title, Category, Account, Amount, Currency, Type, Transfer Amount, Transfer Currency,
 * To Account, Receive Amount, Receive Currency, Description, Due Date, ID.
 * Accounts and categories are matched by name (case-insensitive) or created. Rows already imported
 * (same date, account, kind, amount, title) are skipped, so importing twice is safe.
 * Planned payments (a Due Date but no Date) are skipped.
 */
object IvyImport {
    data class Result(val added: Int, val skipped: Int, val accountsCreated: Int, val categoriesCreated: Int)

    /**
     * File bytes to text. Handles the byte-order mark Ivy and spreadsheets put first (it made the
     * first header read as "\uFEFFdate", so "no date column") and UTF-16 "Unicode text" saves.
     */
    fun decode(b: ByteArray): String {
        val cs = when {
            b.size >= 2 && b[0] == 0xFF.toByte() && b[1] == 0xFE.toByte() -> Charsets.UTF_16LE
            b.size >= 2 && b[0] == 0xFE.toByte() && b[1] == 0xFF.toByte() -> Charsets.UTF_16BE
            // UTF-16 without a mark: every other byte of ASCII text is zero.
            b.size >= 4 && b[1] == 0.toByte() && b[3] == 0.toByte() -> Charsets.UTF_16LE
            else -> Charsets.UTF_8
        }
        return String(b, cs).trimStart('\uFEFF')
    }

    suspend fun import(money: MoneyRepository, text0: String): Result {
        val text = text0.trimStart('\uFEFF')
        val lines = text.lines().filter { it.isNotBlank() }
        require(lines.isNotEmpty()) { "The file is empty" }
        val delim = if (lines[0].count { it == '\t' } > lines[0].count { it == ',' }) '\t' else ','
        val header = split(lines[0], delim).map { it.replace("\uFEFF", "").trim().trim('"').trim().lowercase() }
        fun col(name: String) = header.indexOf(name).also { require(it >= 0) { "Not an Ivy Wallet CSV (no \"$name\" column)" } }
        val iDate = col("date"); val iTitle = col("title"); val iCat = col("category"); val iAcc = col("account")
        val iAmt = col("amount"); val iType = col("type"); val iTo = header.indexOf("to account")
        val iRecv = header.indexOf("receive amount"); val iDesc = header.indexOf("description")

        val accounts = money.accounts.first().associateBy { it.name.lowercase() }.mapValues { it.value.id }.toMutableMap()
        val cats = money.categories.first().associateBy { it.name.lowercase() to it.income }.mapValues { it.value.id }.toMutableMap()
        val seen = money.transactions.first().map { key(it.date, it.accountId, it.kind, kotlin.math.abs(it.amountPaise), it.note) }.toMutableSet()
        var added = 0; var skipped = 0; var newAcc = 0; var newCat = 0

        suspend fun account(name: String): Long = accounts.getOrPut(name.trim().lowercase()) {
            newAcc++
            money.addAccount(name.trim().ifBlank { "Imported" }, if (name.contains("cash", true)) AccountType.Cash else AccountType.Bank,
                ListColor.entries[accounts.size % ListColor.entries.size], 0)
        }
        suspend fun category(name: String, income: Boolean): Long? {
            if (name.isBlank()) return null
            return cats.getOrPut(name.trim().lowercase() to income) {
                newCat++
                money.addCategory(name.trim(), if (income) "savings" else "category",
                    ListColor.entries[(name.lowercase().hashCode() and 0x7fffffff) % ListColor.entries.size], income)
            }
        }

        for (line in lines.drop(1)) {
            val f = split(line, delim)
            fun at(i: Int) = f.getOrNull(i)?.trim().orEmpty()
            val date = parseDate(at(iDate))
            val paise = Money.parsePaise(at(iAmt).replace(",", "").removePrefix("-"))
            if (date == null || paise <= 0) { skipped++; continue }
            val title = at(iTitle).ifBlank { if (iDesc >= 0) at(iDesc) else "" }
            // Empty columns fall back to defaults: no type = expense, no account = "Imported".
            when (at(iType).uppercase().ifBlank { "EXPENSE" }) {
                "EXPENSE", "INCOME" -> {
                    val kind = if (at(iType).uppercase() == "INCOME") TxKind.Income else TxKind.Expense

                    val acc = account(at(iAcc))
                    if (!seen.add(key(date, acc, kind, paise, title))) { skipped++; continue }
                    money.add(kind, paise, acc, category(at(iCat), kind == TxKind.Income), date, title, "")
                    added++
                }
                "TRANSFER" -> {
                    val from = account(at(iAcc))
                    val to = account(at(iTo).ifBlank { "Imported" })
                    if (!seen.add(key(date, from, TxKind.Transfer, paise, title))) { skipped++; continue }
                    // Different currencies on each side aren't converted (Kairo is single-currency); use the sent amount.
                    money.transfer(paise, from, to, date, title)
                    added++
                }
                else -> skipped++
            }
        }
        return Result(added, skipped, newAcc, newCat)
    }

    /**
     * Writes every transaction as an Ivy Wallet CSV with the same 15 columns Ivy exports, so the file
     * imports back into Ivy or into Kairo. Kairo keeps dates without a time, so every row uses 12:00.
     * A transfer is one row (the sending leg) with To Account and Receive Amount filled.
     * IDs are stable UUIDs derived from Kairo's row id, so re-exports keep the same IDs.
     */
    suspend fun export(money: MoneyRepository): Pair<String, Int> {
        val accounts = money.accounts.first().associateBy { it.id }
        val cats = money.categories.first().associateBy { it.id }
        val txs = money.transactions.first()
        val byId = txs.associateBy { it.id }
        val date = DateTimeFormatter.ofPattern("M/d/yyyy")
        fun amt(p: Long) = kotlin.math.abs(p).let { "${it / 100}.${(it % 100).toString().padStart(2, '0')}" }
        fun cell(s: String) = if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
        val sb = StringBuilder(HEADER.joinToString(",")).append("\r\n")
        var rows = 0
        for (t in txs.sortedWith(compareBy({ it.date }, { it.id }))) {
            val acc = accounts[t.accountId]
            val cur = acc?.currency ?: "INR"
            val row = when (t.kind) {
                TxKind.Expense, TxKind.Income -> listOf(
                    "${t.date.format(date)} 12:00", t.note, t.categoryId?.let { cats[it]?.name }.orEmpty(), acc?.name.orEmpty(),
                    amt(t.amountPaise), cur, if (t.kind == TxKind.Income) "INCOME" else "EXPENSE",
                    "", "", "", "", "", "", "", uuid(t.id),
                )
                TxKind.Transfer -> {
                    // Only the sending leg (negative amount); the receiving leg is the same transfer.
                    if (t.amountPaise > 0 && t.transferPeerId != null) continue
                    val peer = t.transferPeerId?.let { byId[it] }
                    val to = peer?.let { accounts[it.accountId] }
                    val toCur = to?.currency ?: cur
                    listOf(
                        "${t.date.format(date)} 12:00", t.note, "", acc?.name.orEmpty(),
                        amt(t.amountPaise), cur, "TRANSFER",
                        amt(t.amountPaise), cur, to?.name.orEmpty(), amt(peer?.amountPaise ?: t.amountPaise), toCur,
                        "", "", uuid(t.id),
                    )
                }
            }
            row.joinTo(sb, ",") { cell(it) }; sb.append("\r\n"); rows++
        }
        return sb.toString() to rows
    }

    private val HEADER = listOf("Date", "Title", "Category", "Account", "Amount", "Currency", "Type", "Transfer Amount",
        "Transfer Currency", "To Account", "Receive Amount", "Receive Currency", "Description", "Due Date", "ID")

    private fun uuid(id: Long) = java.util.UUID.nameUUIDFromBytes("kairo-tx-$id".toByteArray()).toString()

    private fun key(d: LocalDate, acc: Long, k: TxKind, paise: Long, note: String) = "$d|$acc|$k|$paise|${note.trim().lowercase()}"

    private val formats = listOf("M/d/yyyy H:mm", "M/d/yyyy", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd", "d/M/yyyy H:mm")
        .map { DateTimeFormatter.ofPattern(it) }

    internal fun parseDate(s0: String): LocalDate? {
        val s = s0.trim()
        if (s.isBlank()) return null
        // Ivy's own export: ISO "2026-11-21T20:25:44.269" (fraction of any length, maybe a zone).
        runCatching { return java.time.LocalDateTime.parse(s).toLocalDate() }
        runCatching { return java.time.OffsetDateTime.parse(s).toLocalDate() }
        runCatching { return java.time.ZonedDateTime.parse(s).toLocalDate() }
        for (f in formats) {
            runCatching { return java.time.LocalDateTime.parse(s, f).toLocalDate() }
            runCatching { return LocalDate.parse(s, f) }
        }
        return null
    }

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
