package com.sadra.memo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sadra.memo.util.JDate
import com.sadra.memo.util.Jalali
import com.sadra.memo.util.toPersianDigits

@Composable
fun JalaliDatePickerDialog(
    initial: JDate,
    onDismiss: () -> Unit,
    onConfirm: (JDate) -> Unit
) {
    var year by remember { mutableIntStateOf(initial.year) }
    var month by remember { mutableIntStateOf(initial.month) }
    var day by remember { mutableIntStateOf(initial.day) }

    fun clampDay() {
        val max = Jalali.monthLength(year, month)
        if (day > max) day = max
    }

    val maxDay = Jalali.monthLength(year, month)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتخاب تاریخ") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Stepper(
                    label = day.toString().toPersianDigits(),
                    onUp = { day = if (day >= maxDay) 1 else day + 1 },
                    onDown = { day = if (day <= 1) maxDay else day - 1 }
                )
                Stepper(
                    label = Jalali.monthNames[month - 1],
                    onUp = { month = if (month >= 12) 1 else month + 1; clampDay() },
                    onDown = { month = if (month <= 1) 12 else month - 1; clampDay() }
                )
                Stepper(
                    label = year.toString().toPersianDigits(),
                    onUp = { if (year < 1500) year += 1; clampDay() },
                    onDown = { if (year > 1300) year -= 1; clampDay() }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(JDate(year, month, day)) }) { Text("تأیید") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
private fun Stepper(label: String, onUp: () -> Unit, onDown: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(onClick = onUp) { Text("+") }
        Text(label)
        TextButton(onClick = onDown) { Text("−") }
    }
}
