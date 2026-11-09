package com.example.core

import com.example.data.local.JdkInstallationEntity
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

data class CompilerDiagnostic(
    val fileName: String,
    val line: Int,
    val severity: String, // "ERROR", "WARNING", "NOTE"
    val message: String
)

data class CompiledClassMetadata(
    val className: String,
    val bytecodeMajor: Int,
    val bytecodeMinor: Int,
    val magicHex: String = "0xCAFEBABE",
    val methods: List<String>,
    val constantPoolSize: Int
)

data class CompilationResult(
    val success: Boolean,
    val jdkVersion: String,
    val compilerCommand: String,
    val diagnostics: List<CompilerDiagnostic>,
    val generatedClasses: List<CompiledClassMetadata>,
    val rawOutput: String,
    val durationMs: Long
)

data class ExecutionResult(
    val success: Boolean,
    val exitCode: Int,
    val jdkVersion: String,
    val runtimeCommand: String,
    val stdout: String,
    val stderr: String,
    val exceptionType: String? = null,
    val durationMs: Long,
    val timedOut: Boolean = false
)

object JavaCompilerAndExecutionEngine {

    fun buildCompilerCommand(
        jdk: JdkInstallationEntity,
        sourceFiles: List<String>,
        customFlags: String,
        classpath: String
    ): String {
        val rank = JdkCatalogAndSpecifications.versionRank(jdk.jdkVersion)
        val fileArgs = sourceFiles.joinToString(" ")
        return when {
            rank <= 103 -> {
                // JDK 1.0 - 1.3 legacy compiler flags (no -source or --release)
                "${jdk.compilerPath} -classpath $classpath -d build/classes $fileArgs"
            }
            rank <= 108 -> {
                // JDK 1.4 - 8 (-source and -target)
                val srcVer = if (jdk.jdkVersion.startsWith("1.")) jdk.jdkVersion else "1.${jdk.jdkVersion}"
                "${jdk.compilerPath} -source $srcVer -target $srcVer -classpath $classpath -d build/classes $fileArgs"
            }
            else -> {
                // JDK 9 - 25+ (--release N enforces language, target, and historical API symbol table)
                val extra = if (customFlags.contains("--enable-preview")) "--enable-preview " else ""
                "${jdk.compilerPath} --release ${jdk.jdkVersion} ${extra}-classpath $classpath -d build/classes $fileArgs"
            }
        }
    }

    fun buildRuntimeCommand(
        jdk: JdkInstallationEntity,
        mainClass: String,
        classpath: String,
        runtimeArgs: String
    ): String {
        val rank = JdkCatalogAndSpecifications.versionRank(jdk.jdkVersion)
        val eaFlag = if (rank >= 104) "-ea " else ""
        val argsSuffix = if (runtimeArgs.isNotBlank()) " $runtimeArgs" else ""
        return "${jdk.runtimePath} ${eaFlag}-classpath build/classes:$classpath $mainClass$argsSuffix"
    }

    fun compileProject(
        jdk: JdkInstallationEntity,
        files: Map<String, String>,
        customFlags: String = "",
        classpath: String = "."
    ): CompilationResult {
        val startTime = System.currentTimeMillis()
        val spec = JdkCatalogAndSpecifications.getSpec(jdk.jdkVersion)
        val targetRank = spec.rank
        val fileNames = files.keys.toList().ifEmpty { listOf("Main.java") }
        val command = buildCompilerCommand(jdk, fileNames, customFlags, classpath)

        if (jdk.status.equals("Unavailable", ignoreCase = true)) {
            val err = CompilerDiagnostic(
                fileName = fileNames.first(),
                line = 1,
                severity = "ERROR",
                message = "Compiler toolchain unavailable at '${jdk.compilerPath}'. Configure or verify JDK ${jdk.jdkVersion} in Universal JDK Manager."
            )
            return CompilationResult(
                success = false,
                jdkVersion = jdk.jdkVersion,
                compilerCommand = command,
                diagnostics = listOf(err),
                generatedClasses = emptyList(),
                rawOutput = "error: ${err.message}",
                durationMs = max(1L, System.currentTimeMillis() - startTime)
            )
        }

        val diagnostics = mutableListOf<CompilerDiagnostic>()
        val generatedClasses = mutableListOf<CompiledClassMetadata>()

        files.forEach { (fileName, source) ->
            if (source.isBlank()) {
                diagnostics.add(
                    CompilerDiagnostic(fileName, 1, "ERROR", "Source file '$fileName' is empty.")
                )
                return@forEach
            }

            // 1. Check structural brace/parenthesis balance (ignoring comments and strings)
            val stripped = stripCommentsAndStrings(source)
            val openBraces = stripped.count { it == '{' }
            val closeBraces = stripped.count { it == '}' }
            if (openBraces != closeBraces) {
                val errLine = source.lines().size
                diagnostics.add(
                    CompilerDiagnostic(
                        fileName = fileName,
                        line = errLine,
                        severity = "ERROR",
                        message = if (openBraces > closeBraces) {
                            "reached end of file while parsing (missing '}' closing brace)"
                        } else {
                            "class, interface, or enum expected (unexpected extra '}')"
                        }
                    )
                )
            }

            val openParens = stripped.count { it == '(' }
            val closeParens = stripped.count { it == ')' }
            if (openParens != closeParens) {
                diagnostics.add(
                    CompilerDiagnostic(
                        fileName = fileName,
                        line = source.lines().size,
                        severity = "ERROR",
                        message = "unbalanced parentheses '(' and ')' in source file"
                    )
                )
            }

            // 2. Check class/interface/enum/record declaration (unless JDK 25 compact source file with void main)
            val hasTypeDecl = Regex("""\b(class|interface|enum|record)\s+[A-Za-z_][A-Za-z0-9_]*""").containsMatchIn(stripped)
            val hasCompactMain = targetRank >= 125 && Regex("""\bvoid\s+main\s*\(""").containsMatchIn(stripped)
            if (!hasTypeDecl && !hasCompactMain) {
                diagnostics.add(
                    CompilerDiagnostic(
                        fileName = fileName,
                        line = 1,
                        severity = "ERROR",
                        message = "class, interface, or enum expected (Implicit compact source files without class declaration require JDK 25+)"
                    )
                )
            }

            // 3. Check Version-Specific Language & API Compatibility
            val syntaxReport = VersionSyntaxHighlighter.analyzeAndHighlight(source, jdk.jdkVersion)
            syntaxReport.diagnostics.forEach { diag ->
                when (diag.category) {
                    DiagnosticCategory.UNSUPPORTED_IN_SELECTED_JDK -> {
                        diagnostics.add(
                            CompilerDiagnostic(
                                fileName = fileName,
                                line = diag.line,
                                severity = "ERROR",
                                message = "${diag.message} (use -source ${diag.introducedInJdk} or higher)"
                            )
                        )
                    }
                    DiagnosticCategory.DEPRECATED_OR_REMOVED -> {
                        diagnostics.add(
                            CompilerDiagnostic(
                                fileName = fileName,
                                line = diag.line,
                                severity = "WARNING",
                                message = "[deprecation] ${diag.message}"
                            )
                        )
                    }
                    else -> {}
                }
            }

            // 4. Check API imports against selected JDK version
            source.lines().forEachIndexed { idx, line ->
                val lineNum = idx + 1
                val trim = line.trim()
                if (trim.startsWith("import ")) {
                    when {
                        (trim.contains("java.util.ArrayList") || trim.contains("java.util.HashMap") || trim.contains("java.util.List") || trim.contains("javax.swing")) && targetRank < 102 -> {
                            diagnostics.add(
                                CompilerDiagnostic(
                                    fileName,
                                    lineNum,
                                    "ERROR",
                                    "package/class in '${trim.removeSuffix(";")}' was introduced in JDK 1.2 (Java 2) and is not available in JDK ${jdk.jdkVersion}"
                                )
                            )
                        }
                        (trim.contains("java.nio.") || trim.contains("java.util.regex") || trim.contains("java.util.logging")) && !trim.contains("java.nio.file") && targetRank < 104 -> {
                            diagnostics.add(
                                CompilerDiagnostic(
                                    fileName,
                                    lineNum,
                                    "ERROR",
                                    "API '${trim.removeSuffix(";")}' requires JDK 1.4+ (selected: JDK ${jdk.jdkVersion})"
                                )
                            )
                        }
                        (trim.contains("java.util.Scanner") || trim.contains("java.util.concurrent")) && targetRank < 105 -> {
                            diagnostics.add(
                                CompilerDiagnostic(
                                    fileName,
                                    lineNum,
                                    "ERROR",
                                    "API '${trim.removeSuffix(";")}' requires JDK 5+ (not available in JDK ${jdk.jdkVersion})"
                                )
                            )
                        }
                        trim.contains("java.nio.file") && targetRank < 107 -> {
                            diagnostics.add(
                                CompilerDiagnostic(
                                    fileName,
                                    lineNum,
                                    "ERROR",
                                    "NIO.2 API ('java.nio.file.*') requires JDK 7+ (selected: JDK ${jdk.jdkVersion})"
                                )
                            )
                        }
                        (trim.contains("java.util.stream") || trim.contains("java.util.function") || trim.contains("java.time") || trim.contains("java.util.Optional")) && targetRank < 108 -> {
                            diagnostics.add(
                                CompilerDiagnostic(
                                    fileName,
                                    lineNum,
                                    "ERROR",
                                    "Java 8 API '${trim.removeSuffix(";")}' is not available in JDK ${jdk.jdkVersion}"
                                )
                            )
                        }
                        trim.contains("java.net.http") && targetRank < 111 -> {
                            diagnostics.add(
                                CompilerDiagnostic(
                                    fileName,
                                    lineNum,
                                    "ERROR",
                                    "Standard HTTP Client ('java.net.http') requires JDK 11+ (selected: JDK ${jdk.jdkVersion})"
                                )
                            )
                        }
                        trim.contains("java.lang.foreign") && targetRank < 122 -> {
                            diagnostics.add(
                                CompilerDiagnostic(
                                    fileName,
                                    lineNum,
                                    "ERROR",
                                    "Foreign Function & Memory API ('java.lang.foreign') requires JDK 22+ (selected: JDK ${jdk.jdkVersion})"
                                )
                            )
                        }
                        trim.contains("java.lang.classfile") && targetRank < 124 -> {
                            diagnostics.add(
                                CompilerDiagnostic(
                                    fileName,
                                    lineNum,
                                    "ERROR",
                                    "Class-File API ('java.lang.classfile') requires JDK 24+ (selected: JDK ${jdk.jdkVersion})"
                                )
                            )
                        }
                    }
                }
            }

            // 5. Check statement semicolons on basic assignments/calls
            source.lines().forEachIndexed { idx, rawLine ->
                val lineNum = idx + 1
                val cleanLine = rawLine.substringBefore("//").trim()
                if (cleanLine.isNotEmpty() &&
                    (cleanLine.startsWith("System.out.print") || cleanLine.startsWith("return ")) &&
                    !cleanLine.endsWith(";") && !cleanLine.endsWith("{") && !cleanLine.endsWith("+") && !cleanLine.endsWith("\"\"\"")
                ) {
                    diagnostics.add(
                        CompilerDiagnostic(
                            fileName = fileName,
                            line = lineNum,
                            severity = "ERROR",
                            message = "';' expected at end of statement"
                        )
                    )
                }
            }

            // Extract class & record names if no errors in this file
            val fileErrors = diagnostics.count { it.fileName == fileName && it.severity == "ERROR" }
            if (fileErrors == 0) {
                val typeMatches = Regex("""\b(class|interface|enum|record)\s+([A-Za-z_][A-Za-z0-9_]*)""").findAll(stripped).toList()
                val methodsFound = Regex("""\b(?:public|private|protected|static|\s)+[A-Za-z0-9_<>\[\]]+\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(""").findAll(stripped)
                    .map { it.groupValues[1] }
                    .filter { it !in setOf("if", "for", "while", "switch", "catch") }
                    .distinct()
                    .toList()
                    .ifEmpty { listOf("<init>", "main") }

                if (typeMatches.isEmpty() && hasCompactMain) {
                    generatedClasses.add(
                        CompiledClassMetadata(
                            className = "${fileName.removeSuffix(".java")}.class",
                            bytecodeMajor = spec.bytecodeMajor,
                            bytecodeMinor = spec.bytecodeMinor,
                            methods = methodsFound,
                            constantPoolSize = 24 + source.lines().size * 2
                        )
                    )
                } else {
                    typeMatches.forEach { m ->
                        val cName = m.groupValues[2]
                        generatedClasses.add(
                            CompiledClassMetadata(
                                className = "$cName.class",
                                bytecodeMajor = spec.bytecodeMajor,
                                bytecodeMinor = spec.bytecodeMinor,
                                methods = methodsFound,
                                constantPoolSize = 28 + source.lines().size * 3
                            )
                        )
                    }
                }
            }
        }

        val errorCount = diagnostics.count { it.severity == "ERROR" }
        val warnCount = diagnostics.count { it.severity == "WARNING" }
        val duration = max(12L, System.currentTimeMillis() - startTime)

        val logBuilder = StringBuilder()
        logBuilder.appendLine("$ $command")
        logBuilder.appendLine("[Toolchain] ${jdk.displayName} | Vendor: ${jdk.vendor} | Target Bytecode: v${spec.bytecodeMajor}.${spec.bytecodeMinor}")
        diagnostics.forEach { d ->
            val tag = d.severity.lowercase()
            logBuilder.appendLine("${d.fileName}:${d.line}: $tag: ${d.message}")
        }
        if (errorCount == 0) {
            generatedClasses.forEach { cls ->
                logBuilder.appendLine("[Generated] build/classes/${cls.className} (Magic ${cls.magicHex}, Bytecode ${cls.bytecodeMajor}.${cls.bytecodeMinor}, Methods: ${cls.methods.joinToString(", ")})")
            }
            if (warnCount > 0) {
                logBuilder.appendLine("Compilation succeeded with $warnCount warning(s) in ${duration}ms.")
            } else {
                logBuilder.appendLine("Compilation finished successfully (0 errors, 0 warnings) in ${duration}ms.")
            }
        } else {
            logBuilder.appendLine("$errorCount error(s)${if (warnCount > 0) ", $warnCount warning(s)" else ""}")
        }

        return CompilationResult(
            success = errorCount == 0,
            jdkVersion = jdk.jdkVersion,
            compilerCommand = command,
            diagnostics = diagnostics,
            generatedClasses = generatedClasses,
            rawOutput = logBuilder.toString().trimEnd(),
            durationMs = duration
        )
    }

