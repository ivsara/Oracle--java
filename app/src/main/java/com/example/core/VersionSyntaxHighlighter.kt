package com.example.core

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.example.ui.theme.SyntaxAnnotation
import com.example.ui.theme.SyntaxComment
import com.example.ui.theme.SyntaxDeprecated
import com.example.ui.theme.SyntaxKeywordClassic
import com.example.ui.theme.SyntaxKeywordModern
import com.example.ui.theme.SyntaxNumber
import com.example.ui.theme.SyntaxString
import com.example.ui.theme.SyntaxType
import com.example.ui.theme.SyntaxUnsupportedError

enum class DiagnosticCategory {
    UNSUPPORTED_IN_SELECTED_JDK,
    DEPRECATED_OR_REMOVED,
    NEWLY_INTRODUCED_MODERN_FEATURE,
    LEGACY_NOTE
}

data class VersionSyntaxDiagnostic(
    val line: Int,
    val token: String,
    val category: DiagnosticCategory,
    val introducedInJdk: String,
    val message: String
)

data class SyntaxAnalysisReport(
    val jdkVersion: String,
    val lexerProfileName: String,
    val annotatedCode: AnnotatedString,
    val diagnostics: List<VersionSyntaxDiagnostic>,
    val classicKeywordCount: Int,
    val modernFeatureCount: Int,
    val deprecatedFeatureCount: Int,
    val unsupportedFeatureCount: Int
)

object VersionSyntaxHighlighter {

    val baselineKeywords = setOf(
        "abstract", "boolean", "break", "byte", "case", "catch", "char", "class",
        "const", "continue", "default", "do", "double", "else", "extends", "final",
        "finally", "float", "for", "goto", "if", "implements", "import", "instanceof",
        "int", "interface", "long", "native", "new", "package", "private", "protected",
        "public", "return", "short", "static", "super", "switch", "synchronized",
        "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
        "true", "false", "null"
    )

    // Keyword -> Minimum JDK version where it was introduced
    val versionedKeywords = mapOf(
        "strictfp" to "1.2",
        "assert" to "1.4",
        "enum" to "5",
        "module" to "9",
        "requires" to "9",
        "exports" to "9",
        "opens" to "9",
        "provides" to "9",
        "uses" to "9",
        "var" to "10",
        "yield" to "14",
        "record" to "16",
        "sealed" to "17",
        "permits" to "17",
        "when" to "21",
        "ScopedValue" to "25",
        "Gatherers" to "24",
        "ofVirtual" to "21"
    )

    val standardTypes = setOf(
        "String", "Object", "System", "Math", "Thread", "Runnable", "Throwable",
        "Exception", "RuntimeException", "Integer", "Double", "Boolean", "Character",
        "Long", "Short", "Byte", "Float", "StringBuilder", "StringBuffer",
        "Vector", "Hashtable", "Stack", "Enumeration", "List", "ArrayList",
        "LinkedList", "Set", "HashSet", "Map", "HashMap", "TreeMap", "Iterator",
        "Scanner", "Optional", "Stream", "Collectors", "Arrays", "Collections",
        "Files", "Path", "Paths", "HttpClient", "HttpRequest", "HttpResponse",
        "Record", "ScopedValue", "Gatherers", "Applet", "JApplet", "SecurityManager",
        "Frame", "Button", "Label", "TextField", "JFrame", "JButton", "JLabel", "JPanel"
    )

