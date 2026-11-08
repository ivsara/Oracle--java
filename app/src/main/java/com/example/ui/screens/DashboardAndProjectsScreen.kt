package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.JdkCatalogAndSpecifications
import com.example.data.local.ExecutionHistoryEntity
import com.example.data.local.JdkInstallationEntity
import com.example.data.local.LearningProgressEntity
import com.example.data.local.ProjectEntity
import com.example.ui.StudioModule
import com.example.ui.theme.IdeEmeraldSuccess

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeDashboardScreen(
    activeProjectName: String,
    activeJdkVersion: String,
    projects: List<ProjectEntity>,
    jdks: List<JdkInstallationEntity>,
    progressList: List<LearningProgressEntity>,
    historyList: List<ExecutionHistoryEntity>,
    onNavigate: (StudioModule) -> Unit,
    onSelectProject: (ProjectEntity) -> Unit,
    onCreateQuickProject: (String, String, String) -> Unit
) {
    val completedLessons = progressList.count { it.category == "TUTORIAL" && it.isCompleted }
    val completedExercises = progressList.count { it.category == "EXERCISE" && it.isCompleted }
    val latestQuiz = progressList.firstOrNull { it.category == "QUIZ" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_ide_hero_banner_1794128430146),
                        contentDescription = "Universal Java Studio Hero Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(185.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(185.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xEE0B1120),
                                        Color(0xCC0B1120),
                                        Color(0x880B1120)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.align(Alignment.CenterStart),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "OFFLINE ALL-VERSION JAVA IDE • JDK 1.0 TO JDK 25",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = "Universal Java Studio",
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.White
                            )
                            Text(
                                text = "Active Project: $activeProjectName (JDK $activeJdkVersion) • ${jdks.size} Toolchains Registered",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFCBD5E1)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { onNavigate(StudioModule.WEB_APP) },
                                    modifier = Modifier.testTag("dashboard_open_webapp_btn")
                                ) {
                                    Text("Launch Web App IDE")
                                }
                                OutlinedButton(
                                    onClick = { onNavigate(StudioModule.CODE_EDITOR) },
                                    modifier = Modifier.testTag("dashboard_open_editor_btn")
                                ) {
                                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Native Editor")
                                }
                                OutlinedButton(
                                    onClick = { onNavigate(StudioModule.VERSION_EXPLORER) },
                                    modifier = Modifier.testTag("dashboard_version_explorer_btn")
                                ) {
                                    Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Compare JDKs")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Telemetry KPI Row
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 4
            ) {
                MetricStatCard(
                    title = "Active JDK",
                    value = "JDK $activeJdkVersion",
                    subtitle = "Bytecode v${JdkCatalogAndSpecifications.getSpec(activeJdkVersion).bytecodeMajor}.0",
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Installed JDKs",
                    value = "${jdks.size}",
                    subtitle = "JDK 1.0 – JDK 25",
                    accent = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Learning Lab",
                    value = "${completedLessons + completedExercises} Done",
                    subtitle = "$completedLessons Lessons • $completedExercises Labs",
                    accent = IdeEmeraldSuccess,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Quiz Score",
                    value = if (latestQuiz != null) "${latestQuiz.quizScore}/${latestQuiz.quizTotal}" else "Ready",
                    subtitle = "SQLite Tracked",
                    accent = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick-Access IDE Modules Grid
        item {
            Text(
                text = "IDE & Learning Modules",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 3
            ) {
                QuickModuleCard(
                    title = "Java Code Editor",
                    subtitle = "Version-aware syntax highlighting & multi-tab editor",
                    icon = Icons.Default.Code,
                    tag = "quick_card_editor",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(StudioModule.CODE_EDITOR) }
                )
                QuickModuleCard(
                    title = "Universal JDK Manager",
                    subtitle = "Configure & inspect JDK 1.0 through JDK 25 toolchains",
                    icon = Icons.Default.Memory,
                    tag = "quick_card_jdk_manager",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(StudioModule.JDK_MANAGER) }
                )
                QuickModuleCard(
                    title = "Java Version Explorer",
                    subtitle = "Side-by-side Language, API, GC & Bytecode comparison",
                    icon = Icons.Default.CompareArrows,
                    tag = "quick_card_version_explorer",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(StudioModule.VERSION_EXPLORER) }
                )
                QuickModuleCard(
                    title = "Offline Java Tutorials",
                    subtitle = "IIT Kharagpur & Oracle SE 7/Modern 12-part chapters",
                    icon = Icons.Default.School,
                    tag = "quick_card_tutorials",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(StudioModule.TUTORIALS) }
                )
                QuickModuleCard(
                    title = "Offline API Reference",
                    subtitle = "Version-filtered Javadoc browser (never leaks newer APIs)",
                    icon = Icons.Default.LibraryBooks,
                    tag = "quick_card_api_docs",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(StudioModule.API_DOCS) }
                )
                QuickModuleCard(
                    title = "Practice Laboratory",
                    subtitle = "25 categories with instant Compile & Run execution",
                    icon = Icons.Default.Build,
                    tag = "quick_card_practice_lab",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(StudioModule.PRACTICE_LAB) }
                )
                QuickModuleCard(
                    title = "Quizzes & Assessments",
                    subtitle = "MCQ, Output Prediction & Version Error Diagnostics",
                    icon = Icons.Default.Quiz,
                    tag = "quick_card_quiz",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(StudioModule.QUIZ) }
                )
                QuickModuleCard(
                    title = "Project Manager",
                    subtitle = "Per-project remembered JDK (1.2, 1.4, 8, 17, 21, 25)",
                    icon = Icons.Default.FolderOpen,
                    tag = "quick_card_projects",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(StudioModule.PROJECTS) }
                )
                QuickModuleCard(
                    title = "Offline Resource Manager",
                    subtitle = "Import & manage offline HTML/API/PDF bundles",
                    icon = Icons.Default.Storage,
                    tag = "quick_card_resources",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(StudioModule.RESOURCES) }
                )
            }
        }

        // Multi-Version Projects Table (Demonstrating per-project remembered JDK!)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Multi-Version Java Projects (Per-Project JDK Memory)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Tap any project to load its dedicated Java version and source files into the IDE",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { onNavigate(StudioModule.PROJECTS) },
                            modifier = Modifier.testTag("manage_all_projects_btn")
                        ) {
                            Text("Manage Projects")
                        }
                    }

                    projects.forEach { proj ->
                        val isSelected = proj.name == activeProjectName
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    onSelectProject(proj)
                                    onNavigate(StudioModule.CODE_EDITOR)
                                }
                                .testTag("dashboard_project_${proj.name.replace(" ", "_")}"),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = proj.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = proj.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                               Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "JDK ${proj.jdkVersion}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Compilation & Execution Results
        if (historyList.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Recent Compiler & Runtime Activity",
                            style = MaterialTheme.typography.titleMedium
                        )
                        historyList.take(5).forEach { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "[${entry.actionType}] ${entry.projectName} (${entry.fileName}) • JDK ${entry.jdkVersion}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = "${entry.status} (${entry.executionTimeMs}ms)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (entry.status == "SUCCESS") IdeEmeraldSuccess else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuickModuleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(tag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectManagementScreen(
    projects: List<ProjectEntity>,
    activeProjectId: Long,
    onSelectProjectAndOpenEditor: (ProjectEntity) -> Unit,
    onCreateProject: (String, String, String) -> Unit,
    onDeleteProject: (Long) -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var selectedJdk by remember { mutableStateOf("21") }
    var selectedTemplate by remember { mutableStateOf("Console Application") }
    var jdkDropdownExpanded by remember { mutableStateOf(false) }

    val allVersions = JdkCatalogAndSpecifications.allVersionSpecs.map { it.versionId }
    val templates = listOf("Console Application", "Historical Java 2 (JDK 1.2)", "OOP & Assertions", "Modern Records & Virtual Threads")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("projects_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Create New Multi-Version Java Project",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Each project permanently remembers its assigned JDK version, compiler flags, and source files in SQLite (Room).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Project Name") },
                            placeholder = { Text("e.g., LegacyAppletLab or ModernServer") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_project_name_input")
                        )
                        Box {
                            OutlinedButton(
                                onClick = { jdkDropdownExpanded = true },
                                modifier = Modifier.testTag("new_project_jdk_selector")
                            ) {
                                Text("Project JDK: $selectedJdk")
                            }
                            DropdownMenu(
                                expanded = jdkDropdownExpanded,
                                onDismissRequest = { jdkDropdownExpanded = false }
                            ) {
                                allVersions.forEach { ver ->
                                    DropdownMenuItem(
                                        text = { Text("JDK $ver (${JdkCatalogAndSpecifications.getSpec(ver).codename})") },
                                        onClick = {
                                            selectedJdk = ver
                                            jdkDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        templates.forEach { tmpl ->
                            FilterChip(
                                selected = selectedTemplate == tmpl,
                                onClick = { selectedTemplate = tmpl },
                                label = { Text(tmpl) }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onCreateProject(
                                newName.ifBlank { "JavaProject_JDK$selectedJdk" },
                                selectedJdk,
                                selectedTemplate
                            )
                            newName = ""
                        },
                        modifier = Modifier.testTag("create_project_submit_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create & Open Project in Editor")
                    }
                }
            }
        }

        item {
            Text(
                text = "Configured Workspace Projects (${projects.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        items(projects, key = { it.id }) { proj ->
            val isActive = proj.id == activeProjectId
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isActive) 2.dp else 0.dp,
                        color = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = proj.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "JDK ${proj.jdkVersion}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(text = proj.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "Entry Class: ${proj.mainClassName} • Flags: ${proj.compilerFlags}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { onSelectProjectAndOpenEditor(proj) },
                            modifier = Modifier.testTag("open_project_btn_${proj.name.replace(" ", "_")}")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Open Project", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open")
                        }
                        if (projects.size > 1) {
                            IconButton(
                                onClick = { onDeleteProject(proj.id) },
                                modifier = Modifier.testTag("delete_project_btn_${proj.id}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Project", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