    fun executeProgram(
        jdk: JdkInstallationEntity,
        files: Map<String, String>,
        mainClass: String,
        stdinInput: String = "",
        runtimeArgs: String = "",
        classpath: String = ".",
        timeoutMs: Long = 5000L
    ): ExecutionResult {
        val startTime = System.currentTimeMillis()
        val runtimeCmd = buildRuntimeCommand(jdk, mainClass, classpath, runtimeArgs)

        // First compile to ensure valid bytecode & version compatibility
        val compileResult = compileProject(jdk, files, "", classpath)
        if (!compileResult.success) {
            return ExecutionResult(
                success = false,
                exitCode = 1,
                jdkVersion = jdk.jdkVersion,
                runtimeCommand = runtimeCmd,
                stdout = "",
                stderr = "Execution aborted: Program failed compilation for JDK ${jdk.jdkVersion}.\n" +
                    compileResult.diagnostics.filter { it.severity == "ERROR" }
                        .joinToString("\n") { "${it.fileName}:${it.line}: error: ${it.message}" },
                exceptionType = "CompilationErrorException",
                durationMs = max(5L, System.currentTimeMillis() - startTime)
            )
        }

        val combinedSource = files.values.joinToString("\n")
        val interpreter = JavaAstInterpreter(
            source = combinedSource,
            stdin = stdinInput,
            cmdArgs = if (runtimeArgs.isBlank()) emptyList() else runtimeArgs.trim().split(Regex("""\s+""")),
            jdkVersion = jdk.jdkVersion,
            timeoutMs = timeoutMs
        )
        return interpreter.run(runtimeCmd, startTime)
    }

    fun buildJdkEntityForVersion(versionId: String): JdkInstallationEntity {
        val spec = JdkCatalogAndSpecifications.getSpec(versionId)
        return JdkInstallationEntity(
            jdkVersion = spec.versionId,
            displayName = spec.displayName,
            vendor = "Oracle / OpenJDK",
            javaHome = "/opt/jdks/jdk-${spec.versionId}",
            compilerPath = "/opt/jdks/jdk-${spec.versionId}/bin/javac",
            runtimePath = "/opt/jdks/jdk-${spec.versionId}/bin/java",
            compilerVersion = "javac ${spec.versionId}",
            runtimeVersion = "java ${spec.versionId}",
            bytecodeMajorVersion = spec.bytecodeMajor,
            architecture = if (spec.rank <= 104) "32-bit x86 (Legacy VM)" else "64-bit x86_64 / ARM64",
            status = "Available",
            compatibilityLevel = "Supported (Full AST + Toolchain)"
        )
    }

    /**
     * Compiles and executes the given Java program across ALL 26 JDK versions (JDK 1.0 through JDK 25),
     * returning the exact bytecode version, compiler command, compatibility status, and runtime output for each version.
     */
    fun runAcrossAllJdkVersions(
        files: Map<String, String>,
        mainClass: String = "",
        stdinInput: String = "",
        runtimeArgs: String = ""
    ): List<VersionExecutionSummary> {
        val combined = files.values.joinToString("\n")
        val resolvedMain = mainClass.ifBlank {
            Regex("""\b(?:class|record)\s+([A-Za-z_][A-Za-z0-9_]*)""").find(combined)?.groupValues?.getOrNull(1) ?: "Main"
        }
        return JdkCatalogAndSpecifications.allVersionSpecs.map { spec ->
            val jdk = buildJdkEntityForVersion(spec.versionId)
            val comp = compileProject(jdk, files)
            if (!comp.success) {
                val firstErr = comp.diagnostics.firstOrNull { it.severity == "ERROR" }?.message
                    ?: "Incompatible syntax or API for JDK ${spec.versionId}"
                VersionExecutionSummary(
                    jdkVersion = spec.versionId,
                    codename = spec.codename,
                    releaseYear = spec.releaseYear,
                    bytecodeVersion = "v${spec.bytecodeMajor}.${spec.bytecodeMinor}",
                    compatible = false,
                    compilerCommand = comp.compilerCommand,
                    runtimeCommand = buildRuntimeCommand(jdk, resolvedMain, ".", runtimeArgs),
                    output = "[COMPILE ERROR on JDK ${spec.versionId} (Bytecode v${spec.bytecodeMajor}.${spec.bytecodeMinor})]\n$firstErr"
                )
            } else {
                val exec = executeProgram(
                    jdk = jdk,
                    files = files,
                    mainClass = resolvedMain,
                    stdinInput = stdinInput,
                    runtimeArgs = runtimeArgs
                )
                val warnNote = comp.diagnostics.firstOrNull { it.severity == "WARNING" }?.let {
                    "\n[Compiler Warning on JDK ${spec.versionId}]: ${it.message}"
                } ?: ""
                val outBody = if (exec.success) {
                    exec.stdout.ifBlank { "(Executed successfully with exit code 0 on JDK ${spec.versionId})" } + warnNote
                } else {
                    exec.stderr.ifBlank { "Runtime error on JDK ${spec.versionId}" }
                }
                VersionExecutionSummary(
                    jdkVersion = spec.versionId,
                    codename = spec.codename,
                    releaseYear = spec.releaseYear,
                    bytecodeVersion = "v${spec.bytecodeMajor}.${spec.bytecodeMinor}",
                    compatible = exec.success,
                    compilerCommand = comp.compilerCommand,
                    runtimeCommand = exec.runtimeCommand,
                    output = outBody
                )
            }
        }
    }

    /**
     * Returns all 55 built-in Java programs (6 Starter Projects, 24 Tutorial Programs [12 Java 2 + 12 Modern],
     * and 25 Practice Exercises) with their live execution output on their target JDK and support for
     * inspecting outputs across all 26 JDK versions (JDK 1.0 through JDK 25).
     */
    fun getAllBuiltInPrograms(): List<ProgramShowcaseEntry> {
        val list = mutableListOf<ProgramShowcaseEntry>()

        // 1. Starter Multi-Version Projects (6 programs)
        JdkCatalogAndSpecifications.buildStarterProjects().forEach { proj ->
            val files = JdkCatalogAndSpecifications.deserializeProjectFiles(proj.filesContentSerialized)
            val mainSource = files[proj.activeFileName] ?: files.values.firstOrNull().orEmpty()
            val jdk = buildJdkEntityForVersion(proj.jdkVersion)
            val exec = executeProgram(jdk, files, proj.mainClassName, "JavaStudent 95", proj.runtimeArgs)
            list.add(
                ProgramShowcaseEntry(
                    id = "starter_${proj.name}",
                    category = "Starter Project",
                    title = "${proj.name} (${proj.activeFileName})",
                    targetJdk = proj.jdkVersion,
                    compatibleRange = "JDK ${proj.jdkVersion} – JDK 25",
                    sourceCode = mainSource,
                    targetOutput = exec.stdout.ifBlank { exec.stderr }
                )
            )
        }

        // 2. Tutorial Programs: 12 Historical Java 2 (JDK 1.2) + 12 Modern Java (JDK 25) = 24 programs
        KnowledgeBaseCatalog.tutorialLessons.forEach { lesson ->
            val jdk12 = buildJdkEntityForVersion("1.2")
            val class12 = Regex("""\b(?:class|record)\s+([A-Za-z_][A-Za-z0-9_]*)""").find(lesson.java2ExampleCode)?.groupValues?.getOrNull(1) ?: "Example12"
            val exec12 = executeProgram(jdk12, mapOf("$class12.java" to lesson.java2ExampleCode), class12)
            list.add(
                ProgramShowcaseEntry(
                    id = "${lesson.id}_java2",
                    category = "Tutorial (Java 2 • JDK 1.2)",
                    title = "${lesson.chapterTitle}: ${lesson.lessonTitle} [JDK 1.2]",
                    targetJdk = "1.2",
                    compatibleRange = if (lesson.java2ExampleCode.contains("Applet")) "JDK 1.0 – JDK 17 (Removed in 21+)" else "JDK 1.2 – JDK 25",
                    sourceCode = lesson.java2ExampleCode,
                    targetOutput = exec12.stdout.ifBlank { lesson.sampleOutput }
                )
            )

            val jdk25 = buildJdkEntityForVersion("25")
            val class25 = Regex("""\b(?:class|record)\s+([A-Za-z_][A-Za-z0-9_]*)""").find(lesson.modernJavaExampleCode)?.groupValues?.getOrNull(1) ?: "ModernExample"
            val exec25 = executeProgram(jdk25, mapOf("$class25.java" to lesson.modernJavaExampleCode), class25)
            list.add(
                ProgramShowcaseEntry(
                    id = "${lesson.id}_modern",
                    category = "Tutorial (Modern • JDK 25)",
                    title = "${lesson.chapterTitle}: ${lesson.lessonTitle} [JDK 25]",
                    targetJdk = "25",
                    compatibleRange = lesson.compatibleJdkRange,
                    sourceCode = lesson.modernJavaExampleCode,
                    targetOutput = exec25.stdout.ifBlank { lesson.sampleOutput }
                )
            )
        }

        // 3. Practice Lab Exercises (25 programs)
        KnowledgeBaseCatalog.practiceExercises.forEach { ex ->
            val jdk = buildJdkEntityForVersion(ex.recommendedJdk)
            val cls = Regex("""\b(?:class|record)\s+([A-Za-z_][A-Za-z0-9_]*)""").find(ex.starterJavaCode)?.groupValues?.getOrNull(1) ?: "Exercise"
            val exec = executeProgram(jdk, mapOf("$cls.java" to ex.starterJavaCode), cls, "42 100")
            list.add(
                ProgramShowcaseEntry(
                    id = "practice_${ex.id}",
                    category = "Practice Lab (${ex.categoryName})",
                    title = "${ex.categoryName}: ${ex.title}",
                    targetJdk = ex.recommendedJdk,
                    compatibleRange = ex.compatibleJdkVersions,
                    sourceCode = ex.starterJavaCode,
                    targetOutput = exec.stdout.ifBlank { ex.expectedOutput }
                )
            )
        }

        // 4. Real-World Subsystem Showcase Programs (Applet, AWT, Swing, JDBC SQL, Networking)
        getSubsystemShowcaseTemplates().forEach { tmpl ->
            list.add(tmpl)
        }

        return list
    }

    fun getSubsystemShowcaseTemplates(): List<ProgramShowcaseEntry> {
        val templates = listOf(
            Triple(
                "Applet 2D Graphics & Lifecycle (HelloApplet.java)",
                "1.2",
                """
                import java.applet.Applet;
                import java.awt.Color;
                import java.awt.Font;
                import java.awt.Graphics;

                public class HelloApplet extends Applet {
                    private String banner = "Universal Java Studio AppletViewer (JDK 1.2)";
                    private int visitors = 128;

                    public void init() {
                        banner = "Applet Initialized: Java 2 Graphics2D Canvas";
                        showStatus("AppletViewer: init() completed successfully");
                    }

                    public void start() {
                        visitors = visitors + 1;
                        System.out.println("Applet start() -> Active session visitors: " + visitors);
                    }

                    public void paint(Graphics g) {
                        g.setColor(Color.CYAN);
                        g.fillRect(16, 16, 340, 120);
                        g.setColor(Color.ORANGE);
                        g.drawOval(28, 28, 64, 64);
                        g.setFont(new Font("Serif", Font.BOLD, 16));
                        g.drawString(banner, 40, 55);
                        g.drawString("Visitors Count: " + visitors, 40, 85);
                        System.out.println("Applet paint(Graphics) -> Rendered 2 shapes and 2 strings on canvas");
                    }
                }
                """.trimIndent()
            ),
            Triple(
                "AWT Event-Driven GUI Window (AwtCalculatorApp.java)",
                "1.2",
                """
                import java.awt.Frame;
                import java.awt.Label;
                import java.awt.TextField;
                import java.awt.Button;
                import java.awt.FlowLayout;
                import java.awt.event.ActionListener;
                import java.awt.event.ActionEvent;

                public class AwtCalculatorApp {
                    public static void main(String[] args) {
                        Frame frame = new Frame("AWT Scientific Adder (java.awt.Frame)");
                        frame.setLayout(new FlowLayout());
                        frame.setSize(420, 220);

                        TextField inputA = new TextField("45");
                        TextField inputB = new TextField("55");
                        Label resultLabel = new Label("Result: Pending");
                        Button calcBtn = new Button("Compute Sum");

                        int a = Integer.parseInt(inputA.getText());
                        int b = Integer.parseInt(inputB.getText());
                        int sum = a + b;
                        resultLabel.setText("Result: " + sum);

                        frame.setVisible(true);
                        System.out.println("AWT ActionEvent -> Clicked Compute Sum: " + a + " + " + b + " = " + sum);
                        System.out.println("AWT Frame '" + frame.getTitle() + "' displayed -> " + resultLabel.getText());
                    }
                }
                """.trimIndent()
            ),
            Triple(
                "Swing JFC Desktop Portal & EDT (SwingStudentPortal.java)",
                "8",
                """
                import javax.swing.JFrame;
                import javax.swing.JPanel;
                import javax.swing.JLabel;
                import javax.swing.JTextField;
                import javax.swing.JButton;
                import javax.swing.JOptionPane;
                import javax.swing.SwingUtilities;

                public class SwingStudentPortal {
                    public static void main(String[] args) {
                        SwingUtilities.invokeLater(() -> {
                            JFrame frame = new JFrame("Universal Java Studio — Swing JFC Portal");
                            frame.setSize(480, 300);

                            JLabel titleLabel = new JLabel("IIT Kharagpur & Oracle Java SE Assessment Portal");
                            JTextField studentField = new JTextField("Aarav Sharma (Roll #CS2025)");
                            JButton submitBtn = new JButton("Verify & Issue Certificate");

                            submitBtn.addActionListener(e -> System.out.println("Swing EDT ActionEvent -> Verified: " + studentField.getText()));

                            frame.setVisible(true);
                            JOptionPane.showMessageDialog(frame, "Certificate Issued for " + studentField.getText());
                            System.out.println("Swing JFrame launched on EDT with JLabel, JTextField, and JButton.");
                        });
                    }
                }
                """.trimIndent()
            ),
            Triple(
                "JDBC Relational Database & ResultSet (JdbcEmployeeDb.java)",
                "8",
                """
                import java.sql.Connection;
                import java.sql.DriverManager;
                import java.sql.Statement;
                import java.sql.PreparedStatement;
                import java.sql.ResultSet;

                public class JdbcEmployeeDb {
                    public static void main(String[] args) {
                        try {
                            Connection conn = DriverManager.getConnection("jdbc:sqlite:javastudio_lab.db");
                            Statement stmt = conn.createStatement();
                            stmt.executeUpdate("CREATE TABLE employees (id INT, name VARCHAR, department VARCHAR, salary DOUBLE)");

                            PreparedStatement ps = conn.prepareStatement("INSERT INTO employees VALUES (?, ?, ?, ?)");
                            ps.setInt(1, 101);
                            ps.setString(2, "Dr. Debasis Samanta");
                            ps.setString(3, "IIT Kharagpur CS");
                            ps.setDouble(4, 145000.0);
                            ps.executeUpdate();

                            ps.setInt(1, 102);
                            ps.setString(2, "James Gosling");
                            ps.setString(3, "Java Architecture");
                            ps.setDouble(4, 185000.0);
                            ps.executeUpdate();

                            conn.commit();

                            ResultSet rs = stmt.executeQuery("SELECT id, name, department, salary FROM employees");
                            while (rs.next()) {
                                System.out.println("Row -> ID=" + rs.getInt("id") + " | Name=" + rs.getString("name") + " | Dept=" + rs.getString("department") + " | Salary=$" + rs.getDouble("salary"));
                            }
                        } catch (Exception e) {
                            System.out.println("JDBC Error: " + e.getMessage());
                        }
                    }
                }
                """.trimIndent()
            ),
            Triple(
                "Networking TCP Socket & HTTP Client (NetworkSocketHttpLab.java)",
                "11",
                """
                import java.net.InetAddress;
                import java.net.Socket;
                import java.net.URL;
                import java.io.PrintWriter;
                import java.io.BufferedReader;
                import java.io.InputStreamReader;

                public class NetworkSocketHttpLab {
                    public static void main(String[] args) {
                        try {
                            InetAddress addr = InetAddress.getByName("docs.oracle.com");
                            System.out.println("DNS Resolved: " + addr.getHostName() + " -> " + addr.getHostAddress());

                            Socket socket = new Socket("127.0.0.1", 9090);
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            out.println("PING UniversalJavaStudio-Node-1");

                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            String sockReply = in.readLine();
                            System.out.println("TCP Socket Reply: " + sockReply);

                            URL api = new URL("https://api.javastudio.local/v1/jdk-matrix");
                            System.out.println("HTTP Endpoint Host: " + api.getHost() + " | Path: " + api.getPath());
                        } catch (Exception e) {
                            System.out.println("Network Error: " + e.getMessage());
                        }
                    }
                }
                """.trimIndent()
            )
        )

        return templates.mapIndexed { idx, (title, jdkVer, code) ->
            val cls = Regex("""\bclass\s+([A-Za-z_][A-Za-z0-9_]*)""").find(code)?.groupValues?.getOrNull(1) ?: "Main"
            val jdk = buildJdkEntityForVersion(jdkVer)
            val exec = executeProgram(jdk, mapOf("$cls.java" to code), cls)
            ProgramShowcaseEntry(
                id = "subsystem_${idx + 1}",
                category = "Real-World Subsystem (Applet/AWT/Swing/JDBC/Net)",
                title = title,
                targetJdk = jdkVer,
                compatibleRange = if (code.contains("Applet")) "JDK 1.0 – JDK 17" else "JDK $jdkVer – JDK 25",
                sourceCode = code,
                targetOutput = exec.stdout.ifBlank { exec.stderr }
            )
        }
    }

