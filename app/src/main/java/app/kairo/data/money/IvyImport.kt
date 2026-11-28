package app.kairo.data.money

import app.kairo.data.db.AccountType
import app.kairo.data.db.RecurringKind
import app.kairo.data.db.TxKind
import app.kairo.data.model.ListColor
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Imports (and exports) an Ivy Wallet CSV. [preview] says what a file holds before anything is
 * written (canvas "ivy import" board); [import] writes it. Accounts and categories are matched by
 * name (case-insensitive) or created. Rows already in Kairo (same date, account, kind, amount,
 * title) are skipped, so importing twice is safe. Planned payments (a Due Date but no Date) become
 * monthly bills when [import]'s `bills` is on, otherwise they are skipped.
 */
object IvyImport {
    data class Result(val added: Int, val skipped: Int, val accountsCreated: Int, val categoriesCreated: Int, val bills: Int = 0)

    /** What the file holds, against what Kairo already has. */
    data class Preview(
        val rows: Int, val columns: Int, val expenses: Int, val incomes: Int, val transfers: Int, val planned: Int,
        val newAccounts: List<String>, val categories: Int, val newCategories: Int, val duplicates: Int, val unreadable: Int,
    )

    fun decode(b: ByteArray) = IvyCsv.decode(b)

    private fun accName(s: String) = s.trim().ifBlank { "Imported" }

    suspend fun preview(money: MoneyRepository, rows: List<IvyRow>, columns: Int, bills: Boolean): Preview {
        val accs = money.accounts.first().map { it.name.lowercase() }.toSet()
        val cats = money.categories.first().map { it.name.lowercase() to it.income }.toSet()
        val accIds = money.accounts.first().associate { it.name.lowercase() to it.id }
        val seen = money.transactions.first().map { key(it.date, it.accountId, it.kind, kotlin.math.abs(it.amountPaise), it.note) }.toSet()
        val used = rows.filter { (it.date != null && it.paise > 0 && it.type in KINDS) || (bills && it.planned && it.paise > 0) }
        val names = used.flatMap { listOf(accName(it.account)) + if (it.type == "TRANSFER") listOf(accName(it.toAccount)) else emptyList() }
        val catKeys = used.filter { it.type != "TRANSFER" && it.category.isNotBlank() }.map { it.category.trim().lowercase() to (it.type == "INCOME") }.toSet()
        val dup = used.count { r ->
            val id = accIds[accName(r.account).lowercase()] ?: return@count false
            r.date != null && key(r.date, id, kindOf(r.type), r.paise, r.title.ifBlank { r.description }) in seen
        }
        val real = used.filter { !it.planned }
        return Preview(
            rows.size, columns, real.count { it.type == "EXPENSE" }, real.count { it.type == "INCOME" }, real.count { it.type == "TRANSFER" },
            rows.count { it.planned && it.paise > 0 },
            names.distinctBy { it.lowercase() }.filter { it.lowercase() !in accs },
            catKeys.size, catKeys.count { it !in cats }, dup, rows.size - used.size - if (bills) 0 else rows.count { it.planned },
        )
    }

    /** Settings' one-step import (no preview): planned payments are skipped. */
    suspend fun import(money: MoneyRepository, text: String): Result = import(money, IvyCsv.parse(text).first, bills = false)

