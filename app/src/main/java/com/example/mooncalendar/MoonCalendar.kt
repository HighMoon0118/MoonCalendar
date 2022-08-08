package com.example.mooncalendar

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.MutableLiveData
import java.security.cert.TrustAnchor
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

class MoonCalendar(private var calendar: Calendar) {

    private val CELL_SIZE = 48.dp
    private val CALENDAR_NUM = 3
    private val CALENDAR_SIZE = 1200
    private val CALENDAR_MID = CALENDAR_SIZE / 2

    private val _clickedDay: MutableLiveData<Day> by lazy {
        MutableLiveData<Day>(Month.today)
    }
    val clickedDay : MutableLiveData<Day> get() = _clickedDay

    private lateinit var monthList: ArrayList<Month>

    init {
        setCalendarData()
    }

    @ExperimentalMaterialApi
    @Composable
    fun DrawCalendar(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        var currentMonthIdx by remember { mutableStateOf(CALENDAR_MID) }

        // 바뀐 달만 다시 만들어주기 위해 하나씩 생성
        val left = remember { mutableStateOf(CALENDAR_MID - 1)}
        val mid = remember { mutableStateOf(CALENDAR_MID)}
        val right = remember { mutableStateOf(CALENDAR_MID + 1)}

        Column(modifier) {

            val contentModifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)

            val vAnchor = remember { mutableStateOf (listOf(0f, 0f, 0f) ) }
            val vState = remember { mutableStateOf (1080f) }
            val hState = remember { mutableStateOf (0f) }

//            val coroutineScope = rememberCoroutineScope()
//            val moveCalendar = {gap: Int ->
//                coroutineScope.launch {
//                    horizonSwiper.animateTo(horizonSwiper.currentValue + gap)  // animateTo 든 snapTo든 처음 실행할 때 swiper를 재구성함(다시만듦)
//                }
//            } 이상해,,,

            val moveCalendar = {plus: Int ->
                hState.value += plus * vAnchor.value[1]
            }

            CalendarHeader(contentModifier, monthList = { monthList }, currentMonthIdx = { currentMonthIdx + 1})

            Swiper(
                modifier = contentModifier,
                vState = vState, hState = hState, vAnchor = vAnchor,
                left = left, mid = mid, right = right,
                setCurrentMonthIdx = {idx -> currentMonthIdx = idx} // 람다함수를 사용함으로써 재구성을 피함
            ) {

                itemsCalendarMonth(month = { monthList[left.value] }) {
                    for(wI in 0..5) itemsCalendarWeek {
                        for (dI in 0..6) {
                            ItemDay(dI, { monthList[left.value].weeks.value[wI][dI] }, moveCalendar)  // 회색글씨를 클릭하면 다음달 또는 전달로 이동
                        }
                    }
                }
                itemsCalendarMonth(month = { monthList[mid.value] }) {
                    for(wI in 0..5) itemsCalendarWeek {
                        for (dI in 0..6) {
                            ItemDay( dI, { monthList[mid.value].weeks.value[wI][dI] }, moveCalendar)
                        }
                    }
                }
                itemsCalendarMonth(month = { monthList[right.value] }) {
                    for(wI in 0..5) itemsCalendarWeek {
                        for (dI in 0..6) {
                            ItemDay( dI, { monthList[right.value].weeks.value[wI][dI] }, moveCalendar)
                        }
                    }
                }
                content()
            }
        }
    }


    @Composable
    private fun CalendarHeader(
        modifier: Modifier,
        monthList: () -> ArrayList<Month>,
        currentMonthIdx: () -> Int
    )
    {
        val month = monthList()[currentMonthIdx()]

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

    // 안드로이드에서 제공하는 swipeable을 사용했을 때 문제점
    // 가로 세로 스와이퍼를 두개 만들어야함
    // 애니메이션이 부자연스럽고 커스터마이징을 할 수 없음
    // 월을 바꾼다거나 연도를 바꿀때 애니메이션이 가장 문제

//    @OptIn(ExperimentalMaterialApi::class)
//    @Composable
//    private fun Swiper(
//        modifier: Modifier = Modifier,
//        horizonSwiper: () -> SwipeableState<Int>,
//        idxList: () -> List<Int>,
//        setIdxList: (Int, Int, Int) -> Unit,
//        setCurrentMonthIdx: (Int) -> Unit,
//        content: @Composable () -> Unit,
//    ) {
//
//        var maxW by remember { mutableStateOf(1080) }
//        val offset = - maxW * CALENDAR_MID
//
//        val pages = (0 until CALENDAR_SIZE).map { offset + it * maxW }.toList()
//        val hAnchors = pages.mapIndexed {i, it -> it.toFloat() to i}.toMap()
//
//        val calX = (0 until CALENDAR_NUM).map { it * maxW }.toMutableList()
//        var (preX, preI) = arrayOf(0, CALENDAR_MID)
//
//        Log.d("ggggggggggg,", "hhhhhhhhhhhhh")
//        var mid = 1
//
//        Layout(
//            modifier = Modifier
//                .swipeable(
//                    state = horizonSwiper(),
//                    anchors = hAnchors,
//                    thresholds = { _, _ -> FractionalThreshold(0.1f) },
//                    orientation = Orientation.Horizontal,
//                    velocityThreshold = Integer.MAX_VALUE.dp
//                ),
//            content = content
//        ) { measurables, constraints ->
//
//            maxW = constraints.maxWidth
//            val width = maxW * CALENDAR_NUM
//
//            val moveX = horizonSwiper().offset.value.roundToInt() - preX
//            var tmp = calX.map { it + moveX }.toMutableList()
//
//            val nowI = horizonSwiper().progress.from
//            val swiperW = CALENDAR_NUM * maxW
//
//            val postI = horizonSwiper().progress.to
//
//            if (abs(moveX) > maxW / 2) setCurrentMonthIdx(CALENDAR_SIZE - postI)
//            else if (abs(moveX) < maxW / 2) setCurrentMonthIdx(CALENDAR_SIZE - nowI)
//
//            if (nowI != preI) {
//
//                var gap = nowI - preI
//                val plus = gap * maxW
//
//                for (i in 0 until CALENDAR_NUM) {
//                    calX[i] += plus
//                    if (calX[i] == swiperW) {  // 오른쪽으로 이동 (전 달)
//                        when(mid) {
//                            2 -> setIdxList(idxList()[1] - 1, idxList()[1], idxList()[2])
//                            0 -> setIdxList(idxList()[0], idxList()[2] - 1, idxList()[2])
//                            1 -> setIdxList(idxList()[0], idxList()[1], idxList()[0] - 1)
//                        }
//                        mid += 1
//                        if (mid > 2) mid -= 3
//                        calX[i] = 0
//                    }
//                    if (calX[i] < 0) {  // 왼쪽으로 이동 (다음 달)
//                        when(mid) {
//                            1 -> setIdxList(idxList()[2] + 1, idxList()[1], idxList()[2])
//                            2 -> setIdxList(idxList()[0], idxList()[0] + 1, idxList()[2])
//                            0 -> setIdxList(idxList()[0], idxList()[1], idxList()[1] + 1)
//                        }
//                        mid -= 1
//                        if (mid < 0) mid += 3
//                        calX[i] = swiperW - maxW
//                    }
//                }
//                preI = nowI
//                tmp = calX
//                preX += plus
//            }
//
//            val placeables = measurables.map { measurable -> measurable.measure(constraints) }
//            val minHeight = placeables.reduce { a, b ->
//                if (a.height < b.height) a else b
//            }
//            layout(width, minHeight.height){
//                placeables.forEachIndexed { i, placeable ->
//                    placeable.placeRelative(x = tmp[i], y = 0)
//                }
//            }
//        }
//    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun Swiper(
        modifier: Modifier = Modifier,
        left: MutableState<Int>, mid: MutableState<Int>, right: MutableState<Int>,
        setCurrentMonthIdx: (Int) -> Unit,
        vAnchor: MutableState<List<Float>>,
        vState: MutableState<Float>,
        hState: MutableState<Float>,
        content: @Composable () -> Unit,
    ) {

        val vAnimate = animateFloatAsState(
            targetValue = vState.value,
            animationSpec = tween(
                durationMillis = if (vState.value in vAnchor.value) 250 else 0,
                easing = LinearOutSlowInEasing
            ),
        )

        val hAnimate = animateFloatAsState(
            targetValue = hState.value,
            animationSpec = tween(
                durationMillis = 300,
                easing = LinearOutSlowInEasing
            ),
        )
        var size by remember { mutableStateOf(0)}

        var x_List by remember { mutableStateOf ( arrayOf(0, size, size * 2)) }
        if (x_List[1] == 0) x_List = arrayOf(0, size, size*2)

        var lmr by remember { mutableStateOf( arrayOf(0, 1, 2)) }

        Layout(
            modifier = modifier.MySwiper(
                vState = vState,
                hState = hState,
                vAnchor = vAnchor,
                thresholds = 0.1f
            ),
            content = content
        ) { measurables, constraints ->

            val w = constraints.maxWidth * 3
            val itemW = constraints.maxWidth
            val itemH = constraints.maxHeight

            size = itemW

            if (vAnchor.value[1].toInt() != itemW) {
                vAnchor.value = listOf((itemW / 2).toFloat(), itemW.toFloat(), itemH.toFloat())
            }

            val tmp = x_List.map { num -> num + hAnimate.value.toInt() }

            val state = (hState.value / itemW).roundToInt()
            setCurrentMonthIdx(CALENDAR_MID - state)


            if (tmp[lmr[2]] >= w) {  // 전 달로 이동

                when (lmr[1]) {
                    2 -> left.value -= 3  // 1 2 0 -> 0 1 2
                    0 -> mid.value -= 3
                    1 -> right.value -= 3
                }
                x_List[lmr[2]] -= w
                lmr = arrayOf(lmr[2], lmr[0], lmr[1])

            } else if (tmp[lmr[0]] <= -itemW) {  // 다음 달로 이동

                when (lmr[1]) {
                    1 -> left.value += 3
                    2 -> mid.value += 3
                    0 -> right.value += 3
                }
                x_List[lmr[0]] += w
                lmr = arrayOf(lmr[1], lmr[2], lmr[0])
            }


            val h = vAnimate.value.toInt()

            val items = measurables.map { measurable ->
                measurable.measure(Constraints(itemW, itemW, 0, h))
            }

            layout(w, itemH){
                for(i in 0 until 3) items[i].placeRelative(x = tmp[i], y = 0)
                items[3].placeRelative(x = itemW, y = h)
            }
        }
    }