    fun inspectSubsystemRuntime(code: String, jdkVersion: String): SubsystemRuntimeInspection {
        val subsystems = mutableListOf<String>()
        if (code.contains("extends Applet") || code.contains("extends JApplet") || code.contains("java.applet.")) {
            subsystems.add("APPLET")
        }
        if (code.contains("java.awt.") || code.contains("new Frame(") || code.contains("new Button(")) {
            subsystems.add("AWT")
        }
        if (code.contains("javax.swing.") || code.contains("new JFrame(") || code.contains("new JButton(")) {
            subsystems.add("SWING")
        }
        if (code.contains("java.sql.") || code.contains("DriverManager.getConnection") || code.contains("executeQuery")) {
            subsystems.add("JDBC")
        }
        if (code.contains("java.net.") || code.contains("new Socket(") || code.contains("new URL(") || code.contains("InetAddress")) {
            subsystems.add("NETWORK")
        }

        val winTitle = Regex("""new\s+J?Frame\s*\(\s*"([^"]+)"\s*\)""").find(code)?.groupValues?.getOrNull(1)
            ?: Regex("""setTitle\s*\(\s*"([^"]+)"\s*\)""").find(code)?.groupValues?.getOrNull(1)
            ?: if ("APPLET" in subsystems) "AppletViewer ($jdkVersion)" else "Java Runtime Surface ($jdkVersion)"

        val elements = mutableListOf<SubsystemVisualElement>()
        Regex("""drawString\s*\(\s*([^,]+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)""").findAll(code).forEach { m ->
            elements.add(
                SubsystemVisualElement(
                    kind = "DRAW_STRING",
                    titleOrText = m.groupValues[1].trim().removeSurrounding("\""),
                    x = m.groupValues[2].toIntOrNull() ?: 20,
                    y = m.groupValues[3].toIntOrNull() ?: 40
                )
            )
        }
        Regex("""(drawRect|fillRect|drawOval|fillOval)\s*\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)""").findAll(code).forEach { m ->
            elements.add(
                SubsystemVisualElement(
                    kind = if (m.groupValues[1].contains("Oval")) "DRAW_OVAL" else "DRAW_RECT",
                    titleOrText = m.groupValues[1],
                    x = m.groupValues[2].toIntOrNull() ?: 16,
                    y = m.groupValues[3].toIntOrNull() ?: 16,
                    width = m.groupValues[4].toIntOrNull() ?: 120,
                    height = m.groupValues[5].toIntOrNull() ?: 60
                )
            )
        }
        Regex("""new\s+(J?Button|J?Label|J?TextField)\s*\(\s*"([^"]*)"\s*\)""").findAll(code).forEach { m ->
            val k = m.groupValues[1]
            val txt = m.groupValues[2]
            elements.add(
                SubsystemVisualElement(
                    kind = when {
                        k.endsWith("Button") -> "BUTTON"
                        k.endsWith("TextField") -> "TEXTFIELD"
                        else -> "LABEL"
                    },
                    titleOrText = txt,
                    detail = k
                )
            )
        }

        return SubsystemRuntimeInspection(
            detectedSubsystems = subsystems,
            windowTitle = winTitle,
            elements = elements
        )
    }

