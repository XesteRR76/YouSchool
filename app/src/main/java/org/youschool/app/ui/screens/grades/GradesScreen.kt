package org.youschool.app.ui.screens.grades

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.youschool.app.domain.model.Grade
import org.youschool.app.domain.model.SubjectAverage
import org.youschool.app.domain.model.SubjectSimulation
import org.youschool.app.domain.repository.SchoolRepository
import org.youschool.app.ui.screens.schedule.GradeBadge
import org.youschool.app.ui.theme.LocalBottomBarPadding
import org.youschool.app.ui.util.rememberHaptic
import java.text.SimpleDateFormat
import java.util.*

enum class SubjectSortOrder(val title: String) {
    DEFAULT("По названию"),
    SCORE_DESC("Сначала высокий балл"),
    SCORE_ASC("Сначала низкий балл")
}

class GradesViewModel(private val repository: SchoolRepository) : ViewModel() {

    val subjectAverages: StateFlow<List<SubjectAverage>> = repository.getSubjectAverages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGrades: StateFlow<List<Grade>> = repository.getAllGrades()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val newGrades: StateFlow<List<Grade>> = repository.getNewGrades()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _simulations = MutableStateFlow<Map<String, SubjectSimulation>>(emptyMap())
    val simulations: StateFlow<Map<String, SubjectSimulation>> = _simulations.asStateFlow()

    fun updateSimulation(subjectName: String, simulation: SubjectSimulation) {
        val current = _simulations.value.toMutableMap()
        if (!simulation.isActive) {
            current.remove(subjectName)
        } else {
            current[subjectName] = simulation
        }
        _simulations.value = current
    }

    fun resetSimulation(subjectName: String) {
        val current = _simulations.value.toMutableMap()
        current.remove(subjectName)
        _simulations.value = current
    }

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.syncGrades()
            _isRefreshing.value = false
        }
    }

    fun markGradeAsRead(markKey: String) {
        viewModelScope.launch {
            repository.markGradeAsRead(markKey)
        }
    }

    fun markAllGradesAsRead() {
        viewModelScope.launch {
            repository.markAllGradesAsRead()
        }
    }

    fun restoreGradeReadStatus(markKeys: List<String>) {
        viewModelScope.launch {
            repository.restoreGradeReadStatus(markKeys)
        }
    }
}

