package org.youschool.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.youschool.app.domain.model.HomeworkAttachment

private fun getFileIcon(ext: String): ImageVector {
    return when (ext) {
        "pdf" -> Icons.Rounded.PictureAsPdf
        "jpg", "jpeg", "png", "webp", "bmp", "gif" -> Icons.Rounded.Image
        "doc", "docx" -> Icons.Rounded.Description
        "xls", "xlsx" -> Icons.Rounded.TableChart
        "zip", "rar", "7z" -> Icons.Rounded.FolderZip
        else -> Icons.Rounded.AttachFile
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttachmentChips(
    files: List<HomeworkAttachment>,
    downloadingFileId: Long?,
    onFileClick: (HomeworkAttachment) -> Unit,
    isExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (files.isEmpty()) return

    if (!isExpanded) {
        // Compact single-line horizontal scroll for collapsed state
        Row(
            modifier = modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            files.forEach { attachment ->
                val isDownloading = downloadingFileId == attachment.fileId
                val ext = attachment.fileName.substringAfterLast('.', "").lowercase()
                val icon = getFileIcon(ext)

                AssistChip(
                    onClick = { if (!isDownloading) onFileClick(attachment) },
                    label = {
                        Text(
                            text = attachment.fileName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                )
            }
        }
    } else {
        // Expanded: Panels that stretch across the width or wrap cleanly to the next row!
        FlowRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            files.forEach { attachment ->
                val isDownloading = downloadingFileId == attachment.fileId
                val ext = attachment.fileName.substringAfterLast('.', "").lowercase()
                val icon = getFileIcon(ext)

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .then(
                            if (files.size == 1) Modifier.fillMaxWidth()
                            else Modifier.widthIn(min = 160.dp).weight(1f, fill = false)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !isDownloading) { onFileClick(attachment) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isDownloading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                } else {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = attachment.fileName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (ext.isNotBlank()) {
                                Text(
                                    text = ext.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = "Скачать",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