    // Deprecated/Removed APIs -> Pair(DeprecatedInJdk, Description)
    val deprecatedTokens = mapOf(
        "Applet" to ("9" to "java.applet.Applet was deprecated in JDK 9, marked for removal in JDK 17, and removed/obsolete in modern Java."),
        "JApplet" to ("9" to "javax.swing.JApplet was deprecated in JDK 9 and marked for removal in JDK 17."),
        "SecurityManager" to ("17" to "SecurityManager was deprecated for removal in JDK 17 (JEP 411) and permanently disabled in JDK 24+ (JEP 486)."),
        "setSecurityManager" to ("17" to "System.setSecurityManager() is deprecated in JDK 17 and throws UnsupportedOperationException in JDK 24+."),
        "finalize" to ("9" to "Object.finalize() was deprecated in JDK 9 and deprecated for removal in JDK 18 (JEP 421)."),
        "runFinalizersOnExit" to ("1.2" to "Runtime.runFinalizersOnExit() was deprecated in JDK 1.2 and removed in JDK 11."),
        "countStackFrames" to ("1.2" to "Thread.countStackFrames() was deprecated in JDK 1.2 and removed in JDK 22."),
        "destroy" to ("1.5" to "Thread.destroy() was deprecated and removed in JDK 11."),
        "stop" to ("1.2" to "Thread.stop() was deprecated in JDK 1.2 due to monitor state corruption and removed in modern Java."),
        "suspend" to ("1.2" to "Thread.suspend() was deprecated in JDK 1.2 (deadlock prone) and throws UnsupportedOperationException in JDK 20+."),
        "resume" to ("1.2" to "Thread.resume() was deprecated in JDK 1.2 and throws UnsupportedOperationException in JDK 20+.")
    )

