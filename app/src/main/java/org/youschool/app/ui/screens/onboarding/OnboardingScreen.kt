package org.youschool.app.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.youschool.app.ui.screens.schedule.GradeBadge
import org.youschool.app.ui.theme.*
import org.youschool.app.ui.util.rememberHaptic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    themeManager: ThemeManager,
    onFinish: () -> Unit
) {
    val themeSettings by themeManager.themeSettings.collectAsState()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val haptic = rememberHaptic()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Персонализация",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    TextButton(
                        onClick = {
                            haptic.confirm()
                            themeManager.setOnboardingCompleted(true)
                            onFinish()
                        }
                    ) {
                        Text("Пропустить", style = MaterialTheme.typography.labelLarge)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pagerState.currentPage > 0) {
                        OutlinedButton(
                            onClick = {
                                haptic.tick()
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Назад")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Pager dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) { page ->
                            val isSelected = pagerState.currentPage == page
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 10.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (pagerState.currentPage < 2) {
                                haptic.click()
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            } else {
                                haptic.confirm()
                                themeManager.setOnboardingCompleted(true)
                                onFinish()
                            }
                        },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(if (pagerState.currentPage == 2) "Начать" else "Далее")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            if (pagerState.currentPage == 2) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (page) {
                0 -> StepThemeAndPalette(themeManager, themeSettings)
                1 -> StepGradeColorMode(themeManager, themeSettings)
                2 -> StepNavigationAndHaptic(themeManager, themeSettings)
            }
        }
    }
}

@Composable
private fun StepThemeAndPalette(
    themeManager: ThemeManager,
    settings: ThemeSettings
) {
    val scrollState = rememberScrollState()
    val haptic = rememberHaptic()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Palette,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Тема и палитра",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Выберите желаемый режим оформления и цвет акцентов интерфейса",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Visual Preview Card for Theme & Palette
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Пример интерфейса",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${settings.themeMode.title} • ${settings.colorPalette.title}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Алгебра",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "08:30 – 09:15 • Кабинет 204",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Button(
                            onClick = {},
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Решение", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Режим оформления",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppThemeMode.values().forEach { mode ->
                val isSelected = settings.themeMode == mode
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.tick()
                            themeManager.setThemeMode(mode)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = mode.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                haptic.tick()
                                themeManager.setThemeMode(mode)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Акцентная палитра",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppColorPalette.values().forEach { palette ->
                val isSelected = settings.colorPalette == palette
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    label = "border_color"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable {
                            haptic.tick()
                            themeManager.setColorPalette(palette)
                        }
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .border(2.dp, borderColor, CircleShape)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(palette.primaryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = palette.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun StepGradeColorMode(
    themeManager: ThemeManager,
    settings: ThemeSettings
) {
    val scrollState = rememberScrollState()
    val haptic = rememberHaptic()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Grade,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Окрашивание оценок",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Настройте, как оценки отображаются в дневнике и расписании",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Live Preview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Живое превью в реальном времени:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GradeBadge(mark = "5", weight = 1.0)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Вес 1.0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GradeBadge(mark = "4", weight = 1.5)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Вес 1.5", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GradeBadge(mark = "5", weight = 3.0)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Вес 3.0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GradeBadge(mark = "3", weight = 0.5)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Вес 0.5", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }

                if (settings.gradeColorMode == GradeColorMode.BY_WEIGHT_GRADIENT) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        settings.weightGradientSettings.startColor,
                                        settings.weightGradientSettings.endColor
                                    )
                                )
                            )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Мин (0.2)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text("1.0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text("Макс (3.0)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3 modes selector
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GradeColorMode.values().forEach { mode ->
                val isSelected = settings.gradeColorMode == mode
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.tick()
                            themeManager.setGradeColorMode(mode)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                haptic.tick()
                                themeManager.setGradeColorMode(mode)
                            }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = mode.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepNavigationAndHaptic(
    themeManager: ThemeManager,
    settings: ThemeSettings
) {
    val scrollState = rememberScrollState()
    val haptic = rememberHaptic()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Навигация и отклик",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Выберите стиль нижней панели и включите тактильную отдачу",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Visual Preview Card for Navigation Bar Style
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Превью панели на экране:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = settings.navBarStyle.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Miniature screen mockup
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    // Mock content lines in screen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(width = 70.dp, height = 10.dp)
                        ) {}
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                        ) {}
                    }

                    // Navigation bar preview
                    if (settings.navBarStyle == NavigationBarStyle.STANDARD) {
                        Surface(
                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .height(26.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(4) {
                                    Box(
                                        modifier = Modifier
                                            .size(if (it == 0) 14.dp else 8.dp, 8.dp)
                                            .clip(CircleShape)
                                            .background(if (it == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                                    )
                                }
                            }
                        }
                    } else {
                        val previewOffset = (settings.navBarBottomOffsetDp * 0.35f).dp
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 20.dp)
                                .padding(bottom = previewOffset)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(26.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(4) {
                                        Box(
                                            modifier = Modifier
                                                .size(if (it == 0) 18.dp else 8.dp, 8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (it == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Стиль панели навигации",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NavigationBarStyle.values().forEach { navStyle ->
                val isSelected = settings.navBarStyle == navStyle
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.tick()
                            themeManager.setNavigationBarStyle(navStyle)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                haptic.tick()
                                themeManager.setNavigationBarStyle(navStyle)
                            }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = navStyle.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = navStyle.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }

        if (settings.navBarStyle == NavigationBarStyle.FLOATING_PILL) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Отступ панели снизу",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Поднимите панель выше, если срабатывают жесты переключения окон",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(start = 10.dp)
                        ) {
                            Text(
                                text = "${settings.navBarBottomOffsetDp} dp",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Slider(
                        value = settings.navBarBottomOffsetDp.toFloat(),
                        onValueChange = { themeManager.setNavBarBottomOffset(it.toInt()) },
                        valueRange = 4f..48f,
                        steps = 10,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Haptic Feedback switch
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Vibration,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Виброотклик",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Тактильная отдача при переключении табов, калькуляторе и ДЗ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Switch(
                    checked = settings.isHapticFeedbackEnabled,
                    onCheckedChange = {
                        themeManager.setHapticFeedbackEnabled(it)
                        if (it) haptic.click()
                    }
                )
            }
        }
    }
}