    suspend fun import(money: MoneyRepository, rows: List<IvyRow>, bills: Boolean): Result {
        val accounts = money.accounts.first().associateBy { it.name.lowercase() }.mapValues { it.value.id }.toMutableMap()
        val cats = money.categories.first().associateBy { it.name.lowercase() to it.income }.mapValues { it.value.id }.toMutableMap()
        val seen = money.transactions.first().map { key(it.date, it.accountId, it.kind, kotlin.math.abs(it.amountPaise), it.note) }.toMutableSet()
        val billKeys = money.recurring.first().map { "${it.name.lowercase()}|${it.accountId}|${it.amountPaise}" }.toMutableSet()
        var added = 0; var skipped = 0; var newAcc = 0; var newCat = 0; var newBills = 0

        suspend fun account(name0: String): Long { val name = accName(name0)
            return accounts.getOrPut(name.lowercase()) {
                newAcc++
                money.addAccount(name, if (name.contains("cash", true)) AccountType.Cash else AccountType.Bank,
                    ListColor.entries[accounts.size % ListColor.entries.size], 0)
            } }
        suspend fun category(name: String, income: Boolean): Long? {
            if (name.isBlank()) return null
            return cats.getOrPut(name.trim().lowercase() to income) {
                newCat++
                money.addCategory(name.trim(), if (income) "savings" else "category",
                    ListColor.entries[(name.lowercase().hashCode() and 0x7fffffff) % ListColor.entries.size], income)
            }
        }

        for (r in rows) {
            val title = r.title.ifBlank { r.description }
            // Ivy keeps the description apart from the title; Kairo does too since v10.
            val desc = if (r.title.isBlank()) "" else r.description
            if (r.planned) {
                if (!bills || r.paise <= 0 || r.type == "TRANSFER") { skipped++; continue }
                val acc = account(r.account)
                val name = title.ifBlank { r.category.ifBlank { "Planned payment" } }
                if (!billKeys.add("${name.lowercase()}|$acc|${r.paise}")) { skipped++; continue }
                val income = r.type == "INCOME"
                money.addRecurring(name, if (income) RecurringKind.Income else RecurringKind.Bill, r.paise, acc, category(r.category, income), r.due!!)
                newBills++; continue
            }
            if (r.date == null || r.paise <= 0 || r.type !in KINDS) { skipped++; continue }
            when (r.type) {
                "TRANSFER" -> {
                    val from = account(r.account); val to = account(r.toAccount)
                    if (!seen.add(key(r.date, from, TxKind.Transfer, r.paise, title))) { skipped++; continue }
                    // Different currencies on each side aren't converted (Kairo is single-currency); use the sent amount.
                    money.save(null, TxKind.Transfer, r.paise, from, to, null, r.date, r.time, title, desc, "")
                }
                else -> {
                    val kind = kindOf(r.type); val acc = account(r.account)
                    if (!seen.add(key(r.date, acc, kind, r.paise, title))) { skipped++; continue }
                    money.save(null, kind, r.paise, acc, null, category(r.category, kind == TxKind.Income), r.date, r.time, title, desc, "")
                }
            }
            added++
        }
        return Result(added, skipped, newAcc, newCat, newBills)
    }

    private val KINDS = setOf("EXPENSE", "INCOME", "TRANSFER")
    private fun kindOf(t: String) = when (t) { "INCOME" -> TxKind.Income; "TRANSFER" -> TxKind.Transfer; else -> TxKind.Expense }

    /**
     * Writes every transaction as an Ivy Wallet CSV with the same 15 columns Ivy exports, so the file
     * imports back into Ivy or into Kairo. Rows without a time use 12:00.
     * A transfer is one row (the sending leg) with To Account and Receive Amount filled.
     * IDs are stable UUIDs derived from Kairo's row id, so re-exports keep the same IDs.
     */
    suspend fun export(money: MoneyRepository): Pair<String, Int> {
        val accounts = money.accounts.first().associateBy { it.id }
        val cats = money.categories.first().associateBy { it.id }
        val txs = money.transactions.first()
        val byId = txs.associateBy { it.id }
        val date = DateTimeFormatter.ofPattern("M/d/yyyy")
        fun hm(t: app.kairo.data.db.TransactionEntity) = t.time?.let { "${it.hour}:${it.minute.toString().padStart(2, '0')}" } ?: "12:00"
        fun amt(p: Long) = kotlin.math.abs(p).let { "${it / 100}.${(it % 100).toString().padStart(2, '0')}" }
        fun cell(s: String) = if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
        val sb = StringBuilder(HEADER.joinToString(",")).append("\r\n")
        var rows = 0
        for (t in txs.sortedWith(compareBy({ it.date }, { it.id }))) {
            val acc = accounts[t.accountId]
            val cur = acc?.currency ?: "INR"
            val row = when (t.kind) {
                TxKind.Expense, TxKind.Income -> listOf(
                    "${t.date.format(date)} ${hm(t)}", t.note, t.categoryId?.let { cats[it]?.name }.orEmpty(), acc?.name.orEmpty(),
                    amt(t.amountPaise), cur, if (t.kind == TxKind.Income) "INCOME" else "EXPENSE",
                    "", "", "", "", "", t.description, "", uuid(t.id),
                )
                TxKind.Transfer -> {
                    // Only the sending leg (negative amount); the receiving leg is the same transfer.
                    if (t.amountPaise > 0 && t.transferPeerId != null) continue
                    val peer = t.transferPeerId?.let { byId[it] }
                    val to = peer?.let { accounts[it.accountId] }
                    val toCur = to?.currency ?: cur
                    listOf(
                        "${t.date.format(date)} ${hm(t)}", t.note, "", acc?.name.orEmpty(),
                        amt(t.amountPaise), cur, "TRANSFER",
                        amt(t.amountPaise), cur, to?.name.orEmpty(), amt(peer?.amountPaise ?: t.amountPaise), toCur,
                        t.description, "", uuid(t.id),
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
}
