package com.example.mooncalendar

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

private val CELL_SIZE = 48.dp

@ExperimentalMaterialApi
@Composable
fun Calendar(
    month: Month,
    modifier: Modifier = Modifier,
    onDayClick: (Day) -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier) {
        val contentModifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)

        CalendarHeader(contentModifier)

        val calNum = 3

        Swiper(
            modifier = contentModifier,
            calNum = calNum
        )
        {
            val verticalSwiper =  rememberSwipeableState(0)

            for(i in 0 until calNum) {
                itemsCalendarMonth(month = month, verticalSwiper = verticalSwiper) {
                    month.weeks.value.forEach { week ->
                        itemsCalendarWeek {
                            for (day in week)
                                ItemDay(day, onDayClick = onDayClick)
                        }
                    }
                }
            }
        }
        content()
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun Swiper(
    modifier: Modifier = Modifier,
    calNum: Int,
    content: @Composable () -> Unit
) {
    val size = 1000
    val mid = size / 2 - 1

    val horizonSwiper = rememberSwipeableState(mid)
    var maxW by remember { mutableStateOf(1080) }
    val offset = - maxW * mid

    val pages = (0 until size).map { offset + it * maxW }.toList()
    val hAnchors = pages.mapIndexed {i, it -> it.toFloat() to i}.toMap()

    val calX = (0 until calNum).map { it * maxW }.toMutableList()
    var (preX, preI) = arrayOf(0, mid)

    Layout(
        modifier = Modifier
            .swipeable(
                state = horizonSwiper,
                anchors = hAnchors,
                thresholds = { _, _ -> FractionalThreshold(0.3f) },
                orientation = Orientation.Horizontal
            ),
        content = content
    ) { measurables, constraints ->

        maxW = constraints.maxWidth
        val (width, height) = arrayOf(maxW * calNum, constraints.maxHeight)
        val key = horizonSwiper.offset.value
        var tmp = calX.map { it + key.roundToInt() - preX }.toMutableList()

        val nowI = horizonSwiper.progress.from
        val swiperW = calNum * maxW

        if (nowI != preI) {
            val plus = (nowI - preI) * maxW

            for (i in 0 until calNum) {
                calX[i] += plus
                if (calX[i] == swiperW) calX[i] = 0
                if (calX[i] < 0) calX[i] = swiperW - maxW
            }
            tmp = calX
            preI = nowI
            preX += plus
        }
        val placeables = measurables.map { measurable -> measurable.measure(constraints) }
        var minHeight = placeables.reduce { a, b ->
            if (a.height < b.height) a else b
        }
        layout(width, minHeight.height){
            placeables.forEachIndexed { i, placeable ->
                placeable.placeRelative(x = tmp[i], y = 0)
            }
        }
    }
}

@Composable
private fun CalendarHeader(modifier: Modifier) {
    Row(modifier = modifier) {
        for (day in DayOfWeek.values()) {
            Text(
                modifier = Modifier
                    .size(width = CELL_SIZE, height = CELL_SIZE)
                    .wrapContentSize(Alignment.Center),
                text = day.name.take(1),
                style = MaterialTheme.typography.caption.copy(Color.White.copy(alpha = 0.6f)),
                color = Color.Black
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun itemsCalendarMonth(
    month: Month,
    verticalSwiper: SwipeableState<Int>,
    content: @Composable () -> Unit
) {

    var minH by remember { mutableStateOf(1080) }
    var maxH by remember { mutableStateOf(1081) }

    val vAnchors = mapOf(minH.toFloat() to 0, maxH.toFloat() to 1)

    Layout(
        modifier = Modifier
            .swipeable(
                state = verticalSwiper,
                anchors = vAnchors,
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Vertical,
                resistance = ResistanceConfig(0f, 0f, 0f)
            )
            .background(Color.LightGray),
        content = content
    ) { measurables, constraints ->

        val width = constraints.maxWidth
        val height = constraints.maxHeight
        minH = width
        maxH = height

        val value = verticalSwiper.offset.value.roundToInt()
        val itemH = value / month.weeks.value.size

        val placeables = measurables.map { measurable -> measurable.measure(Constraints(width, width, itemH, itemH)) }

        layout(width, value){
            var h = 0
            placeables.forEachIndexed { i, placeable ->
                placeable.placeRelative(x = 0, y = h)
                h += itemH
            }
        }
    }
}

@Composable
private fun itemsCalendarWeek(
    content: @Composable () -> Unit
) {
    Layout(
        modifier = Modifier,
        content = content
    ) { measurables, constraints ->

        val width = constraints.maxWidth
        val height = constraints.maxHeight

        val itemW = width / 7

        val placeables = measurables.map { measurable -> measurable.measure(Constraints(itemW, itemW, height, height)) }

        layout(width, height){
            var w = 0
            placeables.forEachIndexed { i, placeable ->
                placeable.placeRelative(x = w, y = 0)
                w += itemW
            }
        }
    }
}

@Composable
private fun ItemDay(
    day: Day,
    modifier: Modifier = Modifier,
    onDayClick: (Day) -> Unit
) {
    val enabled = day.status != DayStatus.NonClickable
    Surface(
        modifier = modifier.clickable(enabled) { onDayClick(day) }
    ) {
        Text(
            text = day.value,
            style = MaterialTheme.typography.body1.copy(color = Color.Black),
            color = day.status.color()
        )
    }
}

private fun DayStatus.color(): Color = when (this) {
    DayStatus.Today -> Color.Blue
    DayStatus.Clickable -> Color.Black
    else -> Color.LightGray
}