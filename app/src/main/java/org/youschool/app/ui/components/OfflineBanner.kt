package org.youschool.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SyncProblem
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.youschool.app.core.sync.SyncStatus

@Composable
fun OfflineBanner(
    syncStatus: SyncStatus,
    onRetrySync: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVisible = syncStatus.isOfflineMode || syncStatus.isSyncing

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        val (containerColor, contentColor) = when {
            syncStatus.isSyncing -> {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f) to MaterialTheme.colorScheme.onPrimaryContainer
            }
            !syncStatus.isOnline -> {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f) to MaterialTheme.colorScheme.onSurfaceVariant
            }
            syncStatus.pendingCount > 0 -> {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f) to MaterialTheme.colorScheme.onSecondaryContainer
            }
            else -> {
                MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
            }
        }

        val bannerText = when {
            syncStatus.isSyncing -> {
                "Синхронизация с сервером..."
            }
            !syncStatus.isOnline && syncStatus.pendingCount > 0 -> {
                val pluralWord = when {
                    syncStatus.pendingCount % 10 == 1 && syncStatus.pendingCount % 100 != 11 -> "действие"
                    syncStatus.pendingCount % 10 in 2..4 && (syncStatus.pendingCount % 100 !in 12..14) -> "действия"
                    else -> "действий"
                }
                "Офлайн-режим • ${syncStatus.pendingCount} $pluralWord в очереди"
            }
            !syncStatus.isOnline -> {
                "Офлайн-режим • Изменения на устройстве"
            }
            syncStatus.pendingCount > 0 -> {
                "Ожидает отправки: ${syncStatus.pendingCount} • Нажмите для повтора"
            }
            else -> ""
        }

        Surface(
            shape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp),
            color = containerColor,
            border = BorderStroke(1.dp, contentColor.copy(alpha = 0.15f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .then(
                    if (syncStatus.isOnline && !syncStatus.isSyncing && syncStatus.pendingCount > 0) {
                        Modifier.clickable { onRetrySync() }
                    } else Modifier
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (syncStatus.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = contentColor
                    )
                } else if (!syncStatus.isOnline) {
                    Icon(
                        Icons.Rounded.CloudOff,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(15.dp)
                    )
                } else {
                    Icon(
                        Icons.Rounded.SyncProblem,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = bannerText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (syncStatus.isOnline && !syncStatus.isSyncing && syncStatus.pendingCount > 0) {
                    IconButton(
                        onClick = onRetrySync,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "Повторить синхронизацию",
                            tint = contentColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
