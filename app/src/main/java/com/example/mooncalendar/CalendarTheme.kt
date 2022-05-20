package com.example.mooncalendar

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun CalendarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
//        colors = craneColors,
//        typography = craneTypography
    ) {
        content()
    }
}