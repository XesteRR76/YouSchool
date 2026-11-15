package org.youschool.app.ui.screens.schedule

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.youschool.app.YouSchoolApp
import org.youschool.app.domain.model.HomeworkAttachment
import org.youschool.app.domain.model.Lesson
import org.youschool.app.domain.repository.SchoolRepository
import org.youschool.app.ui.components.AttachmentChips
import org.youschool.app.ui.components.ExpandableHomeworkText
import org.youschool.app.ui.components.FileOpener
import org.youschool.app.ui.components.ImageViewerDialog
import org.youschool.app.ui.components.PdfViewerDialog
import org.youschool.app.ui.theme.LocalBottomBarPadding
import org.youschool.app.ui.screens.homework.HomeworkSubmissionBottomSheet
import org.youschool.app.ui.theme.*
import org.youschool.app.ui.util.rememberHaptic
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ScheduleViewModel(val repository: SchoolRepository) : ViewModel() {

    // 40 weeks total: 20 weeks past, 1 current, 19 weeks future
    val totalWeeks = 40
    val initialWeekPage = 20

    // Monday 00:00:00 of the current week containing today
    private val currentWeekMonday: Calendar = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        val dayOfWeek = get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = (dayOfWeek - Calendar.MONDAY + 7) % 7
        add(Calendar.DAY_OF_YEAR, -daysFromMonday)
    }

    // Offset of today from Monday of the current week (0 = Mon .. 6 = Sun)
    val todayDayOfWeekIndex: Int = run {
        val today = Calendar.getInstance()
        (today.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
    }

    // Day page for today in the day pager
    val initialDayPage: Int = initialWeekPage * 7 + todayDayOfWeekIndex
    val totalDays: Int = totalWeeks * 7

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _downloadingFileId = MutableStateFlow<Long?>(null)
    val downloadingFileId: StateFlow<Long?> = _downloadingFileId.asStateFlow()

    var activeImageFile by mutableStateOf<File?>(null)
    var activePdfFile by mutableStateOf<File?>(null)

    init {
        refreshDay(initialDayPage)
    }

    fun getDateMillisForPage(page: Int): Long {
        val week = page / 7
        val dayInWeek = page % 7
        val cal = currentWeekMonday.clone() as Calendar
        cal.add(Calendar.DAY_OF_YEAR, (week - initialWeekPage) * 7 + dayInWeek)
        return cal.timeInMillis
    }

    fun getWeekMondayMillis(week: Int): Long {
        val cal = currentWeekMonday.clone() as Calendar
        cal.add(Calendar.DAY_OF_YEAR, (week - initialWeekPage) * 7)
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

    fun toggleHomework(lessonId: Long, currentDone: Boolean, comment: String? = null) {
        viewModelScope.launch {
            repository.toggleHomeworkStatus(lessonId, !currentDone, comment)
        }
    }

    fun openAttachment(context: Context, attachment: HomeworkAttachment) {
        viewModelScope.launch {
            _downloadingFileId.value = attachment.fileId
            val result = repository.downloadHomeworkFile(attachment.fileId, attachment.fileName, attachment.variantId)
            _downloadingFileId.value = null

            if (result.isSuccess) {
                val file = result.getOrNull()
                if (file != null) {
                    FileOpener.open(
                        context = context,
                        file = file,
                        onShowImage = { activeImageFile = it },
                        onShowPdf = { activePdfFile = it }
                    )
                }
            } else {
                Toast.makeText(context, "Не удалось загрузить файл: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = rememberHaptic()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val downloadingFileId by viewModel.downloadingFileId.collectAsState()

    val dayPagerState = rememberPagerState(
        initialPage = viewModel.initialDayPage,
        pageCount = { viewModel.totalDays }
    )

    val weekPagerState = rememberPagerState(
        initialPage = viewModel.initialWeekPage,
        pageCount = { viewModel.totalWeeks }
    )

    // Sync week pager when day pager scrolls across week boundaries
    LaunchedEffect(dayPagerState.currentPage) {
        val targetWeek = dayPagerState.currentPage / 7
        if (weekPagerState.currentPage != targetWeek && !weekPagerState.isScrollInProgress) {
            weekPagerState.animateScrollToPage(targetWeek)
        }
    }

    val visibleMonthDate = remember(weekPagerState.currentPage) {
        Date(viewModel.getWeekMondayMillis(weekPagerState.currentPage) + 3 * 86400000L)
    }

    val monthYearFormat = remember { SimpleDateFormat("LLLL yyyy", Locale("ru")) }
    var expandedHomeworkIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
    var selectedLessonForSubmission by remember { mutableStateOf<Lesson?>(null) }

    // Dialogs for in-app viewing
    viewModel.activeImageFile?.let { imgFile ->
        ImageViewerDialog(
            file = imgFile,
            onDismiss = { viewModel.activeImageFile = null }
        )
    }

    viewModel.activePdfFile?.let { pdfFile ->
        PdfViewerDialog(
            file = pdfFile,
            onDismiss = { viewModel.activePdfFile = null }
        )
    }

    selectedLessonForSubmission?.let { lessonToSubmit ->
        HomeworkSubmissionBottomSheet(
            lesson = lessonToSubmit,
            repository = viewModel.repository,
            onDismiss = { selectedLessonForSubmission = null },
            onSubmit = { isDone, comment ->
                viewModel.toggleHomework(lessonToSubmit.id, lessonToSubmit.isHomeworkDone, comment)
                selectedLessonForSubmission = lessonToSubmit.copy(isHomeworkDone = isDone)
            },
            onShowImage = { viewModel.activeImageFile = it },
            onShowPdf = { viewModel.activePdfFile = it }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = monthYearFormat.format(visibleMonthDate).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (dayPagerState.currentPage != viewModel.initialDayPage || weekPagerState.currentPage != viewModel.initialWeekPage) {
                        FilledTonalButton(
                            onClick = {
                                coroutineScope.launch {
                                    dayPagerState.animateScrollToPage(viewModel.initialDayPage)
                                    weekPagerState.animateScrollToPage(viewModel.initialWeekPage)
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
                    IconButton(onClick = { viewModel.refreshDay(dayPagerState.currentPage) }) {
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
            // 7-day Monday to Sunday Weekly Carousel
            WeekCarouselBar(
                weekPagerState = weekPagerState,
                selectedDayPage = dayPagerState.currentPage,
                onDaySelected = { targetDayPage ->
                    haptic.tick()
                    coroutineScope.launch {
                        dayPagerState.animateScrollToPage(targetDayPage)
                    }
                },
                getWeekMondayMillis = { viewModel.getWeekMondayMillis(it) },
                initialDayPage = viewModel.initialDayPage
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Main Day Pager
            HorizontalPager(
                state = dayPagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val lessonsFlow = remember(page) { viewModel.getLessonsForPage(page) }
                val lessons by lessonsFlow.collectAsState(initial = null)

                LaunchedEffect(page) {
                    viewModel.refreshDay(page)
                }

                if (lessons == null) {
                    Box(modifier = Modifier.fillMaxSize())
                } else if (lessons!!.isEmpty()) {
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
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp + LocalBottomBarPadding.current
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(lessons!!, key = { it.id }) { lesson ->
                            LessonCard(
                                lesson = lesson,
                                downloadingFileId = downloadingFileId,
                                isHomeworkExpanded = expandedHomeworkIds.contains(lesson.id),
                                onToggleHomeworkExpand = {
                                    expandedHomeworkIds = if (expandedHomeworkIds.contains(lesson.id)) {
                                        expandedHomeworkIds - lesson.id
                                    } else {
                                        expandedHomeworkIds + lesson.id
                                    }
                                },
                                onToggleHomework = {
                                    haptic.tick()
                                    viewModel.toggleHomework(lesson.id, lesson.isHomeworkDone)
                                },
                                onAttachmentClick = { attachment ->
                                    viewModel.openAttachment(context, attachment)
                                },
                                onSubmissionClick = {
                                    haptic.click()
                                    selectedLessonForSubmission = lesson
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
fun WeekCarouselBar(
    weekPagerState: androidx.compose.foundation.pager.PagerState,
    selectedDayPage: Int,
    onDaySelected: (Int) -> Unit,
    getWeekMondayMillis: (Int) -> Long,
    initialDayPage: Int
) {
    val dayNames = remember { listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС") }
    val dayNumFormat = remember { SimpleDateFormat("d", Locale("ru")) }

    HorizontalPager(
        state = weekPagerState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) { weekIndex ->
        val mondayMillis = remember(weekIndex) { getWeekMondayMillis(weekIndex) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (dayInWeek in 0..6) {
                val dayPage = weekIndex * 7 + dayInWeek
                val isSelected = dayPage == selectedDayPage
                val isToday = dayPage == initialDayPage

                val dayDate = remember(mondayMillis, dayInWeek) {
                    Date(mondayMillis + dayInWeek * 86400000L)
                }

                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    label = "day_bg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    label = "day_text"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .background(backgroundColor)
                        .clickable { onDaySelected(dayPage) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayNames[dayInWeek],
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = if (isSelected) 0.9f else 0.7f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dayNumFormat.format(dayDate),
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    // Small indicator dot for today
                    if (isToday) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                        )
                    } else {
                        Spacer(modifier = Modifier.size(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LessonCard(
    lesson: Lesson,
    downloadingFileId: Long?,
    isHomeworkExpanded: Boolean,
    onToggleHomeworkExpand: () -> Unit,
    onToggleHomework: () -> Unit,
    onAttachmentClick: (HomeworkAttachment) -> Unit,
    onSubmissionClick: (() -> Unit)? = null
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

                if (lesson.grades.isEmpty() && !lesson.gradeValue.isNullOrBlank()) {
                    GradeBadge(mark = lesson.gradeValue)
                }
            }

            // Grades under subject title and above homework
            if (lesson.grades.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    lesson.grades.forEach { grade ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            GradeBadge(mark = grade.value, weight = grade.weight)
                            Spacer(modifier = Modifier.width(6.dp))
                            val weightStr = if (grade.weight % 1.0 == 0.0) grade.weight.toInt().toString() else "${grade.weight}"
                            Text(
                                text = "Вес: $weightStr",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (grade.typeName.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = grade.typeName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
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
                        ExpandableHomeworkText(
                            text = lesson.homeworkText.ifBlank { "Прикреплены материалы к уроку" },
                            isDone = lesson.isHomeworkDone,
                            isExpanded = isHomeworkExpanded,
                            onToggleExpand = onToggleHomeworkExpand
                        )

                        if (lesson.homeworkFiles.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            AttachmentChips(
                                files = lesson.homeworkFiles,
                                downloadingFileId = downloadingFileId,
                                onFileClick = onAttachmentClick
                            )
                        }

                        if (onSubmissionClick != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = onSubmissionClick,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudUpload,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Решение",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GradeBadge(
    mark: String,
    weight: Double? = null,
    modifier: Modifier = Modifier.padding(start = 8.dp)
) {
    val themeSettings by YouSchoolApp.instance.themeManager.themeSettings.collectAsState()
    val (bgColor, textColor) = remember(mark, weight, themeSettings) {
        GradeColorCalculator.calculateGradeColor(mark, weight, themeSettings)
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = bgColor,
        modifier = modifier
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
