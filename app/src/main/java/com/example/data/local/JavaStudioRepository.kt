package com.example.data.local

import com.example.core.JdkCatalogAndSpecifications
import kotlinx.coroutines.flow.Flow

class JavaStudioRepository(private val dao: JavaStudioDao) {

    val projectsFlow: Flow<List<ProjectEntity>> = dao.observeProjects()
    val jdkInstallationsFlow: Flow<List<JdkInstallationEntity>> = dao.observeJdkInstallations()
    val learningProgressFlow: Flow<List<LearningProgressEntity>> = dao.observeLearningProgress()
    val offlineResourcesFlow: Flow<List<OfflineResourceEntity>> = dao.observeOfflineResources()
    val executionHistoryFlow: Flow<List<ExecutionHistoryEntity>> = dao.observeExecutionHistory()

    suspend fun ensureSeeded() {
        val existingJdks = dao.getAllJdkInstallations()
        if (existingJdks.isEmpty()) {
            dao.insertAllJdkInstallations(JdkCatalogAndSpecifications.buildInitialJdkInstallations())
        }
        val existingProjects = dao.getAllProjects()
        if (existingProjects.isEmpty()) {
            JdkCatalogAndSpecifications.buildStarterProjects().forEach { project ->
                dao.insertProject(project)
            }
        }
        val existingResources = dao.getAllOfflineResources()
        if (existingResources.isEmpty()) {
            dao.insertAllOfflineResources(JdkCatalogAndSpecifications.buildInitialOfflineResources())
        }
    }

    suspend fun saveProject(project: ProjectEntity): Long {
        return if (project.id == 0L) {
            dao.insertProject(project)
        } else {
            dao.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
            project.id
        }
    }

    suspend fun deleteProject(id: Long) = dao.deleteProjectById(id)

    suspend fun addOrUpdateJdk(jdk: JdkInstallationEntity): Long = dao.insertJdkInstallation(jdk)

    suspend fun setDefaultJdk(id: Long) = dao.setDefaultJdk(id)

    suspend fun removeJdk(id: Long) = dao.deleteJdkInstallation(id)

    suspend fun upsertProgress(progress: LearningProgressEntity) = dao.upsertLearningProgress(progress)

    suspend fun addOfflineResource(resource: OfflineResourceEntity): Long = dao.insertOfflineResource(resource)

    suspend fun removeOfflineResource(id: Long) = dao.deleteOfflineResource(id)

    suspend fun recordExecution(entry: ExecutionHistoryEntity): Long = dao.insertExecutionHistory(entry)

    suspend fun clearExecutionHistory() = dao.clearExecutionHistory()
}
