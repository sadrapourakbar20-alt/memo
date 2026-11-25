package com.sadra.memo.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadra.memo.data.preferences.SettingsRepository
import com.sadra.memo.domain.model.MoneyTx
import com.sadra.memo.domain.model.Note
import com.sadra.memo.domain.model.Task
import com.sadra.memo.domain.model.balanceOf
import com.sadra.memo.domain.nlu.AssistantEngine
import com.sadra.memo.domain.nlu.AssistantIntent
import com.sadra.memo.domain.nlu.MoneyInfo
import com.sadra.memo.domain.nlu.PersianParser
import com.sadra.memo.domain.nlu.TaskFilter
import com.sadra.memo.domain.nlu.TextNorm
import com.sadra.memo.domain.repository.ClassRepository
import com.sadra.memo.domain.repository.FinanceRepository
import com.sadra.memo.domain.repository.NoteRepository
import com.sadra.memo.domain.repository.TaskRepository
import com.sadra.memo.domain.schedule.ClassSchedule
import com.sadra.memo.domain.usecase.TaskManager
import com.sadra.memo.ui.finance.currentMonthRange
import com.sadra.memo.util.Jalali
import com.sadra.memo.util.formatMoney
import com.sadra.memo.util.minutesText
import com.sadra.memo.util.toPersianDigits
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ChatItem(val label: String, val isTask: Boolean, val id: Long)
data class ChatChoice(val label: String, val pendingId: Long, val accountId: Long)
data class ChatMessage(
    val id: Long,
    val fromUser: Boolean,
    val text: String,
    val items: List<ChatItem> = emptyList(),
    val choices: List<ChatChoice> = emptyList()
)

private data class PendingMoney(val info: MoneyInfo)

