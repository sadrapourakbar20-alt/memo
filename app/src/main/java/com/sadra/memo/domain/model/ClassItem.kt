package com.sadra.memo.domain.model

import java.time.LocalDate

data class ClassItem(
    val id: Long = 0,
    val name: String,
    val teacher: String = "",
    val location: String = "",
    val weekday: Int,
    val startMinutes: Int,
    val endMinutes: Int,
    val pattern: Int = 0,
    val alertOffsets: List<Int> = listOf(30)
)

data class SemesterConfig(
    val start: LocalDate? = null,
    val end: LocalDate? = null,
    val invertParity: Boolean = false
)
