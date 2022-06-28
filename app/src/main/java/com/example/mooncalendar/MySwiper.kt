package com.example.mooncalendar

import android.util.Log
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
        val width = vAnchor[1]
        var currentHState = 0
        var currentVState = 1

        coroutineScope {

            while (true) {

                var isVertical = false
                val velocityTracker = VelocityTracker()
                var hDist = 0f


                awaitPointerEventScope {

                    // use (requireUnconsumed = false) instead of true to make sure we get even a consumed even
                    val down = awaitFirstDown(requireUnconsumed = false)
                    Log.d("wwwwwwww", "${hState.value}")
                    val gap: Int = (hState.value - currentHState).toInt() / width.toInt()
                    if (abs(gap) >= 1) currentHState += gap * width.toInt()

                    var moveX = 0f
                    var moveY = 0f

                    var change = awaitDragOrCancellation(down.id)?.apply {
                        moveX = abs(down.position.x - position.x)
                        moveY = abs(down.position.y - position.y)
                        Log.d("ㅇㅇㅇㅇㅇㅇㅇ", "$moveX $moveY")

                        if (moveX < moveY ) isVertical = true
                    }

                    if (moveX < 3 && moveY < 3) change = null

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
            }
        }
    }
}