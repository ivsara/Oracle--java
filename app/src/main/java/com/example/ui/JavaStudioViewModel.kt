package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ApiClassDoc
import com.example.core.CompilationResult
import com.example.core.ExecutionResult
import com.example.core.JavaCompilerAndExecutionEngine
import com.example.core.JavaVersionSpec
import com.example.core.JdkCatalogAndSpecifications
import com.example.core.KnowledgeBaseCatalog
import com.example.core.PracticeExercise
import com.example.core.SyntaxAnalysisReport
import com.example.core.TutorialLesson
import com.example.core.VersionSyntaxHighlighter
import com.example.data.local.ExecutionHistoryEntity
import com.example.data.local.JavaStudioDatabase
import com.example.data.local.JavaStudioRepository
import com.example.data.local.JdkInstallationEntity
import com.example.data.local.LearningProgressEntity
import com.example.data.local.OfflineResourceEntity
import com.example.data.local.ProjectEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class StudioModule(val title: String) {
    DASHBOARD("Dashboard"),
    WEB_APP("Web App IDE"),
    CODE_EDITOR("Code Editor"),
    JDK_MANAGER("JDK Manager"),
    VERSION_EXPLORER("Version Explorer"),
    TUTORIALS("Java Tutorials"),
    API_DOCS("API Reference"),
    PRACTICE_LAB("Practice Lab"),
    QUIZ("Quiz & Assessment"),
    PROJECTS("Projects"),
    RESOURCES("Offline Resources")
}

