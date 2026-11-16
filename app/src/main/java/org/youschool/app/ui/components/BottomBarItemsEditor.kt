package org.youschool.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.youschool.app.ui.navigation.Screen
import org.youschool.app.ui.theme.ThemeManager
import org.youschool.app.ui.theme.ThemeSettings
import org.youschool.app.ui.util.rememberHaptic
import kotlin.math.roundToInt

@Composable
fun BottomBarItemsEditor(
    themeManager: ThemeManager,
    settings: ThemeSettings,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHaptic()
    val coroutineScope = rememberCoroutineScope()
    val routes = settings.bottomBarRoutes
    var activeList by remember { mutableStateOf(routes) }
    var draggingRoute by remember { mutableStateOf<String?>(null) }
    var dragAccumulatedY by remember { mutableFloatStateOf(0f) }
    var snapAnimationJob by remember { mutableStateOf<Job?>(null) }
    val density = LocalDensity.current
    var itemStepPx by remember { mutableFloatStateOf(0f) }
    val fallbackStepPx = with(density) { 66.dp.toPx() }

    // Sync activeList with external updates when not dragging or animating
    LaunchedEffect(routes) {
        if (draggingRoute == null && snapAnimationJob?.isActive != true) {
            activeList = routes
        }
    }

    Column(modifier = modifier) {
        Text(
            text = "Пункты нижней панели",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "От 2 до 5 пунктов. Профиль всегда доступен.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.DragIndicator,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Зажмите значок справа для перемещения плашки",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))

        // Draggable active routes
        activeList.forEach { route ->
            key(route) {
                val screen = Screen.allMainScreens.find { it.route == route }
                if (screen != null) {
                    val isBeingDragged = draggingRoute == route
                    val cardOffset = if (isBeingDragged) dragAccumulatedY.roundToInt() else 0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .onGloballyPositioned { coordinates ->
                                if (itemStepPx == 0f) {
                                    itemStepPx = coordinates.size.height.toFloat() + with(density) { 6.dp.toPx() }
                                }
                            }
                            .zIndex(if (isBeingDragged) 10f else 1f)
                            .offset { IntOffset(0, cardOffset) }
                            .scale(if (isBeingDragged) 1.03f else 1f)
                            .shadow(if (isBeingDragged) 8.dp else 1.dp, shape = RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isBeingDragged)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                modifier = Modifier.semantics { contentDescription = screen.title },
                                checked = true,
                                enabled = screen != Screen.Profile && routes.size > 2,
                                onCheckedChange = { checked ->
                                    if (!checked) {
                                        snapAnimationJob?.cancel()
                                        draggingRoute = null
                                        dragAccumulatedY = 0f
                                        val updated = routes - screen.route
                                        activeList = updated
                                        themeManager.setBottomBarRoutes(updated)
                                    }
                                }
                            )
                            screen.icon?.let { icon ->
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isBeingDragged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .pointerInput(route) {
                                        try {
                                            detectDragGestures(
                                                onDragStart = {
                                                    snapAnimationJob?.cancel()
                                                    draggingRoute = route
                                                    dragAccumulatedY = 0f
                                                    haptic.tick()
                                                },
                                                onDragEnd = {
                                                    snapAnimationJob?.cancel()
                                                    snapAnimationJob = coroutineScope.launch {
                                                        try {
                                                            animate(
                                                                initialValue = dragAccumulatedY,
                                                                targetValue = 0f,
                                                                animationSpec = spring(
                                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                                    stiffness = Spring.StiffnessMediumLow
                                                                )
                                                            ) { value, _ ->
                                                                dragAccumulatedY = value
                                                            }
                                                        } finally {
                                                            draggingRoute = null
                                                            dragAccumulatedY = 0f
                                                            themeManager.setBottomBarRoutes(activeList)
                                                            haptic.click()
                                                        }
                                                    }
                                                },
                                                onDragCancel = {
                                                    snapAnimationJob?.cancel()
                                                    draggingRoute = null
                                                    dragAccumulatedY = 0f
                                                    themeManager.setBottomBarRoutes(activeList)
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    val step = if (itemStepPx > 0f) itemStepPx else fallbackStepPx
                                                    var curIdx = activeList.indexOf(route)
                                                    if (curIdx != -1) {
                                                        val maxDown = (activeList.lastIndex - curIdx) * step + step * 0.7f
                                                        val maxUp = -curIdx * step - step * 0.7f
                                                        dragAccumulatedY = (dragAccumulatedY + dragAmount.y).coerceIn(maxUp, maxDown)

                                                        // Continuous multi-slot reordering
                                                        while (curIdx != -1 && dragAccumulatedY > step * 0.5f && curIdx < activeList.lastIndex) {
                                                            val updated = activeList.toMutableList()
                                                            java.util.Collections.swap(updated, curIdx, curIdx + 1)
                                                            activeList = updated
                                                            dragAccumulatedY -= step
                                                            curIdx++
                                                            haptic.tick()
                                                        }
                                                        while (curIdx != -1 && dragAccumulatedY < -step * 0.5f && curIdx > 0) {
                                                            val updated = activeList.toMutableList()
                                                            java.util.Collections.swap(updated, curIdx, curIdx - 1)
                                                            activeList = updated
                                                            dragAccumulatedY += step
                                                            curIdx--
                                                            haptic.tick()
                                                        }
                                                    }
                                                }
                                            )
                                        } finally {
                                            if (snapAnimationJob?.isActive != true) {
                                                draggingRoute = null
                                                dragAccumulatedY = 0f
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DragIndicator,
                                    contentDescription = "Перетащить: ${screen.title}",
                                    tint = if (isBeingDragged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }

        // Hidden/available screens to add
        val hiddenScreens = Screen.allMainScreens.filter { it.route !in routes }
        if (hiddenScreens.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Скрытые пункты",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(6.dp))
            hiddenScreens.forEach { screen ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            modifier = Modifier.semantics { contentDescription = screen.title },
                            checked = false,
                            enabled = routes.size < 5,
                            onCheckedChange = { checked ->
                                if (checked && routes.size < 5) {
                                    snapAnimationJob?.cancel()
                                    draggingRoute = null
                                    dragAccumulatedY = 0f
                                    val updated = routes + screen.route
                                    activeList = updated
                                    themeManager.setBottomBarRoutes(updated)
                                }
                            }
                        )
                        screen.icon?.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Text(
                            text = screen.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                if (routes.size < 5) {
                                    snapAnimationJob?.cancel()
                                    draggingRoute = null
                                    dragAccumulatedY = 0f
                                    val updated = routes + screen.route
                                    activeList = updated
                                    themeManager.setBottomBarRoutes(updated)
                                }
                            },
                            enabled = routes.size < 5
                        ) {
                            Text("Добавить", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
