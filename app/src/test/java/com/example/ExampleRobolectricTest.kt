package com.example

import com.example.core.JdkCatalogAndSpecifications
import com.example.data.local.ExecutionHistoryEntity
import com.example.data.local.JavaStudioDao
import com.example.data.local.JavaStudioRepository
import com.example.data.local.JdkInstallationEntity
import com.example.data.local.LearningProgressEntity
import com.example.data.local.OfflineResourceEntity
import com.example.data.local.ProjectEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExampleRobolectricTest {

    private lateinit var fakeDao: InMemoryJavaStudioDao
    private lateinit var repository: JavaStudioRepository

    @Before
    fun setup() {
        fakeDao = InMemoryJavaStudioDao()
        repository = JavaStudioRepository(fakeDao)
    }

    @Test
    fun `room repository seeds and persists Projects, JDKInstallations, and LearningProgress offline`() = runBlocking {
        repository.ensureSeeded()

        val projects = fakeDao.getAllProjects()
        val jdks = fakeDao.getAllJdkInstallations()
        val resources = fakeDao.getAllOfflineResources()

        // Verify starter multi-version projects exist and each remembers its own JDK
        assertTrue(projects.size >= 6)
        assertEquals("1.2", projects.first { it.name == "Java Basics" }.jdkVersion)
        assertEquals("1.4", projects.first { it.name == "Java OOP" }.jdkVersion)
        assertEquals("8", projects.first { it.name == "Java Advanced" }.jdkVersion)
        assertEquals("17", projects.first { it.name == "Java Backend" }.jdkVersion)
        assertEquals("21", projects.first { it.name == "Modern Java" }.jdkVersion)
        assertEquals("25", projects.first { it.name == "Latest Java" }.jdkVersion)

        // Verify JDK installations from 1.0 to 25 are registered
        assertTrue(jdks.any { it.jdkVersion == "1.0" })
        assertTrue(jdks.any { it.jdkVersion == "1.2" })
        assertTrue(jdks.any { it.jdkVersion == "25" })
        assertTrue(resources.isNotEmpty())

        // Verify LearningProgress entity persistence
        repository.upsertProgress(
            LearningProgressEntity(
                itemId = "tut_ch1_fundamentals",
                category = "TUTORIAL",
                chapterOrModule = "Chapter 1",
                title = "Fundamentals of Java",
                targetJdkVersion = "1.2",
                isCompleted = true,
                quizScore = 7,
                quizTotal = 7
            )
        )
        val progress = fakeDao.getAllLearningProgress()
        assertEquals(1, progress.size)
        assertTrue(progress.first().isCompleted)
        assertEquals(7, progress.first().quizScore)
    }

    @Test
    fun `project serialization preserves multi-file Java sources and per-project JDK`() {
        val files = linkedMapOf(
            "Main.java" to "public class Main { public static void main(String[] a) {} }",
            "Helper.java" to "public class Helper {}"
        )
        val serialized = JdkCatalogAndSpecifications.serializeProjectFiles(files)
        val restored = JdkCatalogAndSpecifications.deserializeProjectFiles(serialized)
        assertEquals(2, restored.size)
        assertEquals(files["Main.java"], restored["Main.java"])
        assertEquals(files["Helper.java"], restored["Helper.java"])
    }
}

private class InMemoryJavaStudioDao : JavaStudioDao {
    private val projects = mutableListOf<ProjectEntity>()
    private val jdks = mutableListOf<JdkInstallationEntity>()
    private val progress = mutableListOf<LearningProgressEntity>()
    private val resources = mutableListOf<OfflineResourceEntity>()
    private val history = mutableListOf<ExecutionHistoryEntity>()

    private val projectsFlow = MutableStateFlow<List<ProjectEntity>>(emptyList())
    private val jdksFlow = MutableStateFlow<List<JdkInstallationEntity>>(emptyList())
    private val progressFlow = MutableStateFlow<List<LearningProgressEntity>>(emptyList())
    private val resourcesFlow = MutableStateFlow<List<OfflineResourceEntity>>(emptyList())
    private val historyFlow = MutableStateFlow<List<ExecutionHistoryEntity>>(emptyList())

