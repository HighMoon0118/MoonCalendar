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
    val itemWidth = width.value
    var currentHState = 0
    var currentVState = CalendarSize.MID

    var hDist = 0f
    var isDetected = false
    var isVertical = false

    val minHeight = (anchor[0] * 0.8).toFloat()

    detectDragGestures (
        onDragStart = {
            currentHState = hState.value.toInt()

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
                            else -> CalendarSize.SMALL
                        }
                    }
                    CalendarSize.MID -> {
                        when {
                            currentY < anchor[CalendarSize.MID.value] -> CalendarSize.SMALL
                            currentY > anchor[CalendarSize.MID.value] -> CalendarSize.LARGE
                            else -> CalendarSize.MID
                        }
                    }
                    CalendarSize.LARGE -> {
                        when {
                            currentY < anchor[CalendarSize.MID.value] -> CalendarSize.SMALL
                            currentY < anchor[CalendarSize.LARGE.value] -> CalendarSize.MID
                            else -> CalendarSize.LARGE
                        }
                    }
                }
                vState.value = anchor[currentVState.value]
            } else {
                val line = itemWidth * thresholds
                hState.value = when {
                    hDist < -line -> currentHState - itemWidth
                    hDist > line -> currentHState + itemWidth
                    else -> currentHState.toFloat()
                }
            }
        }
    )
}

enum class CalendarSize(val value: Int) {
    SMALL(0), MID(1), LARGE(2)
}

