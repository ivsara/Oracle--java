package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.JavaCompilerAndExecutionEngine
import com.example.core.KnowledgeBaseCatalog
import com.example.core.PracticeExercise
import com.example.core.VersionSyntaxHighlighter
import com.example.data.local.LearningProgressEntity
import com.example.data.local.OfflineResourceEntity
import com.example.ui.theme.IdeEmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SyntaxUnsupportedError

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JavaPracticeLabScreen(
    selectedExercise: PracticeExercise,
    practiceCode: String,
    practiceOutput: String,
    progressList: List<LearningProgressEntity>,
    onSelectExercise: (PracticeExercise) -> Unit,
    onPracticeCodeChange: (String) -> Unit,
    onCompileAndRun: () -> Unit,
    onOpenInMainEditor: (String, String, String) -> Unit
) {
    val exercises = KnowledgeBaseCatalog.practiceExercises
    var showAllVersionsOutput by remember(selectedExercise.id) { mutableStateOf(true) }

    val autoPracticeOutput = remember(practiceCode, selectedExercise.recommendedJdk, practiceOutput) {
        if (practiceOutput.isNotBlank() && !practiceOutput.startsWith("Click ")) {
            practiceOutput
        } else {
            val cls = Regex("""\bclass\s+([A-Za-z_][A-Za-z0-9_]*)""").find(practiceCode)?.groupValues?.get(1) ?: "Main"
            val jdk = JavaCompilerAndExecutionEngine.buildJdkEntityForVersion(selectedExercise.recommendedJdk)
            val exec = JavaCompilerAndExecutionEngine.executeProgram(
                jdk = jdk,
                files = mapOf("$cls.java" to practiceCode),
                mainClass = cls,
                stdinInput = "JavaStudent 95",
                runtimeArgs = ""
            )
            exec.stdout.ifBlank { selectedExercise.expectedOutput }
        }
    }

    val allVersionsPractice = remember(practiceCode) {
        val cls = Regex("""\bclass\s+([A-Za-z_][A-Za-z0-9_]*)""").find(practiceCode)?.groupValues?.get(1) ?: "Main"
        JavaCompilerAndExecutionEngine.runAcrossAllJdkVersions(
            files = mapOf("$cls.java" to practiceCode),
            mainClass = cls,
            stdinInput = "JavaStudent 95",
            runtimeArgs = ""
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("practice_lab_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Java Practice Laboratory (25 Curriculum Categories)",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Modify example source code and view live execution output across all 26 JDK versions (JDK 1.0–25) offline, or open it in the full IDE.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        exercises.forEach { ex ->
                            val done = progressList.any { it.itemId == ex.id && it.isCompleted }
                            FilterChip(
                                selected = ex.id == selectedExercise.id,
                                onClick = { onSelectExercise(ex) },
                                label = { Text("${if (done) "✓ " else ""}${ex.categoryName}") },
                                modifier = Modifier.testTag("exercise_chip_${ex.id}")
                            )
                        }
                    }
                }
            }
        }

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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedExercise.categoryName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = selectedExercise.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Compatible: ${selectedExercise.compatibleJdkVersions}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text("Problem Statement:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(selectedExercise.problemStatement, style = MaterialTheme.typography.bodyMedium)

                    Text("Algorithm:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    selectedExercise.algorithmSteps.forEachIndexed { idx, step ->
                        Text("${idx + 1}. $step", style = MaterialTheme.typography.bodySmall)
                    }

                    val practiceSyntaxReport = remember(practiceCode, selectedExercise.recommendedJdk) {
                        VersionSyntaxHighlighter.analyzeAndHighlight(practiceCode, selectedExercise.recommendedJdk)
                    }
                    val practiceVisualTransformation = remember(practiceSyntaxReport.annotatedCode) {
                        VisualTransformation {
                            TransformedText(practiceSyntaxReport.annotatedCode, OffsetMapping.Identity)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Interactive Editable Exercise Code (Target JDK ${selectedExercise.recommendedJdk}):",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Syntax Errors: ${practiceSyntaxReport.syntaxErrorCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (practiceSyntaxReport.syntaxErrorCount > 0) SyntaxUnsupportedError else IdeEmeraldSuccess
                        )
                    }

                    BasicTextField(
                        value = practiceCode,
                        onValueChange = onPracticeCodeChange,
                        textStyle = TextStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        visualTransformation = practiceVisualTransformation,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp)
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                            .testTag("practice_code_editor")
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onCompileAndRun,
                            colors = ButtonDefaults.buttonColors(containerColor = IdeEmeraldSuccess),
                            modifier = Modifier.testTag("practice_compile_run_btn")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Compile & Run (JDK ${selectedExercise.recommendedJdk})")
                        }
                        OutlinedButton(
                            onClick = { showAllVersionsOutput = !showAllVersionsOutput },
                            modifier = Modifier.testTag("practice_all_versions_btn")
                        ) {
                            Text(if (showAllVersionsOutput) "Hide 26-Version Matrix" else "All 26 Versions Output (1.0–25)")
                        }
                        OutlinedButton(
                            onClick = { onPracticeCodeChange(VersionSyntaxHighlighter.formatJavaCode(practiceCode)) },
                            modifier = Modifier.testTag("practice_format_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Format Code")
                        }
                        OutlinedButton(
                            onClick = {
                                onOpenInMainEditor(
                                    selectedExercise.title,
                                    selectedExercise.recommendedJdk,
                                    practiceCode
                                )
                            },
                            modifier = Modifier.testTag("practice_open_in_ide_btn")
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open in Main Code Editor")
                        }
                        OutlinedButton(
                            onClick = { onPracticeCodeChange(selectedExercise.starterJavaCode) }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Code")
                        }
                    }

                    Text("Live Execution Output (JDK ${selectedExercise.recommendedJdk}):", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = autoPracticeOutput,
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.bodySmall,
                            color = IdeEmeraldSuccess,
                            modifier = Modifier
                                .padding(12.dp)
                                .testTag("practice_output_text")
                        )
                    }

                    if (showAllVersionsOutput) {
                        Text(
                            text = "Output Across All 26 JDK Versions (JDK 1.0 – JDK 25):",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                allVersionsPractice.forEach { verSummary ->
                                    Text(
                                        text = "JDK ${verSummary.jdkVersion} (${verSummary.bytecodeVersion}): ${verSummary.output.replace("\n", " | ")}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (verSummary.compatible) IdeEmeraldSuccess else SyntaxUnsupportedError
                                    )
                                }
                            }
                        }
                    }

                    Text("Expected Output:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = selectedExercise.expectedOutput,
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Text("Explanation: ${selectedExercise.explanation}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun QuizAssessmentScreen(
    selectedAnswers: Map<String, Int>,
    quizSubmitted: Boolean,
    progressList: List<LearningProgressEntity>,
    onSelectAnswer: (String, Int) -> Unit,
    onSubmitQuiz: () -> Unit,
    onResetQuiz: () -> Unit
) {
    val questions = KnowledgeBaseCatalog.quizQuestions
    val correctCount = questions.count { selectedAnswers[it.id] == it.correctIndex }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("quiz_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Java Assessment & Version-Specific Quiz Platform",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Includes Multiple-Choice, Fill-in-the-Blank, Output Prediction, and Find-the-Error questions across JDK 1.2 to JDK 25. Progress is persisted to SQLite via Room.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (quizSubmitted) {
                        Surface(
                            color = IdeEmeraldSuccess.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quiz_score_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Assessment Score: $correctCount / ${questions.size} (${(correctCount * 100) / questions.size}%)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = IdeEmeraldSuccess,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedButton(onClick = onResetQuiz) {
                                    Text("Retry Quiz")
                                }
                            }
                        }
                    }
                }
            }
        }

        items(questions, key = { it.id }) { q ->
            val chosen = selectedAnswers[q.id]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quiz_card_${q.id}"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "[${q.questionType}] ${q.chapterOrVersion}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Target: JDK ${q.targetJdk}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Text(text = q.prompt, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                    q.codeSnippet?.let { snippet ->
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = snippet,
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    q.options.forEachIndexed { idx, optText ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = chosen == idx,
                                onClick = { onSelectAnswer(q.id, idx) },
                                modifier = Modifier.testTag("quiz_${q.id}_opt_$idx")
                            )
                            Text(
                                text = optText,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (quizSubmitted) {
                        val isRight = chosen == q.correctIndex
                        Surface(
                            color = (if (isRight) IdeEmeraldSuccess else MaterialTheme.colorScheme.error).copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = (if (isRight) "✓ Correct! " else "✗ Incorrect. ") + q.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onSubmitQuiz,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("submit_quiz_btn")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submit Assessment & Save Progress to SQLite")
            }
        }
    }
}

@Composable
fun OfflineResourceManagerScreen(
    resources: List<OfflineResourceEntity>,
    onImportResource: (String, String, String, String, String) -> Unit,
    onRemoveResource: (Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var resType by remember { mutableStateOf("API Documentation") }
    var jdkScope by remember { mutableStateOf("JDK 1.2 – JDK 25") }
    var localPath by remember { mutableStateOf("resources/api_docs/java_17/index.html") }
    var attribution by remember { mutableStateOf("Legally Obtained Local Javadoc Archive") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("offline_resources_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Offline Resource Manager (100% Air-Gapped Ready)",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Manage offline HTML tutorials, versioned Java API documentation (JDK 1.0–25), PDF reference guides, and source code packs without requiring an internet connection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "resources/\n  ├── tutorials/ (IIT Kharagpur & Oracle Trails)\n  ├── api_docs/ (java_1_0 .. java_1_2 .. java_7 .. java_8 .. java_17 .. java_21 .. java_25)\n  ├── examples/ (25 Practice Categories)\n  └── icons/",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    HorizontalDivider()
                    Text("Import Additional Offline Resource Bundle", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Resource Title") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_resource_title_input")
                        )
                        OutlinedTextField(
                            value = jdkScope,
                            onValueChange = { jdkScope = it },
                            label = { Text("JDK Version Scope") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = localPath,
                            onValueChange = { localPath = it },
                            label = { Text("Local Relative Path") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = attribution,
                            onValueChange = { attribution = it },
                            label = { Text("License / Attribution") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = {
                            onImportResource(
                                title.ifBlank { "Imported Java Documentation Bundle" },
                                resType,
                                jdkScope,
                                localPath,
                                attribution
                            )
                            title = ""
                        },
                        modifier = Modifier.testTag("import_resource_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import Offline Resource")
                    }
                }
            }
        }

        // Room Database Schema Inspector Card (Projects, JDKInstallations, LearningProgress)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("room_schema_inspector_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Initialized Room SQLite Database Schema (universal_java_studio.db)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    com.example.data.local.RoomSchemaRegistry.tables.forEach { tbl ->
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = "@Entity(tableName = \"${tbl.tableName}\") — ${tbl.entityClassName} [PK: ${tbl.primaryKey}]",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = IdeEmeraldSuccess
                                )
                                Text(
                                    text = "Columns: ${tbl.columns.joinToString(", ")}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = tbl.purpose,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        items(resources, key = { it.id }) { res ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = res.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${res.resourceType} • Scope: ${res.jdkVersionScope} • Status: ${res.status}",
                            style = MaterialTheme.typography.labelSmall,
                            color = IdeEmeraldSuccess
                        )
                        Text(
                            text = "Path: ${res.localPath} | Attribution: ${res.attribution}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onRemoveResource(res.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove Resource", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
