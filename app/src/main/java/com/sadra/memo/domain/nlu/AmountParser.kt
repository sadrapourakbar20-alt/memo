package com.sadra.memo.domain.nlu

data class MoneyInfo(val type: Int, val amount: Long, val title: String, val category: String)

object AmountParser {
    private val a1 = Regex(
        """ (\d+(?:\.\d+)?|یک|یه|دو|سه|چهار|پنج|شش|هفت|هشت|نه|ده|بیست|سی|چهل|پنجاه|صد) (هزار|میلیون|میلیارد)(?: (تومان|تومن|ریال))?(?= )"""
    )
    private val a2 = Regex(""" (\d+) (تومان|تومن|ریال)(?= )""")
    private val a3 = Regex(""" (\d{4,})(?= )""")

    private val numWords = mapOf(
        "یک" to 1.0, "یه" to 1.0, "دو" to 2.0, "سه" to 3.0, "چهار" to 4.0, "پنج" to 5.0,
        "شش" to 6.0, "هفت" to 7.0, "هشت" to 8.0, "نه" to 9.0, "ده" to 10.0,
        "بیست" to 20.0, "سی" to 30.0, "چهل" to 40.0, "پنجاه" to 50.0, "صد" to 100.0
    )
    private val mult = mapOf("هزار" to 1_000.0, "میلیون" to 1_000_000.0, "میلیارد" to 1_000_000_000.0)

    private val incomeWords = listOf("واریز", "دریافت", "درآمد", "حقوق", "گرفتم", "فروختم", "فروش", "اومد")
    private val expenseWords = listOf("خرج", "خرید", "پرداخت", "هزینه", "دادم")

    private val removable = listOf(
        "خرج کردم", "خرج کردیم", "خرج", "خریدم", "خرید", "پرداخت کردم", "پرداختم", "پرداخت",
        "هزینه", "دادم", "پول", "واریز شد", "واریز کردم", "واریز", "دریافت کردم", "دریافت",
        "درآمد", "گرفتم", "فروختم", "اومد", "به حسابم"
    )

    private val categories = listOf(
        "خوراک" to listOf("غذا", "ناهار", "شام", "صبحانه", "رستوران", "خوراک", "میوه", "نون", "کافه"),
        "حمل‌ونقل" to listOf("بنزین", "تاکسی", "اسنپ", "مترو", "اتوبوس", "کرایه"),
        "قبض و شارژ" to listOf("قبض", "برق", "گاز", "آب", "اینترنت", "شارژ", "تلفن"),
        "حقوق" to listOf("حقوق", "دستمزد"),
        "اجاره" to listOf("اجاره", "رهن"),
        "خرید" to listOf("خرید", "لباس", "کفش"),
        "درمان" to listOf("دکتر", "دارو", "درمان", "بیمارستان")
    )

    /** مبلغ رو (به تومان) و بازه‌ی متنش رو برمی‌گردونه. ورودی باید normalize شده باشه. */
    fun findAmount(t: String): Pair<Long, MatchResult>? {
        a1.find(t)?.let { m ->
            val g = m.groupValues
            val base = g[1].toDoubleOrNull() ?: (numWords[g[1]] ?: return@let)
            var v = base * (mult[g[2]] ?: 1.0)
            if (g[3] == "ریال") v /= 10.0
            return Math.round(v) to m
        }
        a2.find(t)?.let { m ->
            var v = m.groupValues[1].toLong()
            if (m.groupValues[2] == "ریال") v /= 10
            return v to m
        }
        a3.find(t)?.let { m -> return m.groupValues[1].toLong() to m }
        return null
    }

    fun moneyType(t: String): Int? {
        if (incomeWords.any { t.contains(it) }) return 1
        if (expenseWords.any { t.contains(it) }) return 0
        return null
    }

    fun guessCategory(t: String, type: Int): String {
        if (type == 1) {
            return if (t.contains("حقوق")) "حقوق" else "سایر"
        }
        for ((name, words) in categories) {
            if (words.any { t.contains(" $it ") || t.contains(it) }) return name
        }
        return "سایر"
    }

    fun extract(text: String): MoneyInfo? {
        val t = TextNorm.normalize(text)
        val found = findAmount(t) ?: return null
        val type = moneyType(t) ?: return null
        var rest = t.substring(0, found.second.range.first) + " " + t.substring(found.second.range.last + 1)
        for (w in removable) rest = rest.replace(" $w ", " ")
        val title = PersianParser.cleanTitle(rest).replace(Regex("""\s+(?:رو|را|به|از)$"""), "").trim()
        return MoneyInfo(type, found.first, title, guessCategory(t, type))
    }
}
