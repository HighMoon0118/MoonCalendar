package com.example.mooncalendar

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.*

typealias Year = List<Month>
typealias Week = List<Day>

enum class DayOfWeek {
    Sunday, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday
}

enum class DayStatus {
    Clickable, NonClickable, Today,
}

class Day(val value: String, status: DayStatus) {
    var status by mutableStateOf(status)
}
data class Month(val calendar: Calendar) {

    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)

    private val days = mutableListOf<Day>().apply {

        val cal = calendar.clone() as Calendar  // calendar를 사용하면 중간에 날짜가 바뀌기 때문에 클릭할때마다 달력이 변함

        val today = cal.get(Calendar.DAY_OF_MONTH)

        // 이번달 일 수
        val curDays = cal.getActualMaximum(Calendar.DATE)
        cal.set(Calendar.DATE, curDays)

        // 달력에 표시될 다음달 일 수
        val postLast = 7 - cal.get(Calendar.DAY_OF_WEEK)
        cal.set(Calendar.DATE, 1)

        // 달력에 표시될 이전달 일 수
        val preDays = cal.get(Calendar.DAY_OF_WEEK) - 1
        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) - 1)

        // 달력에 표시될 이전달 시작, 끝 날짜
        val preStart = cal.getActualMaximum(Calendar.DATE) - preDays
        val preLast = preStart + preDays - 1

        for (day in preStart..preLast) add(Day(day.toString(), DayStatus.NonClickable))
        for (day in 1..curDays)        add(Day(day.toString(), DayStatus.Clickable))
        for (day in 1..postLast)       add(Day(day.toString(), DayStatus.NonClickable))

        get(preDays + today - 1).status = DayStatus.Today
        cal.clear()

    }.toList()

    val weeks = lazy { days.chunked(7) }
}