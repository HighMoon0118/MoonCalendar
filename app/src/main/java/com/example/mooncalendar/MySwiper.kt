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
    thresholds: Float
) = pointerInput(Unit) {

        val anchor = vAnchor.value
        val width = anchor[1]
        var currentHState = 0
        var currentVState = 1

        var hDist = 0f
        var isDetected = false
        var isVertical = false
        var i = 0

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

                    if (target <= 300) target = 300f
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

                if (currentVState == 0) {
                    if (currentY > anchor[1]) currentVState = 2
                    else if (currentY > anchor[0]) currentVState = 1
                } else if (currentVState == 1) {
                    if (currentY < anchor[1]) currentVState = 0
                    else if (currentY > anchor[1]) currentVState = 2
                } else if (currentVState == 2) {
                    if (currentY < anchor[1]) currentVState = 0
                    else if (currentY < anchor[2]) currentVState = 1
                }

                vState.value = anchor[currentVState]
            } else {
                val line = width * thresholds

                if (hDist < -line) {
                    hState.value = currentHState - width
                } else if (hDist > line) {
                    hState.value = currentHState + width
                } else {
                    hState.value = currentHState.toFloat()
                }
            }
        }
    )
}

