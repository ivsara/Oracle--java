package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.JdkCatalogAndSpecifications
import com.example.ui.screens.CodeEditorWorkspaceScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.JavaPracticeLabScreen
import com.example.ui.screens.JavaVersionExplorerScreen
import com.example.ui.screens.OfflineApiDocumentationScreen
import com.example.ui.screens.OfflineResourceManagerScreen
import com.example.ui.screens.OfflineTutorialsScreen
import com.example.ui.screens.ProjectManagementScreen
import com.example.ui.screens.QuizAssessmentScreen
import com.example.ui.screens.UniversalJdkManagerScreen
import com.example.ui.screens.WebAppPortalScreen
import com.example.ui.theme.IdeEmeraldSuccess
import com.example.ui.theme.UniversalJavaStudioTheme

@Composable
fun UniversalJavaStudioApp(viewModel: JavaStudioViewModel) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val currentModule by viewModel.currentModule.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val jdkInstallations by viewModel.jdkInstallations.collectAsStateWithLifecycle()
    val learningProgress by viewModel.learningProgress.collectAsStateWithLifecycle()
    val offlineResources by viewModel.offlineResources.collectAsStateWithLifecycle()
    val executionHistory by viewModel.executionHistory.collectAsStateWithLifecycle()

    val activeProjectId by viewModel.activeProjectId.collectAsStateWithLifecycle()
    val activeProjectName by viewModel.activeProjectName.collectAsStateWithLifecycle()
    val activeJdkVersion by viewModel.activeJdkVersion.collectAsStateWithLifecycle()
    val openFilesMap by viewModel.openFilesMap.collectAsStateWithLifecycle()
    val activeFileName by viewModel.activeFileName.collectAsStateWithLifecycle()
    val editorCode by viewModel.editorCode.collectAsStateWithLifecycle()
    val findQuery by viewModel.findQuery.collectAsStateWithLifecycle()
    val replaceQuery by viewModel.replaceQuery.collectAsStateWithLifecycle()

    val stdinInput by viewModel.stdinInput.collectAsStateWithLifecycle()
    val runtimeArgs by viewModel.runtimeArgs.collectAsStateWithLifecycle()
    val compilerFlags by viewModel.compilerFlags.collectAsStateWithLifecycle()
    val lastCompilationResult by viewModel.lastCompilationResult.collectAsStateWithLifecycle()
    val lastExecutionResult by viewModel.lastExecutionResult.collectAsStateWithLifecycle()
    val activeConsoleTab by viewModel.activeConsoleTab.collectAsStateWithLifecycle()
    val statusBannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val autoSaveStatus by viewModel.autoSaveStatus.collectAsStateWithLifecycle()

    val compareLeftVersion by viewModel.compareLeftVersion.collectAsStateWithLifecycle()
    val compareRightVersion by viewModel.compareRightVersion.collectAsStateWithLifecycle()

    val selectedApiJdkVersion by viewModel.selectedApiJdkVersion.collectAsStateWithLifecycle()
    val apiSearchQuery by viewModel.apiSearchQuery.collectAsStateWithLifecycle()
    val selectedApiClass by viewModel.selectedApiClass.collectAsStateWithLifecycle()

    val selectedTutorial by viewModel.selectedTutorial.collectAsStateWithLifecycle()
    val selectedExercise by viewModel.selectedExercise.collectAsStateWithLifecycle()
    val practiceCode by viewModel.practiceCode.collectAsStateWithLifecycle()
    val practiceOutput by viewModel.practiceOutput.collectAsStateWithLifecycle()
    val quizAnswers by viewModel.quizAnswers.collectAsStateWithLifecycle()
    val quizSubmitted by viewModel.quizSubmitted.collectAsStateWithLifecycle()

    // Ensure system Back returns to Dashboard from any secondary screen
    if (currentModule != StudioModule.DASHBOARD) {
        BackHandler {
            viewModel.navigateTo(StudioModule.DASHBOARD)
        }
    }

    val syntaxReport = remember(editorCode, activeJdkVersion, isDarkTheme) {
        viewModel.computeSyntaxReport(editorCode, activeJdkVersion)
    }

    UniversalJavaStudioTheme(darkTheme = isDarkTheme) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            topBar = {
                IdeTopToolbar(
                    activeProjectName = activeProjectName,
                    activeJdkVersion = activeJdkVersion,
                    isDarkTheme = isDarkTheme,
                    isWebAppMode = currentModule == StudioModule.WEB_APP,
                    onSelectJdk = { viewModel.switchProjectJdkVersion(it) },
                    onQuickCompile = {
                        viewModel.navigateTo(StudioModule.CODE_EDITOR)
                        viewModel.compileActiveProject()
                    },
                    onQuickRun = {
                        viewModel.navigateTo(StudioModule.CODE_EDITOR)
                        viewModel.runActiveProject()
                    },
                    onToggleWebApp = {
                        if (currentModule == StudioModule.WEB_APP) {
                            viewModel.navigateTo(StudioModule.DASHBOARD)
                        } else {
                            viewModel.navigateTo(StudioModule.WEB_APP)
                        }
                    },
                    onToggleTheme = { viewModel.toggleTheme() }
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusBannerMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Offline SQLite Ready • JDK $activeJdkVersion",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val isWideScreen = maxWidth >= 720.dp

                if (isWideScreen) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                        ) {
                            StudioModule.entries.forEach { module ->
                                NavigationRailItem(
                                    selected = currentModule == module,
                                    onClick = { viewModel.navigateTo(module) },
                                    icon = { Icon(iconForModule(module), contentDescription = module.title) },
                                    label = { Text(module.title, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("nav_item_${module.name}")
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            ModuleContentHost(
                                currentModule = currentModule,
                                viewModel = viewModel,
                                activeProjectId = activeProjectId,
                                activeProjectName = activeProjectName,
                                activeJdkVersion = activeJdkVersion,
                                projects = projects,
                                jdkInstallations = jdkInstallations,
                                learningProgress = learningProgress,
                                offlineResources = offlineResources,
                                executionHistory = executionHistory,
                                openFilesMap = openFilesMap,
                                activeFileName = activeFileName,
                                editorCode = editorCode,
                                syntaxReport = syntaxReport,
                                lastCompilationResult = lastCompilationResult,
                                lastExecutionResult = lastExecutionResult,
                                stdinInput = stdinInput,
                                runtimeArgs = runtimeArgs,
                                compilerFlags = compilerFlags,
                                activeConsoleTab = activeConsoleTab,
                                findQuery = findQuery,
                                replaceQuery = replaceQuery,
                                autoSaveStatus = autoSaveStatus,
                                compareLeftVersion = compareLeftVersion,
                                compareRightVersion = compareRightVersion,
                                selectedApiJdkVersion = selectedApiJdkVersion,
                                apiSearchQuery = apiSearchQuery,
                                selectedApiClass = selectedApiClass,
                                selectedTutorial = selectedTutorial,
                                selectedExercise = selectedExercise,
                                practiceCode = practiceCode,
                                practiceOutput = practiceOutput,
                                quizAnswers = quizAnswers,
                                quizSubmitted = quizSubmitted
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ScrollableTabRow(
                            selectedTabIndex = StudioModule.entries.indexOf(currentModule),
                            edgePadding = 8.dp,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            StudioModule.entries.forEach { module ->
                                Tab(
                                    selected = currentModule == module,
                                    onClick = { viewModel.navigateTo(module) },
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                iconForModule(module),
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(module.title, style = MaterialTheme.typography.labelMedium)
                                        }
                                    },
                                    modifier = Modifier.testTag("nav_tab_${module.name}")
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            ModuleContentHost(
                                currentModule = currentModule,
                                viewModel = viewModel,
                                activeProjectId = activeProjectId,
                                activeProjectName = activeProjectName,
                                activeJdkVersion = activeJdkVersion,
                                projects = projects,
                                jdkInstallations = jdkInstallations,
                                learningProgress = learningProgress,
                                offlineResources = offlineResources,
                                executionHistory = executionHistory,
                                openFilesMap = openFilesMap,
                                activeFileName = activeFileName,
                                editorCode = editorCode,
                                syntaxReport = syntaxReport,
                                lastCompilationResult = lastCompilationResult,
                                lastExecutionResult = lastExecutionResult,
                                stdinInput = stdinInput,
                                runtimeArgs = runtimeArgs,
                                compilerFlags = compilerFlags,
                                activeConsoleTab = activeConsoleTab,
                                findQuery = findQuery,
                                replaceQuery = replaceQuery,
                                autoSaveStatus = autoSaveStatus,
                                compareLeftVersion = compareLeftVersion,
                                compareRightVersion = compareRightVersion,
                                selectedApiJdkVersion = selectedApiJdkVersion,
                                apiSearchQuery = apiSearchQuery,
                                selectedApiClass = selectedApiClass,
                                selectedTutorial = selectedTutorial,
                                selectedExercise = selectedExercise,
                                practiceCode = practiceCode,
                                practiceOutput = practiceOutput,
                                quizAnswers = quizAnswers,
                                quizSubmitted = quizSubmitted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IdeTopToolbar(
    activeProjectName: String,
    activeJdkVersion: String,
    isDarkTheme: Boolean,
    isWebAppMode: Boolean,
    onSelectJdk: (String) -> Unit,
    onQuickCompile: () -> Unit,
    onQuickRun: () -> Unit,
    onToggleWebApp: () -> Unit,
    onToggleTheme: () -> Unit
) {
    var jdkDropdownExpanded by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "J",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column {
                    Text(
                        text = "Universal Java Studio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$activeProjectName • JDK $activeJdkVersion",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box {
                    OutlinedButton(
                        onClick = { jdkDropdownExpanded = true },
                        modifier = Modifier.testTag("toolbar_jdk_selector")
                    ) {
                        Text("JDK $activeJdkVersion", style = MaterialTheme.typography.labelMedium)
                    }
                    DropdownMenu(
                        expanded = jdkDropdownExpanded,
                        onDismissRequest = { jdkDropdownExpanded = false }
                    ) {
                        JdkCatalogAndSpecifications.allVersionSpecs.forEach { spec ->
                            DropdownMenuItem(
                                text = { Text("JDK ${spec.versionId} (${spec.codename})") },
                                onClick = {
                                    onSelectJdk(spec.versionId)
                                    jdkDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onToggleWebApp,
                    modifier = Modifier.testTag("toolbar_webapp_btn")
                ) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = "Toggle Web App Mode",
                        tint = if (isWebAppMode) IdeEmeraldSuccess else MaterialTheme.colorScheme.secondary
                    )
                }

                IconButton(
                    onClick = onQuickCompile,
                    modifier = Modifier.testTag("toolbar_compile_btn")
                ) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = "Quick Compile",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                IconButton(
                    onClick = onQuickRun,
                    modifier = Modifier.testTag("toolbar_run_btn")
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Quick Run",
                        tint = IdeEmeraldSuccess
                    )
                }

                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier.testTag("theme_toggle_btn")
                ) {
                    Icon(
                        if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Dark/Light Theme",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleContentHost(
    currentModule: StudioModule,
    viewModel: JavaStudioViewModel,
    activeProjectId: Long,
    activeProjectName: String,
    activeJdkVersion: String,
    projects: List<com.example.data.local.ProjectEntity>,
    jdkInstallations: List<com.example.data.local.JdkInstallationEntity>,
    learningProgress: List<com.example.data.local.LearningProgressEntity>,
    offlineResources: List<com.example.data.local.OfflineResourceEntity>,
    executionHistory: List<com.example.data.local.ExecutionHistoryEntity>,
    openFilesMap: Map<String, String>,
    activeFileName: String,
    editorCode: String,
    syntaxReport: com.example.core.SyntaxAnalysisReport,
    lastCompilationResult: com.example.core.CompilationResult?,
    lastExecutionResult: com.example.core.ExecutionResult?,
    stdinInput: String,
    runtimeArgs: String,
    compilerFlags: String,
    activeConsoleTab: Int,
    findQuery: String,
    replaceQuery: String,
    autoSaveStatus: String,
    compareLeftVersion: String,
    compareRightVersion: String,
    selectedApiJdkVersion: String,
    apiSearchQuery: String,
    selectedApiClass: com.example.core.ApiClassDoc?,
    selectedTutorial: com.example.core.TutorialLesson,
    selectedExercise: com.example.core.PracticeExercise,
    practiceCode: String,
    practiceOutput: String,
    quizAnswers: Map<String, Int>,
    quizSubmitted: Boolean
) {
    when (currentModule) {
        StudioModule.DASHBOARD -> HomeDashboardScreen(
            activeProjectName = activeProjectName,
            activeJdkVersion = activeJdkVersion,
            projects = projects,
            jdks = jdkInstallations,
            progressList = learningProgress,
            historyList = executionHistory,
            onNavigate = { viewModel.navigateTo(it) },
            onSelectProject = { viewModel.selectProject(it) },
            onCreateQuickProject = { n, j, t -> viewModel.createNewProject(n, j, t) }
        )

        StudioModule.WEB_APP -> WebAppPortalScreen(
            activeJdkVersion = activeJdkVersion,
            onSyncFromWeb = { jdk, code ->
                viewModel.onWebAutoSave(jdk, code)
            },
            onSwitchToNativeIde = {
                viewModel.navigateTo(StudioModule.CODE_EDITOR)
            }
        )

        StudioModule.CODE_EDITOR -> CodeEditorWorkspaceScreen(
            projectName = activeProjectName,
            activeJdkVersion = activeJdkVersion,
            openFiles = openFilesMap,
            activeFileName = activeFileName,
            editorCode = editorCode,
            syntaxReport = syntaxReport,
            compilationResult = lastCompilationResult,
            executionResult = lastExecutionResult,
            stdinInput = stdinInput,
            runtimeArgs = runtimeArgs,
            compilerFlags = compilerFlags,
            activeConsoleTab = activeConsoleTab,
            findQuery = findQuery,
            replaceQuery = replaceQuery,
            autoSaveStatus = autoSaveStatus,
            onCodeChange = { viewModel.updateEditorCode(it) },
            onSelectTab = { viewModel.selectEditorTab(it) },
            onAddClassTab = { viewModel.addFileTabToProject(it) },
            onSwitchJdkVersion = { viewModel.switchProjectJdkVersion(it) },
            onFormatCode = { viewModel.formatCurrentEditorCode() },
            onSaveProject = { viewModel.saveCurrentProject() },
            onCompile = { viewModel.compileActiveProject() },
            onRun = { viewModel.runActiveProject() },
            onClean = { viewModel.cleanBuildOutput() },
            onStdinChange = { viewModel.updateStdin(it) },
            onRuntimeArgsChange = { viewModel.updateRuntimeArgs(it) },
            onCompilerFlagsChange = { viewModel.updateCompilerFlags(it) },
            onSelectConsoleTab = { viewModel.setConsoleTab(it) },
            onFindReplaceChange = { f, r -> viewModel.updateFindReplace(f, r) },
            onReplaceAll = { viewModel.performReplaceAll() }
        )

        StudioModule.JDK_MANAGER -> UniversalJdkManagerScreen(
            jdkList = jdkInstallations,
            activeProjectJdk = activeJdkVersion,
            onSelectProjectJdk = { viewModel.switchProjectJdkVersion(it) },
            onSetDefaultJdk = { viewModel.setDefaultJdk(it) },
            onAddCustomJdk = { ver, ven, home, arch ->
                viewModel.addCustomJdkInstallation(ver, ven, home, arch)
            },
            onRemoveJdk = { viewModel.removeJdk(it) }
        )

        StudioModule.VERSION_EXPLORER -> JavaVersionExplorerScreen(
            leftVersionId = compareLeftVersion,
            rightVersionId = compareRightVersion,
            leftSpec = viewModel.getLeftVersionSpec(),
            rightSpec = viewModel.getRightVersionSpec(),
            onSelectComparisonPair = { l, r -> viewModel.setComparisonVersions(l, r) }
        )

        StudioModule.TUTORIALS -> OfflineTutorialsScreen(
            selectedLesson = selectedTutorial,
            progressList = learningProgress,
            onSelectLesson = { viewModel.selectTutorialLesson(it) },
            onMarkCompleted = { l, b -> viewModel.markTutorialCompleted(l, b) },
            onOpenSnippetInEditor = { title, jdk, code ->
                viewModel.openCodeSnippetInMainEditor(title, jdk, code)
            }
        )

        StudioModule.API_DOCS -> OfflineApiDocumentationScreen(
            selectedJdkFilter = selectedApiJdkVersion,
            searchQuery = apiSearchQuery,
            filteredClasses = viewModel.getFilteredApiDocs(selectedApiJdkVersion, apiSearchQuery),
            selectedClassDoc = selectedApiClass,
            onSelectJdkFilter = { viewModel.setApiDocJdkFilter(it) },
            onSearchQueryChange = { viewModel.setApiSearchQuery(it) },
            onSelectClassDoc = { viewModel.selectApiClassDoc(it) }
        )

        StudioModule.PRACTICE_LAB -> JavaPracticeLabScreen(
            selectedExercise = selectedExercise,
            practiceCode = practiceCode,
            practiceOutput = practiceOutput,
            progressList = learningProgress,
            onSelectExercise = { viewModel.selectPracticeExercise(it) },
            onPracticeCodeChange = { viewModel.updatePracticeCode(it) },
            onCompileAndRun = { viewModel.compileAndRunPracticeExercise() },
            onOpenInMainEditor = { title, jdk, code ->
                viewModel.openCodeSnippetInMainEditor(title, jdk, code)
            }
        )

        StudioModule.QUIZ -> QuizAssessmentScreen(
            selectedAnswers = quizAnswers,
            quizSubmitted = quizSubmitted,
            progressList = learningProgress,
            onSelectAnswer = { qId, opt -> viewModel.selectQuizAnswer(qId, opt) },
            onSubmitQuiz = { viewModel.submitQuizAssessment() },
            onResetQuiz = { viewModel.resetQuiz() }
        )

        StudioModule.PROJECTS -> ProjectManagementScreen(
            projects = projects,
            activeProjectId = activeProjectId,
            onSelectProjectAndOpenEditor = {
                viewModel.selectProject(it)
                viewModel.navigateTo(StudioModule.CODE_EDITOR)
            },
            onCreateProject = { n, j, t -> viewModel.createNewProject(n, j, t) },
            onDeleteProject = { viewModel.deleteProject(it) }
        )

        StudioModule.RESOURCES -> OfflineResourceManagerScreen(
            resources = offlineResources,
            onImportResource = { t, rType, scope, path, attr ->
                viewModel.importOfflineResource(t, rType, scope, path, attr)
            },
            onRemoveResource = { viewModel.removeOfflineResource(it) }
        )
    }
}

private fun iconForModule(module: StudioModule): ImageVector {
    return when (module) {
        StudioModule.DASHBOARD -> Icons.Default.Dashboard
        StudioModule.WEB_APP -> Icons.Default.Language
        StudioModule.CODE_EDITOR -> Icons.Default.Code
        StudioModule.JDK_MANAGER -> Icons.Default.Memory
        StudioModule.VERSION_EXPLORER -> Icons.Default.CompareArrows
        StudioModule.TUTORIALS -> Icons.Default.School
        StudioModule.API_DOCS -> Icons.Default.LibraryBooks
        StudioModule.PRACTICE_LAB -> Icons.Default.Build
        StudioModule.QUIZ -> Icons.Default.Quiz
        StudioModule.PROJECTS -> Icons.Default.FolderOpen
        StudioModule.RESOURCES -> Icons.Default.Storage
    }
}
