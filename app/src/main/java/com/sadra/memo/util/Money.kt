package com.sadra.memo.util

import kotlin.math.abs

/** مبلغ (تومان) رو با جداکننده هزارگان و ارقام فارسی نشون می‌ده */
fun formatMoney(amount: Long): String {
    val grouped = abs(amount).toString().reversed().chunked(3).joinToString("٬").reversed()
    val sign = if (amount < 0) "-" else ""
    return (sign + grouped + " تومان").toPersianDigits()
}

fun formatMoneyPlain(amount: Long): String =
    abs(amount).toString().reversed().chunked(3).joinToString("٬").reversed().toPersianDigits()

val accountTypeLabels = listOf("بانکی", "نقدی", "سایر")
val expenseCategories = listOf("خوراک", "حمل‌ونقل", "قبض و شارژ", "خرید", "اجاره", "درمان", "سایر")
val incomeCategories = listOf("حقوق", "درآمد کاری", "هدیه", "سایر")
