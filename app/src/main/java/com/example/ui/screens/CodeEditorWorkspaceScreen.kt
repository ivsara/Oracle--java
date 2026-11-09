package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import com.example.core.CompilationResult
import com.example.core.DiagnosticCategory
import com.example.core.ExecutionResult
import com.example.core.JdkCatalogAndSpecifications
import com.example.core.SyntaxAnalysisReport
import com.example.ui.theme.IdeEmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SyntaxDeprecated
import com.example.ui.theme.SyntaxKeywordModern
import com.example.ui.theme.SyntaxUnsupportedError

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CodeEditorWorkspaceScreen(
    projectName: String,
    activeJdkVersion: String,
    openFiles: Map<String, String>,
    activeFileName: String,
    editorCode: String,
    syntaxReport: SyntaxAnalysisReport,
    compilationResult: CompilationResult?,
    executionResult: ExecutionResult?,
    stdinInput: String,
    runtimeArgs: String,
    compilerFlags: String,
    activeConsoleTab: Int,
    findQuery: String,
    replaceQuery: String,
    autoSaveStatus: String = "Auto-Save Active (2s debounce)",
    onCodeChange: (String) -> Unit,
    onSelectTab: (String) -> Unit,
    onAddClassTab: (String) -> Unit,
    onSwitchJdkVersion: (String) -> Unit,
    onFormatCode: () -> Unit = {},
    onSaveProject: () -> Unit,
    onCompile: () -> Unit,
    onRun: () -> Unit,
    onClean: () -> Unit,
    onStdinChange: (String) -> Unit,
    onRuntimeArgsChange: (String) -> Unit,
    onCompilerFlagsChange: (String) -> Unit,
    onSelectConsoleTab: (Int) -> Unit,
    onFindReplaceChange: (String, String) -> Unit,
    onReplaceAll: () -> Unit
) {
    var jdkMenuExpanded by remember { mutableStateOf(false) }
    var showFindBar by remember { mutableStateOf(false) }
    var showNewFilePrompt by remember { mutableStateOf(false) }
    var newFileNameText by remember { mutableStateOf("") }

    // Undo / Redo History Stack for Real-World IDE Editing
    val undoStack = remember(activeFileName) { mutableStateListOf<String>() }
    val redoStack = remember(activeFileName) { mutableStateListOf<String>() }

    LaunchedEffect(activeFileName) {
        if (undoStack.isEmpty()) {
            undoStack.add(editorCode)
        }
    }

    fun pushCodeUpdate(newCode: String) {
        if (newCode != editorCode) {
            if (undoStack.isEmpty() || undoStack.last() != editorCode) {
                undoStack.add(editorCode)
                if (undoStack.size > 40) undoStack.removeAt(0)
            }
            redoStack.clear()
            onCodeChange(newCode)
        }
    }

    val allVersions = JdkCatalogAndSpecifications.allVersionSpecs.map { it.versionId }
    val editorScrollState = rememberScrollState()
    val consoleScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
            .testTag("code_editor_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Top IDE Action & Version Control Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = projectName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        // SELECT JAVA VERSION Dropdown
                        Box {
                            OutlinedButton(
                                onClick = { jdkMenuExpanded = true },
                                modifier = Modifier.testTag("editor_jdk_selector_button")
                            ) {
                                Text("SELECT JAVA VERSION: JDK $activeJdkVersion")
                            }
                            DropdownMenu(
                                expanded = jdkMenuExpanded,
                                onDismissRequest = { jdkMenuExpanded = false }
                            ) {
                                allVersions.forEach { ver ->
                                    val spec = JdkCatalogAndSpecifications.getSpec(ver)
                                    DropdownMenuItem(
                                        text = {
                                            Text("JDK $ver (${spec.codename} • v${spec.bytecodeMajor}.${spec.bytecodeMinor})")
                                        },
                                        onClick = {
                                            onSwitchJdkVersion(ver)
                                            jdkMenuExpanded = false
                                        },
                                        modifier = Modifier.testTag("editor_select_jdk_$ver")
                                    )
                                }
                            }
                        }
                    }

                    // Build & Run Actions
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = onCompile,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.testTag("editor_compile_button")
                        ) {
                            Icon(Icons.Default.Build, contentDescription = "Compile", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Compile")
                        }
                        Button(
                            onClick = onRun,
                            colors = ButtonDefaults.buttonColors(containerColor = IdeEmeraldSuccess),
                            modifier = Modifier.testTag("editor_run_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Run")
                        }
                        OutlinedButton(
                            onClick = onFormatCode,
                            modifier = Modifier.testTag("editor_format_code_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, contentDescription = "Format Code", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Format Code")
                        }
                        OutlinedButton(
                            onClick = onSaveProject,
                            modifier = Modifier.testTag("editor_save_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }
                        OutlinedButton(
                            onClick = onClean,
                            modifier = Modifier.testTag("editor_clean_button")
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = "Clean", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clean")
                        }
                        OutlinedButton(
                            onClick = { showFindBar = !showFindBar },
                            modifier = Modifier.testTag("editor_find_replace_toggle")
                        ) {
                            Icon(Icons.Default.FindReplace, contentDescription = "Find & Replace", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Version-Specific Syntax Lexer & 2s Auto-Save Status Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("syntax_highlighter_status_bar")
                ) {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = syntaxReport.lexerProfileName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            color = (if (autoSaveStatus.startsWith("Typing")) MaterialTheme.colorScheme.primary else IdeEmeraldSuccess).copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    (if (autoSaveStatus.startsWith("Typing")) MaterialTheme.colorScheme.primary else IdeEmeraldSuccess).copy(alpha = 0.45f),
                                    RoundedCornerShape(6.dp)
                                )
                                .testTag("auto_save_status_badge")
                        ) {
                            Text(
                                text = autoSaveStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (autoSaveStatus.startsWith("Typing")) MaterialTheme.colorScheme.primary else IdeEmeraldSuccess,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        SyntaxCounterBadge(
                            label = "Syntax Errors: ${syntaxReport.syntaxErrorCount}",
                            color = if (syntaxReport.syntaxErrorCount > 0) SyntaxUnsupportedError else IdeEmeraldSuccess
                        )
                        SyntaxCounterBadge(
                            label = "Classic: ${syntaxReport.classicKeywordCount}",
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        SyntaxCounterBadge(
                            label = "Modern (JDK 10–25): ${syntaxReport.modernFeatureCount}",
                            color = SyntaxKeywordModern
                        )
                        SyntaxCounterBadge(
                            label = "Deprecated: ${syntaxReport.deprecatedFeatureCount}",
                            color = SyntaxDeprecated
                        )
                        SyntaxCounterBadge(
                            label = "Unsupported in JDK $activeJdkVersion: ${syntaxReport.unsupportedFeatureCount}",
                            color = SyntaxUnsupportedError
                        )
                    }
                }

                // Optional Find & Replace Bar
                if (showFindBar) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = findQuery,
                            onValueChange = { onFindReplaceChange(it, replaceQuery) },
                            label = { Text("Find") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = replaceQuery,
                            onValueChange = { onFindReplaceChange(findQuery, it) },
                            label = { Text("Replace With") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = onReplaceAll) {
                            Text("Replace All")
                        }
                    }
                }
            }
        }

        // 2. File Tabs + Quick Syntax Showcase Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            openFiles.keys.forEach { fileName ->
                val isSelected = fileName == activeFileName
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectTab(fileName) },
                    label = { Text(fileName, fontFamily = JetBrainsMonoFontFamily) },
                    modifier = Modifier.testTag("file_tab_$fileName")
                )
            }
            OutlinedButton(
                onClick = { showNewFilePrompt = !showNewFilePrompt },
                modifier = Modifier.testTag("add_file_tab_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Java File", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Class")
            }
        }

        if (showNewFilePrompt) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newFileNameText,
                    onValueChange = { newFileNameText = it },
                    label = { Text("New Java Class Name (e.g., Calculator.java)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (newFileNameText.isNotBlank()) {
                            onAddClassTab(newFileNameText)
                            newFileNameText = ""
                            showNewFilePrompt = false
                        }
                    }
                ) {
                    Text("Add File")
                }
            }
        }

        // 3. QScintilla-Equivalent Version-Aware Code Editor Canvas
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Quick Syntax Highlighting Test Presets Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Syntax Highlight Presets:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            pushCodeUpdate(
                                """
                                import java.util.Vector;

                                public class HelloWorld {
                                    public static strictfp void main(String args[]) {
                                        System.out.println("Hello World from Java 2 (JDK 1.2)");
                                        Vector v = new Vector();
                                        v.addElement(new Integer(42));
                                        System.out.println("Element: " + v.elementAt(0));
                                    }
                                }
                                """.trimIndent()
                            )
                        },
                        label = { Text("Java 2 (strictfp / Vector)") },
                        modifier = Modifier.testTag("preset_java2_btn")
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            pushCodeUpdate(
                                """
                                import module java.base;

                                public class ModernShowcase {
                                    /// Markdown Javadoc (JDK 23)
                                    public record Metric(String name, int score) {}

                                    public static void main(String[] args) {
                                        var m = new Metric("JDK 25 Compiler", 100);
                                        String badge = switch (m.score()) {
                                            case 100 -> "GOLD_MASTER";
                                            default -> "PASS";
                                        };
                                        System.out.println(m.name() + " -> " + badge);
                                    }
                                }
                                """.trimIndent()
                            )
                        },
                        label = { Text("Modern (var / record / switch / module)") },
                        modifier = Modifier.testTag("preset_modern_btn")
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            pushCodeUpdate(
                                """
                                import java.applet.Applet;

                                public class LegacyDeprecationCheck {
                                    public static void main(String[] args) {
                                        // Deprecated APIs highlighted in JDK 9 / 17 / 24+:
                                        SecurityManager sm = null;
                                        Integer boxed = new Integer(99);
                                        System.out.println("Check Applet, SecurityManager & Thread.stop() highlights: " + boxed);
                                    }
                                }
                                """.trimIndent()
                            )
                        },
                        label = { Text("Deprecated APIs (Applet / SecurityManager)") },
                        modifier = Modifier.testTag("preset_deprecated_btn")
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            pushCodeUpdate(
                                """
                                public class SyntaxErrorDemo {
                                    public static void main(String[] args) {
                                        int missingSemicolon = 42
                                        System.out.println("Missing semicolon on line above and below")
                                        if (missingSemicolon > 10) {
                                            System.out.println("Unclosed brace and mismatched parenthesis");
                                    }
                                """.trimIndent()
                            )
                        },
                        label = { Text("Test Syntax Errors (Missing ';' & '{')") },
                        modifier = Modifier.testTag("preset_syntax_error_btn")
                    )
                }

                // Real-World IDE Developer Edit & Snippet Toolbar (Undo, Redo, Live Snippets)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "IDE Edit & Snippets:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedButton(
                        onClick = {
                            if (undoStack.size > 1) {
                                redoStack.add(editorCode)
                                val prev = undoStack.removeAt(undoStack.lastIndex)
                                onCodeChange(prev)
                            }
                        },
                        enabled = undoStack.size > 1,
                        modifier = Modifier.testTag("editor_undo_btn")
                    ) {
                        Text("Undo", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = {
                            if (redoStack.isNotEmpty()) {
                                undoStack.add(editorCode)
                                val next = redoStack.removeAt(redoStack.lastIndex)
                                onCodeChange(next)
                            }
                        },
                        enabled = redoStack.isNotEmpty(),
                        modifier = Modifier.testTag("editor_redo_btn")
                    ) {
                        Text("Redo", style = MaterialTheme.typography.labelSmall)
                    }
                    FilterChip(
                        selected = false,
                        onClick = {
                            pushCodeUpdate(
                                editorCode.trimEnd() + "\n    public static void main(String[] args) {\n        System.out.println(\"Ready\");\n    }\n}"
                            )
                        },
                        label = { Text("+ psvm") },
                        modifier = Modifier.testTag("snippet_psvm_btn")
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            val idx = editorCode.lastIndexOf('}')
                            val updated = if (idx > 0) {
                                editorCode.substring(0, idx) + "    System.out.println(\"Output value\");\n" + editorCode.substring(idx)
                            } else {
                                editorCode + "\nSystem.out.println(\"Output value\");"
                            }
                            pushCodeUpdate(updated)
                        },
                        label = { Text("+ sout") },
                        modifier = Modifier.testTag("snippet_sout_btn")
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            val idx = editorCode.lastIndexOf('}')
                            val loopSnippet = "    for (int i = 0; i < 5; i++) {\n            System.out.println(\"Iteration: \" + i);\n        }\n"
                            val updated = if (idx > 0) {
                                editorCode.substring(0, idx) + loopSnippet + editorCode.substring(idx)
                            } else {
                                editorCode + "\n" + loopSnippet
                            }
                            pushCodeUpdate(updated)
                        },
                        label = { Text("+ for-loop") },
                        modifier = Modifier.testTag("snippet_for_btn")
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            val idx = editorCode.lastIndexOf('}')
                            val trySnippet = "    try {\n            System.out.println(\"Protected block\");\n        } catch (Exception e) {\n            System.out.println(\"Caught: \" + e.getMessage());\n        }\n"
                            val updated = if (idx > 0) {
                                editorCode.substring(0, idx) + trySnippet + editorCode.substring(idx)
                            } else {
                                editorCode + "\n" + trySnippet
                            }
                            pushCodeUpdate(updated)
                        },
                        label = { Text("+ try-catch") },
                        modifier = Modifier.testTag("snippet_try_btn")
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            pushCodeUpdate(
                                editorCode + "\n// Record declaration (JDK 16+):\nrecord StudentRecord(String rollNo, int marks) {}\n"
                            )
                        },
                        label = { Text("+ record (16+)") },
                        modifier = Modifier.testTag("snippet_record_btn")
                    )
                    Text(
                        text = "Lines: ${editorCode.lines().size} • Chars: ${editorCode.length}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Line Number Gutter + Directly Editable Real-World Syntax-Highlighted Code Surface
                val codeLines = editorCode.lines()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp, max = 400.dp)
                        .verticalScroll(editorScrollState)
                        .padding(vertical = 10.dp)
                ) {
                    // Line Numbers Gutter
                    Column(
                        modifier = Modifier
                            .width(44.dp)
                            .padding(end = 8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        codeLines.indices.forEach { idx ->
                            val lineNum = idx + 1
                            val hasError = syntaxReport.diagnostics.any {
                                it.line == lineNum && (it.category == DiagnosticCategory.SYNTAX_ERROR || it.category == DiagnosticCategory.UNSUPPORTED_IN_SELECTED_JDK)
                            }
                            val hasWarn = syntaxReport.diagnostics.any {
                                it.line == lineNum && it.category == DiagnosticCategory.DEPRECATED_OR_REMOVED
                            }
                            Text(
                                text = if (hasError) "!$lineNum" else "$lineNum",
                                style = TextStyle(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = if (hasError || hasWarn) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        hasError -> SyntaxUnsupportedError
                                        hasWarn -> SyntaxDeprecated
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                    }
                                )
                            )
                        }
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height((codeLines.size * 20 + 24).dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    )

                    // Single-Layer Directly Editable BasicTextField with Live Syntax Highlighting VisualTransformation
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                            .testTag("highlighted_code_display")
                    ) {
                        val syntaxVisualTransformation = remember(syntaxReport.annotatedCode) {
                            VisualTransformation {
                                TransformedText(syntaxReport.annotatedCode, OffsetMapping.Identity)
                            }
                        }

                        BasicTextField(
                            value = editorCode,
                            onValueChange = { updated -> pushCodeUpdate(updated) },
                            textStyle = TextStyle(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            visualTransformation = syntaxVisualTransformation,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("code_editor_input")
                        )
                    }
                }
            }
        }

        // 4. Live Version-Specific Syntax Diagnostics Strip (if any diagnostics exist)
        if (syntaxReport.diagnostics.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("version_diagnostics_panel"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Version-Specific Syntax & API Analysis (Target: JDK $activeJdkVersion)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    syntaxReport.diagnostics.take(8).forEach { diag ->
                        val badgeColor = when (diag.category) {
                            DiagnosticCategory.SYNTAX_ERROR -> SyntaxUnsupportedError
                            DiagnosticCategory.UNSUPPORTED_IN_SELECTED_JDK -> SyntaxUnsupportedError
                            DiagnosticCategory.DEPRECATED_OR_REMOVED -> SyntaxDeprecated
                            DiagnosticCategory.NEWLY_INTRODUCED_MODERN_FEATURE -> SyntaxKeywordModern
                            DiagnosticCategory.LEGACY_NOTE -> MaterialTheme.colorScheme.secondary
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = badgeColor.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Line ${diag.line} • ${diag.token}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = diag.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 5. Bottom Panel: Compiler Output, Program Output, Diagnostics, Bytecode & Stdin
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("bottom_console_panel"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                val consoleTabs = listOf(
                    "Program Output",
                    "Compiler Output",
                    "Bytecode Inspector",
                    "Stdin & JVM Args"
                )
                ScrollableTabRow(
                    selectedTabIndex = activeConsoleTab.coerceIn(0, consoleTabs.lastIndex),
                    edgePadding = 8.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    consoleTabs.forEachIndexed { idx, title ->
                        Tab(
                            selected = activeConsoleTab == idx,
                            onClick = { onSelectConsoleTab(idx) },
                            text = { Text(title, style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.testTag("console_tab_$idx")
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp, max = 260.dp)
                        .padding(12.dp)
                        .verticalScroll(consoleScrollState)
                ) {
                    when (activeConsoleTab.coerceIn(0, consoleTabs.lastIndex)) {
                        0 -> {
                            // Program Output
                            if (executionResult == null) {
                                Text(
                                    text = "Click 'Run' to compile and execute $activeFileName on JDK $activeJdkVersion runtime.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "$ ${executionResult.runtimeCommand}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    if (executionResult.stdout.isNotBlank()) {
                                        Text(
                                            text = executionResult.stdout,
                                            style = TextStyle(
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            modifier = Modifier.testTag("console_stdout_text")
                                        )
                                    }
                                    if (executionResult.stderr.isNotBlank()) {
                                        Text(
                                            text = executionResult.stderr,
                                            style = TextStyle(
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.error
                                            ),
                                            modifier = Modifier.testTag("console_stderr_text")
                                        )
                                    }
                                    Text(
                                        text = "Process finished with exit code ${executionResult.exitCode} in ${executionResult.durationMs}ms (JDK ${executionResult.jdkVersion}).",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (executionResult.success) IdeEmeraldSuccess else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        1 -> {
                            // Compiler Output
                            if (compilationResult == null) {
                                Text(
                                    text = "Click 'Compile' to invoke the JDK $activeJdkVersion compiler verification engine.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = compilationResult.rawOutput,
                                    style = TextStyle(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        color = if (compilationResult.success) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.testTag("console_compiler_output_text")
                                )
                            }
                        }

                        2 -> {
                            // Bytecode Inspector
                            val spec = JdkCatalogAndSpecifications.getSpec(activeJdkVersion)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Class File Header & Bytecode Target Analysis (JDK $activeJdkVersion)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Magic Number: 0xCAFEBABE | Bytecode Version: ${spec.bytecodeMajor}.${spec.bytecodeMinor} (Major ${spec.bytecodeMajor})",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = "Supported Compiler Flags: ${spec.supportedCompilerFlags.joinToString(" ")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                compilationResult?.generatedClasses?.forEach { cls ->
                                    Text(
                                        text = "• ${cls.className} -> Constant Pool Entries: ${cls.constantPoolSize} | Methods: ${cls.methods.joinToString(", ")}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }

                        3 -> {
                            // Standard Input & Runtime Args
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = stdinInput,
                                    onValueChange = onStdinChange,
                                    label = { Text("Standard Input (stdin for Scanner / BufferedReader)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("console_stdin_input")
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = runtimeArgs,
                                        onValueChange = onRuntimeArgsChange,
                                        label = { Text("Command-Line Args (String[] args)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = compilerFlags,
                                        onValueChange = onCompilerFlagsChange,
                                        label = { Text("Compiler Flags") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyntaxCounterBadge(
    label: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
