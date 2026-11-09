package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ApiClassDoc
import com.example.core.JavaCompilerAndExecutionEngine
import com.example.core.JdkCatalogAndSpecifications
import com.example.core.KnowledgeBaseCatalog
import com.example.core.TutorialLesson
import com.example.core.VersionSyntaxHighlighter
import com.example.data.local.LearningProgressEntity
import com.example.ui.theme.IdeEmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SyntaxDeprecated
import com.example.ui.theme.SyntaxKeywordModern
import com.example.ui.theme.SyntaxUnsupportedError

private data class IitOracleCurriculumMeta(
    val iitModuleRef: String,
    val oracleTrailRef: String,
    val jvmEfficiencyTip: String
)

private fun getIitAndOracleMetadata(lessonId: String): IitOracleCurriculumMeta = when (lessonId) {
    "tut_ch1_fundamentals" -> IitOracleCurriculumMeta(
        iitModuleRef = "IIT Kharagpur NPTEL Module 1 (Week 1): Java Architecture & Bytecode Verification",
        oracleTrailRef = "Oracle Java SE Trail: Getting Started & The Java Technology Phenomenon",
        jvmEfficiencyTip = "HotSpot JIT Tiered Compilation (C1 + C2) profiles frequently executed loops and compiles hot bytecode paths into native machine instructions."
    )
    "tut_ch2_datatypes" -> IitOracleCurriculumMeta(
        iitModuleRef = "IIT Kharagpur NPTEL Module 2 (Week 2): Primitive Types, IEEE 754 & Arrays",
        oracleTrailRef = "Oracle Java SE Trail: Learning the Java Language — Language Basics",
        jvmEfficiencyTip = "Prefer primitive types (int, long, double) over boxed wrappers (Integer, Double) in tight loops to avoid 16-byte object header overhead and GC churn."
    )
    "tut_ch3_oop" -> IitOracleCurriculumMeta(
        iitModuleRef = "IIT Kharagpur NPTEL Module 3 (Week 3–4): Encapsulation, Inheritance & Dynamic Dispatch",
        oracleTrailRef = "Oracle Java SE Trail: Classes and Objects & Interfaces and Inheritance",
        jvmEfficiencyTip = "JVM Escape Analysis performs scalar replacement on short-lived local objects, and Records (JDK 16+) generate compact, immutable value-like carriers."
    )
    "tut_ch4_packages" -> IitOracleCurriculumMeta(
        iitModuleRef = "IIT Kharagpur NPTEL Module 4 (Week 5): Packages, Access Control & Interfaces",
        oracleTrailRef = "Oracle Java SE Trail: Interfaces, Packages & Java Platform Module System (JPMS)",
        jvmEfficiencyTip = "Default interface methods use invokeinterface vtable dispatch; JPMS (JDK 9+) enables jlink custom runtime images with 60% smaller memory footprints."
    )
    "tut_ch5_exceptions" -> IitOracleCurriculumMeta(
        iitModuleRef = "IIT Kharagpur NPTEL Module 5 (Week 6): Exception Hierarchy & Resource Safety",
        oracleTrailRef = "Oracle Java SE Trail: Essential Java Classes — Exceptions & Try-With-Resources",
        jvmEfficiencyTip = "Throwable.fillInStackTrace() walks the call stack (O(depth)); never use exceptions for normal control flow, and always use try-with-resources (JDK 7+)."
    )
    "tut_ch6_multithreading" -> IitOracleCurriculumMeta(
        iitModuleRef = "IIT Kharagpur NPTEL Module 6 (Week 7): Multithreading, Monitors & Synchronization",
        oracleTrailRef = "Oracle Java SE Trail: Concurrency, ExecutorService & Virtual Threads (JEP 444)",
        jvmEfficiencyTip = "Platform threads consume ~1 MB OS stack each; JDK 21+ Virtual Threads (Thread.ofVirtual()) use heap-allocated continuations (~1 KB) for millions of concurrent tasks."
    )
    "tut_ch7_awt" -> IitOracleCurriculumMeta(
        iitModuleRef = "IIT Kharagpur NPTEL Module 7 (Week 8): AWT Components, Event Delegation & Graphics",
        oracleTrailRef = "Oracle Java SE 7 API: java.awt & java.awt.event Event Listener Model",
        jvmEfficiencyTip = "Never run blocking I/O on the AWT EventQueue dispatch thread; offload computation to background worker threads to keep UI rendering under 16ms/frame."
    )
    "tut_ch8_io_nio" -> IitOracleCurriculumMeta(
        iitModuleRef = "IIT Kharagpur NPTEL Module 8 (Week 9): Java I/O Streams, Serialization & NIO Channels",
        oracleTrailRef = "Oracle Java SE Trail: Basic I/O (java.io & java.nio.file.Files)",
        jvmEfficiencyTip = "Unbuffered FileInputStream.read() triggers a kernel syscall per byte; wrapping in BufferedInputStream (8 KB buffer) or NIO FileChannel improves throughput by >25x."
    )
    "tut_ch9_applets" -> IitOracleCurriculumMeta(
        iitModuleRef = "IIT Kharagpur NPTEL Module 9 (Week 10): Legacy Applet Lifecycle & Security Sandbox",
        oracleTrailRef = "Oracle Java SE 7 API: java.applet.Applet (@Deprecated in JDK 9, Removed in JDK 17+)",
        jvmEfficiencyTip = "Applets and SecurityManager are obsolete in modern Java; migrate educational visualizations to standalone Swing JFrame, JavaFX, or Web applications."
    )
    "tut_ch10_swing" -> IitOracleCurriculumMeta(
        iitModuleRef = "IIT Kharagpur NPTEL Module 10 (Week 10): JFC Swing Lightweight Architecture & MVC",
        oracleTrailRef = "Oracle Java SE Trail: Creating a GUI With JFC/Swing (javax.swing)",
        jvmEfficiencyTip = "Swing double-buffers repaints in JVM heap memory; always construct and mutate JComponent trees inside SwingUtilities.invokeLater() on the EDT."
    )
    "tut_ch11_net_jdbc" -> IitOracleCurriculumMeta(
        iitModuleRef = "IIT Kharagpur NPTEL Module 11 (Week 11): TCP/UDP Sockets, URLConnection & JDBC",
        oracleTrailRef = "Oracle Java SE Trail: Custom Networking, java.net.http.HttpClient & JDBC Basics",
        jvmEfficiencyTip = "Always use PreparedStatement instead of Statement string concatenation to prevent SQL injection and reuse pre-compiled database execution plans."
    )
    else -> IitOracleCurriculumMeta(
        iitModuleRef = "IIT Kharagpur NPTEL Module 12 (Week 12): Collections Complexity, Streams & Modern Java",
        oracleTrailRef = "Oracle Java SE Trail: Collections Framework, Stream API & JDK 17–25 LTS Enhancements",
        jvmEfficiencyTip = "Prefer ArrayList (O(1) unsynchronized access) over legacy Vector (synchronized per call), and StringBuilder over String '+' inside loops (avoids O(N²) allocations)."
    )
}

