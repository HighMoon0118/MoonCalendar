package com.example.mooncalendar

import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.*
import kotlin.math.abs

fun Modifier.MySwiper(
    vState: MutableState<Float>,
    hState: MutableState<Float>,
    vAnchor: MutableState<List<Float>>,
    thresholds: Float
) = composed {

    pointerInput(Unit) {

        val vAnchor = vAnchor.value
        var currentHState = 0
        var currentVState = 1

        coroutineScope {

            while (true) {

                var isVertical = false
                val velocityTracker = VelocityTracker()
                var hDist = 0f

                awaitPointerEventScope {



                    val down = awaitFirstDown()

                    var change = awaitDragOrCancellation(down.id)?.apply {

                        if (abs(down.position.x - position.x) < abs(down.position.y - position.y) ) {
                            isVertical = true
                        }
                    }

                    if (change != null && change.pressed) {

                        if (isVertical) {

                            verticalDrag(change.id) { change ->
                                velocityTracker.addPosition(
                                    change.uptimeMillis,
                                    change.position
                                )
                                val a = change.previousPosition.y
                                val b = change.position.y

                                var target = vState.value + (b - a)

                                if (target <= 300) target = 300f
                                else if (target >= vAnchor[2]) target = vAnchor[2]

                                launch {
                                    vState.value = target
                                }
                            }
                        } else {

                            if (hState.value - currentHState >= 1080) currentHState += 1080
                            else if (hState.value - currentHState <= -1080) currentHState -= 1080

                            velocityTracker.addPosition(
                                change.uptimeMillis,
                                change.position
                            )
                            horizontalDrag(change.id) { change ->
                                velocityTracker.addPosition(
                                    change.uptimeMillis,
                                    change.position
                                )

                                val a = change.position.x

                                hDist = a - down.position.x

                                launch {
                                    hState.value = currentHState + hDist
                                }
                            }
                        }
                    }
                }


                launch {
                    if (isVertical) {
                        val currentY = vState.value

                        if (currentVState == 0) {
                            if (currentY > vAnchor[1]) currentVState = 2
                            else if (currentY > vAnchor[0]) currentVState = 1
                        } else if (currentVState == 1) {
                            if (currentY < vAnchor[1]) currentVState = 0
                            else if (currentY > vAnchor[1]) currentVState = 2
                        } else if (currentVState == 2) {
                            if (currentY < vAnchor[1]) currentVState = 0
                            else if (currentY < vAnchor[2]) currentVState = 1
                        }

                        vState.value = vAnchor[currentVState]
                    } else {
                        val width = 1080

                        val line = width * thresholds

                        if (hDist < -line) {
                            hState.value = currentHState - 1080f
                        } else if (hDist > line) {
                            hState.value = currentHState + 1080f
                        } else {
                            hState.value = currentHState.toFloat()
                        }
                    }
                }
            }
        }
    }
}