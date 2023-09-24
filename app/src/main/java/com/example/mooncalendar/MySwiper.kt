package com.example.mooncalendar

import android.util.Log
import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.*
import kotlin.math.abs


fun Modifier.mySwiper(
    vState: MutableState<Float>,
    hState: MutableState<Float>,
    vAnchor: MutableState<List<Float>>,
    width: MutableState<Float>,
    thresholds: Float
) = pointerInput(Unit) {

    val anchor = vAnchor.value
    val width = width.value  // 값이 0이여서 divide by zero 오류가 뜰 때가 있음, 확인 요
    var currentHState = 0
    var currentVState = CalendarSize.MID

    var hDist = 0f
    var isDetected = false
    var isVertical = false
    var i = 0
    val minHeight = (anchor[0] * 0.8).toFloat()

    detectDragGestures (
        onDragStart = {
            val gap = (hState.value - currentHState).toInt() / width.toInt()
            if (abs(gap) >= 1) currentHState += gap * width.toInt()

            hDist = 0f
            isDetected = false
            isVertical = false
        },
        onDrag = { change, offset ->
            if (!isDetected) {
                if (abs(offset.x) < abs(offset.y)) isVertical = true
                isDetected = true
            } else {
                if (isVertical) {
                    var target = vState.value + offset.y

                    if (target <= minHeight) target = minHeight
                    else if (target >= anchor[2]) target = anchor[2]

                    vState.value = target
                }
                else {
                    hDist += offset.x
                    hState.value = currentHState + hDist
                }
            }
        },
        onDragEnd = {
            if (isVertical) {
                val currentY = vState.value
                currentVState = when (currentVState) {
                    CalendarSize.SMALL -> {
                        when {
                            currentY > anchor[CalendarSize.MID.value] -> CalendarSize.LARGE
                            currentY > anchor[CalendarSize.SMALL.value] -> CalendarSize.MID
                            else -> currentVState
                        }
                    }
                    CalendarSize.MID -> {
                        when {
                            currentY < anchor[CalendarSize.MID.value] -> CalendarSize.SMALL
                            currentY > anchor[CalendarSize.MID.value] -> CalendarSize.LARGE
                            else -> currentVState
                        }
                    }
                    CalendarSize.LARGE -> {
                        when {
                            currentY < anchor[CalendarSize.MID.value] -> CalendarSize.SMALL
                            currentY < anchor[CalendarSize.LARGE.value] -> CalendarSize.MID
                            else -> currentVState
                        }
                    }
                }
                vState.value = anchor[currentVState.value]
            } else {
                val line = width * thresholds
                hState.value = when {
                    hDist < -line -> currentHState - width
                    hDist > line -> currentHState + width
                    else -> currentHState.toFloat()
                }
            }
        }
    )
}

enum class CalendarSize(val value: Int) {
    SMALL(0), MID(1), LARGE(2)
}