private fun runInlineSnippet(jdkVersion: String, code: String): String {
    val spec = JdkCatalogAndSpecifications.getSpec(jdkVersion)
    val jdk = com.example.data.local.JdkInstallationEntity(
        jdkVersion = spec.versionId,
        displayName = spec.displayName,
        vendor = "Oracle / OpenJDK",
        javaHome = "/opt/jdks/jdk-${spec.versionId}",
        compilerPath = "/opt/jdks/jdk-${spec.versionId}/bin/javac",
        runtimePath = "/opt/jdks/jdk-${spec.versionId}/bin/java",
        compilerVersion = "javac ${spec.versionId}",
        runtimeVersion = "java ${spec.versionId}",
        bytecodeMajorVersion = spec.bytecodeMajor,
        architecture = "64-bit x86_64 / ARM64",
        status = "Available",
        compatibilityLevel = "Supported (Full AST + Toolchain)"
    )
    val classMatch = Regex("""\bclass\s+([A-Za-z_][A-Za-z0-9_]*)""").find(code)
    val mainClass = classMatch?.groupValues?.getOrNull(1) ?: "Main"
    val files = mapOf("$mainClass.java" to code)
    val exec = JavaCompilerAndExecutionEngine.executeProgram(
        jdk = jdk,
        files = files,
        mainClass = mainClass
    )
    return if (exec.success) {
        "$ ${exec.runtimeCommand}\n${exec.stdout}"
    } else {
        "Compilation / Execution Error:\n${exec.stderr}"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OfflineTutorialsScreen(
    selectedLesson: TutorialLesson,
    progressList: List<LearningProgressEntity>,
    onSelectLesson: (TutorialLesson) -> Unit,
    onMarkCompleted: (TutorialLesson, Boolean) -> Unit,
    onOpenSnippetInEditor: (String, String, String) -> Unit
) {
    val allLessons = KnowledgeBaseCatalog.tutorialLessons
    val isCompleted = progressList.any { it.itemId == selectedLesson.id && it.isCompleted }
    val curriculumMeta = remember(selectedLesson.id) { getIitAndOracleMetadata(selectedLesson.id) }

    // Editable state for Historical Java 2 (JDK 1.2) and Modern Java (JDK 25) tutorial sandboxes
    var editableJava2Code by remember(selectedLesson.id) { mutableStateOf(selectedLesson.java2ExampleCode) }
    var editableModernCode by remember(selectedLesson.id) { mutableStateOf(selectedLesson.modernJavaExampleCode) }
    var java2InlineOutput by remember(selectedLesson.id) { mutableStateOf<String?>(null) }
    var modernInlineOutput by remember(selectedLesson.id) { mutableStateOf<String?>(null) }

    val java2SyntaxReport = remember(editableJava2Code) {
        VersionSyntaxHighlighter.analyzeAndHighlight(editableJava2Code, "1.2")
    }
    val modernSyntaxReport = remember(editableModernCode) {
        VersionSyntaxHighlighter.analyzeAndHighlight(editableModernCode, "25")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("tutorials_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header & Reference Attribution Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Offline Java Tutorial Platform (Chapters 1–12)",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Structured curriculum organized from IIT Kharagpur NPTEL Programming in Java, Oracle Java SE 7 API, and Oracle Java Tutorials. Every code block below is directly editable and executable offline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allLessons.forEach { lesson ->
                            val done = progressList.any { it.itemId == lesson.id && it.isCompleted }
                            FilterChip(
                                selected = lesson.id == selectedLesson.id,
                                onClick = { onSelectLesson(lesson) },
                                label = {
                                    Text("${if (done) "✓ " else ""}${lesson.chapterTitle.substringBefore("–").trim()}")
                                },
                                modifier = Modifier.testTag("tutorial_chip_${lesson.id}")
                            )
                        }
                    }
                }
            }
        }

        // Lesson 12-Part Structured Content Card + Editable Live Sandboxes
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedLesson.chapterTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = selectedLesson.lessonTitle,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = { onMarkCompleted(selectedLesson, true) },
                            modifier = Modifier.testTag("mark_lesson_complete_btn")
                        ) {
                            Icon(
                                if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Bookmark,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isCompleted) "Completed" else "Complete & Bookmark")
                        }
                    }

                    // Compatible JDK Versions & IIT Kharagpur / Oracle Trail Badges
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Min Required: JDK ${selectedLesson.minJdkVersion}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Compatible: ${selectedLesson.compatibleJdkRange}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = curriculumMeta.iitModuleRef,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // IIT Kharagpur & Oracle JVM Efficiency Highlight Box
                    Surface(
                        color = IdeEmeraldSuccess.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, IdeEmeraldSuccess.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "IIT Kharagpur & Oracle JVM Efficiency Guideline (${curriculumMeta.oracleTrailRef})",
                                style = MaterialTheme.typography.labelMedium,
                                color = IdeEmeraldSuccess,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = curriculumMeta.jvmEfficiencyTip,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    selectedLesson.deprecationNote?.let { depNote ->
                        Surface(
                            color = SyntaxDeprecated.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Deprecation / Obsolescence Notice: $depNote",
                                style = MaterialTheme.typography.bodySmall,
                                color = SyntaxDeprecated,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    TutorialSectionBlock("1. Topic Introduction", selectedLesson.topicIntroduction)
                    TutorialSectionBlock("2. Definition", selectedLesson.definition)
                    TutorialSectionBlock("3. Detailed Explanation", selectedLesson.detailedExplanation)

                    // 4. Syntax
                    Text("4. Syntax", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = selectedLesson.syntaxBlock,
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    // 5. Important Concepts
                    Text("5. Important Concepts", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    selectedLesson.importantConcepts.forEach { concept ->
                        Text("• $concept", style = MaterialTheme.typography.bodySmall)
                    }

                    // 6. Directly Editable & Runnable Side-by-Side Java 2 (JDK 1.2) vs Modern Java (JDK 25) Sandboxes
                    Text(
                        text = "6. Interactive Editable Example Programs (Edit & Run Directly Offline or Load in Main IDE)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Historical Java 2 (JDK 1.2) Editable Sandbox
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Editable Java 2 (JDK 1.2)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Errors: ${java2SyntaxReport.syntaxErrorCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (java2SyntaxReport.syntaxErrorCount > 0) SyntaxUnsupportedError else IdeEmeraldSuccess
                                    )
                                }

                                val java2VisualTransformation = remember(java2SyntaxReport.annotatedCode) {
                                    VisualTransformation {
                                        TransformedText(java2SyntaxReport.annotatedCode, OffsetMapping.Identity)
                                    }
                                }

                                BasicTextField(
                                    value = editableJava2Code,
                                    onValueChange = { editableJava2Code = it },
                                    textStyle = TextStyle(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    ),
                                    visualTransformation = java2VisualTransformation,
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 120.dp)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                        .padding(8.dp)
                                        .testTag("tutorial_java2_editable_input")
                                )

                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = {
                                            java2InlineOutput = runInlineSnippet("1.2", editableJava2Code)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = IdeEmeraldSuccess),
                                        modifier = Modifier.testTag("run_inline_java2_btn")
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Run Inline (1.2)", style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = { editableJava2Code = VersionSyntaxHighlighter.formatJavaCode(editableJava2Code) }
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Format", style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            editableJava2Code = selectedLesson.java2ExampleCode
                                            java2InlineOutput = null
                                        }
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Reset", style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onOpenSnippetInEditor(
                                                "${selectedLesson.lessonTitle} (JDK 1.2)",
                                                "1.2",
                                                editableJava2Code
                                            )
                                        },
                                        modifier = Modifier.testTag("open_java2_example_btn")
                                    ) {
                                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Load in Editor", style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                java2InlineOutput?.let { out ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = out,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = IdeEmeraldSuccess,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Modern Java (JDK 17–25) Editable Sandbox
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Editable Modern Java (JDK 25)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Errors: ${modernSyntaxReport.syntaxErrorCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (modernSyntaxReport.syntaxErrorCount > 0) SyntaxUnsupportedError else IdeEmeraldSuccess
                                    )
                                }

                                val modernVisualTransformation = remember(modernSyntaxReport.annotatedCode) {
                                    VisualTransformation {
                                        TransformedText(modernSyntaxReport.annotatedCode, OffsetMapping.Identity)
                                    }
                                }

                                BasicTextField(
                                    value = editableModernCode,
                                    onValueChange = { editableModernCode = it },
                                    textStyle = TextStyle(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    ),
                                    visualTransformation = modernVisualTransformation,
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 120.dp)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                        .padding(8.dp)
                                        .testTag("tutorial_modern_editable_input")
                                )

                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = {
                                            modernInlineOutput = runInlineSnippet("25", editableModernCode)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = IdeEmeraldSuccess),
                                        modifier = Modifier.testTag("run_inline_modern_btn")
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Run Inline (25)", style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = { editableModernCode = VersionSyntaxHighlighter.formatJavaCode(editableModernCode) }
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Format", style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            editableModernCode = selectedLesson.modernJavaExampleCode
                                            modernInlineOutput = null
                                        }
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Reset", style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onOpenSnippetInEditor(
                                                "${selectedLesson.lessonTitle} (Modern)",
                                                "25",
                                                editableModernCode
                                            )
                                        },
                                        modifier = Modifier.testTag("open_modern_example_btn")
                                    ) {
                                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Load in Editor", style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                modernInlineOutput?.let { out ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = out,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = IdeEmeraldSuccess,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    TutorialSectionBlock("7. Program Explanation", selectedLesson.programExplanation)

                    Text("8. Expected Default Output", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = selectedLesson.sampleOutput,
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.bodySmall,
                            color = IdeEmeraldSuccess,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Text("9. Common Errors", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    selectedLesson.commonErrors.forEach { err ->
                        Text("• $err", style = MaterialTheme.typography.bodySmall)
                    }

                    Text("10. Practice Questions", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    selectedLesson.practiceQuestions.forEachIndexed { idx, q ->
                        Text("${idx + 1}. $q", style = MaterialTheme.typography.bodySmall)
                    }

                    Text(
                        text = "11. Related API Documentation: ${selectedLesson.relatedApis.joinToString(", ")}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorialSectionBlock(heading: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = heading, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Text(text = body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OfflineApiDocumentationScreen(
    selectedJdkFilter: String,
    searchQuery: String,
    filteredClasses: List<ApiClassDoc>,
    selectedClassDoc: ApiClassDoc?,
    onSelectJdkFilter: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectClassDoc: (ApiClassDoc) -> Unit
) {
    var versionMenuExpanded by remember { mutableStateOf(false) }
    val docVersions = listOf("1.0", "1.1", "1.2", "1.3", "1.4", "5", "6", "7", "8", "9", "11", "17", "21", "25")
    val targetRank = JdkCatalogAndSpecifications.versionRank(selectedJdkFilter)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("api_docs_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + Version Selector + Search Input
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
                                text = "Offline Java API Reference Browser (Oracle Java SE Spec)",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Version-Accurate Filter: Showing only classes and methods available in or before JDK $selectedJdkFilter (Never leaks newer APIs into historical Java).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Box {
                            Button(
                                onClick = { versionMenuExpanded = true },
                                modifier = Modifier.testTag("api_version_selector_btn")
                            ) {
                                Text("API Docs Version: JDK $selectedJdkFilter")
                            }
                            DropdownMenu(
                                expanded = versionMenuExpanded,
                                onDismissRequest = { versionMenuExpanded = false }
                            ) {
                                docVersions.forEach { ver ->
                                    DropdownMenuItem(
                                        text = { Text("JDK $ver API Specification") },
                                        onClick = {
                                            onSelectJdkFilter(ver)
                                            versionMenuExpanded = false
                                        },
                                        modifier = Modifier.testTag("api_select_version_$ver")
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        label = { Text("Search packages, classes, interfaces, methods, constructors...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_search_input")
                    )

                    // Quick Class Chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filteredClasses.forEach { cls ->
                            FilterChip(
                                selected = cls.id == selectedClassDoc?.id,
                                onClick = { onSelectClassDoc(cls) },
                                label = { Text("${cls.packageName}.${cls.className} (@since ${cls.sinceJdk})") },
                                modifier = Modifier.testTag("api_class_chip_${cls.className}")
                            )
                        }
                    }
                }
            }
        }

        // Selected Class Detailed Javadoc Reference + Live Editable API Sandbox
        item {
            if (selectedClassDoc == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "No classes matching '$searchQuery' exist in JDK $selectedJdkFilter. Switch to a newer API Documentation version or refine your search.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                val visibleMembers = selectedClassDoc.members.filter {
                    JdkCatalogAndSpecifications.versionRank(it.sinceJdk) <= targetRank
                }

                val defaultApiSnippet = remember(selectedClassDoc.id) {
                    """
                    import ${selectedClassDoc.packageName}.${selectedClassDoc.className};

                    public class ${selectedClassDoc.className}ApiDemo {
                        public static void main(String[] args) {
                            System.out.println("Testing ${selectedClassDoc.packageName}.${selectedClassDoc.className} (@since JDK ${selectedClassDoc.sinceJdk})");
                        }
                    }
                    """.trimIndent()
                }
                var apiEditableCode by remember(selectedClassDoc.id) { mutableStateOf(defaultApiSnippet) }
                var apiSandboxOutput by remember(selectedClassDoc.id) { mutableStateOf<String?>(null) }
                val apiSyntaxReport = remember(apiEditableCode, selectedJdkFilter) {
                    VersionSyntaxHighlighter.analyzeAndHighlight(apiEditableCode, selectedJdkFilter)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_class_detail_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Package ${selectedClassDoc.packageName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "${selectedClassDoc.kind} ${selectedClassDoc.className}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Since JDK ${selectedClassDoc.sinceJdk}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            selectedClassDoc.deprecatedInJdk?.let { depVer ->
                                if (targetRank >= JdkCatalogAndSpecifications.versionRank(depVer)) {
                                    Surface(
                                        color = SyntaxDeprecated.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "@Deprecated (since JDK $depVer)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SyntaxDeprecated,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Inheritance Hierarchy: ${selectedClassDoc.inheritanceHierarchy}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = selectedClassDoc.summary,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        HorizontalDivider()
                        Text(
                            text = "Constructors & Methods Available in JDK $selectedJdkFilter (${visibleMembers.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        visibleMembers.forEach { member ->
                            val isMemberDeprecated = member.deprecatedInJdk != null &&
                                targetRank >= JdkCatalogAndSpecifications.versionRank(member.deprecatedInJdk)
                            Surface(
                                color = MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = member.signature,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isMemberDeprecated) SyntaxDeprecated else MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = "@since ${member.sinceJdk}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = member.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        // Interactive Editable API Sandbox
                        Text(
                            text = "Interactive Editable API Sandbox (Target: JDK $selectedJdkFilter)",
                            style = MaterialTheme.typography.titleSmall,
                            color = SyntaxKeywordModern
                        )
                        val apiVisualTransformation = remember(apiSyntaxReport.annotatedCode) {
                            VisualTransformation {
                                TransformedText(apiSyntaxReport.annotatedCode, OffsetMapping.Identity)
                            }
                        }
                        BasicTextField(
                            value = apiEditableCode,
                            onValueChange = { apiEditableCode = it },
                            textStyle = TextStyle(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            visualTransformation = apiVisualTransformation,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp)
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                                .testTag("api_sandbox_editable_input")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    apiSandboxOutput = runInlineSnippet(selectedJdkFilter, apiEditableCode)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = IdeEmeraldSuccess),
                                modifier = Modifier.testTag("api_sandbox_run_btn")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Compile & Run API Demo (JDK $selectedJdkFilter)")
                            }
                            OutlinedButton(
                                onClick = { apiEditableCode = VersionSyntaxHighlighter.formatJavaCode(apiEditableCode) }
                            ) {
                                Text("Format Code")
                            }
                        }

                        apiSandboxOutput?.let { out ->
                            Surface(
                                color = MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = out,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IdeEmeraldSuccess,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Text(
                            text = "Related Classes: ${selectedClassDoc.relatedClasses.joinToString(", ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}
