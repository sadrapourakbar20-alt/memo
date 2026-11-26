package com.sadra.memo.domain.nlu

data class ParsedSms(val type: Int, val amount: Long, val title: String, val category: String)

/** پیامک بانک رو به (نوع، مبلغ به تومان) تبدیل می‌کنه. هر چیزی که شبیه تراکنش نباشه null برمی‌گردونه. */
object SmsParser {
    private val otp = Regex("""رمز|کد |کد:|یکبار|یک بار|OTP|cvv2|پویا|فعالسازی|تایید|تأیید""", RegexOption.IGNORE_CASE)
    private val strictOtp = Regex("""رمز|یکبار مصرف|یک بار مصرف|OTP|CVV2""", RegexOption.IGNORE_CASE)
    private val moneyWords = Regex("""برداشت|واریز""")
    private val date = Regex("""\d{2,4}\s*[/\-.]\s*\d{1,2}\s*[/\-.]\s*\d{1,2}""")
    private val time = Regex("""\d{1,2}:\d{2}(?::\d{2})?""")
    private val balance = Regex(
        """(?:مانده|موجودی)(?: حساب| کل| قابل برداشت| نهایی)?\s*[:：]?\s*[+\-]?[\d,٬.]+\s*(?:ریال|تومان)?\s*[+\-]?"""
    )
    private val masked = Regex("""\d{0,6}[*xX•]{2,}\d{0,6}|[*xX•]+\d+""")
    private val longDigits = Regex("""(?<![\d,])\d{10,}(?![\d,])""")
    private val cardNo = Regex("""(?:کارت|حساب|شماره)\s*[:：]?\s*\d{3,}""")
    private val num = Regex("""\d{1,3}(?:[,٬]\d{3})+|\d{3,}""")

    private val incomeWords = listOf(
        "واریز", "دریافت", "افزایش", "سپرده گذاری", "سپرده‌گذاری", "بستانکار", "به حساب شما", "واریزی"
    )
    private val expenseWords = listOf(
        "برداشت", "خرید", "پرداخت", "انتقال", "کارت به کارت", "کسر", "بدهکار", "قسط", "کارمزد", "پرداخت قبض"
    )
    private val banks = listOf(
        "ملی", "ملت", "صادرات", "سپه", "سامان", "پاسارگاد", "ملل", "کشاورزی", "رفاه", "تجارت", "آینده",
        "شهر", "پارسیان", "مسکن", "سینا", "دی", "اقتصاد نوین", "قوامین", "انصار", "گردشگری",
        "کارآفرین", "ایران زمین", "توسعه", "بلو"
    )

    fun parse(body: String, rialDefault: Boolean = true, fallbackAccountName: String? = null): ParsedSms? {
        var t = TextNorm.digits(body)
            .replace('ي', 'ی').replace('ك', 'ک').replace('\u200c', ' ').replace('\u00a0', ' ')
        if (otp.containsMatchIn(t) && !moneyWords.containsMatchIn(t)) return null
        if (strictOtp.containsMatchIn(t)) return null

        t = date.replace(t, " ")
        t = time.replace(t, " ")
        t = balance.replace(t, " ")
        t = masked.replace(t, " ")
        t = cardNo.replace(t, " ")
        t = longDigits.replace(t, " ")

        var best: MatchResult? = null
        for (kw in listOf("مبلغ") + incomeWords + expenseWords) {
            val i = t.indexOf(kw)
            if (i >= 0) {
                val mm = num.find(t, i)
                if (mm != null && (best == null || mm.range.first < best.range.first)) best = mm
            }
        }
        val m = best ?: num.find(t) ?: return null
        val value = m.value.replace(",", "").replace("٬", "").toLongOrNull() ?: return null

        val after = t.substring(m.range.last + 1, minOf(t.length, m.range.last + 3)).trim().take(1)
        val before = t.substring(maxOf(0, m.range.first - 2), m.range.first).trim().takeLast(1)
        var sign: Int? = null
        if (after == "+" || before == "+") sign = 1
        if (after == "-" || before == "-") sign = 0

        val inc = incomeWords.maxOf { t.lastIndexOf(it) }
        val exp = expenseWords.maxOf { t.lastIndexOf(it) }
        val type = when {
            sign != null -> sign
            inc < 0 && exp < 0 -> return null
            inc > exp -> 1
            else -> 0
        }

        val near = t.substring(maxOf(0, m.range.first - 12), minOf(t.length, m.range.last + 15))
        val rial = when {
            near.contains("تومان") -> false
            near.contains("ریال") -> true
            else -> rialDefault
        }
        val amount = if (rial) value / 10 else value
        if (amount < 100) return null

        val bank = banks.firstOrNull { t.contains("بانک $it") || t.contains(it) }
        val title = when {
            bank == "بلو" -> "پیامک بلوبانک"
            bank != null -> "پیامک بانک $bank"
            !fallbackAccountName.isNullOrBlank() -> "پیامک $fallbackAccountName"
            else -> "پیامک بانکی"
        }
        return ParsedSms(type, amount, title, AmountParser.guessCategory(t, type))
    }
}
