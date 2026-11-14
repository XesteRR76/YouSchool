package org.youschool.app.ui.screens.schedule

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.youschool.app.domain.model.Lesson
import org.youschool.app.domain.repository.SchoolRepository
import org.youschool.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

class ScheduleViewModel(private val repository: SchoolRepository) : ViewModel() {

    // Number of days to show in the pager: 14 days before today, today, 14 days after today (29 days total)
    private val totalDays = 31
    val initialPage = 15

    private val baseCalendar: Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun getDateMillisForPage(page: Int): Long {
        val cal = baseCalendar.clone() as Calendar
        cal.add(Calendar.DAY_OF_YEAR, page - initialPage)
        return cal.timeInMillis
    }

    fun getLessonsForPage(page: Int): Flow<List<Lesson>> {
        val dateMillis = getDateMillisForPage(page)
        return repository.getLessonsForDay(dateMillis)
    }

    fun refreshDay(page: Int) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val dateMillis = getDateMillisForPage(page)
            repository.syncDayLessons(dateMillis)
            _isRefreshing.value = false
        }
    }

    fun toggleHomework(lessonId: Long, currentDone: Boolean) {
        viewModelScope.launch {
            repository.toggleHomeworkStatus(lessonId, !currentDone)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = viewModel.initialPage,
        pageCount = { 31 }
    )
    val coroutineScope = rememberCoroutineScope()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val currentSelectedDate = remember(pagerState.currentPage) {
        Date(viewModel.getDateMillisForPage(pagerState.currentPage))
    }

    val monthYearFormat = remember { SimpleDateFormat("LLLL yyyy", Locale("ru")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = monthYearFormat.format(currentSelectedDate).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Электронный дневник",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (pagerState.currentPage != viewModel.initialPage) {
                        FilledTonalButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(viewModel.initialPage)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Today,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Сегодня", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    IconButton(onClick = { viewModel.refreshDay(pagerState.currentPage) }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Обновить")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Interactive Week/Day Carousel Bar
            DayCarouselBar(
                currentPage = pagerState.currentPage,
                onDaySelected = { targetPage ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(targetPage)
                    }
                },
                getDateMillis = { viewModel.getDateMillisForPage(it) },
                pageCount = 31
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Main Pager with smooth swipe gestures
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val lessonsFlow = remember(page) { viewModel.getLessonsForPage(page) }
                val lessons by lessonsFlow.collectAsState(initial = emptyList())

                LaunchedEffect(page) {
                    // Sync with network in background (stale-while-revalidate)
                    viewModel.refreshDay(page)
                }

                if (lessons.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.EventBusy,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Уроков нет или выходной",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(lessons, key = { it.id }) { lesson ->
                            LessonCard(
                                lesson = lesson,
                                onToggleHomework = {
                                    viewModel.toggleHomework(lesson.id, lesson.isHomeworkDone)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayCarouselBar(
    currentPage: Int,
    onDaySelected: (Int) -> Unit,
    getDateMillis: (Int) -> Long,
    pageCount: Int
) {
    val dayOfWeekFormat = remember { SimpleDateFormat("EE", Locale("ru")) }
    val dayNumFormat = remember { SimpleDateFormat("d", Locale("ru")) }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(pageCount) { page ->
            val date = remember(page) { Date(getDateMillis(page)) }
            val isSelected = page == currentPage

            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                label = "day_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "day_text"
            )

            Column(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(backgroundColor)
                    .clickable { onDaySelected(page) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dayOfWeekFormat.format(date).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = dayNumFormat.format(date),
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun LessonCard(
    lesson: Lesson,
    onToggleHomework: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${lesson.numInDay}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = lesson.subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!lesson.gradeValue.isNullOrBlank()) {
                    GradeBadge(mark = lesson.gradeValue)
                }
            }

            if (lesson.topic.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = lesson.topic,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (lesson.teacherName.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = lesson.teacherName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (lesson.homeworkText.isNotBlank() || lesson.homeworkFiles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = lesson.isHomeworkDone,
                        onCheckedChange = { onToggleHomework() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = lesson.homeworkText.ifBlank { "Прикреплены материалы к уроку" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (lesson.isHomeworkDone) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                        )

                        if (lesson.homeworkFiles.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.AttachFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Файлов: ${lesson.homeworkFiles.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GradeBadge(mark: String) {
    val (bgColor, textColor) = when (mark.trim()) {
        "5" -> Grade5Color to Color.White
        "4" -> Grade4Color to Color.White
        "3" -> Grade3Color to Color.White
        "2", "1" -> Grade2Color to Color.White
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = bgColor,
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(
            text = mark,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
        )
    }
}
