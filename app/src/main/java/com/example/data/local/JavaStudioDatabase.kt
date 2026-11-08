package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProjectEntity::class,
        JdkInstallationEntity::class,
        LearningProgressEntity::class,
        OfflineResourceEntity::class,
        ExecutionHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class JavaStudioDatabase : RoomDatabase() {
    abstract fun dao(): JavaStudioDao

    companion object {
        @Volatile
        private var INSTANCE: JavaStudioDatabase? = null

        fun getInstance(context: Context): JavaStudioDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    JavaStudioDatabase::class.java,
                    "universal_java_studio.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
