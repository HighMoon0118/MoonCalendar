package com.example.mooncalendar

import android.app.Dialog
import android.app.DialogFragment
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
        modifier: Modifier = Modifier.size(300.dp, 300.dp),
        content: @Composable () -> Unit
    ) {
        Log.d("DrawCalendar", "DrawCalendar")
        var currentMonthIdx by remember { mutableStateOf(CALENDAR_MID) }

        var isTitleClicked by remember { mutableStateOf(false) }

        // 바뀐 달만 다시 만들어주기 위해 하나씩 생성
        val left = remember { mutableStateOf(CALENDAR_MID - 1)}
        val mid = remember { mutableStateOf(CALENDAR_MID)}
        val right = remember { mutableStateOf(CALENDAR_MID + 1)}

        var heightUnit by remember { mutableStateOf (0f) }
        val width = remember { mutableStateOf (0f) }
        val vAnchor = remember { mutableStateOf ( listOf(0f, 0f, 0f) ) }
        val vState = remember { mutableStateOf (0f) }
        val hState = remember { mutableStateOf (0f) }

        val density = LocalDensity.current.density

        Column(modifier.onGloballyPositioned { coordinates ->
            width.value = coordinates.size.width.toFloat()
            heightUnit = coordinates.size.height.toFloat() / 10
            vAnchor.value = listOf(heightUnit * 3, heightUnit * 6, heightUnit * 9)
            vState.value = vAnchor.value[1]
        }) {
            Log.d("DrawCalendar", "Column")

//            val coroutineScope = rememberCoroutineScope()
//            val moveCalendar = {gap: Int ->
//                coroutineScope.launch {
//                    horizonSwiper.animateTo(horizonSwiper.currentValue + gap)  // animateTo 든 snapTo든 처음 실행할 때 swiper를 재구성함(다시만듦)
//                }
//            } 이상해,,,

            val moveCalendar = {plus: Int ->
                currentMonthIdx += plus
                if (abs(plus) < 3) {
                    hState.value -= plus * width.value
                } else {
                    offset += plus
                    left.value += plus
                    mid.value += plus
                    right.value += plus
                }
            }

            CalendarHeader(
                modifier = Modifier.height((heightUnit / density).dp),
                monthList = { monthList },
                currentMonthIdx = { currentMonthIdx + 1 }, // + 1을 해줘야 맞음
                moveCalendar = moveCalendar,
                onTitleClicked = { isTitleClicked = true }
            )

            Swiper(
                width = width, vState = vState, hState = hState, vAnchor = vAnchor,
                left = left, mid = mid, right = right,
                setCurrentMonthIdx = {idx -> currentMonthIdx = idx} // 람다함수를 사용함으로써 재구성을 피함
            ) {
                ItemCalendarMonth({ left.value }, { currentMonthIdx }, moveCalendar)
                ItemCalendarMonth({ mid.value }, { currentMonthIdx }, moveCalendar)
                ItemCalendarMonth({ right.value }, { currentMonthIdx }, moveCalendar)
                content()
            }

            if (isTitleClicked) {
                Dialog(
                    onDismissRequest = { isTitleClicked = false },
                    properties = DialogProperties()
                ) {
                    Box(modifier = Modifier
                        .size(300.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .wrapContentSize(Alignment.Center))
                    {
                        Column(
                            modifier = Modifier
                                .background(Color.LightGray)
                                .size(100.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            repeat(10) {
                                Text("Item $it", modifier = Modifier.padding(2.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CalendarHeader(
        modifier: Modifier,
        monthList: () -> ArrayList<Month>,
        currentMonthIdx: () -> Int,
        moveCalendar: (Int) -> Unit,
        onTitleClicked: () -> Unit
    ) {
        Log.d("CalendarHeader", "${currentMonthIdx()}")
        val month = monthList()[currentMonthIdx()]
        val gap = (Month.todayYear - month.year) * 12 + Month.todayMonth - month.month + 1

        Column(modifier) {
            ConstraintLayout(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                val (currentDate, today) = createRefs()

                Text(
                    text = if (month.month == 0) "${month.year - 1}년 12월" else "${month.year}년 ${month.month}월",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier
                        .constrainAs(currentDate) {
                            top.linkTo(parent.top)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }
                        .myClickable { onTitleClicked() }
                )
                Card(
                    border = BorderStroke(2.dp, Color(0xFF0074FF)),
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
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFCDDCEE))
                        .wrapContentSize(Alignment.Center)
                    ){
                        Text(
                            text = Month.today.value,
                            fontWeight = FontWeight.Bold,
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
                        fontWeight = FontWeight.Bold,
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


    @Composable
    private fun Swiper(
        left: MutableState<Int>, mid: MutableState<Int>, right: MutableState<Int>,
        width: MutableState<Float>,
        vAnchor: MutableState<List<Float>>, vState: MutableState<Float>, hState: MutableState<Float>,
        setCurrentMonthIdx: (Int) -> Unit,
        content: @Composable () -> Unit,
    ) {
        Log.d("Swiper", "-")
        val vAnimate = animateFloatAsState(
            targetValue = vState.value,
            animationSpec = tween(
                durationMillis = if (vState.value in vAnchor.value) 250 else 0,
                easing = LinearOutSlowInEasing
            )
        )

        val hAnimate = animateFloatAsState(
            targetValue = hState.value,
            animationSpec = tween(
                durationMillis = 250,
                easing = LinearOutSlowInEasing
            )
        )

        var xList by remember { mutableStateOf ( arrayOf(0, 0, 0)) }
        if (xList[1] == 0) xList = arrayOf(0, width.value.toInt(), width.value.toInt() * 2)

        var lmr by remember { mutableStateOf( arrayOf(0, 1, 2)) }

        Layout(
            modifier = Modifier.mySwiper(
                vState = vState,
                hState = hState,
                vAnchor = vAnchor,
                width = width,
                thresholds = 0.15f
            ),
            content = content
        ) { measurables, constraints ->
            Log.d("Swiper", "Layout")

            val itemW = constraints.maxWidth
            val itemH = constraints.maxHeight
            val totalWidth = itemW * 3

            val state = (hState.value / itemW).roundToInt()
            setCurrentMonthIdx(CALENDAR_MID + offset - state)

            val tmp = xList.map { num -> num + hAnimate.value.toInt() }

            if (tmp[lmr[2]] >= totalWidth) {  // 전 달로 이동

                when (lmr[1]) {
                    2 -> left.value -= 3  // 1 2 0 -> 0 1 2
                    0 -> mid.value -= 3
                    1 -> right.value -= 3
                }
                xList[lmr[2]] -= totalWidth
                lmr = arrayOf(lmr[2], lmr[0], lmr[1])

            } else if (tmp[lmr[0]] <= -itemW) {  // 다음 달로 이동

                when (lmr[1]) {
                    1 -> left.value += 3
                    2 -> mid.value += 3
                    0 -> right.value += 3
                }
                xList[lmr[0]] += totalWidth
                lmr = arrayOf(lmr[1], lmr[2], lmr[0])
            }


            val animatedH = vAnimate.value.toInt()

            val items = measurables.map { measurable ->
                measurable.measure(Constraints(itemW, itemW, 0, animatedH))
            }

            layout(totalWidth, itemH){
                for(i in 0 until 3) items[i].placeRelative(x = tmp[i], y = 0)
                items[3].placeRelative(x = itemW, y = animatedH)
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

    /**
     * @param day : Day객체
     * @param currentMonthIdx : 해당 달의 Index
     * @param moveCalendar : 현재 달에서 보이는 이전 달의 날짜나 다음 달의 날짜를 클릭시 클릭한 달로 Swipe
     */
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    private fun ItemDay(
        day: () -> Day,
        currentMonthIdx: () -> Int,
        moveCalendar: (Int) -> Unit
    ) {
        Log.d("생성", "ItemDay")
        Card(
            border = BorderStroke(
                1.dp,
                if (day().status == DayStatus.Clicked)
                    Color.Gray
                else
                    Color.White
            ),
            modifier = Modifier
                .fillMaxSize()
                .myClickable(
                    onClicked = {
                        Log.d("clickable", "test")
                        // 람다함수를 사용함으로써 재구성을 피함
                        // 기존에 clickable을 빼니까 앱을 켰을 때 세로 드래그시 버벅거림이 사라짐
                        clickedDay.value?.also { day ->
                            day.status =
                                if (day == Month.today)
                                    DayStatus.Today
                                else
                                    DayStatus.Clickable
                        }
                        if (day().status != DayStatus.NonClickable) {
                            clickedDay.value = day().apply {
                                status = DayStatus.Clicked
                            }
                        } else {
                            monthList[currentMonthIdx() + day().gap].days
                                .find { it.value == day().value && it.status == DayStatus.Clickable }
                                ?.also { day ->
                                    clickedDay.value = day.apply {
                                        status = DayStatus.Clicked
                                    }
                                }
                            if (day().gap == -1) moveCalendar(-1)
                            else if (day().gap == 1) moveCalendar(+1)
                        }
                    }
                )
        ) {
            Column {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = day().value,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    style = MaterialTheme.typography.body1.copy(color = Color.Black),
                    color = when (day().status) {
                        DayStatus.Today -> Color.Green
                        DayStatus.Clicked -> Color.Magenta
                        DayStatus.NonClickable -> Color.LightGray
                        else -> day().day.color()
                    }
                )
            }
        }
    }

    private fun Modifier.myClickable(
        // onClicked 함수 인자가 후에 바껴도 적용이 안됨... -> remeberUpdatedState로 해결
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