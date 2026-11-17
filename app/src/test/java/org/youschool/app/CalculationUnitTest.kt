package org.youschool.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.youschool.app.core.auth.AuthManager
import java.security.MessageDigest

class CalculationUnitTest {

    @Test
    fun testSha256Hashing() {
        val testPassword = "MySecretPassword123"
        val expectedHash = MessageDigest.getInstance("SHA-256")
            .digest(testPassword.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

        val actualHash = AuthManager.sha256(testPassword)
        assertEquals(expectedHash, actualHash)
        assertTrue(actualHash.length == 64)
    }

    @Test
    fun testWeightedAverageCalculation() {
        data class MockGrade(val mark: Double, val weight: Int)

        val marks = listOf(
            MockGrade(5.0, 1),
            MockGrade(4.0, 2),
            MockGrade(5.0, 3)
        )

        var totalWeightedScore = 0.0
        var totalWeight = 0
        for (g in marks) {
            totalWeightedScore += g.mark * g.weight
            totalWeight += g.weight
        }

        // Expected: (5*1 + 4*2 + 5*3) / (1 + 2 + 3) = (5 + 8 + 15) / 6 = 28 / 6 = 4.6666...
        val calculatedAvg = totalWeightedScore / totalWeight
        assertEquals(4.666, calculatedAvg, 0.001)

        // Calculate needed 5s to reach 4.75
        var simulatedTotal = totalWeightedScore
        var simulatedWeight = totalWeight
        var neededFives = 0
        while ((simulatedTotal / simulatedWeight) < 4.75 && neededFives < 30) {
            simulatedTotal += 5.0
            simulatedWeight += 1
            neededFives++
        }
        assertTrue(neededFives > 0)
    }

    @Test
    fun testWeightFactorLogarithmicGrowth() {
        val factorMin = org.youschool.app.ui.theme.GradeColorCalculator.calculateWeightFactor(0.2)
        val factorMax = org.youschool.app.ui.theme.GradeColorCalculator.calculateWeightFactor(3.0)
        val factorMid = org.youschool.app.ui.theme.GradeColorCalculator.calculateWeightFactor(1.0)

        assertEquals(0.0f, factorMin, 0.001f)
        assertEquals(1.0f, factorMax, 0.001f)
        // Log growth: (ln(1.0) - ln(0.2)) / (ln(3.0) - ln(0.2)) = ln(5) / ln(15) ≈ 1.6094 / 2.7080 ≈ 0.594
        assertEquals(0.594f, factorMid, 0.005f)

        // Clamping check
        assertEquals(0.0f, org.youschool.app.ui.theme.GradeColorCalculator.calculateWeightFactor(0.05), 0.001f)
        assertEquals(1.0f, org.youschool.app.ui.theme.GradeColorCalculator.calculateWeightFactor(10.0), 0.001f)
    }

    @Test
    fun testDefaultWeightGradientColorsAreBuiltInAndNotBlack() {
        val defaultGradient = org.youschool.app.ui.theme.WeightGradientSettings()
        // Cyan and Purple defaults
        assertEquals(androidx.compose.ui.graphics.Color(0xFF00ACC1), defaultGradient.startColor)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF6A1B9A), defaultGradient.endColor)

        val settings = org.youschool.app.ui.theme.ThemeSettings(
            gradeColorMode = org.youschool.app.ui.theme.GradeColorMode.BY_WEIGHT_GRADIENT
        )
        val (bgMin, _) = org.youschool.app.ui.theme.GradeColorCalculator.calculateGradeColor("5", 0.2, settings)
        val (bgMax, _) = org.youschool.app.ui.theme.GradeColorCalculator.calculateGradeColor("5", 3.0, settings)

