package org.youschool.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ExpandableHomeworkText(
    text: String,
    isDone: Boolean,
    modifier: Modifier = Modifier,
    maxCollapsedLines: Int = 3,
    isExpanded: Boolean? = null,
    onToggleExpand: (() -> Unit)? = null
) {
    var localIsExpanded by remember { mutableStateOf(false) }
    val effectiveExpanded = isExpanded ?: localIsExpanded
    var hasOverflow by remember { mutableStateOf(false) }

    val isLongText = remember(text) {
        text.length > 90 || text.count { it == '\n' } >= 2
    }

    Column(modifier = modifier.animateContentSize()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDone) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
            maxLines = if (effectiveExpanded) Int.MAX_VALUE else maxCollapsedLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { textLayoutResult ->
                if (!effectiveExpanded) {
                    hasOverflow = textLayoutResult.hasVisualOverflow || isLongText
                }
            }
        )

        if (hasOverflow || isLongText || effectiveExpanded) {
            val interactionSource = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        if (onToggleExpand != null) {
                            onToggleExpand()
                        } else {
                            localIsExpanded = !localIsExpanded
                        }
                    }
                    .padding(top = 4.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (effectiveExpanded) "Свернуть" else "Развернуть",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (effectiveExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (effectiveExpanded) "Свернуть" else "Развернуть",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