//    @OptIn(ExperimentalMaterialApi::class)
//    @Composable
//    private fun itemsCalendarMonth(

//        month: () -> Month,
//        verticalSwiper: () -> SwipeableState<Int>,
//        content: @Composable () -> Unit
//    ) {
//        var minH by remember { mutableStateOf(1079) }
//        var midH by remember { mutableStateOf(1080) }
//        var maxH by remember { mutableStateOf(1081) }
//
//        val vAnchors = mapOf(minH.toFloat() to 0, midH.toFloat() to 1, maxH.toFloat() to 2)
//
//        Layout(
//            modifier = Modifier
//                .swipeable(
//                    state = verticalSwiper(),
//                    anchors = vAnchors,
//                    thresholds = { _, _ -> FractionalThreshold(0.5f) },
//                    orientation = Orientation.Vertical,
//                    resistance = ResistanceConfig(0f, 0f, 0f)
//                ),
//            content = content
//        ) { measurables, constraints ->
//
//            val width = constraints.maxWidth
//            val height = constraints.maxHeight
//            minH = width / 2
//            midH = width
//            maxH = height
//
//            val value = verticalSwiper().offset.value.roundToInt()
//
//            val itemH = value / month().weekSize
//
//            val placeables = measurables.map { measurable -> measurable.measure(Constraints(width, width, 0, itemH)) }
//
//
//            layout(width, value){
//                var h = 0
//                placeables.forEachIndexed { i, placeable ->
//                    if (month().weekSize == 5 && i == 5) return@forEachIndexed
//                    placeable.placeRelative(x = 0, y = h)
//                    h += itemH
//                }
//            }
//        }
//    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun itemsCalendarMonth(
        month: () -> Month,
        content: @Composable () -> Unit
    ) {
        Layout(
            modifier = Modifier,
            content = content
        ) { measurables, constraints ->

            val w = constraints.maxWidth
            val h = constraints.maxHeight

            val itemH = h / month().weekSize

            val placeables = measurables.map { measurable -> measurable.measure(Constraints(w, w, 0, itemH)) }


            layout(w, h){
                var y = 0
                placeables.forEachIndexed { i, placeable ->
                    if (month().weekSize == 5 && i == 5) return@forEachIndexed
                    placeable.placeRelative(x = 0, y = y)
                    y += itemH
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

            val w = constraints.maxWidth
            val h = constraints.maxHeight

            val itemW = w / 7

            val placeables = measurables.map { measurable -> measurable.measure(Constraints(itemW, itemW, 0, h)) }

            layout(w, h){
                var x = 0
                placeables.forEachIndexed { i, placeable ->
                    placeable.placeRelative(x = x, y = 0)
                    x += itemW
                }
            }
        }
    }

    @Composable
    private fun ItemDay(
        idx: Int,
        day: () -> Day,
        moveCalendar: (Int) -> Unit,
        modifier: Modifier = Modifier
    ) {
        val clickedColor = if (day().status == DayStatus.Clicked) Color.Gray else Color.White

        Card(
            modifier = modifier
                .MyClickable {  // 람다함수를 사용함으로써 재구성을 피함, 기존에 clickable을 빼니까 버벅거림이 사라짐
                    if (day().status != DayStatus.NonClickable) {
                        clickedDay.value!!.status =
                            if (clickedDay.value!! == Month.today) DayStatus.Today else DayStatus.Clickable

                        day().status = DayStatus.Clicked
                        clickedDay.value = day()
                    } else {
                        if (day().gap == -1) {
                            moveCalendar(1)
                        } else if (day().gap == 1) {
                            moveCalendar(-1)
                        }
                    }
                }
                .fillMaxSize(),
            border = BorderStroke(1.dp, clickedColor)
        ) {
            Column {
                Text(
                    text = day().value,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body1.copy(color = Color.Black),
                    color = when(day().status){
                        DayStatus.Today -> Color.Green
                        DayStatus.Clicked -> Color.Magenta
                        DayStatus.NonClickable -> Color.LightGray
                        else -> day().day.color()
                    }
                )
            }
        }
    }

    fun Modifier.MyClickable(
        onClicked: () -> Unit
    ) = composed {

        pointerInput(Unit) {
            detectTapGestures (
                onTap = { onClicked() }
            )
        }
    }

    private fun DayOfWeek.color(): Color = when (this) {
        DayOfWeek.Sunday -> Color.Red
        DayOfWeek.Saturday -> Color.Blue
        else -> Color.Black
    }

    private fun setCalendarData(date: Date = calendar.time) : ArrayList<Month>{
        calendar = Calendar.getInstance()
        calendar.time = date

        val tmpList = ArrayList<Month>()
        var tmpCal = calendar.clone() as Calendar

        tmpCal.set(Calendar.MONTH, tmpCal.get(Calendar.MONTH) - CALENDAR_MID)

        for (i in 0 until CALENDAR_SIZE) {
            tmpList.add(Month(tmpCal))
            tmpCal = tmpCal.clone() as Calendar
            tmpCal.set(Calendar.MONTH, tmpCal.get(Calendar.MONTH) + 1)
        }

        monthList = tmpList
        return monthList
    }
}
