package org.youschool.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.youschool.app.domain.model.HomeworkAttachment

@Composable
fun AttachmentChips(
    files: List<HomeworkAttachment>,
    downloadingFileId: Long?,
    onFileClick: (HomeworkAttachment) -> Unit,
    modifier: Modifier = Modifier
) {
    if (files.isEmpty()) return

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

            val icon = when (ext) {
                "pdf" -> Icons.Rounded.PictureAsPdf
                "jpg", "jpeg", "png", "webp", "bmp", "gif" -> Icons.Rounded.Image
                "doc", "docx" -> Icons.Rounded.Description
                "xls", "xlsx" -> Icons.Rounded.TableChart
                "zip", "rar", "7z" -> Icons.Rounded.FolderZip
                else -> Icons.Rounded.AttachFile
            }

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
}
