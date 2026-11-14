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
}
