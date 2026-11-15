package org.youschool.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.youschool.app.core.database.dao.*
import org.youschool.app.core.database.entities.*

@Database(
    entities = [
        LessonEntity::class,
        GradeEntity::class,
        HomeworkStatusEntity::class,
        ReadGradeEntity::class,
        PendingHomeworkSyncEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class YouSchoolDatabase : RoomDatabase() {
    abstract fun lessonDao(): LessonDao
    abstract fun gradeDao(): GradeDao
    abstract fun homeworkStatusDao(): HomeworkStatusDao
    abstract fun readGradeDao(): ReadGradeDao
    abstract fun pendingHomeworkSyncDao(): PendingHomeworkSyncDao

    companion object {
        @Volatile
        private var INSTANCE: YouSchoolDatabase? = null

        fun getInstance(context: Context): YouSchoolDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    YouSchoolDatabase::class.java,
                    "youschool_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
