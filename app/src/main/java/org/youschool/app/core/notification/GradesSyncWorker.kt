package org.youschool.app.core.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.youschool.app.YouSchoolApp

class GradesSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? YouSchoolApp ?: YouSchoolApp.instance
        val authManager = app.authManager
        val repository = app.repository
        val database = app.database
        val themeManager = app.themeManager

        // If user is not logged in or notifications are disabled in settings, finish
        if (!authManager.hasCredentials() || !themeManager.themeSettings.value.areNotificationsEnabled) {
            return Result.success()
        }

        return try {
            // 1. Snapshot existing grades before sync
            val beforeKeys = database.gradeDao().getAllGradesSnapshot().map { entity ->
                "${entity.lessonId ?: 0}_${entity.markDate}_${entity.markVal}"
            }.toSet()

            // 2. Perform server sync
            val syncResult = repository.syncGrades()
            if (syncResult.isSuccess) {
                // 3. Find newly added grades that were not present in DB before sync and not marked read
                val afterGrades = database.gradeDao().getAllGradesSnapshot()
                val readKeys = database.readGradeDao().getAllReadGradeKeys().toSet()

                val newlyArrived = afterGrades.filter { entity ->
                    val key = "${entity.lessonId ?: 0}_${entity.markDate}_${entity.markVal}"
                    key !in beforeKeys && key !in readKeys
                }

                if (newlyArrived.isNotEmpty()) {
                    NotificationHelper.showNewGradesNotification(applicationContext, newlyArrived)
                }
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
