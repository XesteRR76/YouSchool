package org.youschool.app.ui.screens.homework

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.youschool.app.domain.model.HomeworkResultFile
import org.youschool.app.domain.model.Lesson
import org.youschool.app.domain.repository.SchoolRepository
import org.youschool.app.ui.components.CopyHomeworkButton
import org.youschool.app.ui.components.FileOpener
import org.youschool.app.ui.util.rememberHaptic
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeworkSubmissionBottomSheet(
    lesson: Lesson,
    repository: SchoolRepository,
    onDismiss: () -> Unit,
    onSubmit: (isDone: Boolean, comment: String?) -> Unit,
    onShowImage: (File) -> Unit,
    onShowPdf: (File) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = rememberHaptic()

    val partId = lesson.homeworkPartId

    var resultFiles by remember { mutableStateOf<List<HomeworkResultFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isUploading by remember { mutableStateOf(false) }
    var downloadingFileId by remember { mutableStateOf<Long?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val pendingUploads by (if (partId != null) repository.getPendingHomeworkUploads(partId) else flowOf(emptyList())).collectAsState(initial = emptyList())

    fun refreshFiles() {
        if (partId == null) {
            isLoading = false
            return
        }
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            val result = repository.getHomeworkResultFiles(partId)
            isLoading = false
            if (result.isSuccess) {
                resultFiles = result.getOrNull() ?: emptyList()
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Не удалось загрузить список файлов"
            }
        }
    }

    LaunchedEffect(partId) {
        refreshFiles()
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && partId != null) {
            coroutineScope.launch {
                isUploading = true
                try {
                    val tempFile = withContext(Dispatchers.IO) {
                        val fileName = getFileName(context, uri)
                        val cacheFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}_$fileName")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            cacheFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        cacheFile
                    }

                    val uploadResult = repository.uploadHomeworkResult(partId, tempFile)
                    isUploading = false
                    if (uploadResult.isSuccess) {
                        Toast.makeText(context, "Файл успешно прикреплен к заданию!", Toast.LENGTH_SHORT).show()
                        refreshFiles()
                    } else {
                        val enqueueRes = repository.enqueueHomeworkResultUpload(partId, tempFile)
                        if (enqueueRes.isSuccess) {
                            Toast.makeText(context, "Файл сохранен! Он отправится учителю при появлении интернета", Toast.LENGTH_LONG).show()
                        } else {
                            val err = uploadResult.exceptionOrNull()?.message ?: "Ошибка прикрепления файла"
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    isUploading = false
                    Toast.makeText(context, "Ошибка чтения файла: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            BottomSheetDefaults.DragHandle()
        }
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(scrollState)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Отправка задания",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${lesson.subjectName} • ${SimpleDateFormat("d MMMM", Locale("ru")).format(Date(lesson.dateMillis))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Закрыть")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Homework Task Text
            if (lesson.homeworkText.isNotBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.Assignment,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Задание учителя:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            CopyHomeworkButton(lesson.homeworkText)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = lesson.homeworkText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Attached Solutions Section Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Прикрепленные решения (${resultFiles.size + pendingUploads.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (partId != null && !isUploading) {
                    FilledTonalButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Прикрепить", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isUploading) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Отправка файла учителю на сервер...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            } else if (resultFiles.isEmpty() && pendingUploads.isEmpty() && !isUploading) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Rounded.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Вы пока не прикрепили решения к этому заданию.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Нажмите «Прикрепить», чтобы выбрать фото или файл с тетрадью.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pendingUploads.forEach { pending ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.CloudOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = pending.fileName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Ожидает отправки (офлайн)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.removePendingHomeworkUpload(pending.id)
                                            Toast.makeText(context, "Файл удален из очереди", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = "Удалить",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    resultFiles.forEach { file ->
                        ResultFileRow(
                            file = file,
                            isDownloading = downloadingFileId == file.fileId,
                            onClick = {
                                coroutineScope.launch {
                                    downloadingFileId = file.fileId
                                    val destFile = File(context.cacheDir, file.fileName)
                                    val dl = repository.downloadHomeworkFile(file.fileId, file.fileName, null)
                                    downloadingFileId = null
                                    if (dl.isSuccess) {
                                        val f = dl.getOrNull()
                                        if (f != null) {
                                            FileOpener.open(
                                                context = context,
                                                file = f,
                                                onShowImage = onShowImage,
                                                onShowPdf = onShowPdf
                                            )
                                        }
                                    } else {
                                        Toast.makeText(context, "Не удалось открыть файл", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onDelete = {
                                if (partId != null) {
                                    coroutineScope.launch {
                                        val delResult = repository.deleteHomeworkResult(partId, file.fileId)
                                        if (delResult.isSuccess) {
                                            resultFiles = resultFiles.filter { it.fileId != file.fileId }
                                            Toast.makeText(context, "Файл удален", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Ошибка удаления файла", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            var pupilComment by remember { mutableStateOf("") }

            if (!lesson.isHomeworkDone) {
                OutlinedTextField(
                    value = pupilComment,
                    onValueChange = { pupilComment = it },
                    label = { Text("Комментарий к решению (необязательно)") },
                    placeholder = { Text("Например: номер 4 решил другим способом...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        haptic.confirm()
                        onSubmit(true, pupilComment.ifBlank { null })
                    },
                    enabled = !isUploading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (resultFiles.isNotEmpty()) "Сдать работу (файлов: ${resultFiles.size})" else "Сдать работу на проверку",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Работа сдана на проверку",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Учитель видит решение и файлы в электронном журнале",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        haptic.click()
                        onSubmit(false, null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Отозвать работу / Внести изменения",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 2,
                        softWrap = true,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ResultFileRow(
    file: HomeworkResultFile,
    isDownloading: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isImage = file.fileName.endsWith(".jpg", true) ||
                    file.fileName.endsWith(".jpeg", true) ||
                    file.fileName.endsWith(".png", true)
            val icon = if (isImage) Icons.Rounded.Image else Icons.Rounded.Description

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sizeText = formatFileSize(file.fileSize)
                Text(
                    text = sizeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long?): String {
    if (bytes == null || bytes <= 0) return "Файл решения"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.1f МБ", mb)
    } else {
        String.format(Locale.US, "%.0f КБ", kb)
    }
}

private fun getFileName(context: Context, uri: Uri): String {
    var name = "homework_solution_${System.currentTimeMillis()}"
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                name = it.getString(nameIndex) ?: name
            }
        }
    }
    return name
}
