package com.example.mooncalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val calendar = Calendar.getInstance()
        calendar.time = Date()

        setContent {
            var clicked by remember { mutableStateOf(" ") }
            Column {
                CalendarTheme {
                    Calendar(
                        month = Month(calendar),
                        onDayClicked = {day, month -> clicked = day.value},
                        Modifier
                    )
                }
                Text(clicked)
            }
        }
    }
}