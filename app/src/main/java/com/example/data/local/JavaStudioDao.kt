package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JavaStudioDao {

    // Projects
    @Query("SELECT * FROM Projects ORDER BY updatedAt DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM Projects ORDER BY updatedAt DESC")
    suspend fun getAllProjects(): List<ProjectEntity>

    @Query("SELECT * FROM Projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM Projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    // JDK Installations
    @Query("SELECT * FROM JDKInstallations ORDER BY bytecodeMajorVersion ASC, id ASC")
    fun observeJdkInstallations(): Flow<List<JdkInstallationEntity>>

    @Query("SELECT * FROM JDKInstallations ORDER BY bytecodeMajorVersion ASC, id ASC")
    suspend fun getAllJdkInstallations(): List<JdkInstallationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJdkInstallation(jdk: JdkInstallationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllJdkInstallations(jdks: List<JdkInstallationEntity>)

    @Update
    suspend fun updateJdkInstallation(jdk: JdkInstallationEntity)

    @Query("UPDATE JDKInstallations SET isDefault = (id = :selectedId)")
    suspend fun setDefaultJdk(selectedId: Long)

    @Query("DELETE FROM JDKInstallations WHERE id = :id")
    suspend fun deleteJdkInstallation(id: Long)

    // Learning Progress
    @Query("SELECT * FROM LearningProgress ORDER BY lastAccessedAt DESC")
    fun observeLearningProgress(): Flow<List<LearningProgressEntity>>

    @Query("SELECT * FROM LearningProgress")
    suspend fun getAllLearningProgress(): List<LearningProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLearningProgress(progress: LearningProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllLearningProgress(items: List<LearningProgressEntity>)

    // Offline Resources
    @Query("SELECT * FROM OfflineResources ORDER BY importedAt DESC")
    fun observeOfflineResources(): Flow<List<OfflineResourceEntity>>

    @Query("SELECT * FROM OfflineResources")
    suspend fun getAllOfflineResources(): List<OfflineResourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflineResource(resource: OfflineResourceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllOfflineResources(resources: List<OfflineResourceEntity>)

    @Query("DELETE FROM OfflineResources WHERE id = :id")
    suspend fun deleteOfflineResource(id: Long)

    // Execution History
    @Query("SELECT * FROM ExecutionHistory ORDER BY timestamp DESC LIMIT 50")
    fun observeExecutionHistory(): Flow<List<ExecutionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExecutionHistory(entry: ExecutionHistoryEntity): Long

    @Query("DELETE FROM ExecutionHistory")
    suspend fun clearExecutionHistory()
}