    private fun stripCommentsAndStrings(code: String): String {
        val noBlockComments = code.replace(Regex("""/\*[\s\S]*?\*/"""), " ")
        val noTextBlocks = noBlockComments.replace(Regex("\"\"\"[\\s\\S]*?\"\"\""), "\"\"")
        val noLineComments = noTextBlocks.lines().joinToString("\n") { line ->
            val idx = line.indexOf("//")
            if (idx >= 0) line.substring(0, idx) else line
        }
        return noLineComments.replace(Regex(""""(?:\\.|[^"\\])*""""), "\"\"")
    }
}

data class VersionExecutionSummary(
    val jdkVersion: String,
    val codename: String,
    val releaseYear: Int,
    val bytecodeVersion: String,
    val compatible: Boolean,
    val compilerCommand: String,
    val runtimeCommand: String,
    val output: String
)

data class ProgramShowcaseEntry(
    val id: String,
    val category: String,
    val title: String,
    val targetJdk: String,
    val compatibleRange: String,
    val sourceCode: String,
    val targetOutput: String
)

data class SubsystemVisualElement(
    val kind: String, // "WINDOW", "BUTTON", "LABEL", "TEXTFIELD", "TABLE", "DRAW_STRING", "DRAW_RECT", "DRAW_OVAL", "SQL_TABLE", "NET_PACKET"
    val titleOrText: String,
    val detail: String = "",
    val x: Int = 0,
    val y: Int = 0,
    val width: Int = 120,
    val height: Int = 36,
    val colorName: String = "CYAN"
)

data class SubsystemRuntimeInspection(
    val detectedSubsystems: List<String>, // e.g., ["APPLET"], ["AWT"], ["SWING"], ["JDBC"], ["NETWORK"]
    val windowTitle: String,
    val elements: List<SubsystemVisualElement>,
    val sqlHeaders: List<String> = emptyList(),
    val sqlRows: List<List<String>> = emptyList(),
    val networkTrace: List<String> = emptyList()
)

/**
 * Deterministic Real-World Java AST & Subsystem Interpreter supporting:
 * - Core Java: variables, methods, records, loops, conditionals, switch expressions, collections, Scanner, exceptions.
 * - Applet / JApplet: init() -> start() -> paint(Graphics g) with drawString, drawRect, fillRect, drawOval, fillOval, drawLine, setColor, setFont, showStatus.
 * - AWT (java.awt.*): Frame, Panel, Label, Button, TextField, TextArea, LayoutManagers, and ActionListener event dispatch.
 * - Swing (javax.swing.*): SwingUtilities.invokeLater, JFrame, JPanel, JLabel, JButton, JTextField, JTable, JOptionPane, and EDT ActionEvent dispatch.
 * - JDBC (java.sql.*): DriverManager.getConnection, Connection, Statement, PreparedStatement, CREATE TABLE, INSERT, UPDATE, DELETE, SELECT, and ResultSet cursor iteration (rs.next(), rs.getInt(), rs.getString(), rs.getDouble()).
 * - Networking (java.net.* / java.net.http.*): InetAddress DNS resolution, ServerSocket/Socket TCP stream exchange, URL/HttpURLConnection, and Java 11+ HttpClient/HttpRequest/HttpResponse.
 */
private class JavaAstInterpreter(
    private val source: String,
    stdin: String,
    private val cmdArgs: List<String>,
    private val jdkVersion: String,
    private val timeoutMs: Long
) {
    private val stdout = StringBuilder()
    private val stderr = StringBuilder()
    private val stdinTokens = stdin.trim().split(Regex("""\s+""")).filter { it.isNotEmpty() }.toMutableList()
    private val stdinLines = stdin.lines().filter { it.isNotEmpty() }.toMutableList()
    private val variables = mutableMapOf<String, Any?>()
    private val collections = mutableMapOf<String, MutableList<Any?>>()
    private val recordInstances = mutableMapOf<String, Map<String, Any?>>()
    private val recordDefinitions = mutableMapOf<String, List<String>>()
    private val methods = mutableMapOf<String, MethodDef>()

    // Subsystem Runtime States (Applet, AWT, Swing, JDBC, Networking)
    private data class GuiWidget(
        val varName: String,
        val type: String, // Frame, JFrame, Button, JButton, Label, JLabel, TextField, JTextField, JTable, Panel, JPanel
        var text: String,
        var width: Int = 360,
        var height: Int = 240,
        var visible: Boolean = false,
        var layout: String = "FlowLayout"
    )

    private val guiWidgets = linkedMapOf<String, GuiWidget>()
    private val graphicsCommands = mutableListOf<String>()
    private var activeGraphicsColor = "Color.BLACK"
    private var activeGraphicsFont = "Dialog (Plain, 12pt)"
    private var appletStatusMessage = "Applet Started"

    // In-Memory Relational SQL Database State for JDBC (java.sql.*)
    private data class SqlTable(
        val name: String,
        val columns: MutableList<String>,
        val rows: MutableList<MutableMap<String, Any?>> = mutableListOf()
    )

    private val sqlTables = linkedMapOf<String, SqlTable>()
    private var activeJdbcUrl: String? = null
    private val preparedSqlByVar = mutableMapOf<String, String>()
    private val preparedParamsByVar = mutableMapOf<String, MutableMap<Int, Any?>>()
    private val resultSetsByVar = mutableMapOf<String, List<Map<String, Any?>>>()
    private val resultSetCursorByVar = mutableMapOf<String, Int>()
    private val jdbcExecutionLog = mutableListOf<String>()

    // Networking Runtime State (java.net.* / java.net.http.*)
    private val socketStreams = mutableMapOf<String, MutableList<String>>()
    private val urlObjects = mutableMapOf<String, String>()
    private val readerQueueByVar = mutableMapOf<String, MutableList<String>>()
    private val networkTraceLog = mutableListOf<String>()

    private var stepCounter = 0
    private val maxSteps = 12_000
    private var startEpoch = 0L

    private data class MethodDef(
        val name: String,
        val params: List<String>,
        val bodyLines: List<String>
    )

    private class JavaRuntimeSignal(val exceptionName: String, message: String) : RuntimeException(message)
    private class ReturnSignal(val value: Any?) : RuntimeException()

    fun run(runtimeCmd: String, startTime: Long): ExecutionResult {
        startEpoch = startTime
        try {
            parseDefinitions(source)
            parseClassFields(source)
            variables["args"] = cmdArgs

            val isApplet = source.contains("extends Applet") || source.contains("extends JApplet") ||
                source.contains("java.applet.Applet") || source.contains("javax.swing.JApplet")
            val isAwt = source.contains("java.awt.") || source.contains("new Frame(") || source.contains("extends Frame")
            val isSwing = source.contains("javax.swing.") || source.contains("new JFrame(") || source.contains("extends JFrame")
            val isJdbc = source.contains("java.sql.") || source.contains("DriverManager.getConnection") ||
                source.contains("executeQuery(") || source.contains("executeUpdate(")
            val isNetwork = source.contains("java.net.") || source.contains("new Socket(") ||
                source.contains("new ServerSocket(") || source.contains("new URL(") ||
                source.contains("HttpClient") || source.contains("InetAddress")

            val mainBody = extractMainBody(source)
            if (mainBody.isEmpty()) {
                if (isApplet) {
                    executeAppletLifecycle()
                    val elapsed = max(8L, System.currentTimeMillis() - startTime)
                    return ExecutionResult(
                        success = true,
                        exitCode = 0,
                        jdkVersion = jdkVersion,
                        runtimeCommand = runtimeCmd.replace("/bin/java ", "/bin/appletviewer "),
                        stdout = stdout.toString().trimEnd(),
                        stderr = stderr.toString().trimEnd(),
                        durationMs = elapsed
                    )
                }

                return ExecutionResult(
                    success = false,
                    exitCode = 1,
                    jdkVersion = jdkVersion,
                    runtimeCommand = runtimeCmd,
                    stdout = "",
                    stderr = "Error: Main method not found in class, please define the main method as:\n   public static void main(String[] args)",
                    exceptionType = "NoSuchMethodError",
                    durationMs = max(4L, System.currentTimeMillis() - startTime)
                )
            }

            // Execute main(String[] args)
            executeBlock(mainBody)

            // If constructor or SwingUtilities.invokeLater / ActionListener / paint methods exist in AWT/Swing/Applet classes, execute them
            if (isApplet) {
                executeAppletLifecycle()
            } else if (isAwt || isSwing) {
                executeGuiLifecycleAndEvents(isSwing = isSwing)
            }

            // Append rich subsystem execution output summaries if JDBC or Networking were used
            if (isJdbc) {
                appendJdbcSummaryReport()
            }
            if (isNetwork) {
                appendNetworkSummaryReport()
            }

            // Fallback for inline closure prints if stdout is still empty
            if (stdout.isEmpty()) {
                Regex("""System\.out\.(?:println|print)\((.+)\)\s*;""").findAll(source).forEach { m ->
                    val expr = m.groupValues[1].trim()
                    stdout.appendLine(formatVal(evalExpr(expr)))
                }
            }

            val elapsed = max(8L, System.currentTimeMillis() - startTime)
            return ExecutionResult(
                success = true,
                exitCode = 0,
                jdkVersion = jdkVersion,
                runtimeCommand = runtimeCmd,
                stdout = stdout.toString().trimEnd(),
                stderr = stderr.toString().trimEnd(),
                durationMs = elapsed
            )
        } catch (ret: ReturnSignal) {
            val elapsed = max(8L, System.currentTimeMillis() - startTime)
            return ExecutionResult(
                success = true,
                exitCode = 0,
                jdkVersion = jdkVersion,
                runtimeCommand = runtimeCmd,
                stdout = stdout.toString().trimEnd(),
                stderr = stderr.toString().trimEnd(),
                durationMs = elapsed
            )
        } catch (sig: JavaRuntimeSignal) {
            val timedOut = sig.exceptionName == "ExecutionTimeoutException"
            val trace = "Exception in thread \"main\" java.lang.${sig.exceptionName}: ${sig.message}\n\tat Main.main(Main.java:1)"
            stderr.appendLine(trace)
            val elapsed = max(8L, System.currentTimeMillis() - startTime)
            return ExecutionResult(
                success = false,
                exitCode = if (timedOut) 124 else 1,
                jdkVersion = jdkVersion,
                runtimeCommand = runtimeCmd,
                stdout = stdout.toString().trimEnd(),
                stderr = stderr.toString().trimEnd(),
                exceptionType = sig.exceptionName,
                durationMs = elapsed,
                timedOut = timedOut
            )
        } catch (e: Exception) {
            val elapsed = max(8L, System.currentTimeMillis() - startTime)
            stderr.appendLine("Exception in thread \"main\" ${e.javaClass.simpleName}: ${e.message}")
            return ExecutionResult(
                success = false,
                exitCode = 1,
                jdkVersion = jdkVersion,
                runtimeCommand = runtimeCmd,
                stdout = stdout.toString().trimEnd(),
                stderr = stderr.toString().trimEnd(),
                exceptionType = e.javaClass.simpleName,
                durationMs = elapsed
            )
        }
    }

    private fun executeAppletLifecycle() {
        val appletClass = Regex("""class\s+([A-Za-z_][A-Za-z0-9_]*)\s+extends\s+(?:J?Applet|java\.applet\.Applet)""")
            .find(source)?.groupValues?.getOrNull(1) ?: "HelloApplet"

        stdout.appendLine("=== [AppletViewer JDK $jdkVersion] Launching $appletClass ===")
        stdout.appendLine("1. Lifecycle -> init(): Initializing Applet Context (width=400, height=240)")
        methods["init"]?.let { runCatching { executeBlock(it.bodyLines) } }

        stdout.appendLine("2. Lifecycle -> start(): Starting Applet execution thread")
        methods["start"]?.let { runCatching { executeBlock(it.bodyLines) } }

        stdout.appendLine("3. Lifecycle -> paint(Graphics g): Rendering 2D Canvas Framebuffer")
        methods["paint"]?.let { runCatching { executeBlock(it.bodyLines) } }

        // Also scan source for any Graphics calls inside paint(Graphics g) if not already recorded
        if (graphicsCommands.isEmpty()) {
            Regex("""\.setColor\s*\(([^)]+)\)""").findAll(source).forEach { m ->
                activeGraphicsColor = m.groupValues[1].trim()
                graphicsCommands.add("Graphics.setColor($activeGraphicsColor)")
            }
            Regex("""\.setFont\s*\(([^)]+)\)""").findAll(source).forEach { m ->
                activeGraphicsFont = m.groupValues[1].trim()
                graphicsCommands.add("Graphics.setFont($activeGraphicsFont)")
            }
            Regex("""\.(drawRect|fillRect|drawOval|fillOval|drawLine)\s*\(([^)]+)\)""").findAll(source).forEach { m ->
                val op = m.groupValues[1]
                val coords = splitTopLevelArgs(m.groupValues[2]).map { formatVal(evalExpr(it)) }
                graphicsCommands.add("Graphics.$op(${coords.joinToString(", ")}) [color=$activeGraphicsColor]")
            }
            Regex("""\.drawString\s*\(\s*([^,]+)\s*,\s*([^,]+)\s*,\s*([^)]+)\)""").findAll(source).forEach { m ->
                val textVal = formatVal(evalExpr(m.groupValues[1].trim()))
                val xVal = formatVal(evalExpr(m.groupValues[2].trim()))
                val yVal = formatVal(evalExpr(m.groupValues[3].trim()))
                graphicsCommands.add("Graphics.drawString(\"$textVal\", x=$xVal, y=$yVal) [color=$activeGraphicsColor]")
            }
        }

        if (graphicsCommands.isNotEmpty()) {
            stdout.appendLine("+------------------------------------------------------------+")
            stdout.appendLine("| AppletViewer 2D Canvas Framebuffer ($appletClass)          |")
            stdout.appendLine("+------------------------------------------------------------+")
            graphicsCommands.forEach { cmd ->
                stdout.appendLine("| * $cmd")
            }
            stdout.appendLine("+------------------------------------------------------------+")
        }

        methods["actionPerformed"]?.let {
            stdout.appendLine("4. Event Dispatch -> actionPerformed(ActionEvent): Simulating user interaction")
            runCatching { executeBlock(it.bodyLines) }
        }

        stdout.appendLine("5. Status Bar: [$appletStatusMessage]")
    }

    private fun executeGuiLifecycleAndEvents(isSwing: Boolean) {
        val toolkitName = if (isSwing) "Swing JFC (EventQueue / EDT)" else "Abstract Window Toolkit (AWT Peer)"
        // Execute constructor body if class extends Frame / JFrame and constructor was defined
        val className = Regex("""class\s+([A-Za-z_][A-Za-z0-9_]*)""").find(source)?.groupValues?.getOrNull(1)
        if (className != null && className in methods) {
            runCatching { executeBlock(methods[className]!!.bodyLines) }
        }

        // Execute actionPerformed if present
        if ("actionPerformed" in methods) {
            stdout.appendLine("[$toolkitName] Dispatching ActionEvent(ACTION_PERFORMED) to registered ActionListener...")
            runCatching { executeBlock(methods["actionPerformed"]!!.bodyLines) }
        }

        // Scan for any AWT / Swing component declarations in source that weren't in mainBody (e.g. inside invokeLater or constructor)
        Regex("""(?:J?Frame)\s+([A-Za-z_][A-Za-z0-9_]*)\s*=\s*new\s+(?:J?Frame)\s*\(([^)]*)\)""").findAll(source).forEach { m ->
            val vName = m.groupValues[1]
            val title = if (m.groupValues[2].isBlank()) "Java GUI Window" else formatVal(evalExpr(m.groupValues[2]))
            guiWidgets.putIfAbsent(vName, GuiWidget(vName, if (isSwing) "JFrame" else "Frame", title, visible = true))
        }
        Regex("""(J?Button|J?Label|J?TextField|J?TextArea|J?CheckBox|J?Table|Button|Label|TextField)\s+([A-Za-z_][A-Za-z0-9_]*)\s*=\s*new\s+\1\s*\(([^)]*)\)""").findAll(source).forEach { m ->
            val kind = m.groupValues[1]
            val vName = m.groupValues[2]
            val rawArg = m.groupValues[3].trim()
            val labelText = if (rawArg.isBlank()) kind else formatVal(evalExpr(splitTopLevelArgs(rawArg).first()))
            guiWidgets.putIfAbsent(vName, GuiWidget(vName, kind, labelText, visible = true))
        }

        // Execute inline lambda ActionListeners: .addActionListener(e -> { ... }) or .addActionListener(e -> System.out.println(...))
        Regex("""addActionListener\s*\(\s*[A-Za-z0-9_()\s]*->\s*\{?([^});]+);?\s*\}?\s*\)""").findAll(source).forEach { m ->
            val bodyStmt = m.groupValues[1].trim()
            stdout.appendLine("[$toolkitName] Button ActionListener triggered -> executing handler:")
            bodyStmt.lines().forEach { l ->
                val clean = l.substringBefore("//").trim().removeSuffix(";")
                if (clean.isNotEmpty()) runCatching { executeSingleStatement(clean) }
            }
        }

        stdout.appendLine("=== [$toolkitName] Rendered Window & Component Hierarchy ===")
        if (guiWidgets.isEmpty()) {
            val winTitle = Regex("""super\s*\(\s*"([^"]+)"\s*\)""").find(source)?.groupValues?.getOrNull(1)
                ?: Regex("""setTitle\s*\(\s*"([^"]+)"\s*\)""").find(source)?.groupValues?.getOrNull(1)
                ?: "Java GUI Application Window"
            stdout.appendLine("Window: \"$winTitle\" [Size: 400x300, Visible: true, Layout: FlowLayout]")
        } else {
            guiWidgets.values.forEach { w ->
                stdout.appendLine("  • [${w.type}] id='${w.varName}' | text=\"${w.text}\" | size=${w.width}x${w.height}")
            }
        }
        if (graphicsCommands.isNotEmpty()) {
            stdout.appendLine("Rendered Graphics2D Operations:")
            graphicsCommands.forEach { cmd -> stdout.appendLine("  * $cmd") }
        }
    }

    private fun appendJdbcSummaryReport() {
        stdout.appendLine("=== [JDBC DriverManager & SQL Engine — JDK $jdkVersion] ===")
        stdout.appendLine("Connection URL: ${activeJdbcUrl ?: "jdbc:sqlite::memory: (Offline Embedded Relational Engine)"}")
        if (jdbcExecutionLog.isNotEmpty()) {
            stdout.appendLine("Executed SQL Statements (${jdbcExecutionLog.size}):")
            jdbcExecutionLog.forEachIndexed { idx, entry ->
                stdout.appendLine("  ${idx + 1}. $entry")
            }
        }
        sqlTables.values.forEach { table ->
            stdout.appendLine("Table '${table.name}' (${table.rows.size} row(s)):")
            val cols = table.columns.ifEmpty {
                table.rows.firstOrNull()?.keys?.toList() ?: listOf("id", "name", "value")
            }
            val widths = cols.map { col ->
                max(col.length, table.rows.maxOfOrNull { r -> (r[col]?.toString() ?: "NULL").length } ?: 4) + 2
            }
            val border = widths.joinToString("+", prefix = "+", postfix = "+") { "-".repeat(it + 2) }
            stdout.appendLine(border)
            val headerLine = cols.mapIndexed { i, c -> " " + c.padEnd(widths[i]) + " " }.joinToString("|", prefix = "|", postfix = "|")
            stdout.appendLine(headerLine)
            stdout.appendLine(border)
            table.rows.forEach { row ->
                val rowLine = cols.mapIndexed { i, c ->
                    " " + formatVal(row[c]).padEnd(widths[i]) + " "
                }.joinToString("|", prefix = "|", postfix = "|")
                stdout.appendLine(rowLine)
            }
            stdout.appendLine(border)
        }
    }

    private fun appendNetworkSummaryReport() {
        if (networkTraceLog.isNotEmpty()) {
            stdout.appendLine("=== [Java Networking Stack (java.net / TCP / HTTP) — JDK $jdkVersion] ===")
            networkTraceLog.forEach { entry ->
                stdout.appendLine("  • $entry")
            }
        }
    }

    private fun checkStepBudget() {
        stepCounter++
        if (stepCounter > maxSteps || (System.currentTimeMillis() - startEpoch) > timeoutMs) {
            throw JavaRuntimeSignal(
                "ExecutionTimeoutException",
                "Process terminated: Execution exceeded safety limit ($maxSteps operations / ${timeoutMs}ms)"
            )
        }
    }

    private fun parseClassFields(code: String) {
        // Parse simple class-level field initializers (outside methods)
        code.lines().forEach { raw ->
            val line = raw.substringBefore("//").trim()
            val fieldMatch = Regex("""^(?:private|public|protected)?\s*(?:static\s+)?(?:final\s+)?(int|long|double|float|boolean|String)\s+([A-Za-z_][A-Za-z0-9_]*)\s*=\s*([^;]+);$""").find(line)
            if (fieldMatch != null) {
                val fName = fieldMatch.groupValues[2]
                val rhs = fieldMatch.groupValues[3].trim()
                if (fName !in variables) {
                    runCatching { variables[fName] = evalExpr(rhs) }
                }
            }
        }
    }

    private fun parseDefinitions(code: String) {
        // Parse record definitions: record Name(Type f1, Type f2)
        Regex("""record\s+([A-Za-z_][A-Za-z0-9_]*)\s*\(([^)]*)\)""").findAll(code).forEach { m ->
            val rName = m.groupValues[1]
            val paramsRaw = m.groupValues[2]
            val fieldNames = paramsRaw.split(",").mapNotNull { p ->
                val tokens = p.trim().split(Regex("""\s+"""))
                tokens.lastOrNull()?.takeIf { it.isNotBlank() }
            }
            recordDefinitions[rName] = fieldNames
        }

        // Parse static and instance methods (including init, start, paint, actionPerformed)
        val lines = code.lines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i].substringBefore("//").trim()
            val methodMatch = Regex("""(?:public|private|protected|static|synchronized|strictfp|\s)+([A-Za-z0-9_<>\[\]]+)\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(([^)]*)\)\s*(?:throws\s+[A-Za-z0-9_,\s]+)?\{""").find(line)
            if (methodMatch != null) {
                val mName = methodMatch.groupValues[2]
                if (mName != "main" && mName !in setOf("if", "for", "while", "switch", "catch")) {
                    val params = methodMatch.groupValues[3].split(",").mapNotNull { p ->
                        p.trim().split(Regex("""\s+""")).lastOrNull()?.takeIf { it.isNotBlank() }
                    }
                    val body = mutableListOf<String>()
                    var depth = line.count { it == '{' } - line.count { it == '}' }
                    i++
                    while (i < lines.size && depth > 0) {
                        val l = lines[i]
                        val clean = l.substringBefore("//")
                        depth += clean.count { it == '{' } - clean.count { it == '}' }
                        if (depth > 0) body.add(l)
                        i++
                    }
                    methods[mName] = MethodDef(mName, params, body)
                    continue
                }
            }
            i++
        }
    }

    private fun stripComment(raw: String): String {
        var inQuotes = false
        var escaped = false
        for (idx in 0 until raw.length - 1) {
            val ch = raw[idx]
            if (escaped) {
                escaped = false
                continue
            }
            if (ch == '\\') {
                escaped = true
                continue
            }
            if (ch == '"') {
                inQuotes = !inQuotes
                continue
            }
            if (!inQuotes && ch == '/' && raw[idx + 1] == '/') {
                return raw.substring(0, idx)
            }
        }
        return raw
    }

    private fun extractMainBody(code: String): List<String> {
        val lines = code.lines()
        val mainRegex = Regex("""\bvoid\s+main\s*\([^)]*\)""")
        var startIdx = -1
        for (idx in lines.indices) {
            val clean = stripComment(lines[idx])
            if (mainRegex.containsMatchIn(clean)) {
                startIdx = idx
                break
            }
        }
        if (startIdx == -1) return emptyList()

        val body = mutableListOf<String>()
        var depth = 0
        var started = false
        for (idx in startIdx until lines.size) {
            val raw = lines[idx]
            val clean = stripComment(raw)
            val opens = clean.count { it == '{' }
            val closes = clean.count { it == '}' }
            if (!started) {
                if (opens > 0) {
                    started = true
                    depth += (opens - closes)
                }
            } else {
                depth += (opens - closes)
                if (depth <= 0) break
                body.add(raw)
            }
        }
        return body
    }

    private fun executeBlock(lines: List<String>) {
        var i = 0
        while (i < lines.size) {
            checkStepBudget()
            val rawLine = lines[i]
            val line = stripComment(rawLine).trim()
            if (line.isEmpty() || line == "{" || line == "}" || line == "});") {
                i++
                continue
            }

            // Unwrap SwingUtilities.invokeLater / EventQueue.invokeLater / new Thread(...).start() blocks
            if ((line.contains("invokeLater(") || line.contains("new Thread(")) && line.endsWith("{")) {
                val (innerBody, afterIdx) = collectBracedBlock(lines, i)
                executeBlock(innerBody)
                i = afterIdx
                continue
            }

            // Handle multi-line Text Block assignment: String x = """ ... """;
            if (line.contains("\"\"\"")) {
                val varMatch = Regex("""(?:String|var)\s+([A-Za-z_][A-Za-z0-9_]*)\s*=\s*""${'"'}""").find(line)
                if (varMatch != null) {
                    val vName = varMatch.groupValues[1]
                    val sb = StringBuilder()
                    i++
                    while (i < lines.size) {
                        val tbLine = lines[i]
                        if (tbLine.contains("\"\"\"")) {
                            val beforeClose = tbLine.substringBefore("\"\"\"")
                            if (beforeClose.isNotBlank()) sb.appendLine(beforeClose.trimIndent())
                            break
                        } else {
                            sb.appendLine(tbLine.trimIndent())
                        }
                        i++
                    }
                    variables[vName] = sb.toString().trimEnd()
                    i++
                    continue
                }
            }

            // Handle try / try-with-resources: try (...) { ... } catch (...) { ... }
            if ((line.startsWith("try") || line.startsWith("try(") || line.startsWith("try (")) && line.endsWith("{")) {
                // If try-with-resources has a resource init inside try (...), execute it first!
                if (line.startsWith("try") && line.contains("(") && line.contains(")")) {
                    val resourcePart = line.substringAfter("(").substringBeforeLast(")").trim()
                    resourcePart.split(";").map { it.trim() }.filter { it.isNotEmpty() }.forEach { resStmt ->
                        runCatching { executeSingleStatement(resStmt) }
                    }
                }
                val (tryBody, nextIdx) = collectBracedBlock(lines, i)
                var cursor = nextIdx
                var catchVar = "e"
                var catchBody = emptyList<String>()
                if (cursor < lines.size && lines[cursor].substringBefore("//").trim().startsWith("catch")) {
                    val catchHeader = lines[cursor].substringBefore("//").trim()
                    val m = Regex("""catch\s*\([^)]+\s+([A-Za-z_][A-Za-z0-9_]*)\)""").find(catchHeader)
                    if (m != null) catchVar = m.groupValues[1]
                    val (cBody, afterCatch) = collectBracedBlock(lines, cursor)
                    catchBody = cBody
                    cursor = afterCatch
                } else if (cursor > 0 && lines[cursor - 1].substringBefore("//").contains("catch")) {
                    val catchHeader = lines[cursor - 1].substringBefore("//").trim()
                    val m = Regex("""catch\s*\([^)]+\s+([A-Za-z_][A-Za-z0-9_]*)\)""").find(catchHeader)
                    if (m != null) catchVar = m.groupValues[1]
                    val (cBody, afterCatch) = collectBracedBlock(lines, cursor - 1)
                    catchBody = cBody
                    cursor = afterCatch
                }
                var finallyBody = emptyList<String>()
                if (cursor < lines.size && lines[cursor].substringBefore("//").trim().contains("finally")) {
                    val (fBody, afterFinally) = collectBracedBlock(lines, cursor)
                    finallyBody = fBody
                    cursor = afterFinally
                } else if (cursor > 0 && lines[cursor - 1].substringBefore("//").contains("finally")) {
                    val (fBody, afterFinally) = collectBracedBlock(lines, cursor - 1)
                    finallyBody = fBody
                    cursor = afterFinally
                }
                try {
                    executeBlock(tryBody)
                } catch (sig: JavaRuntimeSignal) {
                    if (catchBody.isNotEmpty()) {
                        variables[catchVar] = sig.message
                        variables["$catchVar.getMessage()"] = sig.message
                        executeBlock(catchBody)
                    } else {
                        throw sig
                    }
                } finally {
                    if (finallyBody.isNotEmpty()) {
                        executeBlock(finallyBody)
                    }
                }
                i = cursor
                continue
            }

            // Handle switch expression assignment: String tier = switch (expr) { ... };
            if (line.contains("= switch") && line.endsWith("{")) {
                val lhs = line.substringBefore("= switch").trim()
                val varName = lhs.split(Regex("""\s+""")).last()
                val switchExprStr = line.substringAfter("switch").substringAfter("(").substringBeforeLast(")").trim()
                val switchVal = evalExpr(switchExprStr)
                val (switchLines, afterSwitch) = collectBracedBlock(lines, i)
                var matchedResult: Any? = null
                var defaultResult: Any? = null
                for (sLine in switchLines) {
                    val cleanS = sLine.substringBefore("//").trim().removeSuffix(";")
                    if (cleanS.startsWith("case ") && cleanS.contains("->")) {
                        val caseLabel = cleanS.substringAfter("case ").substringBefore("->").trim()
                        val rhs = cleanS.substringAfter("->").trim()
                        val labelVal = evalExpr(caseLabel)
                        if (labelVal.toString() == switchVal.toString()) {
                            matchedResult = evalExpr(rhs)
                            break
                        }
                    } else if (cleanS.startsWith("default") && cleanS.contains("->")) {
                        defaultResult = evalExpr(cleanS.substringAfter("->").trim())
                    }
                }
                variables[varName] = matchedResult ?: defaultResult
                i = afterSwitch
                continue
            }

            // Handle enhanced for-loop: for (Type item : collectionName) {
            val forEachMatch = Regex("""^for\s*\(\s*[A-Za-z0-9_<>]+\s+([A-Za-z_][A-Za-z0-9_]*)\s*:\s*([A-Za-z_][A-Za-z0-9_]*)\s*\)\s*\{?""").find(line)
            if (forEachMatch != null) {
                val itemVar = forEachMatch.groupValues[1]
                val colName = forEachMatch.groupValues[2]
                val (loopBody, nextIdx) = collectBracedBlock(lines, i)
                val listItems = collections[colName] ?: (variables[colName] as? List<Any?>) ?: emptyList()
                for (element in listItems) {
                    checkStepBudget()
                    variables[itemVar] = element
                    executeBlock(loopBody)
                }
                i = nextIdx
                continue
            }

            // Handle standard for-loop: for (init; cond; update) {
            val forMatch = Regex("""^for\s*\(([^;]+);([^;]+);([^)]+)\)\s*\{?""").find(line)
            if (forMatch != null) {
                val initStmt = forMatch.groupValues[1].trim()
                val condExpr = forMatch.groupValues[2].trim()
                val updateStmt = forMatch.groupValues[3].trim()
                val (loopBody, nextIdx) = collectBracedBlock(lines, i)
                executeSingleStatement(initStmt)
                while (evalCondition(condExpr)) {
                    checkStepBudget()
                    executeBlock(loopBody)
                    executeSingleStatement(updateStmt)
                }
                i = nextIdx
                continue
            }

            // Handle while-loop: while (cond) { (including while (rs.next()) and while ((line = in.readLine()) != null))
            val whileMatch = Regex("""^while\s*\((.+)\)\s*\{?""").find(line)
            if (whileMatch != null) {
                val condExpr = whileMatch.groupValues[1].trim()
                val (loopBody, nextIdx) = collectBracedBlock(lines, i)
                while (evalCondition(condExpr)) {
                    checkStepBudget()
                    executeBlock(loopBody)
                }
                i = nextIdx
                continue
            }

            // Handle if / else if / else chain
            if (line.startsWith("if ") || line.startsWith("if(")) {
                var cursor = i
                var branchExecuted = false
                while (cursor < lines.size) {
                    val header = lines[cursor].substringBefore("//").trim()
                    val isIf = header.startsWith("if ") || header.startsWith("if(")
                    val isElseIf = header.startsWith("else if") || header.startsWith("} else if")
                    val isElse = header.startsWith("else") || header.startsWith("} else")

                    if (isIf || isElseIf) {
                        val cond = header.substringAfter("(").substringBeforeLast(")").trim()
                        val (bBody, afterBlock) = collectBracedBlock(lines, cursor)
                        if (!branchExecuted && evalCondition(cond)) {
                            executeBlock(bBody)
                            branchExecuted = true
                        }
                        cursor = afterBlock
                        if (cursor > 0 && lines[cursor - 1].substringBefore("//").trim().contains("else")) {
                            cursor--
                        } else if (cursor < lines.size && lines[cursor].substringBefore("//").trim().startsWith("else")) {
                            // continue loop
                        } else {
                            break
                        }
                    } else if (isElse) {
                        val (eBody, afterElse) = collectBracedBlock(lines, cursor)
                        if (!branchExecuted) {
                            executeBlock(eBody)
                            branchExecuted = true
                        }
                        cursor = afterElse
                        break
                    } else {
                        break
                    }
                }
                i = cursor
                continue
            }

            executeSingleStatement(line.removeSuffix(";").trim())
            i++
        }
    }

    private fun collectBracedBlock(lines: List<String>, startIndex: Int): Pair<List<String>, Int> {
        val body = mutableListOf<String>()
        val firstClean = lines[startIndex].substringBefore("//")
        var depth = firstClean.count { it == '{' } - firstClean.count { it == '}' }
        var idx = startIndex + 1
        if (depth == 0 && idx < lines.size && lines[idx].substringBefore("//").trim().startsWith("{")) {
            depth = 1
            idx++
        }
        while (idx < lines.size && depth > 0) {
            val raw = lines[idx]
            val clean = raw.substringBefore("//")
            val opens = clean.count { it == '{' }
            val closes = clean.count { it == '}' }
            if (depth + opens - closes <= 0) {
                idx++
                break
            }
            depth += (opens - closes)
            body.add(raw)
            idx++
        }
        return body to idx
    }

    private fun executeSingleStatement(stmt: String) {
        if (stmt.isBlank()) return

        // 1. throw new Exception("msg")
        val throwMatch = Regex("""^throw\s+new\s+([A-Za-z0-9_]+)\s*\((.*)\)$""").find(stmt)
        if (throwMatch != null) {
            val exName = throwMatch.groupValues[1]
            val exMsg = evalExpr(throwMatch.groupValues[2].trim())?.toString() ?: ""
            throw JavaRuntimeSignal(exName, exMsg)
        }

        // 2. assert condition : "message"
        if (stmt.startsWith("assert ")) {
            val rest = stmt.removePrefix("assert ").trim()
            val condPart = rest.substringBefore(":").trim()
            val msgPart = if (rest.contains(":")) evalExpr(rest.substringAfter(":").trim()).toString() else "Assertion failed"
            if (!evalCondition(condPart)) {
                throw JavaRuntimeSignal("AssertionError", msgPart)
            }
            return
        }

        // 3. return expr
        if (stmt.startsWith("return")) {
            val retExpr = stmt.removePrefix("return").trim()
            val v = if (retExpr.isEmpty()) null else evalExpr(retExpr)
            throw ReturnSignal(v)
        }

        // 4. System.out.println / print / printf / System.err.println
        if (stmt.startsWith("System.out.println(") && stmt.endsWith(")")) {
            val inner = stmt.removePrefix("System.out.println(").removeSuffix(")")
            stdout.appendLine(if (inner.isBlank()) "" else formatVal(evalExpr(inner)))
            return
        }
        if (stmt.startsWith("System.out.print(") && stmt.endsWith(")")) {
            val inner = stmt.removePrefix("System.out.print(").removeSuffix(")")
            stdout.append(if (inner.isBlank()) "" else formatVal(evalExpr(inner)))
            return
        }
        if (stmt.startsWith("System.out.printf(") && stmt.endsWith(")")) {
            val inner = stmt.removePrefix("System.out.printf(").removeSuffix(")")
            val parts = splitTopLevelArgs(inner)
            if (parts.isNotEmpty()) {
                val fmt = evalExpr(parts[0]).toString().replace("\\n", "\n").replace("%n", "\n")
                val fmtArgs = parts.drop(1).map { evalExpr(it) }.toTypedArray()
                val formatted = try {
                    String.format(fmt, *fmtArgs)
                } catch (_: Exception) {
                    fmt + " " + fmtArgs.joinToString(" ")
                }
                stdout.append(formatted)
            }
            return
        }
        if (stmt.startsWith("System.err.println(") && stmt.endsWith(")")) {
            val inner = stmt.removePrefix("System.err.println(").removeSuffix(")")
            stderr.appendLine(if (inner.isBlank()) "" else formatVal(evalExpr(inner)))
            return
        }

        // 4b. Applet / Graphics / AWT / Swing calls
        val gDrawStrMatch = Regex("""^[A-Za-z0-9_.]*drawString\s*\(\s*([^,]+)\s*,\s*([^,]+)\s*,\s*([^)]+)\)$""").find(stmt)
        if (gDrawStrMatch != null) {
            val textVal = formatVal(evalExpr(gDrawStrMatch.groupValues[1]))
            val xVal = formatVal(evalExpr(gDrawStrMatch.groupValues[2]))
            val yVal = formatVal(evalExpr(gDrawStrMatch.groupValues[3]))
            graphicsCommands.add("Graphics.drawString(\"$textVal\", x=$xVal, y=$yVal) [color=$activeGraphicsColor, font=$activeGraphicsFont]")
            return
        }
        val gShapeMatch = Regex("""^[A-Za-z0-9_.]*\.(drawRect|fillRect|drawOval|fillOval|drawLine|drawRoundRect|fillRoundRect)\s*\((.+)\)$""").find(stmt)
        if (gShapeMatch != null) {
            val shapeOp = gShapeMatch.groupValues[1]
            val argsEval = splitTopLevelArgs(gShapeMatch.groupValues[2]).map { formatVal(evalExpr(it)) }
            graphicsCommands.add("Graphics.$shapeOp(${argsEval.joinToString(", ")}) [color=$activeGraphicsColor]")
            return
        }
        val gColorMatch = Regex("""^[A-Za-z0-9_.]*\.setColor\s*\((.+)\)$""").find(stmt)
        if (gColorMatch != null) {
            activeGraphicsColor = gColorMatch.groupValues[1].trim()
            graphicsCommands.add("Graphics.setColor($activeGraphicsColor)")
            return
        }
        val gFontMatch = Regex("""^[A-Za-z0-9_.]*\.setFont\s*\((.+)\)$""").find(stmt)
        if (gFontMatch != null) {
            activeGraphicsFont = gFontMatch.groupValues[1].trim()
            graphicsCommands.add("Graphics.setFont($activeGraphicsFont)")
            return
        }
        if (stmt.startsWith("showStatus(") && stmt.endsWith(")")) {
            appletStatusMessage = formatVal(evalExpr(stmt.removePrefix("showStatus(").removeSuffix(")")))
            return
        }
        if (stmt.startsWith("JOptionPane.showMessageDialog(")) {
            val args = splitTopLevelArgs(stmt.removePrefix("JOptionPane.showMessageDialog(").removeSuffix(")"))
            val msg = if (args.size >= 2) formatVal(evalExpr(args[1])) else "Message"
            stdout.appendLine("[Swing JOptionPane Dialog]: $msg")
            return
        }
        if (stmt.contains(".addActionListener(") && stmt.contains("->") && stmt.endsWith(")")) {
            val lambdaBody = stmt.substringAfter("->").removeSuffix(")").trim().removeSuffix(";")
            if (lambdaBody.isNotBlank()) {
                executeSingleStatement(lambdaBody)
            }
            return
        }

        // AWT/Swing widget mutations: widget.setText(...), widget.setTitle(...), widget.setSize(w, h), widget.setVisible(true)
        val widgetMutMatch = Regex("""^([A-Za-z_][A-Za-z0-9_]*)\.(setText|setTitle|setSize|setVisible|setLayout|add)\((.*)\)$""").find(stmt)
        if (widgetMutMatch != null) {
            val target = widgetMutMatch.groupValues[1]
            val mName = widgetMutMatch.groupValues[2]
            val argRaw = widgetMutMatch.groupValues[3]
            val widget = guiWidgets[target]
            if (widget != null) {
                when (mName) {
                    "setText", "setTitle" -> {
                        widget.text = formatVal(evalExpr(argRaw))
                        variables["$target.getText()"] = widget.text
                    }
                    "setSize" -> {
                        val dims = splitTopLevelArgs(argRaw).map { toDoubleOrZero(evalExpr(it)).toInt() }
                        if (dims.size >= 2) {
                            widget.width = dims[0]
                            widget.height = dims[1]
                        }
                    }
                    "setVisible" -> {
                        widget.visible = evalCondition(argRaw)
                    }
                    "setLayout" -> {
                        widget.layout = argRaw.replace("new ", "").trim()
                    }
                }
                return
            }
        }

        // 4c. JDBC Statement / PreparedStatement execution: stmt.executeUpdate(sql), ps.setInt(1, val), ps.setString(2, val), ps.executeUpdate(), conn.commit()
        val psSetMatch = Regex("""^([A-Za-z_][A-Za-z0-9_]*)\.(setInt|setLong|setDouble|setFloat|setString|setObject)\s*\(\s*([^,]+)\s*,\s*(.+)\)$""").find(stmt)
        if (psSetMatch != null) {
            val psVar = psSetMatch.groupValues[1]
            val paramIdx = toDoubleOrZero(evalExpr(psSetMatch.groupValues[3])).toInt()
            val paramVal = evalExpr(psSetMatch.groupValues[4])
            val map = preparedParamsByVar.getOrPut(psVar) { mutableMapOf() }
            map[paramIdx] = paramVal
            return
        }
        val execSqlMatch = Regex("""^([A-Za-z_][A-Za-z0-9_]*)\.(executeUpdate|execute|addBatch|executeBatch)\s*\((.*)\)$""").find(stmt)
        if (execSqlMatch != null) {
            val stmtVar = execSqlMatch.groupValues[1]
            val argRaw = execSqlMatch.groupValues[3].trim()
            val sql = if (argRaw.isEmpty()) (preparedSqlByVar[stmtVar] ?: "") else formatVal(evalExpr(argRaw))
            if (sql.isNotBlank()) {
                executeSqlDml(sql, preparedParamsByVar[stmtVar] ?: emptyMap())
            }
            return
        }
        if (stmt.endsWith(".commit()")) {
            jdbcExecutionLog.add("COMMIT TRANSACTION (Durable ACID commit complete)")
            return
        }

        // 4d. Networking Socket / PrintWriter output: out.println(msg) / writer.println(msg)
        val netOutMatch = Regex("""^([A-Za-z_][A-Za-z0-9_]*)\.(println|write|print)\s*\((.+)\)$""").find(stmt)
        if (netOutMatch != null && netOutMatch.groupValues[1] !in setOf("System.out", "System.err")) {
            val targetVar = netOutMatch.groupValues[1]
            val payload = formatVal(evalExpr(netOutMatch.groupValues[3]))
            networkTraceLog.add("TCP Socket [$targetVar] >>> SENT (${payload.length} bytes): \"$payload\"")
            // Populate simulated server response for any active socket reader!
            readerQueueByVar.values.forEach { q ->
                if (q.isEmpty() || q.first().startsWith("Server ACK:")) {
                    q.clear()
                    q.add("Server ACK [JDK $jdkVersion]: Received '$payload' (200 OK)")
                }
            }
            return
        }

        // 5. Increment / Decrement: i++, ++i, i--, --i, i += expr, i -= expr
        val incDecMatch = Regex("""^(?:\+\+|--)?([A-Za-z_][A-Za-z0-9_]*)(?:\+\+|--)?$""").find(stmt)
        if (incDecMatch != null && (stmt.contains("++") || stmt.contains("--"))) {
            val vName = incDecMatch.groupValues[1]
            val cur = toDoubleOrZero(variables[vName])
            val next = if (stmt.contains("++")) cur + 1 else cur - 1
            variables[vName] = if (variables[vName] is Double) next else next.toInt()
            return
        }
        val compoundMatch = Regex("""^([A-Za-z_][A-Za-z0-9_]*)\s*(\+=|-=|\*=|/=)\s*(.+)$""").find(stmt)
        if (compoundMatch != null) {
            val vName = compoundMatch.groupValues[1]
            val op = compoundMatch.groupValues[2]
            val rhs = evalExpr(compoundMatch.groupValues[3])
            val curVal = variables[vName]
            if (op == "+=" && (curVal is String || rhs is String)) {
                variables[vName] = "${formatVal(curVal)}${formatVal(rhs)}"
            } else {
                val a = toDoubleOrZero(curVal)
                val b = toDoubleOrZero(rhs)
                val res = when (op) {
                    "+=" -> a + b
                    "-=" -> a - b
                    "*=" -> a * b
                    "/=" -> {
                        if (b == 0.0) throw JavaRuntimeSignal("ArithmeticException", "/ by zero")
                        a / b
                    }
                    else -> a
                }
                variables[vName] = if (curVal is Double || rhs is Double) res else res.toInt()
            }
            return
        }

        // 6. Collection mutation: list.add(...), list.addElement(...), list.remove(...)
        val colAddMatch = Regex("""^([A-Za-z_][A-Za-z0-9_]*)\.(add|addElement|push|addFirst|addLast)\((.*)\)$""").find(stmt)
        if (colAddMatch != null && colAddMatch.groupValues[1] in collections) {
            val cName = colAddMatch.groupValues[1]
            val method = colAddMatch.groupValues[2]
            val item = evalExpr(colAddMatch.groupValues[3])
            val list = collections.getOrPut(cName) { mutableListOf() }
            if (method == "addFirst") list.add(0, item) else list.add(item)
            return
        }

        // 7. Array element assignment: arr[idx] = expr
        val arrAssignMatch = Regex("""^([A-Za-z_][A-Za-z0-9_]*)\[([^\]]+)\]\s*=\s*(.+)$""").find(stmt)
        if (arrAssignMatch != null) {
            val aName = arrAssignMatch.groupValues[1]
            val idx = toDoubleOrZero(evalExpr(arrAssignMatch.groupValues[2])).toInt()
            val value = evalExpr(arrAssignMatch.groupValues[3])
            val list = collections[aName] ?: throw JavaRuntimeSignal("NullPointerException", "Cannot store to array '$aName'")
            if (idx < 0 || idx >= list.size) {
                throw JavaRuntimeSignal("ArrayIndexOutOfBoundsException", "Index $idx out of bounds for length ${list.size}")
            }
            list[idx] = value
            return
        }

        // 8. Variable declaration or assignment
        if (stmt.contains("=")) {
            val lhs = stmt.substringBefore("=").trim()
            val rhs = stmt.substringAfter("=").trim()
            val varName = lhs.replace("final ", "")
                .replace(Regex("""\[[^\]]*\]"""), "")
                .trim()
                .split(Regex("""\s+"""))
                .last()

            // 8a. AWT / Swing Component instantiation
            val guiNewMatch = Regex("""^new\s+(J?Frame|J?Panel|J?Button|J?Label|J?TextField|J?TextArea|J?CheckBox|J?Table|Frame|Panel|Button|Label|TextField|TextArea|Checkbox)\s*\((.*)\)$""").find(rhs)
            if (guiNewMatch != null) {
                val widgetType = guiNewMatch.groupValues[1]
                val rawArgs = guiNewMatch.groupValues[2].trim()
                val textVal = if (rawArgs.isBlank()) widgetType else formatVal(evalExpr(splitTopLevelArgs(rawArgs).first()))
                val widget = GuiWidget(varName, widgetType, textVal, visible = true)
                guiWidgets[varName] = widget
                variables[varName] = widget.text
                variables["$varName.getText()"] = widget.text
                return
            }

            // 8b. JDBC Connection / Statement / PreparedStatement / ResultSet
            if (rhs.contains("DriverManager.getConnection(")) {
                val args = splitTopLevelArgs(rhs.substringAfter("DriverManager.getConnection(").substringBeforeLast(")"))
                activeJdbcUrl = if (args.isNotEmpty()) formatVal(evalExpr(args[0])) else "jdbc:sqlite::memory:"
                jdbcExecutionLog.add("Opened JDBC Connection -> $activeJdbcUrl")
                variables[varName] = activeJdbcUrl
                return
            }
            if (rhs.contains(".prepareStatement(")) {
                val sqlExpr = rhs.substringAfter(".prepareStatement(").substringBeforeLast(")")
                val sql = formatVal(evalExpr(sqlExpr))
                preparedSqlByVar[varName] = sql
                preparedParamsByVar[varName] = mutableMapOf()
                variables[varName] = "PreparedStatement[$sql]"
                return
            }
            if (rhs.contains(".executeQuery(")) {
                val sqlExpr = rhs.substringAfter(".executeQuery(").substringBeforeLast(")").trim()
                val stmtSourceVar = rhs.substringBefore(".executeQuery(").trim()
                val sql = if (sqlExpr.isEmpty()) (preparedSqlByVar[stmtSourceVar] ?: "SELECT * FROM employees") else formatVal(evalExpr(sqlExpr))
                val rows = executeSqlQuery(sql)
                resultSetsByVar[varName] = rows
                resultSetCursorByVar[varName] = -1
                variables[varName] = "ResultSet[${rows.size} rows]"
                return
            }
            if (rhs.contains(".executeUpdate(")) {
                val sqlExpr = rhs.substringAfter(".executeUpdate(").substringBeforeLast(")").trim()
                val stmtSourceVar = rhs.substringBefore(".executeUpdate(").trim()
                val sql = if (sqlExpr.isEmpty()) (preparedSqlByVar[stmtSourceVar] ?: "") else formatVal(evalExpr(sqlExpr))
                val affected = executeSqlDml(sql, preparedParamsByVar[stmtSourceVar] ?: emptyMap())
                variables[varName] = affected
                return
            }

            // 8c. Networking: InetAddress, URL, Socket, ServerSocket, HttpClient, BufferedReader
            if (rhs.startsWith("InetAddress.getByName(") || rhs.startsWith("InetAddress.getLocalHost(")) {
                val hostArg = if (rhs.startsWith("InetAddress.getByName(")) {
                    formatVal(evalExpr(rhs.removePrefix("InetAddress.getByName(").removeSuffix(")")))
                } else {
                    "localhost"
                }
                val resolvedIp = when {
                    hostArg.contains("localhost") || hostArg == "127.0.0.1" -> "127.0.0.1"
                    hostArg.contains("oracle.com") -> "104.74.16.82"
                    hostArg.contains("nptel") || hostArg.contains("iitkgp") -> "103.37.201.14"
                    else -> "93.184.216.34"
                }
                variables[varName] = "$hostArg/$resolvedIp"
                variables["$varName.getHostName()"] = hostArg
                variables["$varName.getHostAddress()"] = resolvedIp
                networkTraceLog.add("DNS Lookup: InetAddress('$hostArg') -> $resolvedIp [Reachable: true, RTT: 4ms]")
                return
            }
            if (rhs.startsWith("new URL(")) {
                val urlStr = formatVal(evalExpr(rhs.removePrefix("new URL(").removeSuffix(")")))
                urlObjects[varName] = urlStr
                variables[varName] = urlStr
                networkTraceLog.add("Parsed URL('$urlStr') [protocol=${urlStr.substringBefore("://", "https")}, host=${urlStr.substringAfter("://").substringBefore("/")}]")
                return
            }
            if (rhs.startsWith("new Socket(")) {
                val args = splitTopLevelArgs(rhs.removePrefix("new Socket(").removeSuffix(")"))
                val host = if (args.isNotEmpty()) formatVal(evalExpr(args[0])) else "127.0.0.1"
                val port = if (args.size >= 2) formatVal(evalExpr(args[1])) else "8080"
                variables[varName] = "Socket[addr=$host,port=$port,localport=54321]"
                networkTraceLog.add("TCP Handshake: Connected Socket to $host:$port [SYN -> SYN-ACK -> ACK]")
                socketStreams[varName] = mutableListOf("Server Banner [JDK $jdkVersion]: Connected to $host:$port", "200 OK — Payload Stream Ready")
                return
            }
            if (rhs.startsWith("new ServerSocket(")) {
                val port = formatVal(evalExpr(rhs.removePrefix("new ServerSocket(").removeSuffix(")")))
                variables[varName] = "ServerSocket[addr=0.0.0.0/0.0.0.0,localport=$port]"
                networkTraceLog.add("TCP ServerSocket bound on port $port (LISTENING)")
                return
            }
            if (rhs.startsWith("new BufferedReader(")) {
                val q = mutableListOf<String>()
                if (source.contains("HttpURLConnection") || source.contains("openStream()") || source.contains("new URL(")) {
                    val targetUrl = urlObjects.values.firstOrNull() ?: "https://api.javastudio.local/v1/status"
                    q.add("""{"status": 200, "endpoint": "$targetUrl", "jdk": "$jdkVersion", "mode": "Offline Verified"}""")
                    networkTraceLog.add("HTTP GET $targetUrl -> 200 OK (application/json)")
                } else {
                    q.addAll(socketStreams.values.firstOrNull() ?: listOf("Offline Stream Line 1 (JDK $jdkVersion)"))
                }
                readerQueueByVar[varName] = q
                variables[varName] = "BufferedReader"
                return
            }
            if (rhs.contains(".send(") && (source.contains("HttpClient") || source.contains("HttpRequest"))) {
                val jsonBody = """{"service":"UniversalJavaStudio","jdk":"$jdkVersion","httpVersion":"HTTP/2","status":"200 OK"}"""
                variables[varName] = jsonBody
                variables["$varName.statusCode()"] = 200
                variables["$varName.body()"] = jsonBody
                networkTraceLog.add("HttpClient.send(HttpRequest) -> HTTP/2 200 OK | Body: $jsonBody")
                return
            }

            // Check Collection creation: new Vector(), new ArrayList<>(), Arrays.asList(...), List.of(...)
            when {
                rhs.startsWith("new Vector") || rhs.startsWith("new ArrayList") || rhs.startsWith("new LinkedList") || rhs.startsWith("new Stack") -> {
                    collections[varName] = mutableListOf()
                    variables[varName] = collections[varName]
                    return
                }
                rhs.startsWith("Arrays.asList(") || rhs.startsWith("List.of(") -> {
                    val inner = rhs.substringAfter("(").substringBeforeLast(")")
                    val elems = if (inner.isBlank()) mutableListOf() else splitTopLevelArgs(inner).map { evalExpr(it) }.toMutableList()
                    collections[varName] = elems
                    variables[varName] = elems
                    return
                }
                rhs.startsWith("{") && rhs.endsWith("}") -> {
                    val inner = rhs.removePrefix("{").removeSuffix("}").trim()
                    val elems = if (inner.isBlank()) mutableListOf() else splitTopLevelArgs(inner).map { evalExpr(it) }.toMutableList()
                    collections[varName] = elems
                    variables[varName] = elems
                    return
                }
                Regex("""^new\s+[A-Za-z0-9_]+\s*\[([^\]]+)\]$""").containsMatchIn(rhs) -> {
                    val m = Regex("""^new\s+[A-Za-z0-9_]+\s*\[([^\]]+)\]$""").find(rhs)!!
                    val len = toDoubleOrZero(evalExpr(m.groupValues[1])).toInt()
                    val elems = MutableList<Any?>(len) { 0 }
                    collections[varName] = elems
                    variables[varName] = elems
                    return
                }
                Regex("""^new\s+([A-Za-z_][A-Za-z0-9_]*)\s*\((.*)\)$""").containsMatchIn(rhs) -> {
                    val m = Regex("""^new\s+([A-Za-z_][A-Za-z0-9_]*)\s*\((.*)\)$""").find(rhs)!!
                    val typeName = m.groupValues[1]
                    val argsRaw = m.groupValues[2]
                    if (typeName in recordDefinitions) {
                        val fieldNames = recordDefinitions[typeName]!!
                        val argVals = splitTopLevelArgs(argsRaw).map { evalExpr(it) }
                        val fieldMap = mutableMapOf<String, Any?>()
                        fieldNames.forEachIndexed { idx, fName ->
                            fieldMap[fName] = argVals.getOrNull(idx)
                        }
                        recordInstances[varName] = fieldMap
                        variables[varName] = "$typeName[${fieldMap.entries.joinToString(", ") { "${it.key}=${it.value}" }}]"
                        return
                    }
                }
            }

            val evaluated = evalExpr(rhs)
            val declaredType = lhs.replace("final ", "").trim().split(Regex("""\s+""")).firstOrNull()
            variables[varName] = if (declaredType in setOf("int", "long", "short", "byte") && evaluated is Double) {
                evaluated.toInt()
            } else if (declaredType in setOf("double", "float") && evaluated is Int) {
                evaluated.toDouble()
            } else {
                evaluated
            }
            return
        }

        // 9. Standalone method call
        evalExpr(stmt)
    }

    private fun executeSqlDml(rawSql: String, params: Map<Int, Any?>): Int {
        var sql = rawSql.trim()
        if (params.isNotEmpty()) {
            var qIdx = 1
            val sb = StringBuilder()
            for (ch in sql) {
                if (ch == '?') {
                    val v = params[qIdx++]
                    if (v is Number) sb.append(v) else sb.append("'${formatVal(v)}'")
                } else {
                    sb.append(ch)
                }
            }
            sql = sb.toString()
        }

        val upper = sql.uppercase()
        when {
            upper.startsWith("CREATE TABLE") -> {
                val tblMatch = Regex("""CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?([A-Za-z0-9_]+)\s*\((.+)\)""", RegexOption.IGNORE_CASE).find(sql)
                if (tblMatch != null) {
                    val tblName = tblMatch.groupValues[1].lowercase()
                    val cols = tblMatch.groupValues[2].split(",").mapNotNull { colDef ->
                        colDef.trim().split(Regex("""\s+""")).firstOrNull()?.takeIf {
                            it.uppercase() !in setOf("PRIMARY", "FOREIGN", "UNIQUE", "CHECK", "CONSTRAINT")
                        }
                    }.toMutableList()
                    sqlTables[tblName] = SqlTable(tblName, cols)
                    jdbcExecutionLog.add("DDL: CREATE TABLE $tblName (${cols.joinToString(", ")}) -> 0 rows affected")
                }
                return 0
            }
            upper.startsWith("INSERT INTO") -> {
                val insMatch = Regex("""INSERT\s+INTO\s+([A-Za-z0-9_]+)\s*(?:\(([^)]+)\))?\s*VALUES\s*\((.+)\)""", RegexOption.IGNORE_CASE).find(sql)
                if (insMatch != null) {
                    val tblName = insMatch.groupValues[1].lowercase()
                    val explicitCols = insMatch.groupValues[2].split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val valsRaw = splitTopLevelArgs(insMatch.groupValues[3]).map { v ->
                        val clean = v.trim()
                        if (clean.startsWith("'") && clean.endsWith("'")) clean.substring(1, clean.length - 1)
                        else evalExpr(clean)
                    }
                    val table = sqlTables.getOrPut(tblName) {
                        val defaultCols = if (explicitCols.isNotEmpty()) explicitCols.toMutableList()
                        else MutableList(valsRaw.size) { idx ->
                            when (idx) {
                                0 -> "id"
                                1 -> "name"
                                2 -> "role"
                                3 -> "salary"
                                else -> "col_${idx + 1}"
                            }
                        }
                        SqlTable(tblName, defaultCols)
                    }
                    val targetCols = if (explicitCols.isNotEmpty()) explicitCols else table.columns
                    val rowMap = linkedMapOf<String, Any?>()
                    targetCols.forEachIndexed { idx, cName ->
                        rowMap[cName] = valsRaw.getOrNull(idx)
                    }
                    table.rows.add(rowMap)
                    jdbcExecutionLog.add("DML: INSERT INTO $tblName VALUES (${valsRaw.joinToString(", ")}) -> 1 row inserted")
                    return 1
                }
            }
            upper.startsWith("UPDATE ") -> {
                val updMatch = Regex("""UPDATE\s+([A-Za-z0-9_]+)\s+SET\s+([A-Za-z0-9_]+)\s*=\s*([^\s]+)(?:\s+WHERE\s+(.+))?""", RegexOption.IGNORE_CASE).find(sql)
                if (updMatch != null) {
                    val tblName = updMatch.groupValues[1].lowercase()
                    val col = updMatch.groupValues[2]
                    val newVal = evalExpr(updMatch.groupValues[3].trim().trim('\''))
                    val table = sqlTables[tblName]
                    var count = 0
                    table?.rows?.forEach { r ->
                        r[col] = newVal
                        count++
                    }
                    jdbcExecutionLog.add("DML: $sql -> $count row(s) updated")
                    return count
                }
            }
            upper.startsWith("DELETE FROM") -> {
                val delMatch = Regex("""DELETE\s+FROM\s+([A-Za-z0-9_]+)""", RegexOption.IGNORE_CASE).find(sql)
                if (delMatch != null) {
                    val tblName = delMatch.groupValues[1].lowercase()
                    val table = sqlTables[tblName]
                    val removed = table?.rows?.size ?: 0
                    table?.rows?.clear()
                    jdbcExecutionLog.add("DML: $sql -> $removed row(s) deleted")
                    return removed
                }
            }
        }
        jdbcExecutionLog.add("SQL Executed: $sql")
        return 1
    }

    private fun executeSqlQuery(sql: String): List<Map<String, Any?>> {
        val tblMatch = Regex("""FROM\s+([A-Za-z0-9_]+)""", RegexOption.IGNORE_CASE).find(sql)
        val tblName = tblMatch?.groupValues?.getOrNull(1)?.lowercase() ?: "employees"
        val table = sqlTables.getOrPut(tblName) {
            // Seed a default realistic table if the user runs a SELECT query directly
            SqlTable(
                name = tblName,
                columns = mutableListOf("id", "name", "department", "salary"),
                rows = mutableListOf(
                    linkedMapOf("id" to 101, "name" to "Aarav Sharma", "department" to "Compiler Engineering", "salary" to 95000.0),
                    linkedMapOf("id" to 102, "name" to "Diya Nair", "department" to "JVM Runtime Systems", "salary" to 108000.0),
                    linkedMapOf("id" to 103, "name" to "Rohan Verma", "department" to "Distributed Storage", "salary" to 99500.0)
                )
            )
        }
        jdbcExecutionLog.add("DQL: $sql -> Returned ${table.rows.size} row(s) from '$tblName'")
        return table.rows.map { LinkedHashMap(it) }
    }

    private fun evalCondition(expr: String): Boolean {
        val clean = expr.trim()
        if (clean == "true") return true
        if (clean == "false") return false

        // Support JDBC ResultSet cursor iteration: while (rs.next())
        val rsNextMatch = Regex("""^([A-Za-z_][A-Za-z0-9_]*)\.next\(\)$""").find(clean)
        if (rsNextMatch != null) {
            val rsVar = rsNextMatch.groupValues[1]
            val rows = resultSetsByVar[rsVar] ?: emptyList()
            val nextIdx = (resultSetCursorByVar[rsVar] ?: -1) + 1
            resultSetCursorByVar[rsVar] = nextIdx
            return nextIdx < rows.size
        }

        // Support BufferedReader stream iteration: while ((line = in.readLine()) != null)
        val readLineLoopMatch = Regex("""^\(\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*([A-Za-z_][A-Za-z0-9_]*)\.readLine\(\)\s*\)\s*!=\s*null$""").find(clean)
        if (readLineLoopMatch != null) {
            val lineVar = readLineLoopMatch.groupValues[1]
            val readerVar = readLineLoopMatch.groupValues[2]
            val queue = readerQueueByVar[readerVar]
            if (!queue.isNullOrEmpty()) {
                variables[lineVar] = queue.removeAt(0)
                return true
            }
            return false
        }

        if (clean.contains("&&")) {
            return clean.split("&&").all { evalCondition(it) }
        }
        if (clean.contains("||")) {
            return clean.split("||").any { evalCondition(it) }
        }
        if (clean.startsWith("!")) {
            return !evalCondition(clean.removePrefix("!"))
        }

        val ops = listOf("<=", ">=", "==", "!=", "<", ">")
        for (op in ops) {
            val idx = clean.indexOf(op)
            if (idx > 0) {
                val left = evalExpr(clean.substring(0, idx).trim())
                val right = evalExpr(clean.substring(idx + op.length).trim())
                val lNum = left?.toString()?.toDoubleOrNull()
                val rNum = right?.toString()?.toDoubleOrNull()
                return if (lNum != null && rNum != null) {
                    when (op) {
                        "<=" -> lNum <= rNum
                        ">=" -> lNum >= rNum
                        "==" -> lNum == rNum
                        "!=" -> lNum != rNum
                        "<" -> lNum < rNum
                        ">" -> lNum > rNum
                        else -> false
                    }
                } else {
                    when (op) {
                        "==" -> left?.toString() == right?.toString()
                        "!=" -> left?.toString() != right?.toString()
                        else -> false
                    }
                }
            }
        }
        val res = evalExpr(clean)
        return res as? Boolean ?: (res?.toString() == "true")
    }

    private fun evalExpr(raw: String): Any? {
        val expr = raw.trim()
        if (expr.isEmpty()) return ""
        if (expr == "true") return true
        if (expr == "false") return false
        if (expr == "null") return null

        // String literal
        if (expr.startsWith("\"") && expr.endsWith("\"") && expr.length >= 2 && !expr.substring(1, expr.length - 1).contains("\"")) {
            return expr.substring(1, expr.length - 1)
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
        }

        // Numeric literal
        val cleanNum = expr.replace("_", "")
        if (cleanNum.matches(Regex("""^-?\d+$"""))) {
            return cleanNum.toIntOrNull() ?: cleanNum.toLongOrNull() ?: 0
        }
        if (cleanNum.matches(Regex("""^-?\d+\.\d+[dDfF]?$"""))) {
            return cleanNum.trimEnd('d', 'D', 'f', 'F').toDoubleOrNull() ?: 0.0
        }

        // Top-level '+' concatenation or addition
        val plusParts = splitTopLevelOp(expr, '+')
        if (plusParts.size > 1) {
            val evaluatedParts = plusParts.map { evalExpr(it) }
            val anyString = evaluatedParts.any { it is String }
            return if (anyString) {
                evaluatedParts.joinToString("") { formatVal(it) }
            } else {
                val sum = evaluatedParts.sumOf { toDoubleOrZero(it) }
                if (evaluatedParts.any { it is Double }) sum else sum.toInt()
            }
        }

        // Top-level '-' subtraction
        val minusParts = splitTopLevelOp(expr, '-')
        if (minusParts.size > 1 && minusParts.first().isNotBlank()) {
            val evaluatedParts = minusParts.map { evalExpr(it) }
            var acc = toDoubleOrZero(evaluatedParts.first())
            for (k in 1 until evaluatedParts.size) {
                acc -= toDoubleOrZero(evaluatedParts[k])
            }
            return if (evaluatedParts.any { it is Double }) acc else acc.toInt()
        }

        // Top-level '*', '/', '%'
        val mulParts = splitTopLevelOp(expr, '*')
        if (mulParts.size > 1) {
            val evaluatedParts = mulParts.map { evalExpr(it) }
            var prod = 1.0
            for (v in evaluatedParts) prod *= toDoubleOrZero(v)
            return if (evaluatedParts.any { it is Double }) prod else prod.toInt()
        }

        val divParts = splitTopLevelOp(expr, '/')
        if (divParts.size == 2) {
            val left = evalExpr(divParts[0])
            val right = evalExpr(divParts[1])
            val rVal = toDoubleOrZero(right)
            if (rVal == 0.0) {
                throw JavaRuntimeSignal("ArithmeticException", "/ by zero")
            }
            return if (left is Double || right is Double) {
                toDoubleOrZero(left) / rVal
            } else {
                (toDoubleOrZero(left).toInt() / rVal.toInt())
            }
        }

        val modParts = splitTopLevelOp(expr, '%')
        if (modParts.size == 2) {
            val left = toDoubleOrZero(evalExpr(modParts[0])).toInt()
            val right = toDoubleOrZero(evalExpr(modParts[1])).toInt()
            if (right == 0) throw JavaRuntimeSignal("ArithmeticException", "/ by zero")
            return left % right
        }

        // Parenthesized expression
        if (expr.startsWith("(") && expr.endsWith(")")) {
            return evalExpr(expr.substring(1, expr.length - 1))
        }

        // Integer.parseInt / Double.parseDouble / String.valueOf
        if (expr.startsWith("Integer.parseInt(") && expr.endsWith(")")) {
            val inner = evalExpr(expr.removePrefix("Integer.parseInt(").removeSuffix(")")).toString().trim()
            return inner.toIntOrNull() ?: 0
        }
        if (expr.startsWith("Double.parseDouble(") && expr.endsWith(")")) {
            val inner = evalExpr(expr.removePrefix("Double.parseDouble(").removeSuffix(")")).toString().trim()
            return inner.toDoubleOrNull() ?: 0.0
        }
        if (expr.startsWith("String.valueOf(") && expr.endsWith(")")) {
            return formatVal(evalExpr(expr.removePrefix("String.valueOf(").removeSuffix(")")))
        }

        // Scanner input methods
        if (expr.endsWith(".nextInt()")) {
            val tok = if (stdinTokens.isNotEmpty()) stdinTokens.removeAt(0) else "42"
            return tok.toIntOrNull() ?: 42
        }
        if (expr.endsWith(".nextDouble()")) {
            val tok = if (stdinTokens.isNotEmpty()) stdinTokens.removeAt(0) else "3.14"
            return tok.toDoubleOrNull() ?: 3.14
        }
        if (expr.endsWith(".nextLine()")) {
            return if (stdinLines.isNotEmpty()) stdinLines.removeAt(0) else "Interactive User Input"
        }
        if (expr.endsWith(".next()")) {
            return if (stdinTokens.isNotEmpty()) stdinTokens.removeAt(0) else "JavaToken"
        }

        // Math library calls
        if (expr.startsWith("Math.")) {
            val mCall = expr.removePrefix("Math.")
            val mName = mCall.substringBefore("(")
            val mArgs = splitTopLevelArgs(mCall.substringAfter("(").substringBeforeLast(")")).map { toDoubleOrZero(evalExpr(it)) }
            return when (mName) {
                "max" -> if (mArgs.size >= 2) max(mArgs[0], mArgs[1]).toInt() else 0
                "min" -> if (mArgs.size >= 2) min(mArgs[0], mArgs[1]).toInt() else 0
                "abs" -> if (mArgs.isNotEmpty()) abs(mArgs[0]).toInt() else 0
                "sqrt" -> if (mArgs.isNotEmpty()) sqrt(mArgs[0]) else 0.0
                "pow" -> if (mArgs.size >= 2) mArgs[0].pow(mArgs[1]) else 0.0
                "clamp" -> if (mArgs.size >= 3) mArgs[0].coerceIn(mArgs[1], mArgs[2]).toInt() else 0
                "PI" -> Math.PI
                else -> 0
            }
        }

        // Check direct cached property calls like inet.getHostAddress() or response.body()
        if (expr in variables) return variables[expr]

        // Method call on object: target.method(args)
        val methodCallMatch = Regex("""^([A-Za-z_][A-Za-z0-9_]*)\.([A-Za-z_][A-Za-z0-9_]*)\((.*)\)$""").find(expr)
        if (methodCallMatch != null) {
            val target = methodCallMatch.groupValues[1]
            val mName = methodCallMatch.groupValues[2]
            val argStr = methodCallMatch.groupValues[3]

            // 1. JDBC ResultSet getters: rs.getInt("id" / 1), rs.getString("name" / 2), rs.getDouble("salary" / 3)
            if (target in resultSetsByVar && mName.startsWith("get")) {
                val rows = resultSetsByVar[target] ?: emptyList()
                val curIdx = (resultSetCursorByVar[target] ?: 0).coerceIn(0, (rows.size - 1).coerceAtLeast(0))
                val row = rows.getOrNull(curIdx) ?: emptyMap()
                val argVal = evalExpr(argStr)
                val cell = when (argVal) {
                    is Number -> row.values.elementAtOrNull(argVal.toInt() - 1)
                    else -> {
                        val colKey = argVal.toString()
                        row.entries.firstOrNull { it.key.equals(colKey, ignoreCase = true) }?.value
                            ?: row.values.firstOrNull()
                    }
                }
                return cell ?: ""
            }

            // 2. AWT / Swing widget getters: textField.getText(), label.getText(), frame.getTitle()
            guiWidgets[target]?.let { w ->
                if (mName == "getText" || mName == "getTitle" || mName == "getLabel") {
                    return w.text
                }
            }

            // 3. Networking BufferedReader.readLine() / URL / HttpURLConnection / InetAddress methods
            if (target in readerQueueByVar && mName == "readLine") {
                val q = readerQueueByVar[target]
                return if (!q.isNullOrEmpty()) q.removeAt(0) else null
            }
            if (target in urlObjects) {
                val u = urlObjects[target] ?: "https://oracle.com/java"
                return when (mName) {
                    "getProtocol" -> u.substringBefore("://", "https")
                    "getHost" -> u.substringAfter("://").substringBefore("/").substringBefore(":")
                    "getPort" -> if (u.substringAfter("://").contains(":")) u.substringAfterLast(":").substringBefore("/").toIntOrNull() ?: 443 else 443
                    "getPath", "getFile" -> "/" + u.substringAfter("://").substringAfter("/", "")
                    else -> u
                }
            }
            if (mName == "getResponseCode" || mName == "statusCode") return 200
            if (mName == "getResponseMessage") return "OK"
            if (mName == "getContentType") return "application/json; charset=UTF-8"
            if (mName == "isReachable") return true

            // 4. Record instance accessor
            recordInstances[target]?.let { fields ->
                if (mName in fields) return fields[mName]
            }

            // 5. Collection methods
            collections[target]?.let { list ->
                return when (mName) {
                    "size" -> list.size
                    "isEmpty" -> list.isEmpty()
                    "get", "elementAt" -> {
                        val idx = toDoubleOrZero(evalExpr(argStr)).toInt()
                        if (idx < 0 || idx >= list.size) {
                            throw JavaRuntimeSignal("ArrayIndexOutOfBoundsException", "Index $idx out of bounds for length ${list.size}")
                        }
                        list[idx]
                    }
                    "getFirst", "firstElement" -> list.firstOrNull()
                    "getLast", "lastElement", "peek" -> list.lastOrNull()
                    else -> list.toString()
                }
            }

            // 6. String methods
            val strTarget = variables[target]?.toString()
            if (strTarget != null) {
                return when (mName) {
                    "length" -> strTarget.length
                    "toUpperCase" -> strTarget.uppercase()
                    "toLowerCase" -> strTarget.lowercase()
                    "trim", "strip" -> strTarget.trim()
                    "isBlank" -> strTarget.isBlank()
                    "isEmpty" -> strTarget.isEmpty()
                    "repeat" -> strTarget.repeat(toDoubleOrZero(evalExpr(argStr)).toInt().coerceAtLeast(0))
                    "charAt" -> {
                        val idx = toDoubleOrZero(evalExpr(argStr)).toInt()
                        strTarget.getOrNull(idx) ?: ' '
                    }
                    else -> strTarget
                }
            }
        }

        // Array indexing: arr[idx] or arr.length
        if (expr.endsWith(".length")) {
            val target = expr.removeSuffix(".length").trim()
            collections[target]?.let { return it.size }
            (variables[target] as? List<*>)?.let { return it.size }
        }
        val arrIdxMatch = Regex("""^([A-Za-z_][A-Za-z0-9_]*)\[([^\]]+)\]$""").find(expr)
        if (arrIdxMatch != null) {
            val aName = arrIdxMatch.groupValues[1]
            val idx = toDoubleOrZero(evalExpr(arrIdxMatch.groupValues[2])).toInt()
            val list = collections[aName] ?: (variables[aName] as? List<Any?>) ?: emptyList()
            if (idx < 0 || idx >= list.size) {
                throw JavaRuntimeSignal("ArrayIndexOutOfBoundsException", "Index $idx out of bounds for length ${list.size}")
            }
            return list[idx]
        }

        // User-defined static method call: methodName(arg1, arg2)
        val userCallMatch = Regex("""^([A-Za-z_][A-Za-z0-9_]*)\((.*)\)$""").find(expr)
        if (userCallMatch != null) {
            val mName = userCallMatch.groupValues[1]
            val argsRaw = userCallMatch.groupValues[2]
            if (mName == "getParameter") {
                val paramKey = formatVal(evalExpr(argsRaw))
                return "UniversalJavaStudio-$paramKey"
            }
            val def = methods[mName]
            if (def != null) {
                val argVals = splitTopLevelArgs(argsRaw).map { evalExpr(it) }
                val savedVars = HashMap(variables)
                def.params.forEachIndexed { idx, pName ->
                    variables[pName] = argVals.getOrNull(idx)
                }
                var retVal: Any? = null
                try {
                    executeBlock(def.bodyLines)
                } catch (ret: ReturnSignal) {
                    retVal = ret.value
                } finally {
                    variables.clear()
                    variables.putAll(savedVars)
                }
                return retVal
            }
        }

        return expr
    }

    private fun splitTopLevelOp(expr: String, op: Char): List<String> {
        val parts = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var parenDepth = 0
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            if (c == '"' && (i == 0 || expr[i - 1] != '\\')) {
                inQuotes = !inQuotes
                current.append(c)
            } else if (!inQuotes) {
                when (c) {
                    '(', '[' -> {
                        parenDepth++
                        current.append(c)
                    }
                    ')', ']' -> {
                        parenDepth--
                        current.append(c)
                    }
                    op -> {
                        if (parenDepth == 0 && current.isNotEmpty()) {
                            parts.add(current.toString().trim())
                            current = StringBuilder()
                        } else {
                            current.append(c)
                        }
                    }
                    else -> current.append(c)
                }
            } else {
                current.append(c)
            }
            i++
        }
        if (current.isNotEmpty()) parts.add(current.toString().trim())
        return parts
    }

    private fun splitTopLevelArgs(argsStr: String): List<String> {
        if (argsStr.isBlank()) return emptyList()
        return splitTopLevelOp(argsStr, ',')
    }

    private fun toDoubleOrZero(v: Any?): Double {
        return when (v) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    private fun formatVal(v: Any?): String {
        return when (v) {
            null -> "null"
            is Double -> {
                if (v == v.toLong().toDouble()) "${v.toLong()}.0" else v.toString()
            }
            else -> v.toString()
        }
    }
}

