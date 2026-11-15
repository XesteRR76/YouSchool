package org.youschool.app.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.youschool.app.MainActivity
import org.youschool.app.R
import org.youschool.app.core.database.entities.GradeEntity
import org.youschool.app.ui.screens.grades.formatGradeDisplayDate
import java.util.Locale

object NotificationHelper {

    const val CHANNEL_ID = "grades_channel"
    private const val CHANNEL_NAME = "Новые оценки"
    private const val NOTIFICATION_ID = 2001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о выставлении новых оценок в электронном журнале"
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showNewGradesNotification(context: Context, newGrades: List<GradeEntity>) {
        if (newGrades.isEmpty()) return

        // On Android 13+, check POST_NOTIFICATIONS runtime permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "grades")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        if (newGrades.size == 1) {
            val grade = newGrades.first()
            val title = "Новая оценка: ${grade.subject} (${grade.markVal})"
            val lessonDay = formatGradeDisplayDate(grade.lessonDate)
            val weightStr = if (grade.weight % 1.0 == 0.0) {
                grade.weight.toInt().toString()
            } else {
                String.format(Locale.US, "%.1f", grade.weight)
            }

            val body = buildString {
                if (grade.workType.isNotBlank()) append(grade.workType)
                if (grade.weight > 0 && grade.weight != 1.0) {
                    if (isNotEmpty()) append(" • ")
                    append("Вес: $weightStr")
                }
                if (lessonDay.isNotBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append("На урок: $lessonDay")
                }
            }

            builder.setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        } else {
            val title = "Новые оценки (${newGrades.size})"
            val summaryText = newGrades.take(3).joinToString(", ") { "${it.subject}: ${it.markVal}" }

            val inbox = NotificationCompat.InboxStyle()
                .setBigContentTitle(title)

            newGrades.forEach { g ->
                val weightStr = if (g.weight % 1.0 == 0.0) g.weight.toInt().toString() else "${g.weight}"
                val type = if (g.workType.isNotBlank()) g.workType else "Урок"
                inbox.addLine("${g.subject}: ${g.markVal} (вес $weightStr, $type)")
            }

            builder.setContentTitle(title)
                .setContentText(summaryText)
                .setStyle(inbox)
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Permission revoked concurrently
        }
    }
}
