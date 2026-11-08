package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String,
    val jdkVersion: String, // e.g., "1.2", "1.4", "8", "17", "21", "25"
    val packageName: String,
    val mainClassName: String,
    val activeFileName: String,
    val filesContentSerialized: String, // File1.java:::content|||File2.java:::content
    val compilerFlags: String = "-g",
    val runtimeArgs: String = "",
    val classpath: String = ".",
    val lastCompileStatus: String = "NOT_COMPILED",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "JDKInstallations")
data class JdkInstallationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val jdkVersion: String, // "1.0", "1.1", "1.2", ..., "25"
    val displayName: String,
    val vendor: String, // "Sun Microsystems", "Oracle", "OpenJDK / Eclipse Temurin", etc.
    val javaHome: String,
    val compilerPath: String, // javac path
    val runtimePath: String, // java path
    val compilerVersion: String,
    val runtimeVersion: String,
    val bytecodeMajorVersion: Int, // 45..69
    val architecture: String, // "32-bit x86 (Legacy VM)", "64-bit x86_64 / ARM64"
    val status: String, // "Available", "Legacy Sandbox Ready", "Host Runtime Detected", "Unavailable"
    val compatibilityLevel: String, // "Supported (Full AST + Toolchain)" / "Legacy VM Isolated" / "Restricted"
    val isDefault: Boolean = false,
    val isManualConfig: Boolean = false
)

@Entity(tableName = "LearningProgress")
data class LearningProgressEntity(
    @PrimaryKey val itemId: String, // e.g., "tutorial_ch1_l1", "exercise_oop_1", "quiz_ch1"
    val category: String, // "TUTORIAL", "EXERCISE", "QUIZ"
    val chapterOrModule: String,
    val title: String,
    val targetJdkVersion: String,
    val isCompleted: Boolean = false,
    val isBookmarked: Boolean = false,
    val quizScore: Int = 0,
    val quizTotal: Int = 0,
    val timeSpentMinutes: Int = 0,
    val savedCodeSnippet: String = "",
    val lastAccessedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "OfflineResources")
data class OfflineResourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val resourceType: String, // "HTML Tutorial", "API Documentation", "PDF Guide", "Example Pack"
    val jdkVersionScope: String, // "JDK 1.2", "Java SE 7", "JDK 8–25", etc.
    val localPath: String,
    val attribution: String,
    val status: String, // "Installed Offline", "Verified", "Imported"
    val importedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ExecutionHistory")
data class ExecutionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val projectName: String,
    val fileName: String,
    val jdkVersion: String,
    val actionType: String, // "COMPILE" or "RUN"
    val commandUsed: String,
    val status: String, // "SUCCESS", "COMPILE_ERROR", "RUNTIME_EXCEPTION", "TIMEOUT"
    val outputSummary: String,
    val executionTimeMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

typealias Projects = ProjectEntity
typealias JDKInstallations = JdkInstallationEntity
typealias LearningProgress = LearningProgressEntity

data class RoomTableSchemaSummary(
    val tableName: String,
    val entityClassName: String,
    val primaryKey: String,
    val columns: List<String>,
    val purpose: String
)

object RoomSchemaRegistry {
    val tables: List<RoomTableSchemaSummary> = listOf(
        RoomTableSchemaSummary(
            tableName = "Projects",
            entityClassName = "ProjectEntity",
            primaryKey = "id (Long, autoGenerate)",
            columns = listOf("id", "name", "description", "jdkVersion", "packageName", "mainClassName", "activeFileName", "filesContentSerialized", "compilerFlags", "runtimeArgs", "classpath", "lastCompileStatus", "createdAt", "updatedAt"),
            purpose = "Stores multi-file Java projects with per-project remembered JDK version and build flags."
        ),
        RoomTableSchemaSummary(
            tableName = "JDKInstallations",
            entityClassName = "JdkInstallationEntity",
            primaryKey = "id (Long, autoGenerate)",
            columns = listOf("id", "jdkVersion", "displayName", "vendor", "javaHome", "compilerPath", "runtimePath", "compilerVersion", "runtimeVersion", "bytecodeMajorVersion", "architecture", "status", "compatibilityLevel", "isDefault", "isManualConfig"),
            purpose = "Stores detected and manually configured JDK 1.0–25 toolchains, paths, and bytecode capabilities."
        ),
        RoomTableSchemaSummary(
            tableName = "LearningProgress",
            entityClassName = "LearningProgressEntity",
            primaryKey = "itemId (String)",
            columns = listOf("itemId", "category", "chapterOrModule", "title", "targetJdkVersion", "isCompleted", "isBookmarked", "quizScore", "quizTotal", "timeSpentMinutes", "savedCodeSnippet", "lastAccessedAt"),
            purpose = "Tracks offline tutorial completion, bookmarked chapters, practice lab solutions, and quiz scores."
        ),
        RoomTableSchemaSummary(
            tableName = "OfflineResources",
            entityClassName = "OfflineResourceEntity",
            primaryKey = "id (Long, autoGenerate)",
            columns = listOf("id", "title", "resourceType", "jdkVersionScope", "localPath", "attribution", "status", "importedAt"),
            purpose = "Catalogs imported offline HTML tutorials, API Javadoc archives, and example bundles."
        ),
        RoomTableSchemaSummary(
            tableName = "ExecutionHistory",
            entityClassName = "ExecutionHistoryEntity",
            primaryKey = "id (Long, autoGenerate)",
            columns = listOf("id", "projectName", "fileName", "jdkVersion", "actionType", "commandUsed", "status", "outputSummary", "executionTimeMs", "timestamp"),
            purpose = "Logs historical javac compilation and java runtime execution sessions."
        )
    )
}