class JavaStudioViewModel @JvmOverloads constructor(
    application: Application,
    private val customRepository: JavaStudioRepository? = null
) : AndroidViewModel(application) {

    private val repository: JavaStudioRepository = customRepository ?: JavaStudioRepository(
        JavaStudioDatabase.getInstance(application).dao()
    )

    // Reactive Database Flows
    val projects: StateFlow<List<ProjectEntity>> = repository.projectsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val jdkInstallations: StateFlow<List<JdkInstallationEntity>> = repository.jdkInstallationsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val learningProgress: StateFlow<List<LearningProgressEntity>> = repository.learningProgressFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val offlineResources: StateFlow<List<OfflineResourceEntity>> = repository.offlineResourcesFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val executionHistory: StateFlow<List<ExecutionHistoryEntity>> = repository.executionHistoryFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Theme & Navigation State
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _currentModule = MutableStateFlow(StudioModule.DASHBOARD)
    val currentModule: StateFlow<StudioModule> = _currentModule.asStateFlow()

    // Active Project & Multi-Tab Code Editor State
    private val _activeProjectId = MutableStateFlow(0L)
    val activeProjectId: StateFlow<Long> = _activeProjectId.asStateFlow()

    private val _activeProjectName = MutableStateFlow("Java Basics")
    val activeProjectName: StateFlow<String> = _activeProjectName.asStateFlow()

    private val _activeJdkVersion = MutableStateFlow("1.2")
    val activeJdkVersion: StateFlow<String> = _activeJdkVersion.asStateFlow()

    private val _openFilesMap = MutableStateFlow<LinkedHashMap<String, String>>(
         linkedMapOf(
            "HelloWorld.java" to """
                import java.util.Vector;

                public class HelloWorld {
                    public static strictfp void main(String args[]) {
                        System.out.println("Hello World from Java 2 (JDK 1.2)!");
                        Vector items = new Vector();
                        items.addElement("J2SE 1.2 Playground");
                        items.addElement("Strictfp & Collections");
                        System.out.println("Items count: " + items.size());
                    }
                }
            """.trimIndent()
        )
    )
    val openFilesMap: StateFlow<LinkedHashMap<String, String>> = _openFilesMap.asStateFlow()

    private val _activeFileName = MutableStateFlow("HelloWorld.java")
    val activeFileName: StateFlow<String> = _activeFileName.asStateFlow()

    private val _editorCode = MutableStateFlow(_openFilesMap.value["HelloWorld.java"] ?: "")
    val editorCode: StateFlow<String> = _editorCode.asStateFlow()

    private val _findQuery = MutableStateFlow("")
    val findQuery: StateFlow<String> = _findQuery.asStateFlow()

    private val _replaceQuery = MutableStateFlow("")
    val replaceQuery: StateFlow<String> = _replaceQuery.asStateFlow()

    // Compiler & Execution Console State
    private val _stdinInput = MutableStateFlow("15\n27")
    val stdinInput: StateFlow<String> = _stdinInput.asStateFlow()

    private val _runtimeArgs = MutableStateFlow("")
    val runtimeArgs: StateFlow<String> = _runtimeArgs.asStateFlow()

    private val _compilerFlags = MutableStateFlow("-g")
    val compilerFlags: StateFlow<String> = _compilerFlags.asStateFlow()

    private val _lastCompilationResult = MutableStateFlow<CompilationResult?>(null)
    val lastCompilationResult: StateFlow<CompilationResult?> = _lastCompilationResult.asStateFlow()

    private val _lastExecutionResult = MutableStateFlow<ExecutionResult?>(null)
    val lastExecutionResult: StateFlow<ExecutionResult?> = _lastExecutionResult.asStateFlow()

    private val _activeConsoleTab = MutableStateFlow(0) // 0=Program Output, 1=Compiler Output, 2=Version Diagnostics, 3=Bytecode, 4=Stdin
    val activeConsoleTab: StateFlow<Int> = _activeConsoleTab.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow("Ready — Offline Toolchain & Room Database Active")
    val statusBannerMessage: StateFlow<String> = _statusBannerMessage.asStateFlow()

    // Auto-Save State (2-second debounce after user stops typing)
    private val _autoSaveStatus = MutableStateFlow("Auto-Save Active (2s debounce)")
    val autoSaveStatus: StateFlow<String> = _autoSaveStatus.asStateFlow()

    private val _lastAutoSaveTimeMs = MutableStateFlow(0L)
    val lastAutoSaveTimeMs: StateFlow<Long> = _lastAutoSaveTimeMs.asStateFlow()

    private var autoSaveJob: Job? = null

    // Version Explorer Comparison State
    private val _compareLeftVersion = MutableStateFlow("8")
    val compareLeftVersion: StateFlow<String> = _compareLeftVersion.asStateFlow()

    private val _compareRightVersion = MutableStateFlow("17")
    val compareRightVersion: StateFlow<String> = _compareRightVersion.asStateFlow()

    // Offline API Documentation Browser State
    private val _selectedApiJdkVersion = MutableStateFlow("7")
    val selectedApiJdkVersion: StateFlow<String> = _selectedApiJdkVersion.asStateFlow()

    private val _apiSearchQuery = MutableStateFlow("")
    val apiSearchQuery: StateFlow<String> = _apiSearchQuery.asStateFlow()

    private val _selectedApiClass = MutableStateFlow<ApiClassDoc?>(KnowledgeBaseCatalog.apiDocumentationCatalog.firstOrNull())
    val selectedApiClass: StateFlow<ApiClassDoc?> = _selectedApiClass.asStateFlow()

    // Tutorial & Practice Lab State
    private val _selectedTutorial = MutableStateFlow(KnowledgeBaseCatalog.tutorialLessons.first())
    val selectedTutorial: StateFlow<TutorialLesson> = _selectedTutorial.asStateFlow()

    private val _selectedExercise = MutableStateFlow(KnowledgeBaseCatalog.practiceExercises.first())
    val selectedExercise: StateFlow<PracticeExercise> = _selectedExercise.asStateFlow()

    private val _practiceCode = MutableStateFlow(KnowledgeBaseCatalog.practiceExercises.first().starterJavaCode)
    val practiceCode: StateFlow<String> = _practiceCode.asStateFlow()

    private val _practiceOutput = MutableStateFlow("Click 'Compile & Run Exercise' to execute on the compatible JDK runtime.")
    val practiceOutput: StateFlow<String> = _practiceOutput.asStateFlow()

    // Quiz State
    private val _quizAnswers = MutableStateFlow<Map<String, Int>>(emptyMap())
    val quizAnswers: StateFlow<Map<String, Int>> = _quizAnswers.asStateFlow()

    private val _quizSubmitted = MutableStateFlow(false)
    val quizSubmitted: StateFlow<Boolean> = _quizSubmitted.asStateFlow()

    init {
        restoreFromLocalStorageSnapshotIfAvailable()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { repository.ensureSeeded() }
            }
            val allProjs = projects.value
            if (allProjs.isNotEmpty() && _activeProjectId.value == 0L) {
                selectProject(allProjs.first(), fromInit = true)
            }
        }
        viewModelScope.launch {
            projects.collect { list ->
                if (list.isNotEmpty() && _activeProjectId.value == 0L) {
                    selectProject(list.first(), fromInit = true)
                }
            }
        }
    }

    private fun restoreFromLocalStorageSnapshotIfAvailable() {
        runCatching {
            val prefs = getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedCode = prefs.getString(KEY_AUTOSAVE_CODE, null)
            val savedJdk = prefs.getString(KEY_AUTOSAVE_JDK, null)
            val savedFile = prefs.getString(KEY_AUTOSAVE_FILE, null)
            val savedProjectName = prefs.getString(KEY_AUTOSAVE_PROJECT_NAME, null)
            val savedProjectId = prefs.getLong(KEY_AUTOSAVE_PROJECT_ID, 0L)
            if (!savedCode.isNullOrBlank() && !savedJdk.isNullOrBlank()) {
                val targetFile = savedFile ?: "HelloWorld.java"
                _activeJdkVersion.value = savedJdk
                if (savedProjectId > 0L) _activeProjectId.value = savedProjectId
                if (!savedProjectName.isNullOrBlank()) _activeProjectName.value = savedProjectName
                _activeFileName.value = targetFile
                _editorCode.value = savedCode
                val updatedMap = LinkedHashMap(_openFilesMap.value)
                updatedMap[targetFile] = savedCode
                _openFilesMap.value = updatedMap
                _autoSaveStatus.value = "Restored auto-saved session (JDK $savedJdk)"
            }
        }
    }

    private fun persistToLocalStorageSnapshot(
        projectId: Long,
        projectName: String,
        jdkVersion: String,
        fileName: String,
        code: String,
        timestampMs: Long
    ) {
        runCatching {
            val prefs = getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putLong(KEY_AUTOSAVE_PROJECT_ID, projectId)
                .putString(KEY_AUTOSAVE_PROJECT_NAME, projectName)
                .putString(KEY_AUTOSAVE_JDK, jdkVersion)
                .putString(KEY_AUTOSAVE_FILE, fileName)
                .putString(KEY_AUTOSAVE_CODE, code)
                .putLong(KEY_AUTOSAVE_TIMESTAMP, timestampMs)
                .apply()
        }
    }

    fun navigateTo(module: StudioModule) {
        _currentModule.value = module
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun computeSyntaxReport(code: String = _editorCode.value, jdkVer: String = _activeJdkVersion.value): SyntaxAnalysisReport {
        return VersionSyntaxHighlighter.analyzeAndHighlight(
            sourceCode = code,
            jdkVersion = jdkVer,
            isDarkTheme = _isDarkTheme.value
        )
    }

    // Project & Editor Operations
    fun selectProject(project: ProjectEntity, fromInit: Boolean = false) {
        autoSaveJob?.cancel()
        _activeProjectId.value = project.id
        _activeProjectName.value = project.name
        _activeJdkVersion.value = project.jdkVersion
        _compilerFlags.value = project.compilerFlags
        _runtimeArgs.value = project.runtimeArgs
        val files = JdkCatalogAndSpecifications.deserializeProjectFiles(project.filesContentSerialized)
        if (files.isEmpty()) {
            files["${project.mainClassName}.java"] = "public class ${project.mainClassName} {\n    public static void main(String[] args) {\n        System.out.println(\"${project.name}\");\n    }\n}"
        }
        _openFilesMap.value = files
        val activeFile = if (project.activeFileName in files) project.activeFileName else files.keys.first()
        _activeFileName.value = activeFile
        _editorCode.value = files[activeFile] ?: ""
        _statusBannerMessage.value = "Loaded project '${project.name}' configured with JDK ${project.jdkVersion}"
        if (!fromInit) {
            _autoSaveStatus.value = "Synced '${project.name}' (JDK ${project.jdkVersion})"
        }
    }

    /**
     * Updates the current code editor content and schedules a 2-second debounced auto-save
     * that persists both the editor content and selected JDK version to the Room database and localStorage.
     */
    fun updateEditorCode(newCode: String, debounceMs: Long = AUTO_SAVE_DEBOUNCE_MS) {
        _editorCode.value = newCode
        val updatedMap = LinkedHashMap(_openFilesMap.value)
        updatedMap[_activeFileName.value] = newCode
        _openFilesMap.value = updatedMap
        scheduleDebouncedAutoSave(debounceMs)
    }

    /**
     * Formats the current editor code using the auto-indentation and whitespace cleanup algorithm
     * and schedules the 2-second debounced auto-save.
     */
    fun formatCurrentEditorCode() {
        val formatted = VersionSyntaxHighlighter.formatJavaCode(_editorCode.value)
        updateEditorCode(formatted)
        _statusBannerMessage.value = "Formatted '${_activeFileName.value}' (cleaned whitespace & auto-indented)"
    }

    private fun scheduleDebouncedAutoSave(debounceMs: Long = AUTO_SAVE_DEBOUNCE_MS) {
        autoSaveJob?.cancel()
        _autoSaveStatus.value = "Typing... (auto-saves in 2s)"
        autoSaveJob = viewModelScope.launch {
            delay(debounceMs)
            performProjectSave(isAutoSave = true)
        }
    }

    fun selectEditorTab(fileName: String) {
        val currentMap = LinkedHashMap(_openFilesMap.value)
        currentMap[_activeFileName.value] = _editorCode.value
        _openFilesMap.value = currentMap
        _activeFileName.value = fileName
        _editorCode.value = currentMap[fileName] ?: ""
        scheduleDebouncedAutoSave()
    }

    fun addFileTabToProject(rawFileName: String) {
        val cleanName = if (rawFileName.endsWith(".java")) rawFileName.trim() else "${rawFileName.trim()}.java"
        val className = cleanName.removeSuffix(".java").replace(Regex("""[^A-Za-z0-9_]"""), "")
            .ifEmpty { "HelperClass" }
        val updated = LinkedHashMap(_openFilesMap.value)
        updated[_activeFileName.value] = _editorCode.value
        updated[cleanName] = """
            public class $className {
                public static void runHelper() {
                    System.out.println("Helper class $className invoked.");
                }
            }
        """.trimIndent()
        _openFilesMap.value = updated
        _activeFileName.value = cleanName
        _editorCode.value = updated[cleanName] ?: ""
        saveCurrentProject()
    }

    fun switchProjectJdkVersion(newJdkVersion: String) {
        _activeJdkVersion.value = newJdkVersion
        _statusBannerMessage.value = "Switched project '${_activeProjectName.value}' to JDK $newJdkVersion (Syntax rules updated)"
        scheduleDebouncedAutoSave()
    }

    fun onWebAutoSave(jdkVersion: String, code: String) {
        _activeJdkVersion.value = jdkVersion
        if (code.isNotBlank()) {
            _editorCode.value = code
            val updatedMap = LinkedHashMap(_openFilesMap.value)
            updatedMap[_activeFileName.value] = code
            _openFilesMap.value = updatedMap
        }
        viewModelScope.launch {
            performProjectSave(isAutoSave = true)
        }
    }

    fun saveCurrentProject() {
        autoSaveJob?.cancel()
        viewModelScope.launch {
            performProjectSave(isAutoSave = false)
        }
    }

    private suspend fun performProjectSave(isAutoSave: Boolean) {
        val currentCode = _editorCode.value
        val currentJdk = _activeJdkVersion.value
        val currentFile = _activeFileName.value
        val currentMap = LinkedHashMap(_openFilesMap.value)
        currentMap[currentFile] = currentCode
        _openFilesMap.value = currentMap

        val now = System.currentTimeMillis()
        val existing = projects.value.find { it.id == _activeProjectId.value }
        val mainClass = currentFile.removeSuffix(".java")
        val entity = (existing ?: ProjectEntity(
            name = _activeProjectName.value,
            description = "Java Project targeting JDK $currentJdk",
            jdkVersion = currentJdk,
            packageName = "default",
            mainClassName = mainClass,
            activeFileName = currentFile,
            filesContentSerialized = ""
        )).copy(
            jdkVersion = currentJdk,
            mainClassName = mainClass,
            activeFileName = currentFile,
            compilerFlags = _compilerFlags.value,
            runtimeArgs = _runtimeArgs.value,
            filesContentSerialized = JdkCatalogAndSpecifications.serializeProjectFiles(currentMap),
            updatedAt = now
        )
        val savedId = repository.saveProject(entity)
        _activeProjectId.value = savedId
        _lastAutoSaveTimeMs.value = now
        persistToLocalStorageSnapshot(
            projectId = savedId,
            projectName = entity.name,
            jdkVersion = currentJdk,
            fileName = currentFile,
            code = currentCode,
            timestampMs = now
        )
        val formattedTime = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(now))
        if (isAutoSave) {
            _autoSaveStatus.value = "Auto-saved to Room DB & localStorage at $formattedTime (JDK $currentJdk)"
            _statusBannerMessage.value = "Auto-saved '${entity.name}' ($currentFile • JDK $currentJdk) at $formattedTime"
        } else {
            _autoSaveStatus.value = "Saved to Room DB at $formattedTime (JDK $currentJdk)"
            _statusBannerMessage.value = "Saved '${entity.name}' (JDK ${entity.jdkVersion})"
        }
    }

    fun createNewProject(name: String, jdkVersion: String, templateType: String) {
        viewModelScope.launch {
            val className = name.replace(Regex("""[^A-Za-z0-9]"""), "").ifEmpty { "MainApp" }
            val fileName = "$className.java"
            val initialCode = when {
                jdkVersion in listOf("1.0", "1.1", "1.2", "1.3") -> """
                    import java.util.Vector;

                    public class $className {
                        public static strictfp void main(String args[]) {
                            System.out.println("Project: $name (Historical JDK $jdkVersion)");
                            Vector v = new Vector();
                            v.addElement("Legacy Java Environment");
                            System.out.println("Vector elements: " + v.size());
                        }
                    }
                """.trimIndent()
                jdkVersion in listOf("1.4", "5", "6", "7", "8") -> """
                    public class $className {
                        public static void main(String[] args) {
                            System.out.println("Project: $name (JDK $jdkVersion)");
                            int result = 21 * 2;
                            System.out.println("Computed value: " + result);
                        }
                    }
                """.trimIndent()
                else -> """
                    public class $className {
                        public record AppMeta(String project, String jdk) {}

                        public static void main(String[] args) {
                            var meta = new AppMeta("$name", "JDK $jdkVersion");
                            System.out.println("Launched " + meta.project() + " on " + meta.jdk());
                        }
                    }
                """.trimIndent()
            }
            val newProj = ProjectEntity(
                name = name.ifBlank { "Untitled Java Project" },
                description = "$templateType Project targeting JDK $jdkVersion",
                jdkVersion = jdkVersion,
                packageName = "default",
                mainClassName = className,
                activeFileName = fileName,
                compilerFlags = if (JdkCatalogAndSpecifications.versionRank(jdkVersion) >= 109) "--release $jdkVersion" else "-source $jdkVersion",
                filesContentSerialized = JdkCatalogAndSpecifications.serializeProjectFiles(mapOf(fileName to initialCode))
            )
            val id = repository.saveProject(newProj)
            selectProject(newProj.copy(id = id))
            _currentModule.value = StudioModule.CODE_EDITOR
        }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            repository.deleteProject(id)
            val remaining = projects.value.filter { it.id != id }
            if (remaining.isNotEmpty() && _activeProjectId.value == id) {
                selectProject(remaining.first())
            }
        }
    }

    fun updateFindReplace(find: String, replace: String) {
        _findQuery.value = find
        _replaceQuery.value = replace
    }

    fun performReplaceAll() {
        val f = _findQuery.value
        if (f.isEmpty()) return
        val updated = _editorCode.value.replace(f, _replaceQuery.value)
        updateEditorCode(updated)
        _statusBannerMessage.value = "Replaced occurrences of '$f' with '${_replaceQuery.value}'"
    }

    fun updateStdin(input: String) {
        _stdinInput.value = input
    }

    fun updateRuntimeArgs(args: String) {
        _runtimeArgs.value = args
    }

    fun updateCompilerFlags(flags: String) {
        _compilerFlags.value = flags
    }

    fun setConsoleTab(tabIndex: Int) {
        _activeConsoleTab.value = tabIndex
    }

    // Compilation & Execution Engine Actions
    fun compileActiveProject() {
        viewModelScope.launch {
            val currentMap = LinkedHashMap(_openFilesMap.value)
            currentMap[_activeFileName.value] = _editorCode.value
            _openFilesMap.value = currentMap

            val jdk = resolveJdkEntity(_activeJdkVersion.value)
            val result = withContext(Dispatchers.Default) {
                JavaCompilerAndExecutionEngine.compileProject(
                    jdk = jdk,
                    files = currentMap,
                    customFlags = _compilerFlags.value
                )
            }
            _lastCompilationResult.value = result
            _activeConsoleTab.value = 1 // Show Compiler Output
            _statusBannerMessage.value = if (result.success) {
                "Compilation Succeeded (JDK ${jdk.jdkVersion} -> Bytecode v${jdk.bytecodeMajorVersion}.0)"
            } else {
                "Compilation Failed (${result.diagnostics.count { it.severity == "ERROR" }} error(s) for JDK ${jdk.jdkVersion})"
            }

            repository.recordExecution(
                ExecutionHistoryEntity(
                    projectName = _activeProjectName.value,
                    fileName = _activeFileName.value,
                    jdkVersion = jdk.jdkVersion,
                    actionType = "COMPILE",
                    commandUsed = result.compilerCommand,
                    status = if (result.success) "SUCCESS" else "COMPILE_ERROR",
                    outputSummary = result.rawOutput.lines().lastOrNull() ?: "",
                    executionTimeMs = result.durationMs
                )
            )
            saveCurrentProject()
        }
    }

    fun runActiveProject() {
        viewModelScope.launch {
            val currentMap = LinkedHashMap(_openFilesMap.value)
            currentMap[_activeFileName.value] = _editorCode.value
            _openFilesMap.value = currentMap

            val jdk = resolveJdkEntity(_activeJdkVersion.value)
            val mainClass = _activeFileName.value.removeSuffix(".java")

            val compResult = withContext(Dispatchers.Default) {
                JavaCompilerAndExecutionEngine.compileProject(
                    jdk = jdk,
                    files = currentMap,
                    customFlags = _compilerFlags.value
                )
            }
            _lastCompilationResult.value = compResult
            if (!compResult.success) {
                _activeConsoleTab.value = 1
                _statusBannerMessage.value = "Build failed — fix compiler errors before running"
                return@launch
            }

            val execResult = withContext(Dispatchers.Default) {
                JavaCompilerAndExecutionEngine.executeProgram(
                    jdk = jdk,
                    files = currentMap,
                    mainClass = mainClass,
                    stdinInput = _stdinInput.value,
                    runtimeArgs = _runtimeArgs.value
                )
            }
            _lastExecutionResult.value = execResult
            _activeConsoleTab.value = 0 // Program Output
            _statusBannerMessage.value = if (execResult.success) {
                "Executed $mainClass on JDK ${jdk.jdkVersion} in ${execResult.durationMs}ms (exit code 0)"
            } else {
                "Runtime Exception: ${execResult.exceptionType ?: "Error"} (exit code ${execResult.exitCode})"
            }

            repository.recordExecution(
                ExecutionHistoryEntity(
                    projectName = _activeProjectName.value,
                    fileName = _activeFileName.value,
                    jdkVersion = jdk.jdkVersion,
                    actionType = "RUN",
                    commandUsed = execResult.runtimeCommand,
                    status = if (execResult.success) "SUCCESS" else "RUNTIME_EXCEPTION",
                    outputSummary = (execResult.stdout.ifBlank { execResult.stderr }).take(180),
                    executionTimeMs = execResult.durationMs
                )
            )
        }
    }

    fun cleanBuildOutput() {
        _lastCompilationResult.value = null
        _lastExecutionResult.value = null
        _statusBannerMessage.value = "Cleaned build/classes artifacts and console output"
    }

    // JDK Manager Operations
    fun addCustomJdkInstallation(
        jdkVersion: String,
        vendor: String,
        javaHome: String,
        architecture: String
    ) {
        viewModelScope.launch {
            val spec = JdkCatalogAndSpecifications.getSpec(jdkVersion)
            val cleanHome = javaHome.ifBlank { "/opt/jdks/jdk-$jdkVersion" }.trimEnd('/')
            val entity = JdkInstallationEntity(
                jdkVersion = spec.versionId,
                displayName = "JDK ${spec.versionId} ($vendor)",
                vendor = vendor.ifBlank { "Custom OpenJDK Vendor" },
                javaHome = cleanHome,
                compilerPath = "$cleanHome/bin/javac",
                runtimePath = "$cleanHome/bin/java",
                compilerVersion = "javac ${spec.versionId}.0 (Bytecode ${spec.bytecodeMajor}.0)",
                runtimeVersion = "java ${spec.versionId} Runtime",
                bytecodeMajorVersion = spec.bytecodeMajor,
                architecture = architecture,
                status = "Available",
                compatibilityLevel = "Supported (Configured Toolchain)",
                isDefault = false,
                isManualConfig = true
            )
            repository.addOrUpdateJdk(entity)
            _statusBannerMessage.value = "Added JDK ${spec.versionId} ($vendor) at $cleanHome"
        }
    }

    fun setDefaultJdk(jdk: JdkInstallationEntity) {
        viewModelScope.launch {
            repository.setDefaultJdk(jdk.id)
            _activeJdkVersion.value = jdk.jdkVersion
            _statusBannerMessage.value = "Set JDK ${jdk.jdkVersion} (${jdk.vendor}) as Default JDK"
        }
    }

    fun removeJdk(jdk: JdkInstallationEntity) {
        viewModelScope.launch {
            repository.removeJdk(jdk.id)
            _statusBannerMessage.value = "Removed JDK configuration: ${jdk.displayName}"
        }
    }

    // Version Explorer Operations
    fun setComparisonVersions(leftVer: String, rightVer: String) {
        _compareLeftVersion.value = leftVer
        _compareRightVersion.value = rightVer
    }

    fun getLeftVersionSpec(): JavaVersionSpec = JdkCatalogAndSpecifications.getSpec(_compareLeftVersion.value)
    fun getRightVersionSpec(): JavaVersionSpec = JdkCatalogAndSpecifications.getSpec(_compareRightVersion.value)

    // API Documentation Filtering (Never shows modern APIs when viewing older JDK docs!)
    fun setApiDocJdkFilter(versionId: String) {
        _selectedApiJdkVersion.value = versionId
        val filtered = getFilteredApiDocs(versionId, _apiSearchQuery.value)
        if (_selectedApiClass.value !in filtered) {
            _selectedApiClass.value = filtered.firstOrNull()
        }
    }

    fun setApiSearchQuery(query: String) {
        _apiSearchQuery.value = query
        val filtered = getFilteredApiDocs(_selectedApiJdkVersion.value, query)
        if (_selectedApiClass.value !in filtered && filtered.isNotEmpty()) {
            _selectedApiClass.value = filtered.first()
        }
    }

    fun selectApiClassDoc(doc: ApiClassDoc) {
        _selectedApiClass.value = doc
    }

    fun getFilteredApiDocs(
        jdkVersion: String = _selectedApiJdkVersion.value,
        query: String = _apiSearchQuery.value
    ): List<ApiClassDoc> {
        val maxRank = JdkCatalogAndSpecifications.versionRank(jdkVersion)
        val q = query.trim().lowercase()
        return KnowledgeBaseCatalog.apiDocumentationCatalog.filter { doc ->
            val docSinceRank = JdkCatalogAndSpecifications.versionRank(doc.sinceJdk)
            // Strict requirement: Never show Java 7 or modern APIs as if they were available in Java 2!
            val availableInSelectedJdk = docSinceRank <= maxRank
            val matchesQuery = q.isEmpty() ||
                doc.className.lowercase().contains(q) ||
                doc.packageName.lowercase().contains(q) ||
                doc.summary.lowercase().contains(q) ||
                doc.members.any { it.signature.lowercase().contains(q) }
            availableInSelectedJdk && matchesQuery
        }
    }

    // Tutorials Operations
    fun selectTutorialLesson(lesson: TutorialLesson) {
        _selectedTutorial.value = lesson
    }

    fun markTutorialCompleted(lesson: TutorialLesson, bookmarked: Boolean = false) {
        viewModelScope.launch {
            repository.upsertProgress(
                LearningProgressEntity(
                    itemId = lesson.id,
                    category = "TUTORIAL",
                    chapterOrModule = lesson.chapterTitle,
                    title = lesson.lessonTitle,
                    targetJdkVersion = lesson.minJdkVersion,
                    isCompleted = true,
                    isBookmarked = bookmarked,
                    timeSpentMinutes = 15
                )
            )
            _statusBannerMessage.value = "Completed lesson: ${lesson.lessonTitle}"
        }
    }

    fun openCodeSnippetInMainEditor(title: String, targetJdk: String, code: String) {
        val className = Regex("""\bclass\s+([A-Za-z_][A-Za-z0-9_]*)""").find(code)?.groupValues?.get(1) ?: "LabSample"
        val fileName = "$className.java"
        val updated = LinkedHashMap(_openFilesMap.value)
        updated[fileName] = code
        _openFilesMap.value = updated
        _activeFileName.value = fileName
        _editorCode.value = code
        _activeJdkVersion.value = targetJdk
        _currentModule.value = StudioModule.CODE_EDITOR
        _statusBannerMessage.value = "Loaded '$title' into Code Editor with JDK $targetJdk"
    }

    // Practice Lab Operations
    fun selectPracticeExercise(exercise: PracticeExercise) {
        _selectedExercise.value = exercise
        _practiceCode.value = exercise.starterJavaCode
        _practiceOutput.value = "Ready to compile & run '${exercise.title}' with JDK ${exercise.recommendedJdk}."
    }

    fun updatePracticeCode(code: String) {
        _practiceCode.value = code
    }

    fun compileAndRunPracticeExercise() {
        viewModelScope.launch {
            val ex = _selectedExercise.value
            val jdk = resolveJdkEntity(ex.recommendedJdk)
            val code = _practiceCode.value
            val mainClass = Regex("""\bclass\s+([A-Za-z_][A-Za-z0-9_]*)""").find(code)?.groupValues?.get(1) ?: "Exercise"
            val files = mapOf("$mainClass.java" to code)

            val comp = withContext(Dispatchers.Default) {
                JavaCompilerAndExecutionEngine.compileProject(jdk, files)
            }
            if (!comp.success) {
                _practiceOutput.value = "[COMPILATION FAILED on JDK ${jdk.jdkVersion}]\n${comp.rawOutput}"
                return@launch
            }
            val exec = withContext(Dispatchers.Default) {
                JavaCompilerAndExecutionEngine.executeProgram(
                    jdk = jdk,
                    files = files,
                    mainClass = mainClass,
                    stdinInput = _stdinInput.value
                )
            }
            _practiceOutput.value = buildString {
                appendLine("$ ${comp.compilerCommand}")
                appendLine("$ ${exec.runtimeCommand}")
                appendLine("--- STDOUT (JDK ${jdk.jdkVersion} | Bytecode v${jdk.bytecodeMajorVersion}.0) ---")
                if (exec.stdout.isNotBlank()) appendLine(exec.stdout)
                if (exec.stderr.isNotBlank()) {
                    appendLine("--- STDERR ---")
                    appendLine(exec.stderr)
                }
                appendLine("Execution completed in ${exec.durationMs}ms.")
            }.trimEnd()

            repository.upsertProgress(
                LearningProgressEntity(
                    itemId = ex.id,
                    category = "EXERCISE",
                    chapterOrModule = ex.categoryName,
                    title = ex.title,
                    targetJdkVersion = ex.recommendedJdk,
                    isCompleted = exec.success,
                    savedCodeSnippet = code,
                    timeSpentMinutes = 10
                )
            )
        }
    }

    // Quiz Operations
    fun selectQuizAnswer(questionId: String, optionIndex: Int) {
        val updated = HashMap(_quizAnswers.value)
        updated[questionId] = optionIndex
        _quizAnswers.value = updated
    }

    fun submitQuizAssessment() {
        viewModelScope.launch {
            _quizSubmitted.value = true
            val questions = KnowledgeBaseCatalog.quizQuestions
            var correct = 0
            questions.forEach { q ->
                if (_quizAnswers.value[q.id] == q.correctIndex) {
                    correct++
                }
            }
            repository.upsertProgress(
                LearningProgressEntity(
                    itemId = "quiz_master_assessment",
                    category = "QUIZ",
                    chapterOrModule = "All-Version Java Assessment",
                    title = "Historical & Modern Java Mastery Quiz",
                    targetJdkVersion = "1.2–25",
                    isCompleted = true,
                    quizScore = correct,
                    quizTotal = questions.size,
                    timeSpentMinutes = 12
                )
            )
            _statusBannerMessage.value = "Quiz submitted! Score: $correct / ${questions.size} saved to Room SQLite database."
        }
    }

    fun resetQuiz() {
        _quizAnswers.value = emptyMap()
        _quizSubmitted.value = false
    }

    // Offline Resource Manager Operations
    fun importOfflineResource(
        title: String,
        resourceType: String,
        jdkScope: String,
        localPath: String,
        attribution: String
    ) {
        viewModelScope.launch {
            repository.addOfflineResource(
                OfflineResourceEntity(
                    title = title.ifBlank { "Custom Offline Java Pack" },
                    resourceType = resourceType,
                    jdkVersionScope = jdkScope,
                    localPath = localPath.ifBlank { "resources/custom/imported_bundle/" },
                    attribution = attribution.ifBlank { "User Imported Legal Resource" },
                    status = "Imported Offline"
                )
            )
            _statusBannerMessage.value = "Imported offline resource '$title' ($jdkScope)"
        }
    }

    fun removeOfflineResource(id: Long) {
        viewModelScope.launch {
            repository.removeOfflineResource(id)
        }
    }

    private fun resolveJdkEntity(versionId: String): JdkInstallationEntity {
        val clean = versionId.trim().removePrefix("JDK ").removePrefix("Java ").trim()
        val match = jdkInstallations.value.find { it.jdkVersion == clean }
        if (match != null) return match
        val spec = JdkCatalogAndSpecifications.getSpec(clean)
        return JdkInstallationEntity(
            jdkVersion = spec.versionId,
            displayName = spec.displayName,
            vendor = spec.vendorDefault,
            javaHome = "/opt/universal-java-studio/toolchains/jdk-${spec.versionId}",
            compilerPath = "/opt/universal-java-studio/toolchains/jdk-${spec.versionId}/bin/javac",
            runtimePath = "/opt/universal-java-studio/toolchains/jdk-${spec.versionId}/bin/java",
            compilerVersion = "javac ${spec.versionId}.0",
            runtimeVersion = "java ${spec.versionId}",
            bytecodeMajorVersion = spec.bytecodeMajor,
            architecture = if (spec.rank <= 104) "32-bit x86 (Legacy VM)" else "64-bit",
            status = "Available",
            compatibilityLevel = "Supported"
        )
    }

    companion object {
        const val AUTO_SAVE_DEBOUNCE_MS = 2000L
        const val PREFS_NAME = "universal_java_studio_local_storage"
        const val KEY_AUTOSAVE_PROJECT_ID = "ujs_autosave_project_id"
        const val KEY_AUTOSAVE_PROJECT_NAME = "ujs_autosave_project_name"
        const val KEY_AUTOSAVE_JDK = "ujs_autosave_jdk"
        const val KEY_AUTOSAVE_FILE = "ujs_autosave_file"
        const val KEY_AUTOSAVE_CODE = "ujs_autosave_code"
        const val KEY_AUTOSAVE_TIMESTAMP = "ujs_autosave_timestamp"
    }
}

