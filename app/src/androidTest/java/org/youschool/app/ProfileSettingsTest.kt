package org.youschool.app

import android.graphics.Bitmap
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.youschool.app.domain.model.UserProfile
import org.youschool.app.ui.navigation.BottomBarConfig
import org.youschool.app.ui.screens.profile.ProfileScreen
import org.youschool.app.ui.theme.*
import java.io.File

@RunWith(AndroidJUnit4::class)
class ProfileSettingsTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val repository = object : UnusedRepository() {
        override fun getUserProfile() = UserProfile(1, 1, "Тестовый пользователь", "Школа", "ROLE_STUDENT")
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(context.getExternalFilesDir(null), "profile-review").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun categoriesKeepExistingControlsAndBothBackActions() {
        val manager = ThemeManager(context)
        val original = manager.themeSettings.value
        manager.setBottomBarRoutes(BottomBarConfig.defaults)
        val restore = StateRestorationTester(compose)
        try {
            restore.setContent {
                val settings by manager.themeSettings.collectAsState()
                YouSchoolTheme(settings) { ProfileScreen(repository, manager, onLogout = {}) }
            }
            compose.onNodeWithText("Оформление").assertIsDisplayed()
            screenshot("profile")
            val pages = listOf(
                "Оформление" to "Режим отображения",
                "Навигация и отклик" to "Пункты нижней панели",
                "Уведомления" to "Новые оценки",
                "Оценки" to "Превью в текущем режиме:",
                "О приложении" to "Текущая версия"
            )
            pages.forEachIndexed { index, (title, control) ->
                compose.onNodeWithText(title).performScrollTo().performClick()
                compose.onNodeWithText(control).performScrollTo().assertIsDisplayed()
                screenshot("section-$index")
                compose.onNodeWithContentDescription("Назад").performClick()
                compose.onNodeWithText("Оформление").assertExists()
                compose.onNodeWithText("Режим отображения").assertDoesNotExist()
            }
            compose.onNodeWithText("Уведомления").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Новые оценки").performScrollTo().performClick()
            val expected = manager.themeSettings.value.gradeNotificationsEnabled
            restore.emulateSavedInstanceStateRestore()
            compose.onNodeWithContentDescription("Новые оценки").assertExists()
            assertEquals(expected, manager.themeSettings.value.gradeNotificationsEnabled)
            pressBack()
            compose.onNodeWithText("Оформление").assertExists()
            compose.onNodeWithText("Уведомления").performScrollTo().performClick()
            assertEquals(expected, manager.themeSettings.value.gradeNotificationsEnabled)
            pressBack()
            compose.onNodeWithText("Оформление").assertExists()
        } finally {
            manager.setGradeNotificationsEnabled(original.gradeNotificationsEnabled)
            manager.setBottomBarRoutes(original.bottomBarRoutes)
        }
    }

    @Test fun hiddenAppSectionsStayReachableFromProfile() {
        val manager = ThemeManager(context)
        val original = manager.themeSettings.value
        var route: String? = null
        try {
            manager.setBottomBarRoutes(listOf("profile", "messages"))
            compose.setContent {
                val settings by manager.themeSettings.collectAsState()
                YouSchoolTheme(settings) { ProfileScreen(repository, manager, onLogout = {}, onNavigate = { route = it }) }
            }
            compose.onNodeWithText("Разделы приложения").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Расписание").performScrollTo().performClick()
            assertEquals("schedule", route)
        } finally { manager.setBottomBarRoutes(original.bottomBarRoutes) }
    }
}
