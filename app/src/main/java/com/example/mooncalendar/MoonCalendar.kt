package com.example.mooncalendar

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.MutableLiveData
import java.security.cert.TrustAnchor
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.roundToInt

class MoonCalendar(private var calendar: Calendar) {

    private val CELL_SIZE = 48.dp
    private val CALENDAR_NUM = 3
    private val CALENDAR_SIZE = 1200
    private val CALENDAR_MID = CALENDAR_SIZE / 2

    private val _clickedDay: MutableLiveData<Day> by lazy {  // 화면 전환시 다시 오늘로 바뀜
        MutableLiveData<Day>(Month.today)
    }
    val clickedDay : MutableLiveData<Day> get() = _clickedDay

    private lateinit var monthList: ArrayList<Month>
    private var offset = 0

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
                currentMonthIdx += plus
                if (abs(plus) < 3) {
                    hState.value -= plus * vAnchor.value[1]
                } else {
                    offset += plus
                    left.value += plus
                    mid.value += plus
                    right.value += plus
                }
            }

            CalendarHeader(
                monthList = { monthList },
                currentMonthIdx = { currentMonthIdx + 1 }, // + 1을 해줘야 맞음
                moveCalendar = moveCalendar
            )

            Swiper(
                vState = vState, hState = hState, vAnchor = vAnchor,
                left = left, mid = mid, right = right,
                setCurrentMonthIdx = {idx -> currentMonthIdx = idx} // 람다함수를 사용함으로써 재구성을 피함
            ) {
                ItemCalendarMonth({ left.value }, { currentMonthIdx + 1 }, moveCalendar)
                ItemCalendarMonth({ mid.value }, { currentMonthIdx + 1 }, moveCalendar)
                ItemCalendarMonth({ right.value }, { currentMonthIdx + 1 }, moveCalendar)
                content()
            }
        }
    }


    @Composable
    private fun CalendarHeader(
        monthList: () -> ArrayList<Month>,
        currentMonthIdx: () -> Int,
        moveCalendar: (Int) -> Unit
    ) {
        Log.d("CalendarHeader", "${currentMonthIdx()}")
        val month = monthList()[currentMonthIdx()]
        val gap = (Month.todayYear - month.year) * 12 + Month.todayMonth - month.month + 1

        Column {
            ConstraintLayout(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                val (currentDate, today) = createRefs()

                Text(
                    text = if (month.month == 0) "${month.year - 1}년 12월" else "${month.year}년 ${month.month}월",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.constrainAs(currentDate) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                )
                Card(
                    border = BorderStroke(1.dp, Color(0xFF0074FF)),
                    modifier = Modifier
                        .constrainAs(today) {
                            top.linkTo(parent.top)
                            end.linkTo(parent.end)
                            bottom.linkTo(currentDate.bottom)
                        }
                        .size(36.dp)
                        .myClickable {
                            clickedDay.value?.also { day ->
                                day.status =
                                    if (day == Month.today) DayStatus.Today else DayStatus.Clickable
                            }
                            clickedDay.value = Month.today.apply { status = DayStatus.Clicked }
                            moveCalendar(gap)
                        }
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFCDDCEE)).wrapContentSize(Alignment.Center)) {
                        Text(
                            text = Month.today.value,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp, 0.dp)
                    .wrapContentWidth(Alignment.CenterHorizontally)
            ) {
                for (day in DayOfWeek.values()) {
                    Text(
                        modifier = Modifier
                            .weight(1 / 7f, true)
                            .padding(0.dp, 8.dp)
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


    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun Swiper(
        modifier: Modifier = Modifier,
        left: MutableState<Int>, mid: MutableState<Int>, right: MutableState<Int>,
        setCurrentMonthIdx: (Int) -> Unit,
        vAnchor: MutableState<List<Float>>, vState: MutableState<Float>, hState: MutableState<Float>,
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
            animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing)
        )
        var size by remember { mutableStateOf(0)}

        var x_List by remember { mutableStateOf ( arrayOf(0, size, size * 2)) }
        if (x_List[1] == 0) x_List = arrayOf(0, size, size*2)

        var lmr by remember { mutableStateOf( arrayOf(0, 1, 2)) }

        Layout(
            modifier = modifier.mySwiper(
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
            setCurrentMonthIdx(CALENDAR_MID + offset - state)


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

    @Composable
    private fun ItemCalendarMonth(
        monthIdx: () -> Int,
        currentMonthIdx: () -> Int,
        moveCalendar: (Int) -> Unit
    ) {
        Log.d("생성", "ItemMonth")

        val rows = if (monthList[monthIdx()].weekSize == 5) 5 else 6

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp, 0.dp)
        ) {
            repeat(rows) { r ->
                Row(Modifier.weight(1 / rows.toFloat(), true)) {
                    repeat(7) { c ->
                        Box(Modifier.weight(1 / 7f, true)){
                            ItemDay({ monthList[monthIdx()].weeks.value[r][c] }, currentMonthIdx, moveCalendar)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ItemDay(
        day: () -> Day,
        currentMonthIdx: () -> Int,
        moveCalendar: (Int) -> Unit
    ) {
        Log.d("생성", "ItemDay")
        Card(
            border = BorderStroke(1.dp, if (day().status == DayStatus.Clicked) Color.Gray else Color.White),
            modifier = Modifier
                .fillMaxSize()
                .myClickable {  // 람다함수를 사용함으로써 재구성을 피함, 기존에 clickable을 빼니까 앱을 켰을 때 세로 드래그시 버벅거림이 사라짐
                    clickedDay.value?.also { day ->
                        day.status =
                            if (day == Month.today) DayStatus.Today else DayStatus.Clickable
                    }
                    if (day().status != DayStatus.NonClickable) {
                        clickedDay.value = day().apply { status = DayStatus.Clicked }
                    } else {
                        monthList[currentMonthIdx() + day().gap].days
                            .find { it.value == day().value }
                            ?.also { day ->
                                clickedDay.value = day.apply { status = DayStatus.Clicked }
                            }
                        if (day().gap == -1) moveCalendar(-1)
                        else if (day().gap == 1) moveCalendar(+1)
                    }
                }
        ) {
            Column {
                Text(
                    modifier = Modifier.fillMaxWidth(),
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

    private fun Modifier.myClickable(  // onClicked 함수가 후에 바껴도 적용이 안됨... -> remeberUpdatedState로 해결
        onClicked: () -> Unit
    ) = composed {
        val onClickState = rememberUpdatedState(onClicked)
        pointerInput(Unit) {
            detectTapGestures (
                onTap = { onClickState.value() }
            )
        }
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
