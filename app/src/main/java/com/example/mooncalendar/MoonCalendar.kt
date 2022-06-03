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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.roundToInt

class MoonCalendar(private val calendar: Calendar) {

    private val CELL_SIZE = 48.dp
    private val CALENDAR_NUM = 3
    private val CALENDAR_SIZE = 1000
    private val CALENDAR_MID = CALENDAR_SIZE / 2
    private lateinit var monthList: ArrayList<Month>

    init {
        setCalendarData()
    }

    @ExperimentalMaterialApi
    @Composable
    fun DrawCalendar(
        modifier: Modifier = Modifier,
        onDayClick: (Day) -> Unit,
        content: @Composable () -> Unit
    ) {
        var currentMonthIdx by remember { mutableStateOf(CALENDAR_MID + 1) }
        // 바뀐 달만 다시 만들어주기 위해 하나씩 생성
        var left by remember { mutableStateOf(CALENDAR_MID - 1) }
        var mid by remember { mutableStateOf(CALENDAR_MID) }
        var right by remember { mutableStateOf(CALENDAR_MID + 1) }

        Column(modifier) {

            val contentModifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)

            CalendarHeader(contentModifier, currentMonthIdx = { currentMonthIdx })
            Swiper(
                modifier = contentModifier,
                idxList = { listOf(left, mid, right)},
                setIdxList = {l,m,r -> left=l; mid=m; right=r},
                currentMonthIdx = { currentMonthIdx },
                setCurrentMonthIdx = {idx -> currentMonthIdx = idx}
            ) {

                val verticalSwiper =  rememberSwipeableState(0)

                // 현재 왼쪽, 가운데, 오른쪽으로 하나씩 만들었는데 리스트를 remember해서 바뀐부분만 업데이트 해줄 수 있는 방안을 찾아서 간소화 시키기

                itemsCalendarMonth(month = { monthList[left] }, verticalSwiper = verticalSwiper) {
                    monthList[left].weeks.value.forEach { week ->
                        itemsCalendarWeek {
                            for (day in week)
                                ItemDay(day, onDayClick = onDayClick)
                        }
                    }
                }
                itemsCalendarMonth(month = { monthList[mid] }, verticalSwiper = verticalSwiper) {
                    monthList[mid].weeks.value.forEach { week ->
                        itemsCalendarWeek {
                            for (day in week)
                                ItemDay(day, onDayClick = onDayClick)
                        }
                    }
                }
                itemsCalendarMonth(month = { monthList[right] }, verticalSwiper = verticalSwiper) {
                    monthList[right].weeks.value.forEach { week ->
                        itemsCalendarWeek {
                            for (day in week)
                                ItemDay(day, onDayClick = onDayClick)
                        }
                    }
                }
            }
            content()
        }
    }


    @Composable
    private fun CalendarHeader(
        modifier: Modifier,
        currentMonthIdx: () -> Int
    )
    {
        val month = monthList[currentMonthIdx()]

        Column() {
            Row(modifier = modifier.padding(20.dp)) {
                Text(
                    text = if (month.month == 0) "${month.year - 1}년 12월" else "${month.year}년 ${month.month}월",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    fontStyle = FontStyle.Italic
                )
            }

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
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun Swiper(
        modifier: Modifier = Modifier,
        idxList: () -> List<Int>,
        setIdxList: (Int, Int, Int) -> Unit,
        currentMonthIdx: () -> Int,
        setCurrentMonthIdx: (Int) -> Unit,
        content: @Composable () -> Unit
    ) {

        val horizonSwiper = rememberSwipeableState(CALENDAR_MID)
        var maxW by remember { mutableStateOf(1080) }
        val offset = - maxW * CALENDAR_MID

        val pages = (0 until CALENDAR_SIZE).map { offset + it * maxW }.toList()
        val hAnchors = pages.mapIndexed {i, it -> it.toFloat() to i}.toMap()

        val calX = (0 until CALENDAR_NUM).map { it * maxW }.toMutableList()
        var (preX, preI) = arrayOf(0, CALENDAR_MID)

        Layout(
            modifier = Modifier
                .swipeable(
                    state = horizonSwiper,
                    anchors = hAnchors,
                    thresholds = { _, _ -> FractionalThreshold(0.3f) },
                    orientation = Orientation.Horizontal,
                    velocityThreshold = Integer.MAX_VALUE.dp
                ),
            content = content
        ) { measurables, constraints ->

            maxW = constraints.maxWidth
            val (width, height) = arrayOf(maxW * CALENDAR_NUM, constraints.maxHeight)
            val key = horizonSwiper.offset.value
            var tmp = calX.map { it + key.roundToInt() - preX }.toMutableList()

            val nowI = horizonSwiper.progress.from
            val swiperW = CALENDAR_NUM * maxW

            if (nowI != preI) {
                val gap = nowI - preI
                val plus = gap * maxW
                setCurrentMonthIdx(currentMonthIdx() - gap)

                for (i in 0 until CALENDAR_NUM) {
                    calX[i] += plus
                    if (calX[i] == swiperW) {  // 오른쪽으로 이동
                        when(i) {
                            0 -> setIdxList(idxList()[1] - 1, idxList()[1], idxList()[2])
                            1 -> setIdxList(idxList()[0], idxList()[2] - 1, idxList()[2])
                            else -> setIdxList(idxList()[0], idxList()[1], idxList()[0] - 1)
                        }
                        calX[i] = 0
                    }
                    if (calX[i] < 0) {  // 왼쪽으로 이동
                        when(i) {
                            0 -> setIdxList(idxList()[2] + 1, idxList()[1], idxList()[2])
                            1 -> setIdxList(idxList()[0], idxList()[0] + 1, idxList()[2])
                            else -> setIdxList(idxList()[0], idxList()[1], idxList()[1] + 1)
                        }
                        calX[i] = swiperW - maxW
                    }
                }
                preI = nowI
                tmp = calX
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

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun itemsCalendarMonth(
        month: () -> Month,
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
            val itemH = value / month().weeks.value.size

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

    private fun setCalendar(calendar: Calendar) {  // calendar말고 String으로 설정하도록 수정
        this.calendar.time = calendar.time
    }

    private fun setCalendarData() {
        val tmpList = ArrayList<Month>()
        var tmpCal = calendar.clone() as Calendar

        tmpCal.set(Calendar.MONTH, tmpCal.get(Calendar.MONTH) - CALENDAR_MID)

        for (i in 0 until CALENDAR_SIZE) {
            tmpList.add(Month(tmpCal))
            tmpCal = tmpCal.clone() as Calendar
            tmpCal.set(Calendar.MONTH, tmpCal.get(Calendar.MONTH) + 1)
        }

        monthList = tmpList
    }

}