    private val identifierRegex = Regex("""\b[A-Za-z_][A-Za-z0-9_]*\b""")
    private val numberRegex = Regex("""\b(?:0b[01_]+|0x[0-9a-fA-F_]+|\d[\d_]*(?:\.[\d_]+)?[LFDlfd]?)\b""")
    private val stringRegex = Regex(""""(?:\\.|[^"\\])*"""")
    private val annotationRegex = Regex("""@[A-Za-z_][A-Za-z0-9_]*""")
    private val genericsRegex = Regex("""<\s*[A-Z][A-Za-z0-9_,\s?<>]*>""")

    fun analyzeAndHighlight(
        sourceCode: String,
        jdkVersion: String,
        isDarkTheme: Boolean = true
    ): SyntaxAnalysisReport {
        val targetRank = JdkCatalogAndSpecifications.versionRank(jdkVersion)
        val diagnostics = mutableListOf<VersionSyntaxDiagnostic>()
        var classicCount = 0
        var modernCount = 0
        var deprecatedCount = 0
        var unsupportedCount = 0

        val lines = sourceCode.lines()
        lines.forEachIndexed { index, lineText ->
            val lineNum = index + 1
            val trimmed = lineText.trim()

            // Check Markdown doc comments (JDK 23+)
            if (trimmed.startsWith("///")) {
                if (targetRank < JdkCatalogAndSpecifications.versionRank("23")) {
                    unsupportedCount++
                    diagnostics.add(
                        VersionSyntaxDiagnostic(
                            line = lineNum,
                            token = "///",
                            category = DiagnosticCategory.UNSUPPORTED_IN_SELECTED_JDK,
                            introducedInJdk = "23",
                            message = "Markdown Javadoc comments ('///') require JDK 23+ (JEP 467); treated as regular comment in JDK $jdkVersion."
                        )
                    )
                } else {
                    modernCount++
                    diagnostics.add(
                        VersionSyntaxDiagnostic(
                            line = lineNum,
                            token = "///",
                            category = DiagnosticCategory.NEWLY_INTRODUCED_MODERN_FEATURE,
                            introducedInJdk = "23",
                            message = "Markdown Documentation Comment (JEP 467) active in JDK $jdkVersion."
                        )
                    )
                }
            }

            if (!trimmed.startsWith("//") && !trimmed.startsWith("*")) {
                // Check 'import module' (JDK 25)
                if (trimmed.startsWith("import module ")) {
                    val minRank = JdkCatalogAndSpecifications.versionRank("25")
                    if (targetRank < minRank) {
                        unsupportedCount++
                        diagnostics.add(
                            VersionSyntaxDiagnostic(
                                line = lineNum,
                                token = "import module",
                                category = DiagnosticCategory.UNSUPPORTED_IN_SELECTED_JDK,
                                introducedInJdk = "25",
                                message = "'import module' declarations are standardized in JDK 25 (unsupported in JDK $jdkVersion)."
                            )
                        )
                    } else {
                        modernCount++
                        diagnostics.add(
                            VersionSyntaxDiagnostic(
                                line = lineNum,
                                token = "import module",
                                category = DiagnosticCategory.NEWLY_INTRODUCED_MODERN_FEATURE,
                                introducedInJdk = "25",
                                message = "Module Import Declaration ('import module') enabled in JDK $jdkVersion."
                            )
                        )
                    }
                }

                // Check Text Blocks """ (JDK 15+)
                if (lineText.contains("\"\"\"")) {
                    val minRank = JdkCatalogAndSpecifications.versionRank("15")
                    if (targetRank < minRank) {
                        unsupportedCount++
                        diagnostics.add(
                            VersionSyntaxDiagnostic(
                                line = lineNum,
                                token = "\"\"\"",
                                category = DiagnosticCategory.UNSUPPORTED_IN_SELECTED_JDK,
                                introducedInJdk = "15",
                                message = "Text Blocks (\"\"\"...\"\"\") require JDK 15+ (unsupported in JDK $jdkVersion)."
                            )
                        )
                    } else {
                        modernCount++
                        diagnostics.add(
                            VersionSyntaxDiagnostic(
                                line = lineNum,
                                token = "\"\"\"",
                                category = DiagnosticCategory.NEWLY_INTRODUCED_MODERN_FEATURE,
                                introducedInJdk = "15",
                                message = "Multi-line Text Block literal (JEP 378) supported in JDK $jdkVersion."
                            )
                        )
                    }
                }

                // Check Lambda or Switch Arrow '->'
                if (lineText.contains("->")) {
                    val isSwitchArrow = trimmed.startsWith("case ") || trimmed.startsWith("default ")
                    val requiredJdk = if (isSwitchArrow) "14" else "8"
                    val minRank = JdkCatalogAndSpecifications.versionRank(requiredJdk)
                    if (targetRank < minRank) {
                        unsupportedCount++
                        diagnostics.add(
                            VersionSyntaxDiagnostic(
                                line = lineNum,
                                token = "->",
                                category = DiagnosticCategory.UNSUPPORTED_IN_SELECTED_JDK,
                                introducedInJdk = requiredJdk,
                                message = if (isSwitchArrow) {
                                    "Switch Expression arrow labels ('case ... ->') require JDK 14+ (selected: JDK $jdkVersion)."
                                } else {
                                    "Lambda expressions ('->') require JDK 8+ (selected: JDK $jdkVersion)."
                                }
                            )
                        )
                    } else {
                        modernCount++
                    }
                }

                // Check Diamond Operator <> (JDK 7+)
                if (lineText.contains("<>")) {
                    val minRank = JdkCatalogAndSpecifications.versionRank("7")
                    if (targetRank < minRank) {
                        unsupportedCount++
                        diagnostics.add(
                            VersionSyntaxDiagnostic(
                                line = lineNum,
                                token = "<>",
                                category = DiagnosticCategory.UNSUPPORTED_IN_SELECTED_JDK,
                                introducedInJdk = "7",
                                message = "Diamond operator ('<>') for generic type inference requires JDK 7+ (selected: JDK $jdkVersion)."
                            )
                        )
                    }
                }

                // Check Generics <Type> (JDK 5+)
                if (genericsRegex.containsMatchIn(lineText)) {
                    val minRank = JdkCatalogAndSpecifications.versionRank("5")
                    if (targetRank < minRank) {
                        unsupportedCount++
                        diagnostics.add(
                            VersionSyntaxDiagnostic(
                                line = lineNum,
                                token = "<T>",
                                category = DiagnosticCategory.UNSUPPORTED_IN_SELECTED_JDK,
                                introducedInJdk = "5",
                                message = "Generics parameterized types (<...>) require JDK 5+ (J2SE 5.0). Not supported in JDK $jdkVersion."
                            )
                        )
                    }
                }

                // Check Annotations @... (JDK 5+)
                annotationRegex.findAll(lineText).forEach { match ->
                    val minRank = JdkCatalogAndSpecifications.versionRank("5")
                    if (targetRank < minRank) {
                        unsupportedCount++
                        diagnostics.add(
                            VersionSyntaxDiagnostic(
                                line = lineNum,
                                token = match.value,
                                category = DiagnosticCategory.UNSUPPORTED_IN_SELECTED_JDK,
                                introducedInJdk = "5",
                                message = "Metadata Annotations (${match.value}) require JDK 5+ (unsupported in JDK $jdkVersion)."
                            )
                        )
                    }
                }

                // Check Wrapper Constructors deprecated in JDK 9+
                if (Regex("""new\s+(Integer|Double|Boolean|Long|Float|Short|Byte|Character)\s*\(""").containsMatchIn(lineText)) {
                    if (targetRank >= JdkCatalogAndSpecifications.versionRank("9")) {
                        deprecatedCount++
                        diagnostics.add(
                            VersionSyntaxDiagnostic(
                                line = lineNum,
                                token = "new Wrapper(...)",
                                category = DiagnosticCategory.DEPRECATED_OR_REMOVED,
                                introducedInJdk = "9",
                                message = "Primitive wrapper constructors are @Deprecated(since=\"9\", forRemoval=true); use valueOf() in JDK $jdkVersion."
                            )
                        )
                    }
                }

                // Check non-sealed contextual keyword (JDK 17+)
                if (lineText.contains("non-sealed")) {
                    if (targetRank < JdkCatalogAndSpecifications.versionRank("17")) {
                        unsupportedCount++
                        diagnostics.add(
                            VersionSyntaxDiagnostic(
                                line = lineNum,
                                token = "non-sealed",
                                category = DiagnosticCategory.UNSUPPORTED_IN_SELECTED_JDK,
                                introducedInJdk = "17",
                                message = "'non-sealed' keyword requires JDK 17+ (selected: JDK $jdkVersion)."
                            )
                        )
                    } else {
                        modernCount++
                    }
                }

                // Check identifiers and versioned keywords
                identifierRegex.findAll(lineText).forEach { match ->
                    val word = match.value
                    if (word in baselineKeywords) {
                        classicCount++
                    }

                    // Single underscore '_' rule across versions
                    if (word == "_") {
                        if (targetRank in JdkCatalogAndSpecifications.versionRank("9")..JdkCatalogAndSpecifications.versionRank("21")) {
                            unsupportedCount++
                            diagnostics.add(
                                VersionSyntaxDiagnostic(
                                    line = lineNum,
                                    token = "_",
                                    category = DiagnosticCategory.UNSUPPORTED_IN_SELECTED_JDK,
                                    introducedInJdk = "22",
                                    message = "Single underscore '_' is a reserved keyword in JDK 9–21; unnamed variables ('_') require JDK 22+ (JEP 456)."
                                )
                            )
                        } else if (targetRank >= JdkCatalogAndSpecifications.versionRank("22")) {
                            modernCount++
                            diagnostics.add(
                                VersionSyntaxDiagnostic(
                                    line = lineNum,
                                    token = "_",
                                    category = DiagnosticCategory.NEWLY_INTRODUCED_MODERN_FEATURE,
                                    introducedInJdk = "22",
                                    message = "Unnamed variable/pattern ('_') active in JDK $jdkVersion (JEP 456)."
                                )
                            )
                        }
                    }

                    versionedKeywords[word]?.let { minJdk ->
                        val minRank = JdkCatalogAndSpecifications.versionRank(minJdk)
                        if (targetRank < minRank) {
                            unsupportedCount++
                            diagnostics.add(
                                VersionSyntaxDiagnostic(
                                    line = lineNum,
                                    token = word,
                                    category = DiagnosticCategory.UNSUPPORTED_IN_SELECTED_JDK,
                                    introducedInJdk = minJdk,
                                    message = "Keyword '$word' was introduced in JDK $minJdk and is not supported in JDK $jdkVersion."
                                )
                            )
                        } else if (word == "strictfp" && targetRank >= JdkCatalogAndSpecifications.versionRank("17")) {
                            deprecatedCount++
                            diagnostics.add(
                                VersionSyntaxDiagnostic(
                                    line = lineNum,
                                    token = "strictfp",
                                    category = DiagnosticCategory.DEPRECATED_OR_REMOVED,
                                    introducedInJdk = "17",
                                    message = "'strictfp' is obsolete/redundant in JDK 17+ (JEP 306 restores always-strict floating-point semantics)."
                                )
                            )
                        } else if (minRank >= JdkCatalogAndSpecifications.versionRank("10")) {
                            modernCount++
                            diagnostics.add(
                                VersionSyntaxDiagnostic(
                                    line = lineNum,
                                    token = word,
                                    category = DiagnosticCategory.NEWLY_INTRODUCED_MODERN_FEATURE,
                                    introducedInJdk = minJdk,
                                    message = "Modern Java feature '$word' (introduced in JDK $minJdk) active in JDK $jdkVersion."
                                )
                            )
                        }
                    }

                    deprecatedTokens[word]?.let { (depJdk, reason) ->
                        val depRank = JdkCatalogAndSpecifications.versionRank(depJdk)
                        if (targetRank >= depRank) {
                            // Avoid flagging unrelated 'stop' unless Thread.stop or .stop(
                            val shouldFlag = if (word in setOf("stop", "suspend", "resume")) {
                                lineText.contains("Thread.$word") || lineText.contains(".$word(")
                            } else true
                            if (shouldFlag) {
                                deprecatedCount++
                                diagnostics.add(
                                    VersionSyntaxDiagnostic(
                                        line = lineNum,
                                        token = word,
                                        category = DiagnosticCategory.DEPRECATED_OR_REMOVED,
                                        introducedInJdk = depJdk,
                                        message = reason
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        val annotated = buildHighlightedString(sourceCode, targetRank, isDarkTheme)
        val spec = JdkCatalogAndSpecifications.getSpec(jdkVersion)

        return SyntaxAnalysisReport(
            jdkVersion = jdkVersion,
            lexerProfileName = "QScintilla-JavaLexer [JDK $jdkVersion | Bytecode v${spec.bytecodeMajor}.${spec.bytecodeMinor}]",
            annotatedCode = annotated,
            diagnostics = diagnostics.distinctBy { "${it.line}:${it.token}:${it.category}" },
            classicKeywordCount = classicCount,
            modernFeatureCount = modernCount,
            deprecatedFeatureCount = deprecatedCount,
            unsupportedFeatureCount = unsupportedCount
        )
    }

    private fun buildHighlightedString(
        code: String,
        targetRank: Int,
        isDarkTheme: Boolean
    ): AnnotatedString {
        return buildAnnotatedString {
            append(code)

            // 1. Highlight Numbers
            numberRegex.findAll(code).forEach { match ->
                addStyle(
                    SpanStyle(color = if (isDarkTheme) SyntaxNumber else Color(0xFFB45309)),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // 2. Highlight Identifiers, Keywords, Types, Modern & Deprecated Tokens
            identifierRegex.findAll(code).forEach { match ->
                val word = match.value
                val start = match.range.first
                val end = match.range.last + 1

                when {
                    word in versionedKeywords -> {
                        val minJdk = versionedKeywords[word]!!
                        val minRank = JdkCatalogAndSpecifications.versionRank(minJdk)
                        when {
                            targetRank < minRank -> {
                                // Unsupported in selected older JDK!
                                addStyle(
                                    SpanStyle(
                                        color = SyntaxUnsupportedError,
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = TextDecoration.Underline,
                                        background = SyntaxUnsupportedError.copy(alpha = 0.16f)
                                    ),
                                    start,
                                    end
                                )
                            }
                            word == "strictfp" && targetRank >= JdkCatalogAndSpecifications.versionRank("17") -> {
                                // Redundant/obsolete in JDK 17+
                                addStyle(
                                    SpanStyle(
                                        color = SyntaxDeprecated,
                                        fontStyle = FontStyle.Italic,
                                        textDecoration = TextDecoration.LineThrough
                                    ),
                                    start,
                                    end
                                )
                            }
                            minRank >= JdkCatalogAndSpecifications.versionRank("10") -> {
                                // Newly introduced modern Java keyword (var, yield, record, sealed, permits, when)
                                addStyle(
                                    SpanStyle(
                                        color = if (isDarkTheme) SyntaxKeywordModern else Color(0xFF0284C7),
                                        fontWeight = FontWeight.Bold,
                                        background = SyntaxKeywordModern.copy(alpha = 0.12f)
                                    ),
                                    start,
                                    end
                                )
                            }
                            else -> {
                                addStyle(
                                    SpanStyle(
                                        color = if (isDarkTheme) SyntaxKeywordClassic else Color(0xFF7C3AED),
                                        fontWeight = FontWeight.Bold
                                    ),
                                    start,
                                    end
                                )
                            }
                        }
                    }

                    word in deprecatedTokens -> {
                        val (depJdk, _) = deprecatedTokens[word]!!
                        val depRank = JdkCatalogAndSpecifications.versionRank(depJdk)
                        if (targetRank >= depRank) {
                            addStyle(
                                SpanStyle(
                                    color = SyntaxDeprecated,
                                    fontWeight = FontWeight.SemiBold,
                                    textDecoration = TextDecoration.LineThrough,
                                    background = SyntaxDeprecated.copy(alpha = 0.14f)
                                ),
                                start,
                                end
                            )
                        } else if (word in standardTypes) {
                            addStyle(
                                SpanStyle(color = if (isDarkTheme) SyntaxType else Color(0xFF2563EB)),
                                start,
                                end
                            )
                        }
                    }

                    word in baselineKeywords -> {
                        addStyle(
                            SpanStyle(
                                color = if (isDarkTheme) SyntaxKeywordClassic else Color(0xFF7C3AED),
                                fontWeight = FontWeight.Bold
                            ),
                            start,
                            end
                        )
                    }

                    word in standardTypes -> {
                        addStyle(
                            SpanStyle(
                                color = if (isDarkTheme) SyntaxType else Color(0xFF2563EB),
                                fontWeight = FontWeight.Medium
                            ),
                            start,
                            end
                        )
                    }
                }
            }

            // 3. Highlight Annotations (@Override, etc.)
            annotationRegex.findAll(code).forEach { match ->
                val isSupported = targetRank >= JdkCatalogAndSpecifications.versionRank("5")
                addStyle(
                    SpanStyle(
                        color = if (isSupported) SyntaxAnnotation else SyntaxUnsupportedError,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (isSupported) TextDecoration.None else TextDecoration.Underline
                    ),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // 4. Highlight Strings
            stringRegex.findAll(code).forEach { match ->
                addStyle(
                    SpanStyle(color = if (isDarkTheme) SyntaxString else Color(0xFF059669)),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // 5. Highlight Comments (// and ///)
            var currentOffset = 0
            code.lines().forEach { line ->
                val commentIdx = line.indexOf("//")
                if (commentIdx >= 0) {
                    val start = currentOffset + commentIdx
                    val end = currentOffset + line.length
                    val isMarkdownDoc = line.substring(commentIdx).startsWith("///")
                    val color = if (isMarkdownDoc && targetRank >= JdkCatalogAndSpecifications.versionRank("23")) {
                        SyntaxKeywordModern
                    } else {
                        SyntaxComment
                    }
                    addStyle(
                        SpanStyle(color = color, fontStyle = FontStyle.Italic),
                        start,
                        end
                    )
                }
                currentOffset += line.length + 1
            }
        }
    }
}
