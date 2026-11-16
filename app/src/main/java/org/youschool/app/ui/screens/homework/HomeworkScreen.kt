package org.youschool.app.ui.screens.homework

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.youschool.app.domain.model.HomeworkAttachment
import org.youschool.app.domain.model.Lesson
import org.youschool.app.domain.repository.SchoolRepository
import org.youschool.app.ui.components.AttachmentChips
import org.youschool.app.ui.components.ExpandableHomeworkText
import org.youschool.app.ui.components.FileOpener
import org.youschool.app.ui.components.ImageViewerDialog
import org.youschool.app.ui.components.PdfViewerDialog
import org.youschool.app.ui.theme.LocalBottomBarPadding
import org.youschool.app.ui.util.rememberHaptic
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class HomeworkFilter(val label: String) {
    ALL("Все"),
    PENDING("Только невыполненные")
}

class HomeworkViewModel(val repository: SchoolRepository) : ViewModel() {

    private val _filter = MutableStateFlow(HomeworkFilter.ALL)
    val filter: StateFlow<HomeworkFilter> = _filter.asStateFlow()

    private val _downloadingFileId = MutableStateFlow<Long?>(null)
    val downloadingFileId: StateFlow<Long?> = _downloadingFileId.asStateFlow()

    var activeImageFile by mutableStateOf<File?>(null)
    var activePdfFile by mutableStateOf<File?>(null)

    val homeworks: StateFlow<List<Lesson>> = repository.getUpcomingHomeworks()
        .combine(_filter) { list, filter ->
            when (filter) {
                HomeworkFilter.ALL -> list
                HomeworkFilter.PENDING -> list.filter { !it.isHomeworkDone }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: HomeworkFilter) {
        _filter.value = filter
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
fun HomeworkScreen(
    viewModel: HomeworkViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = rememberHaptic()
    val currentFilter by viewModel.filter.collectAsState()
    val homeworkList by viewModel.homeworks.collectAsState()
    val downloadingFileId by viewModel.downloadingFileId.collectAsState()

    var expandedHomeworkIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
    var selectedLessonForSubmission by remember { mutableStateOf<Lesson?>(null) }

    val groupedHomework = remember(homeworkList) {
        val cal = Calendar.getInstance()
        homeworkList.groupBy { lesson ->
            cal.timeInMillis = lesson.dateMillis
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.toSortedMap()
    }

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

    selectedLessonForSubmission?.let { sel ->
        val currentLesson = homeworkList.find { it.id == sel.id } ?: sel
        HomeworkSubmissionBottomSheet(
            lesson = currentLesson,
            repository = viewModel.repository,
            onDismiss = { selectedLessonForSubmission = null },
            onSubmit = { isDone, comment ->
                viewModel.toggleHomework(currentLesson.id, currentLesson.isHomeworkDone, comment)
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
                        text = "Домашние задания",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(HomeworkFilter.values()) { filter ->
                    FilterChip(
                        selected = filter == currentFilter,
                        onClick = {
                            haptic.tick()
                            viewModel.setFilter(filter)
                        },
                        label = { Text(filter.label) },
                        leadingIcon = if (filter == currentFilter) {
                            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            if (homeworkList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.AssignmentTurnedIn,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (currentFilter == HomeworkFilter.PENDING) "Все задания выполнены!" else "Заданий не найдено",
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
                        top = 8.dp,
                        end = 16.dp,
                        bottom = 16.dp + LocalBottomBarPadding.current
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedHomework.forEach { (dateMillis, itemsForDate) ->
                        val pendingCount = itemsForDate.count { !it.isHomeworkDone }

                        item(key = "header_$dateMillis") {
                            HomeworkDateHeader(
                                dateMillis = dateMillis,
                                totalCount = itemsForDate.size,
                                pendingCount = pendingCount
                            )
                        }

                        items(itemsForDate, key = { it.id }) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (item.isHomeworkDone)
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = item.isHomeworkDone,
                                        onCheckedChange = {
                                            haptic.tick()
                                            viewModel.toggleHomework(item.id, item.isHomeworkDone)
                                        }
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.subjectName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        ExpandableHomeworkText(
                                            text = item.homeworkText.ifBlank { "Прикреплены материалы" },
                                            isDone = item.isHomeworkDone,
                                            isExpanded = expandedHomeworkIds.contains(item.id),
                                            onToggleExpand = {
                                                expandedHomeworkIds = if (expandedHomeworkIds.contains(item.id)) {
                                                    expandedHomeworkIds - item.id
                                                } else {
                                                    expandedHomeworkIds + item.id
                                                }
                                            }
                                        )

                                        if (item.homeworkFiles.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            AttachmentChips(
                                                files = item.homeworkFiles,
                                                downloadingFileId = downloadingFileId,
                                                isExpanded = expandedHomeworkIds.contains(item.id),
                                                onFileClick = { attachment ->
                                                    viewModel.openAttachment(context, attachment)
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    haptic.click()
                                                    selectedLessonForSubmission = item
                                                },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.CloudUpload,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Решение",
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item(key = "spacer_$dateMillis") {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeworkDateHeader(
    dateMillis: Long,
    totalCount: Int,
    pendingCount: Int
) {
    val date = remember(dateMillis) { Date(dateMillis) }
    val dayFormat = remember { SimpleDateFormat("EEEE, d MMMM", Locale("ru")) }
    val titleText = remember(date) {
        dayFormat.format(date).replaceFirstChar { it.uppercase() }
    }

    val calNow = Calendar.getInstance()
    val todayMillis = remember {
        calNow.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val tomorrowMillis = todayMillis + 86400000L

    val isToday = dateMillis == todayMillis
    val isTomorrow = dateMillis == tomorrowMillis

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Rounded.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (isToday) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "Сегодня",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else if (isTomorrow) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "Завтра",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            val countText = if (pendingCount > 0 && pendingCount != totalCount) {
                "$pendingCount / $totalCount"
            } else {
                "$totalCount"
            }
            Text(
                text = countText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
