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
import kotlin.math.roundToInt

private val CELL_SIZE = 48.dp

@Composable
fun Calendar(
    month: Month,
    onDayClicked: (Day, Month) -> Unit,
    modifier: Modifier
) {
    Column(modifier) {
        val contentModifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)

        CalendarHeader(contentModifier)

        val calNum = 9

        Swiper(calNum = calNum)
        {
            for(i in 0 until calNum) {
                itemsCalendarMonth(
                    month = month,
                    onDayClicked = onDayClicked
                )
            }
        }
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
    var (preValue, preI, preX) = arrayOf(mid, 0, 0)

    Layout(
        modifier = Modifier
            .swipeable(
                state = horizonSwiper,
                anchors = hAnchors,
                thresholds = { _, _ -> FractionalThreshold(0.3f) },
                orientation = Orientation.Horizontal
            )
            .background(Color.DarkGray),
        content = content
    ) { measurables, constraints ->

        maxW = constraints.maxWidth

        setH(constraints.maxWidth, constraints.maxHeight)

        val (width, height) = arrayOf(maxW * calNum, constraints.maxHeight)
        val key = horizonSwiper.offset.value
        var tmp = calX.map { it + key.roundToInt() - preX }.toMutableList()

        hAnchors[key]?.let { currentValue ->
            val gap = currentValue - preValue
            var sI = preI - gap % calNum + calNum
            for (i in 0 until calNum) {
                if (sI >= calNum) sI %= calNum
                calX[sI] = i * maxW
                sI ++
            }
            preI = sI
            preX += gap * maxW
            preValue = currentValue

            tmp = calX
        }


        val placeables = measurables.map { measurable -> measurable.measure(constraints) }

        layout(width, height){
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
    onDayClicked: (Day, Month) -> Unit
) {

    val verticalSwiper =  rememberSwipeableState(1)
    val vAnchors = mapOf(minH.toFloat() to 0, maxH.toFloat() to 1)

    val itemH = verticalSwiper.offset.value / month.weeks.value.size

    Box(modifier = Modifier.swipeable(
        state = verticalSwiper,
        anchors = vAnchors,
        thresholds = { _, _ -> FractionalThreshold(0.5f) },
        orientation = Orientation.Vertical,
        resistance = ResistanceConfig(0f, 0f, 0f))
        .background(Color.Green)
    ) {
        LazyColumn(modifier = Modifier.background(Color.Blue)) {
            month.weeks.value.forEachIndexed { idx, week ->
                item(key = "${month.year}/${month.month}/${idx + 1}") {
                    Row(modifier = Modifier) {
                        for (day in week) {
                            ItemDay(day,onDayClicked = {item -> onDayClicked(item, month)})
                        }
                    }
                }
            }
        }
        Text(modifier = Modifier.size(verticalSwiper.offset.value.roundToInt().dp), text = "hi")
    }

}

@Composable
private fun ItemDay(
    day: Day,
    onDayClicked: (Day) -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = day.status != DayStatus.NonClickable
    Surface(
        modifier = modifier
            .size(CELL_SIZE)
            .clickable(enabled) { if (day.status != DayStatus.NonClickable) onDayClicked(day) }
    ) {
        Text(
            modifier = Modifier
                .size(CELL_SIZE)
                .wrapContentSize(Alignment.Center),
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