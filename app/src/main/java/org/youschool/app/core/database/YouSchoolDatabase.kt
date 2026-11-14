package org.youschool.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.youschool.app.core.database.dao.GradeDao
import org.youschool.app.core.database.dao.HomeworkStatusDao
import org.youschool.app.core.database.dao.LessonDao
import org.youschool.app.core.database.entities.GradeEntity
import org.youschool.app.core.database.entities.HomeworkStatusEntity
import org.youschool.app.core.database.entities.LessonEntity

@Database(
    entities = [
        LessonEntity::class,
        GradeEntity::class,
        HomeworkStatusEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class YouSchoolDatabase : RoomDatabase() {
    abstract fun lessonDao(): LessonDao
    abstract fun gradeDao(): GradeDao
    abstract fun homeworkStatusDao(): HomeworkStatusDao

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