data class UndoActionState(
    val message: String,
    val totalSeconds: Int = 5,
    val onUndo: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesScreen(
    viewModel: GradesViewModel,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHaptic()
    val averages by viewModel.subjectAverages.collectAsState()
    val allGrades by viewModel.allGrades.collectAsState()
    val newGrades by viewModel.newGrades.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val simulations by viewModel.simulations.collectAsState()

    var sortOrder by rememberSaveable { mutableStateOf(SubjectSortOrder.DEFAULT) }
    var showSortMenu by remember { mutableStateOf(false) }
    var expandedSubjectNames by rememberSaveable { mutableStateOf(setOf<String>()) }
    var calculatorSubjectName by rememberSaveable { mutableStateOf<String?>(null) }

    val sortedAverages = remember(averages, sortOrder) {
        when (sortOrder) {
            SubjectSortOrder.DEFAULT -> averages.sortedBy { it.subjectName }
            SubjectSortOrder.SCORE_DESC -> averages.sortedByDescending { it.averageScore }
            SubjectSortOrder.SCORE_ASC -> averages.sortedBy { it.averageScore }
        }
    }

    var activeUndoAction by remember { mutableStateOf<UndoActionState?>(null) }
    var secondsRemaining by remember { mutableIntStateOf(5) }

    LaunchedEffect(activeUndoAction) {
        val current = activeUndoAction
        if (current != null) {
            for (sec in current.totalSeconds downTo 1) {
                secondsRemaining = sec
                delay(1000L)
            }
            secondsRemaining = 0
            activeUndoAction = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Оценки и успеваемость",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Сортировка")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SubjectSortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = order.title,
                                            fontWeight = if (order == sortOrder) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        sortOrder = order
                                        showSortMenu = false
                                    },
                                    leadingIcon = if (order == sortOrder) {
                                        { Icon(Icons.Rounded.Check, contentDescription = null) }
                                    } else null
                                )
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Обновить")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 12.dp,
                end = 16.dp,
                bottom = 16.dp + LocalBottomBarPadding.current
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // New Grades Section (auto-hidden when empty)
            if (newGrades.isNotEmpty()) {
                item(key = "new_grades_section") {
                    NewGradesSection(
                        newGrades = newGrades,
                        onDismissGrade = { grade ->
                            val markKey = grade.markKey
                            viewModel.markGradeAsRead(markKey)
                            activeUndoAction = UndoActionState(
                                message = "Оценка по ${grade.subjectName} прочитана",
                                totalSeconds = 5,
                                onUndo = {
                                    viewModel.restoreGradeReadStatus(listOf(markKey))
                                }
                            )
                            secondsRemaining = 5
                        },
                        onDismissAll = {
                            val keys = newGrades.map { it.markKey }
                            viewModel.markAllGradesAsRead()
                            activeUndoAction = UndoActionState(
                                message = "Все новые оценки прочитаны",
                                totalSeconds = 5,
                                onUndo = {
                                    viewModel.restoreGradeReadStatus(keys)
                                }
                            )
                            secondsRemaining = 5
                        }
                    )
                }
            }

            if (sortedAverages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Оценок пока нет",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                items(sortedAverages, key = { it.subjectName }) { item ->
                    val subjectGrades = remember(allGrades, item.subjectName) {
                        allGrades.filter { it.subjectName == item.subjectName }
                    }
                    val isExpanded = expandedSubjectNames.contains(item.subjectName)
                    val itemSimulation = simulations[item.subjectName]
                    SubjectGradeCard(
                        item = item,
                        grades = subjectGrades,
                        simulation = itemSimulation,
                        isExpanded = isExpanded,
                        onToggleExpand = {
                            expandedSubjectNames = if (isExpanded) {
                                expandedSubjectNames - item.subjectName
                            } else {
                                expandedSubjectNames + item.subjectName
                            }
                        },
                        onOpenCalculator = {
                            calculatorSubjectName = item.subjectName
                        },
                        onResetSimulation = {
                            viewModel.resetSimulation(item.subjectName)
                        }
                    )
                }
            }
        }

        if (calculatorSubjectName != null) {
            val targetSubject = averages.firstOrNull { it.subjectName == calculatorSubjectName }
            if (targetSubject != null) {
                val targetGrades = remember(allGrades, targetSubject.subjectName) {
                    allGrades.filter { it.subjectName == targetSubject.subjectName }
                }
                val currentSim = simulations[targetSubject.subjectName] ?: SubjectSimulation()
                GradeCalculatorBottomSheet(
                    subject = targetSubject,
                    grades = targetGrades,
                    initialSimulation = currentSim,
                    onSimulationChange = { sim ->
                        viewModel.updateSimulation(targetSubject.subjectName, sim)
                    },
                    onResetSimulation = {
                        viewModel.resetSimulation(targetSubject.subjectName)
                    },
                    onDismiss = { calculatorSubjectName = null }
                )
            }
        }
    }

    // Floating Undo Banner positioned safely above the navigation bar with countdown
    AnimatedVisibility(
        visible = activeUndoAction != null,
        enter = slideInVertically(initialOffsetY = { it * 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it * 2 }) + fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp + LocalBottomBarPadding.current)
    ) {
        activeUndoAction?.let { undoState ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = undoState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            haptic.confirm()
                            undoState.onUndo()
                            activeUndoAction = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.inversePrimary,
                            contentColor = MaterialTheme.colorScheme.inverseSurface
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Отменить (${secondsRemaining}с)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { activeUndoAction = null },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Закрыть",
                            tint = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGradesSection(
    newGrades: List<Grade>,
    onDismissGrade: (Grade) -> Unit,
    onDismissAll: () -> Unit
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    val blockDismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                onDismissAll()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = blockDismissState,
        backgroundContent = {
            if (blockDismissState.dismissDirection != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 24.dp),
                    contentAlignment = if (blockDismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                        Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.DoneAll,
                            contentDescription = "Прочитать все",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Пометить все прочитанными",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .clickable { isExpanded = !isExpanded },
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Новые оценки",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.defaultMinSize(minWidth = 24.dp, minHeight = 20.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${newGrades.size}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (isExpanded) "Нажмите, чтобы скрыть" else "Нажмите для просмотра",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Compact preview badges or action buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (!isExpanded) {
                            // Show small preview of recent marks
                            newGrades.take(2).forEach { g ->
                                GradeBadge(
                                    mark = g.value,
                                    weight = g.weight,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        } else {
                            IconButton(onClick = onDismissAll) {
                                Icon(
                                    Icons.Rounded.DoneAll,
                                    contentDescription = "Прочитать все",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Expandable list of new grades
                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        newGrades.forEach { grade ->
                            key(grade.id) {
                                NewGradeDismissibleItem(
                                    grade = grade,
                                    onDismiss = { onDismissGrade(grade) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGradeDismissibleItem(
    grade: Grade,
    onDismiss: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            if (dismissState.dismissDirection != null) {
                val isStart = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 16.dp),
                    contentAlignment = if (isStart) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Done,
                            contentDescription = "Прочитано",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Прочитано",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val gradeWeightStr = if (grade.weight % 1.0 == 0.0) {
                    grade.weight.toInt().toString()
                } else {
                    String.format(Locale.US, "%.1f", grade.weight)
                }
                val subtitleText = buildString {
                    append("Вес: $gradeWeightStr")
                    if (grade.typeName.isNotBlank() && grade.typeName.length <= 30 && !grade.typeName.contains(".")) {
                        append(" • ")
                        append(grade.typeName)
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = grade.subjectName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildGradeDatesText(grade.lessonDate, grade.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                GradeBadge(mark = grade.value, weight = grade.weight)
            }
        }
    }
}

fun buildGradeDatesText(lessonDate: String, markDate: String): String {
    val lessonDay = formatGradeDisplayDate(lessonDate)
    val markRelative = formatRelativeGradeDate(markDate)

    return if (lessonDay.isNotBlank() && markDate.isNotBlank()) {
        val lessonIsoDay = lessonDate.substringBefore("T")
        val markIsoDay = markDate.substringBefore("T")
        if (lessonIsoDay == markIsoDay) {
            "На урок: $lessonDay ($markRelative)"
        } else {
            "На урок: $lessonDay • Поставлена: $markRelative"
        }
    } else if (lessonDay.isNotBlank()) {
        "На урок: $lessonDay"
    } else {
        "Поставлена: $markRelative"
    }
}

fun formatMarkPostingInfo(lessonDate: String, markDate: String): String? {
    if (markDate.isBlank()) return null
    val lessonDay = lessonDate.substringBefore("T")
    val markDay = markDate.substringBefore("T")
    val relative = formatRelativeGradeDate(markDate)
    return if (lessonDay.isNotBlank() && lessonDay == markDay) {
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = parser.parse(markDate)
            if (date != null) {
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                if (timeStr != "00:00") "Поставлена в $timeStr" else null
            } else null
        } catch (e: Exception) {
            null
        }
    } else {
        "Поставлена: $relative"
    }
}

fun formatRelativeGradeDate(isoDate: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = parser.parse(isoDate) ?: return isoDate.substringBefore("T")

        val now = Calendar.getInstance()
        val gradeCal = Calendar.getInstance().apply { time = date }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeStr = timeFormat.format(date)

        val isToday = now.get(Calendar.YEAR) == gradeCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == gradeCal.get(Calendar.DAY_OF_YEAR)

        now.add(Calendar.DAY_OF_YEAR, -1)
        val isYesterday = now.get(Calendar.YEAR) == gradeCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == gradeCal.get(Calendar.DAY_OF_YEAR)

        when {
            isToday -> "Сегодня в $timeStr"
            isYesterday -> "Вчера в $timeStr"
            else -> {
                val fullFormat = SimpleDateFormat("d MMM в HH:mm", Locale("ru"))
                fullFormat.format(date)
            }
        }
    } catch (e: Exception) {
        isoDate.substringBefore("T")
    }
}

fun formatGradeDisplayDate(isoDate: String): String {
    return try {
        val parser = if (isoDate.contains("T")) {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        }
        val date = parser.parse(isoDate) ?: return isoDate.substringBefore("T")
        val formatter = SimpleDateFormat("d MMMM", Locale("ru"))
        formatter.format(date)
    } catch (e: Exception) {
        isoDate.substringBefore("T")
    }
}

@Composable
fun SubjectGradeCard(
    item: SubjectAverage,
    grades: List<Grade>,
    simulation: SubjectSimulation? = null,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpenCalculator: () -> Unit,
    onResetSimulation: () -> Unit = {}
) {
    val hasSimulation = simulation?.isActive == true
    val activeRealGrades = if (hasSimulation) {
        grades.filter { it.id !in (simulation?.excludedGradeKeys ?: emptySet()) }
    } else {
        grades
    }
    val simulatedGrades = simulation?.simulatedGrades ?: emptyList()

    val totalRealScore = activeRealGrades.sumOf { it.numericValue * it.weight }
    val totalRealWeight = activeRealGrades.sumOf { it.weight }

    val totalSimScore = simulatedGrades.sumOf { it.numericValue * it.weight }
    val totalSimWeight = simulatedGrades.sumOf { it.weight }

    val displayTotalWeight = if (hasSimulation) (totalRealWeight + totalSimWeight) else item.totalWeight
    val displayAverage = if (hasSimulation) {
        if (displayTotalWeight > 0) (totalRealScore + totalSimScore) / displayTotalWeight else 0.0
    } else {
        item.averageScore
    }
    val delta = displayAverage - item.averageScore

    val weightStr = if (displayTotalWeight % 1.0 == 0.0) {
        displayTotalWeight.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", displayTotalWeight)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (hasSimulation) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (hasSimulation) {
                            "Оценок: ${activeRealGrades.size + simulatedGrades.size} • Вес: $weightStr (прогноз)"
                        } else {
                            "Оценок: ${item.gradesCount} • Вес: $weightStr"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasSimulation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        fontWeight = if (hasSimulation) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenCalculator,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Calculate,
                            contentDescription = "Калькулятор балла",
                            tint = if (hasSimulation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        if (hasSimulation) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.2f", item.averageScore),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    textDecoration = TextDecoration.LineThrough
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "→",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format(Locale.US, "%.2f", displayAverage),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val deltaStr = if (delta >= 0) "+${String.format(Locale.US, "%.2f", delta)}" else String.format(Locale.US, "%.2f", delta)
                                val (deltaBg, deltaTextColor) = when {
                                    delta > 0.005 -> Color(0xFF2E7D32).copy(alpha = 0.15f) to Color(0xFF2E7D32)
                                    delta < -0.005 -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.outline
                                }
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = deltaBg
                                ) {
                                    Text(
                                        text = deltaStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = deltaTextColor,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = String.format(Locale.US, "%.2f", item.averageScore),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Recent marks when collapsed
            if (!isExpanded && (grades.isNotEmpty() || simulatedGrades.isNotEmpty())) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Show simulated marks first
                    simulatedGrades.take(2).forEach { sim ->
                        GradeBadge(mark = sim.displayValue, weight = sim.weight)
                    }
                    val remainingReal = activeRealGrades.take(5 - simulatedGrades.take(2).size)
                    remainingReal.forEach { g ->
                        GradeBadge(mark = g.value, weight = g.weight)
                    }
                    val totalDisplayCount = activeRealGrades.size + simulatedGrades.size
                    val shownCount = simulatedGrades.take(2).size + remainingReal.size
                    if (totalDisplayCount > shownCount) {
                        Text(
                            text = "+${totalDisplayCount - shownCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Simulation Banner if active
                    if (hasSimulation) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.Calculate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Прогноз: добавлены или скрыты оценки",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Text(
                                    text = "Сбросить",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable { onResetSimulation() }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }

                    // Simulated grades in expanded view
                    if (simulatedGrades.isNotEmpty()) {
                        simulatedGrades.forEach { sim ->
                            val simWStr = if (sim.weight % 1.0 == 0.0) sim.weight.toInt().toString() else "${sim.weight}"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Вес: $simWStr • Смоделировано",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "+ Добавлена в калькуляторе",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                GradeBadge(mark = sim.displayValue, weight = sim.weight)
                            }
                        }
                    }

                    if (grades.isEmpty() && simulatedGrades.isEmpty()) {
                        Text(
                            text = "Детали оценок отсутствуют",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        grades.forEach { g ->
                            val isExcluded = simulation?.excludedGradeKeys?.contains(g.id) == true
                            val gradeWeightStr = if (g.weight % 1.0 == 0.0) {
                                g.weight.toInt().toString()
                            } else {
                                String.format(Locale.US, "%.1f", g.weight)
                            }
                            val lessonDay = formatGradeDisplayDate(g.lessonDate)
                            val primaryDateStr = if (lessonDay.isNotBlank()) "Урок: $lessonDay" else formatGradeDisplayDate(g.date)
                            val postingInfo = formatMarkPostingInfo(g.lessonDate, g.date)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Вес: $gradeWeightStr • $primaryDateStr",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isExcluded) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                        textDecoration = if (isExcluded) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                    if (isExcluded) {
                                        Text(
                                            text = "Исключена в калькуляторе",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    } else {
                                        val subtitleParts = listOfNotNull(
                                            g.typeName.takeIf { it.isNotBlank() && it.length <= 30 && !it.contains(".") },
                                            postingInfo
                                        ).joinToString(" • ")

                                        if (subtitleParts.isNotBlank()) {
                                            Text(
                                                text = subtitleParts,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                                GradeBadge(
                                    mark = g.value,
                                    weight = g.weight,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = onOpenCalculator,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Rounded.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (hasSimulation) "Изменить прогноз в калькуляторе" else "Калькулятор балла")
                    }
                }
            }
        }
    }
}
