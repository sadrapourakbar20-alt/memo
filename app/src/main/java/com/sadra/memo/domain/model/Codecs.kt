package com.sadra.memo.domain.model

fun encodeOffsets(list: List<Int>): String = list.joinToString(",")

fun decodeOffsets(text: String): List<Int> =
    text.split(",").mapNotNull { it.trim().toIntOrNull() }

fun encodeSubtasks(list: List<Subtask>): String =
    list.joinToString("\n") { (if (it.done) "1|" else "0|") + it.title.replace("\n", " ") }

fun decodeSubtasks(text: String): List<Subtask> =
    text.split("\n").filter { it.isNotBlank() }.map { line ->
        val i = line.indexOf('|')
        if (i < 0) Subtask(line, false) else Subtask(line.substring(i + 1), line.substring(0, i) == "1")
    }
