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

@OptIn(ExperimentalComposeUiApi::class)
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

        var hDist = 0f
        var isDetected = false
        var isVertical = false

        coroutineScope {
            detectDragGestures (
                onDragStart = {
                    hDist = 0f
                    isDetected = false
                    isVertical = false
                },
                onDrag = { change, offset ->
                    if (!isDetected) {
                        if (abs(offset.x) < abs(offset.y)) isVertical = true
                        isDetected = true
                    }
                    if (isDetected && isVertical) {
                        var target = vState.value + offset.y

                        if (target <= 300) target = 300f
                        else if (target >= vAnchor[2]) target = vAnchor[2]

                        launch {
                            vState.value = target
                        }
                    }
                    else if(isDetected && !isVertical) {
                        launch {
                            hDist += offset.x
                            hState.value = currentHState + hDist
                        }
                    }
                },
                onDragEnd = {
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
            )
//
//            while (true) {
//
//                var hDist = 0f
//
//                awaitPointerEventScope {
//
//                    Log.d("ㅇㅇㅇ", "MySwiper1")
//                    // use (requireUnconsumed = false) instead of true to make sure we get even a consumed event
//                    val down = awaitFirstDown(requireUnconsumed = false)
//                    Log.d("ㅇㅇㅇ", "MySwiper2")
//
//
//                    val gap: Int = (hState.value - currentHState).toInt() / width.toInt()
//                    if (abs(gap) >= 1) currentHState += gap * width.toInt()
//
//                    awaitDragOrCancellation(down.id)?.apply {
//                        val moveX = abs(down.position.x - position.x)
//                        val moveY = abs(down.position.y - position.y)
//
//
//                        if (moveX < moveY ) {
//                            verticalDrag(id) { change ->
//
//                                val a = change.previousPosition.y
//                                val b = change.position.y
//
//                                var target = vState.value + (b - a)
//
//                                if (target <= 300) target = 300f
//                                else if (target >= vAnchor[2]) target = vAnchor[2]
//
//                                launch {
//                                    vState.value = target
//                                }
//                            }
//                            launch {
//                                val currentY = vState.value
//
//                                if (currentVState == 0) {
//                                    if (currentY > vAnchor[1]) currentVState = 2
//                                    else if (currentY > vAnchor[0]) currentVState = 1
//                                } else if (currentVState == 1) {
//                                    if (currentY < vAnchor[1]) currentVState = 0
//                                    else if (currentY > vAnchor[1]) currentVState = 2
//                                } else if (currentVState == 2) {
//                                    if (currentY < vAnchor[1]) currentVState = 0
//                                    else if (currentY < vAnchor[2]) currentVState = 1
//                                }
//
//                                vState.value = vAnchor[currentVState]
//                            }
//                        } else {
//                            horizontalDrag(id) { change ->
//
//                                val a = change.position.x
//
//                                hDist = a - down.position.x
//
//                                launch {
//                                    hState.value = currentHState + hDist
//                                }
//                            }
//                            launch {
//                                val line = width * thresholds
//
//                                if (hDist < -line) {
//                                    hState.value = currentHState - width
//                                } else if (hDist > line) {
//                                    hState.value = currentHState + width
//                                } else {
//                                    hState.value = currentHState.toFloat()
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//


        }
    }
}