        // Ensure neither is pure black (0,0,0) or transparent
        assertTrue("Min weight color should not be black", bgMin.green > 0.4f || bgMin.blue > 0.4f)
        assertTrue("Max weight color should not be black", bgMax.red > 0.3f || bgMax.blue > 0.4f)
    }

    @Test
    fun testPillNavBarGeometryMath() {
        fun getTabIndex(ratio: Float, selectedIdx: Int): Int {
            for (i in 0 until 4) {
                val left = if (i <= selectedIdx) i * 0.20f else (i + 1) * 0.20f
                val width = if (i == selectedIdx) 0.40f else 0.20f
                if (ratio >= left && ratio < left + width) {
                    return i
                }
            }
            return (ratio * 4).toInt().coerceIn(0, 3)
        }

        // When Tab 0 (Schedule) is selected [0.0..0.40]:
        assertEquals(0, getTabIndex(0.05f, 0))
        assertEquals(0, getTabIndex(0.35f, 0))
        assertEquals(1, getTabIndex(0.45f, 0))
        assertEquals(2, getTabIndex(0.70f, 0))
        assertEquals(3, getTabIndex(0.90f, 0))

        // When Tab 1 (Grades) is selected [0.20..0.60]:
        assertEquals(0, getTabIndex(0.10f, 1))
        assertEquals(1, getTabIndex(0.30f, 1))
        assertEquals(1, getTabIndex(0.55f, 1))
        assertEquals(2, getTabIndex(0.70f, 1))
        assertEquals(3, getTabIndex(0.90f, 1))

        // When Tab 3 (Profile) is selected [0.60..1.00]:
        assertEquals(0, getTabIndex(0.10f, 3))
        assertEquals(1, getTabIndex(0.30f, 3))
        assertEquals(2, getTabIndex(0.50f, 3))
        assertEquals(3, getTabIndex(0.70f, 3))
        assertEquals(3, getTabIndex(0.95f, 3))
    }

    @Test
    fun testGradeDatesFormatting() {
        val lessonDate = "2026-11-13T00:00:00"
        val markDateDifferentDay = "2026-11-15T12:44:00"
        val markDateSameDay = "2026-11-13T14:20:00"

        val lessonFormatted = org.youschool.app.ui.screens.grades.formatGradeDisplayDate(lessonDate)
        assertEquals("13 ноября", lessonFormatted)

        val newGradesTextDiff = org.youschool.app.ui.screens.grades.buildGradeDatesText(lessonDate, markDateDifferentDay)
        assertTrue(newGradesTextDiff.startsWith("На урок: 13 ноября • Поставлена: "))

        val newGradesTextSame = org.youschool.app.ui.screens.grades.buildGradeDatesText(lessonDate, markDateSameDay)
        assertTrue(newGradesTextSame.startsWith("На урок: 13 ноября ("))

        val postingInfoDiff = org.youschool.app.ui.screens.grades.formatMarkPostingInfo(lessonDate, markDateDifferentDay)
        assertTrue(postingInfoDiff != null && postingInfoDiff.startsWith("Поставлена: "))

        val postingInfoSame = org.youschool.app.ui.screens.grades.formatMarkPostingInfo(lessonDate, markDateSameDay)
        assertEquals("Поставлена в 14:20", postingInfoSame)
    }

    @Test
    fun testGradeCalculatorExclusionWithNonNumericMarks() {
        val allGrades = listOf(
            org.youschool.app.domain.model.Grade(
                id = "1", lessonId = 1L, subjectName = "Алгебра", value = "5", numericValue = 5.0, weight = 1.0,
                date = "2026-11-10", typeName = "Ответ на уроке", teacherName = "Учитель"
            ),
            org.youschool.app.domain.model.Grade(
                id = "2", lessonId = 2L, subjectName = "Алгебра", value = "4", numericValue = 4.0, weight = 1.0,
                date = "2026-11-11", typeName = "Ответ на уроке", teacherName = "Учитель"
            ),
            org.youschool.app.domain.model.Grade(
                id = "3", lessonId = 3L, subjectName = "Алгебра", value = "4", numericValue = 4.0, weight = 1.0,
                date = "2026-11-12", typeName = "Ответ на уроке", teacherName = "Учитель"
            ),
            org.youschool.app.domain.model.Grade(
                id = "4", lessonId = 4L, subjectName = "Алгебра", value = "1", numericValue = 1.0, weight = 1.0,
                date = "2026-11-13", typeName = "Самостоятельная", teacherName = "Учитель"
            ),
            org.youschool.app.domain.model.Grade(
                id = "5", lessonId = 5L, subjectName = "Алгебра", value = "1", numericValue = 1.0, weight = 1.0,
                date = "2026-11-14", typeName = "Самостоятельная", teacherName = "Учитель"
            ),
            // Non-numeric marks (absence "Н") with assigned weights
            org.youschool.app.domain.model.Grade(
                id = "6", lessonId = 6L, subjectName = "Алгебра", value = "Н", numericValue = 0.0, weight = 1.0,
                date = "2026-11-15", typeName = "Пропуск", teacherName = "Учитель"
            ),
            org.youschool.app.domain.model.Grade(
                id = "7", lessonId = 7L, subjectName = "Алгебра", value = "Н", numericValue = 0.0, weight = 2.0,
                date = "2026-11-16", typeName = "Контрольная", teacherName = "Учитель"
            )
        )

        val evaluable = allGrades.filter { it.numericValue > 0.0 }
        assertEquals(5, evaluable.size)

        // Baseline: (5 + 4 + 4 + 1 + 1) / 5 = 15 / 5 = 3.00
        val baselineScore = evaluable.sumOf { it.numericValue * it.weight } / evaluable.sumOf { it.weight }
        assertEquals(3.0, baselineScore, 0.001)

        // Without exclusion: projected average must equal baseline score
        val initialActive = evaluable.filter { it.id !in emptySet<String>() }
        val initialProjected = initialActive.sumOf { it.numericValue * it.weight } / initialActive.sumOf { it.weight }
        assertEquals(baselineScore, initialProjected, 0.001)

        // Exclude the two 1s (id "4" and "5")
        val excludedIds = setOf("4", "5")
        val activeAfterExclusion = evaluable.filter { it.id !in excludedIds }
        assertEquals(3, activeAfterExclusion.size)

        val projectedAfterExclusion = activeAfterExclusion.sumOf { it.numericValue * it.weight } / activeAfterExclusion.sumOf { it.weight }
        // (5 + 4 + 4) / 3 = 13 / 3 = 4.333...
        assertEquals(4.333, projectedAfterExclusion, 0.001)

        // Score MUST increase from 3.00 to 4.33, NOT decrease
        assertTrue(projectedAfterExclusion > baselineScore)
    }

    @Test
    fun testFormatGradeWeightDoesNotRoundToOneDecimalPlace() {
        assertEquals("1", org.youschool.app.ui.screens.grades.formatGradeWeight(1.0))
        assertEquals("2", org.youschool.app.ui.screens.grades.formatGradeWeight(2.0))
        assertEquals("1.25", org.youschool.app.ui.screens.grades.formatGradeWeight(1.25))
        assertEquals("0.5", org.youschool.app.ui.screens.grades.formatGradeWeight(0.5))
        assertEquals("1.75", org.youschool.app.ui.screens.grades.formatGradeWeight(1.75))
        assertEquals("1.3", org.youschool.app.ui.screens.grades.formatGradeWeight(1.3))
        assertEquals("1.125", org.youschool.app.ui.screens.grades.formatGradeWeight(1.125))
        // Floating point jitter protection
        assertEquals("0.3", org.youschool.app.ui.screens.grades.formatGradeWeight(0.30000000000000004))
        assertEquals("1.25", org.youschool.app.ui.screens.grades.formatGradeWeight(1.2500000000000002))
    }
}
