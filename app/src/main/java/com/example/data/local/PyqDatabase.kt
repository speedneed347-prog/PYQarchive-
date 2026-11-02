package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PaperEntity::class,
        UniversityEntity::class,
        CourseEntity::class,
        SemesterEntity::class,
        SubjectEntity::class,
        YearEntity::class,
        PaperTypeEntity::class,
        LanguageEntity::class,
        ExamScheduleEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PyqDatabase : RoomDatabase() {
    abstract fun paperDao(): PaperDao
    abstract fun referenceDao(): ReferenceDao

    companion object {
        @Volatile
        private var INSTANCE: PyqDatabase? = null

        fun getInstance(context: Context): PyqDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PyqDatabase::class.java,
                    "pyq_archive.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