class AssistantViewModel(
    private val noteRepository: NoteRepository,
    private val taskRepository: TaskRepository,
    private val taskManager: TaskManager,
    private val classRepository: ClassRepository,
    private val financeRepository: FinanceRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private var nextId = 1L
    private val pending = mutableMapOf<Long, PendingMoney>()

    private val _messages = MutableStateFlow(
        listOf(
            ChatMessage(
                nextId++, false,
                "سلام! من دستیار Memo‌ام و همه‌چیز رو روی خود گوشی و بدون اینترنت انجام می‌دم. مثلاً بگو:\n" +
                    "• فردا ساعت ۱۰ صبح با مشتری تماس بگیر\n" +
                    "• یادداشت کن فردا اظهارنامه مشتری رو بررسی کنم\n" +
                    "• ۵۰۰ هزار تومن خرج کردم برای غذا\n" +
                    "• کارهای مهم این هفته رو نشون بده"
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _askPermission = MutableStateFlow(false)
    val askPermission: StateFlow<Boolean> = _askPermission

    fun permissionHandled() { _askPermission.value = false }

    private fun add(m: ChatMessage) { _messages.value = _messages.value + m }

    private fun reply(text: String, items: List<ChatItem> = emptyList(), choices: List<ChatChoice> = emptyList()) {
        add(ChatMessage(nextId++, false, text, items, choices))
    }

    fun send(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) return
        add(ChatMessage(nextId++, true, text))
        viewModelScope.launch {
            try {
                handle(text)
            } catch (e: Exception) {
                reply("یه مشکل پیش اومد: " + (e.message ?: "خطای نامشخص"))
            }
        }
    }

    private suspend fun handle(text: String) {
        when (val intent = AssistantEngine.classify(text)) {
            is AssistantIntent.CreateNote -> {
                val now = System.currentTimeMillis()
                val id = noteRepository.save(Note(title = "", content = intent.content, createdAt = now, updatedAt = now))
                reply("یادداشت ذخیره شد.", items = listOf(ChatItem("باز کردن یادداشت", false, id).copy(isTask = false)))
            }
            is AssistantIntent.CreateTask -> {
                val task = Task(
                    title = intent.title,
                    dueAt = intent.dueAt,
                    createdAt = System.currentTimeMillis(),
                    alertOffsets = listOf(0)
                )
                val id = taskManager.save(task)
                val due = intent.dueAt
                val line = if (due != null) {
                    "کار ثبت شد: «${intent.title}»\nزمان: " + Jalali.formatDueAt(due) + " (یادآوری سر ساعت)"
                } else {
                    "کار ثبت شد: «${intent.title}» (بدون تاریخ)"
                }
                if (due != null) _askPermission.value = true
                reply(line, items = listOf(ChatItem("ویرایش کار", true, id)))
            }
            is AssistantIntent.ShowTasks -> showTasks(intent.filter)
            is AssistantIntent.FindNote -> findNotes(intent.query)
            is AssistantIntent.PlanDay -> planDay(intent.offsetDays)
            is AssistantIntent.RecordMoney -> recordMoney(intent)
            AssistantIntent.Balances -> balances()
            AssistantIntent.MonthSummary -> monthSummary()
            AssistantIntent.Unknown -> reply(
                "متوجه نشدم. اینا رو امتحان کن:\n" +
                    "• «فردا ساعت ۱۰ با مشتری تماس بگیر» (کار با یادآوری)\n" +
                    "• «یادداشت کن ...» (یادداشت)\n" +
                    "• «۲۰۰ هزار تومن خرج کردم برای شام» یا «حقوقم رو گرفتم ۲۰ میلیون» (مالی)\n" +
                    "• «کارهای امروز»، «چه کارهایی رو انجام ندادم؟»\n" +
                    "• «یادداشت مربوط به ... رو پیدا کن»\n" +
                    "• «برنامه امروز»، «موجودی حساب‌ها»، «خرج این ماه»"
            )
        }
    }

    // ---------- کارها ----------
    private fun taskLabel(t: Task): String {
        val due = t.dueAt
        return if (due != null) t.title + " — " + Jalali.formatDueAt(due) else t.title
    }

    private suspend fun showTasks(filter: TaskFilter) {
        val all = taskRepository.observeAll().first()
        val now = System.currentTimeMillis()
        val startToday = Jalali.startOfTodayMillis()
        val day = 24L * 60 * 60 * 1000
        val weekStartDate = LocalDate.now().minusDays(ClassSchedule.weekdayIndex(LocalDate.now()).toLong())
        val weekStart = weekStartDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val weekEnd = weekStart + 7 * day

        fun inRange(t: Task, a: Long, b: Long): Boolean {
            val d = t.dueAt ?: return false
            return d in a until b
        }

        val (title, list) = when (filter) {
            TaskFilter.TODAY -> "کارهای امروز" to all.filter { !it.isDone && inRange(it, startToday, startToday + day) }
            TaskFilter.TOMORROW -> "کارهای فردا" to all.filter { !it.isDone && inRange(it, startToday + day, startToday + 2 * day) }
            TaskFilter.WEEK -> "کارهای این هفته" to all.filter { !it.isDone && inRange(it, weekStart, weekEnd) }
            TaskFilter.IMPORTANT_WEEK -> "کارهای مهم این هفته (اولویت زیاد)" to
                all.filter { !it.isDone && it.priority == 2 && inRange(it, weekStart, weekEnd) }
            TaskFilter.OVERDUE -> "کارهای عقب‌افتاده" to
                all.filter { val d = it.dueAt; !it.isDone && d != null && d < now }
            TaskFilter.PENDING -> "کارهای انجام‌نشده" to all.filter { !it.isDone }
        }
        if (list.isEmpty()) {
            reply("$title: چیزی پیدا نشد.")
        } else {
            val shown = list.take(10)
            val more = if (list.size > shown.size) "\n(و " + (list.size - shown.size).toString().toPersianDigits() + " مورد دیگه)" else ""
            reply(
                title + " (" + list.size.toString().toPersianDigits() + " مورد):" + more,
                items = shown.map { ChatItem(taskLabel(it), true, it.id) }
            )
        }
    }

    // ---------- یادداشت ----------
    private suspend fun findNotes(query: String) {
        val words = TextNorm.normalize(query).trim().split(" ").filter { it.isNotBlank() }
        if (words.isEmpty()) {
            reply("دنبال چی بگردم؟ مثلاً: «یادداشت مربوط به خرید تجهیزات دفتر رو پیدا کن»")
            return
        }
        val notes = noteRepository.observeNotes().first()
        fun hay(n: Note) = TextNorm.normalize(n.title + " " + n.content + " " + n.category)
        var found = notes.filter { n -> val h = hay(n); words.all { h.contains(it) } }
        if (found.isEmpty()) found = notes.filter { n -> val h = hay(n); words.any { h.contains(it) } }
        if (found.isEmpty()) {
            reply("یادداشتی با این کلمه‌ها پیدا نشد.")
        } else {
            reply(
                found.size.toString().toPersianDigits() + " یادداشت پیدا شد:",
                items = found.take(8).map { n ->
                    val label = n.title.ifBlank { n.content.lineSequence().firstOrNull().orEmpty() }.ifBlank { "بدون عنوان" }
                    ChatItem(label.take(60), false, n.id)
                }
            )
        }
    }

    // ---------- برنامه روز ----------
    private suspend fun planDay(offset: Int) {
        val date = LocalDate.now().plusDays(offset.toLong())
        val sem = settings.semesterConfig.first()
        val classes = classRepository.getAll()
        val cancelled = classRepository.getAllCancellations()
        val tasks = taskRepository.observeAll().first()
        val zoneStart = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val zoneEnd = zoneStart + 24L * 60 * 60 * 1000

        val dayClasses = classes
            .filter { ClassSchedule.occursBase(it, date, sem) && (it.id to date.toEpochDay()) !in cancelled }
            .sortedBy { it.startMinutes }
        val dayTasks = tasks
            .filter { val d = it.dueAt; !it.isDone && d != null && d >= zoneStart && d < zoneEnd }
            .sortedBy { it.dueAt }
        val overdue = if (offset == 0) tasks.count { val d = it.dueAt; !it.isDone && d != null && d < System.currentTimeMillis() } else 0

        val sb = StringBuilder()
        sb.append(if (offset == 0) "برنامه امروز" else "برنامه فردا")
        sb.append(" (").append(Jalali.weekdayName(date)).append(" ").append(Jalali.format(Jalali.fromLocalDate(date))).append(")\n")
        val holiday = ClassSchedule.holidayLabel(date)
        if (holiday != null) sb.append("تعطیل: ").append(holiday).append("\n")
        if (dayClasses.isNotEmpty()) {
            sb.append("\nکلاس‌ها:\n")
            dayClasses.forEach {
                sb.append("• ").append(minutesText(it.startMinutes)).append(" تا ").append(minutesText(it.endMinutes))
                    .append(" — ").append(it.name)
                if (it.location.isNotBlank()) sb.append(" (").append(it.location).append(")")
                sb.append("\n")
            }
        }
        if (dayTasks.isNotEmpty()) {
            sb.append("\nکارها:\n")
            dayTasks.forEach {
                val d = it.dueAt ?: 0L
                val dt = Jalali.dateTimeOf(d)
                sb.append("• ").append(Jalali.formatTime(dt.hour, dt.minute)).append(" — ").append(it.title)
                if (it.priority == 2) sb.append(" (مهم)")
                sb.append("\n")
            }
        }
        if (dayClasses.isEmpty() && dayTasks.isEmpty()) sb.append("\nبرنامه‌ای ثبت نشده.\n")
        if (overdue > 0) {
            sb.append("\nپیشنهاد: ").append(overdue.toString().toPersianDigits())
                .append(" کار عقب‌افتاده داری؛ بهتره اول بهشون برسی.")
        }
        reply(sb.toString().trim(), items = dayTasks.take(5).map { ChatItem(taskLabel(it), true, it.id) })
    }

    // ---------- مالی ----------
    private suspend fun recordMoney(intent: AssistantIntent.RecordMoney) {
        val accounts = financeRepository.getAccounts()
        if (accounts.isEmpty()) {
            reply("هنوز حسابی ثبت نکردی. اول از تب «مالی» حساب‌هات رو وارد کن، بعد دوباره بگو.")
            return
        }
        val matches = accounts.filter { a ->
            val n = TextNorm.normalize(a.name).trim()
            n.isNotEmpty() && intent.normalizedText.contains(" $n ")
        }
        val info = intent.info
        if (matches.size == 1) {
            // اسم حساب رو از توضیح تراکنش حذف می‌کنیم
            val accountName = TextNorm.normalize(matches[0].name).trim()
            val stripped = TextNorm.normalize(info.title).replace(" $accountName ", " ")
            val cleaned = PersianParser.cleanTitle(stripped)
                .replace(Regex("""\s+(?:از|به|حساب)$"""), "").trim()
            saveMoney(info.copy(title = cleaned), matches[0].id)
            return
        }
        val pendingId = nextId++
        pending[pendingId] = PendingMoney(info)
        val kind = if (info.type == 0) "خرج" else "واریز"
        val ask = if (info.type == 0) "از کدوم حساب؟" else "به کدوم حساب؟"
        reply(
            "$kind ${formatMoney(info.amount)}" + (if (info.title.isNotBlank()) " — «${info.title}»" else "") + "\n$ask",
            choices = accounts.map { ChatChoice(it.name, pendingId, it.id) }
        )
    }

    fun chooseAccount(choice: ChatChoice) {
        val p = pending.remove(choice.pendingId) ?: return
        add(ChatMessage(nextId++, true, choice.label))
        viewModelScope.launch {
            try {
                saveMoney(p.info, choice.accountId)
            } catch (e: Exception) {
                reply("ثبت نشد: " + (e.message ?: "خطای نامشخص"))
            }
        }
    }

    private suspend fun saveMoney(info: MoneyInfo, accountId: Long) {
        val account = financeRepository.getAccount(accountId)
        if (account == null) {
            reply("این حساب دیگه وجود نداره.")
            return
        }
        val now = System.currentTimeMillis()
        financeRepository.saveTransaction(
            MoneyTx(
                accountId = accountId, type = info.type, amount = info.amount,
                title = info.title, category = info.category, dateMillis = now, createdAt = now
            )
        )
        val balance = balanceOf(account, financeRepository.getAllTransactions())
        val kind = if (info.type == 0) "خرج" else "واریز"
        reply(
            "ثبت شد: $kind ${formatMoney(info.amount)} (${info.category}) تو حساب «${account.name}».\n" +
                "موجودی این حساب الان: " + formatMoney(balance)
        )
    }

    private suspend fun balances() {
        val accounts = financeRepository.getAccounts()
        if (accounts.isEmpty()) {
            reply("هنوز حسابی ثبت نکردی. از تب «مالی» اضافه‌شون کن.")
            return
        }
        val txs = financeRepository.getAllTransactions()
        var total = 0L
        val sb = StringBuilder("موجودی حساب‌ها:\n")
        accounts.forEach {
            val b = balanceOf(it, txs)
            total += b
            sb.append("• ").append(it.name).append(": ").append(formatMoney(b)).append("\n")
        }
        sb.append("مجموع: ").append(formatMoney(total))
        reply(sb.toString())
    }

    private suspend fun monthSummary() {
        val (start, end) = currentMonthRange()
        val txs = financeRepository.getAllTransactions().filter { it.dateMillis in start until end }
        val income = txs.filter { it.type == 1 }.sumOf { it.amount }
        val expense = txs.filter { it.type == 0 }.sumOf { it.amount }
        val j = Jalali.today()
        val top = txs.filter { it.type == 0 }.groupBy { it.category }
            .mapValues { e -> e.value.sumOf { it.amount } }.entries.sortedByDescending { it.value }.take(3)
        val sb = StringBuilder("خلاصه ${Jalali.monthNames[j.month - 1]} ماه:\n")
        sb.append("واریز: ").append(formatMoney(income)).append("\n")
        sb.append("خرج: ").append(formatMoney(expense)).append("\n")
        sb.append("تفاضل: ").append(formatMoney(income - expense))
        if (top.isNotEmpty()) {
            sb.append("\n\nبیشترین خرج‌ها:\n")
            top.forEach { sb.append("• ").append(it.key).append(": ").append(formatMoney(it.value)).append("\n") }
        }
        reply(sb.toString().trim())
    }
}
