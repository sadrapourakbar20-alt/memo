package com.sadra.memo.domain.nlu

object TextNorm {
    private val thousands = Regex("""(?<=\d)[,٬](?=\d)""")
    private val lonelyDot = Regex("""(?<!\d)\.|\.(?!\d)""")
    private const val punct = ",!?؟،;\"«»()[]"

    fun digits(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) {
            sb.append(
                when (c) {
                    in '۰'..'۹' -> '0' + (c - '۰')
                    in '٠'..'٩' -> '0' + (c - '٠')
                    else -> c
                }
            )
        }
        return sb.toString()
    }

    /** متن رو برای تطبیق آماده می‌کنه؛ خروجی با یک فاصله اول و آخر پد شده. */
    fun normalize(text: String): String {
        var t = digits(text)
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('\u200c', ' ')
            .replace('\u00a0', ' ')
            .replace('٫', '.')
        t = thousands.replace(t, "")
        for (ch in punct) t = t.replace(ch, ' ')
        t = lonelyDot.replace(t, " ")
        t = t.replace(Regex("""\s+"""), " ").trim()
        return " $t "
    }
}