    override fun observeProjects(): Flow<List<ProjectEntity>> = projectsFlow
    override suspend fun getAllProjects(): List<ProjectEntity> = projects.toList()
    override suspend fun getProjectById(id: Long): ProjectEntity? = projects.find { it.id == id }

    override suspend fun insertProject(project: ProjectEntity): Long {
        val nextId = if (project.id == 0L) (projects.size + 1).toLong() else project.id
        projects.removeAll { it.id == nextId }
        projects.add(project.copy(id = nextId))
        projectsFlow.value = projects.toList()
        return nextId
    }

    override suspend fun updateProject(project: ProjectEntity) {
        val idx = projects.indexOfFirst { it.id == project.id }
        if (idx >= 0) projects[idx] = project
        projectsFlow.value = projects.toList()
    }

    override suspend fun deleteProjectById(id: Long) {
        projects.removeAll { it.id == id }
        projectsFlow.value = projects.toList()
    }

    override fun observeJdkInstallations(): Flow<List<JdkInstallationEntity>> = jdksFlow
    override suspend fun getAllJdkInstallations(): List<JdkInstallationEntity> = jdks.toList()

    override suspend fun insertJdkInstallation(jdk: JdkInstallationEntity): Long {
        val nextId = if (jdk.id == 0L) (jdks.size + 1).toLong() else jdk.id
        jdks.removeAll { it.id == nextId }
        jdks.add(jdk.copy(id = nextId))
        jdksFlow.value = jdks.toList()
        return nextId
    }

    override suspend fun insertAllJdkInstallations(jdkList: List<JdkInstallationEntity>) {
        jdkList.forEach { insertJdkInstallation(it) }
    }

    override suspend fun updateJdkInstallation(jdk: JdkInstallationEntity) {
        insertJdkInstallation(jdk)
    }

    override suspend fun setDefaultJdk(selectedId: Long) {
        val updated = jdks.map { it.copy(isDefault = it.id == selectedId) }
        jdks.clear()
        jdks.addAll(updated)
        jdksFlow.value = jdks.toList()
    }

    override suspend fun deleteJdkInstallation(id: Long) {
        jdks.removeAll { it.id == id }
        jdksFlow.value = jdks.toList()
    }

    override fun observeLearningProgress(): Flow<List<LearningProgressEntity>> = progressFlow
    override suspend fun getAllLearningProgress(): List<LearningProgressEntity> = progress.toList()

    override suspend fun upsertLearningProgress(item: LearningProgressEntity) {
        progress.removeAll { it.itemId == item.itemId }
        progress.add(item)
        progressFlow.value = progress.toList()
    }

    override suspend fun upsertAllLearningProgress(items: List<LearningProgressEntity>) {
        items.forEach { upsertLearningProgress(it) }
    }

    override fun observeOfflineResources(): Flow<List<OfflineResourceEntity>> = resourcesFlow
    override suspend fun getAllOfflineResources(): List<OfflineResourceEntity> = resources.toList()

    override suspend fun insertOfflineResource(resource: OfflineResourceEntity): Long {
        val nextId = if (resource.id == 0L) (resources.size + 1).toLong() else resource.id
        resources.removeAll { it.id == nextId }
        resources.add(resource.copy(id = nextId))
        resourcesFlow.value = resources.toList()
        return nextId
    }

    override suspend fun insertAllOfflineResources(resList: List<OfflineResourceEntity>) {
        resList.forEach { insertOfflineResource(it) }
    }

    override suspend fun deleteOfflineResource(id: Long) {
        resources.removeAll { it.id == id }
        resourcesFlow.value = resources.toList()
    }

    override fun observeExecutionHistory(): Flow<List<ExecutionHistoryEntity>> = historyFlow
    override suspend fun insertExecutionHistory(entry: ExecutionHistoryEntity): Long {
        val nextId = (history.size + 1).toLong()
        history.add(0, entry.copy(id = nextId))
        historyFlow.value = history.toList()
        return nextId
    }

    override suspend fun clearExecutionHistory() {
        history.clear()
        historyFlow.value = emptyList()
    }
}
