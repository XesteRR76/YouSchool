package org.youschool.app.ui.screens.homework

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.youschool.app.domain.model.Lesson
import org.youschool.app.domain.repository.SchoolRepository
import java.text.SimpleDateFormat
import java.util.*

enum class HomeworkFilter(val label: String) {
    ALL("Все"),
    PENDING("Невыполненные"),
    COMPLETED("Выполненные")
}

class HomeworkViewModel(private val repository: SchoolRepository) : ViewModel() {

    private val _filter = MutableStateFlow(HomeworkFilter.ALL)
    val filter: StateFlow<HomeworkFilter> = _filter.asStateFlow()

    val homeworks: StateFlow<List<Lesson>> = repository.getAllHomeworks()
        .combine(_filter) { list, filter ->
            when (filter) {
                HomeworkFilter.ALL -> list
                HomeworkFilter.PENDING -> list.filter { !it.isHomeworkDone }
                HomeworkFilter.COMPLETED -> list.filter { it.isHomeworkDone }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: HomeworkFilter) {
        _filter.value = filter
    }

    fun toggleHomework(lessonId: Long, currentDone: Boolean) {
        viewModelScope.launch {
            repository.toggleHomeworkStatus(lessonId, !currentDone)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeworkScreen(
    viewModel: HomeworkViewModel,
    modifier: Modifier = Modifier
) {
    val currentFilter by viewModel.filter.collectAsState()
    val homeworkList by viewModel.homeworks.collectAsState()

    val dateFormat = remember { SimpleDateFormat("d MMMM (EE)", Locale("ru")) }

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
                        onClick = { viewModel.setFilter(filter) },
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
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(homeworkList, key = { it.id }) { item ->
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
                                        viewModel.toggleHomework(item.id, item.isHomeworkDone)
                                    }
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = item.subjectName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = dateFormat.format(Date(item.dateMillis)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = item.homeworkText.ifBlank { "Прикреплены материалы" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (item.isHomeworkDone)
                                            MaterialTheme.colorScheme.outline
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )

                                    if (item.homeworkFiles.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Rounded.AttachFile,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${item.homeworkFiles.size} файл(ов)",
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
        }
    }
}
