package com.example.mooncalendar

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.MutableLiveData
import java.util.*

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val calendar = Calendar.getInstance()
            calendar.time = Date()

            val mC = MoonCalendar(calendar)

            Column {

                var clickedDay by remember { mutableStateOf(" ") }

                mC.clickedDay.observe(this@MainActivity) { day ->
                    clickedDay = day.value
                }

                CalendarTheme {
                    mC.DrawCalendar {

                        Column {
                            Text("일정")
                            ClickedDay { clickedDay }  // 값이 ClickedDay 내부에서 읽히므로 값이 변해도 ClickedDay 만 재구성됩니다.
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ClickedDay(day: () -> String) {
        Text(day())
    }

}