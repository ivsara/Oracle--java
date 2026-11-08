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
    SYNTAX_ERROR,
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
    val syntaxErrorCount: Int,
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

    private data class DelimiterToken(
        val char: Char,
        val line: Int,
        val globalOffset: Int
    )

    fun analyzeAndHighlight(
        sourceCode: String,
        jdkVersion: String,
        isDarkTheme: Boolean = true
    ): SyntaxAnalysisReport {
        val targetRank = JdkCatalogAndSpecifications.versionRank(jdkVersion)
        val diagnostics = mutableListOf<VersionSyntaxDiagnostic>()
        val errorHighlightRanges = mutableListOf<IntRange>()
        var syntaxErrCount = 0
        var classicCount = 0
        var modernCount = 0
        var deprecatedCount = 0
        var unsupportedCount = 0

        val lines = sourceCode.lines()

        // 1. Real-Time Delimiter Matcher: Braces {}, Parentheses (), Brackets []
        val delimiterStack = ArrayDeque<DelimiterToken>()
        var inBlockComment = false
        var inTextBlock = false
        var globalCursor = 0

        lines.forEachIndexed { lineIdx, rawLine ->
            val lineNum = lineIdx + 1
            var i = 0
            var inLineString = false
            var inCharLiteral = false
            var stringOpenOffset = -1

            while (i < rawLine.length) {
                val c = rawLine[i]
                val next = if (i + 1 < rawLine.length) rawLine[i + 1] else '\u0000'
                val next2 = if (i + 2 < rawLine.length) rawLine[i + 2] else '\u0000'

                if (inBlockComment) {
                    if (c == '*' && next == '/') {
                        inBlockComment = false
                        i += 2
                        continue
                    }
                    i++
                    continue
                }

                if (!inLineString && !inCharLiteral && c == '/' && next == '/') {
                    break // Rest of line is a single-line comment
                }

                if (!inLineString && !inCharLiteral && c == '/' && next == '*') {
                    inBlockComment = true
                    i += 2
                    continue
                }

                // Check triple-quote text block """
                if (!inCharLiteral && c == '"' && next == '"' && next2 == '"') {
                    inTextBlock = !inTextBlock
                    i += 3
                    continue
                }
                if (inTextBlock) {
                    i++
                    continue
                }

                // Check standard single-line string literal
                if (!inCharLiteral && c == '"' && (i == 0 || rawLine[i - 1] != '\\')) {
                    inLineString = !inLineString
                    if (inLineString) stringOpenOffset = globalCursor + i
                    i++
                    continue
                }

                if (!inLineString && c == '\'' && (i == 0 || rawLine[i - 1] != '\\')) {
                    inCharLiteral = !inCharLiteral
                    i++
                    continue
                }

                if (!inLineString && !inCharLiteral) {
                    when (c) {
                        '{', '(', '[' -> {
                            delimiterStack.addLast(DelimiterToken(c, lineNum, globalCursor + i))
                        }
                        '}', ')', ']' -> {
                            val expectedOpen = when (c) {
                                '}' -> '{'
                                ')' -> '('
                                else -> '['
                            }
                            if (delimiterStack.isEmpty()) {
                                syntaxErrCount++
                                val pos = globalCursor + i
                                errorHighlightRanges.add(pos..pos)
                                diagnostics.add(
                                    VersionSyntaxDiagnostic(
                                        line = lineNum,
                                        token = "$c",
                                        category = DiagnosticCategory.SYNTAX_ERROR,
                                        introducedInJdk = jdkVersion,
                                        message = "Syntax Error: Unmatched closing '$c' with no corresponding opening '$expectedOpen'."
                                    )
                                )
                            } else {
                                val top = delimiterStack.removeLast()
                                if (top.char != expectedOpen) {
                                    syntaxErrCount++
                                    val pos = globalCursor + i
                                    errorHighlightRanges.add(top.globalOffset..top.globalOffset)
                                    errorHighlightRanges.add(pos..pos)
                                    diagnostics.add(
                                        VersionSyntaxDiagnostic(
                                            line = lineNum,
                                            token = "${top.char}...$c",
                                            category = DiagnosticCategory.SYNTAX_ERROR,
                                            introducedInJdk = jdkVersion,
                                            message = "Syntax Error: Mismatched delimiter '$c' on line $lineNum (opened '${top.char}' on line ${top.line})."
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                i++
            }

            // Check unclosed string literal on this line
            if (inLineString && stringOpenOffset >= 0) {
                syntaxErrCount++
                val lineEndOffset = (globalCursor + rawLine.length - 1).coerceAtLeast(stringOpenOffset)
                errorHighlightRanges.add(stringOpenOffset..lineEndOffset)
                diagnostics.add(
                    VersionSyntaxDiagnostic(
                        line = lineNum,
                        token = "\"...",
                        category = DiagnosticCategory.SYNTAX_ERROR,
                        introducedInJdk = jdkVersion,
                        message = "Syntax Error: Unclosed string literal on line $lineNum (missing closing '\"')."
                    )
                )
            }

            globalCursor += rawLine.length + 1
        }

        // Any remaining unclosed opening delimiters on the stack
        while (delimiterStack.isNotEmpty()) {
            val unclosed = delimiterStack.removeLast()
            val expectedClose = when (unclosed.char) {
                '{' -> '}'
                '(' -> ')'
                else -> ']'
            }
            syntaxErrCount++
            errorHighlightRanges.add(unclosed.globalOffset..unclosed.globalOffset)
            diagnostics.add(
                VersionSyntaxDiagnostic(
                    line = unclosed.line,
                    token = "${unclosed.char}",
                    category = DiagnosticCategory.SYNTAX_ERROR,
                    introducedInJdk = jdkVersion,
                    message = "Syntax Error: Unclosed '${unclosed.char}' opened on line ${unclosed.line} (missing closing '$expectedClose')."
                )
            )
        }

        // 2. Real-Time Line-by-Line Semicolon & Version-Specific Syntax Analysis
        var blockCommentScan = false
        var textBlockScan = false
        var lineOffsetCursor = 0

        lines.forEachIndexed { index, lineText ->
            val lineNum = index + 1
            val trimmed = lineText.trim()

            // Track multi-line comments and text blocks
            if (trimmed.startsWith("/*") && !trimmed.contains("*/")) {
                blockCommentScan = true
            }
            if (blockCommentScan) {
                if (trimmed.contains("*/")) blockCommentScan = false
                lineOffsetCursor += lineText.length + 1
                return@forEachIndexed
            }

            if (lineText.contains("\"\"\"")) {
                val count = Regex("\"\"\"").findAll(lineText).count()
                if (count % 2 == 1) textBlockScan = !textBlockScan
            }

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

            if (!trimmed.startsWith("//") && !trimmed.startsWith("*") && !textBlockScan) {
                val codeOnly = lineText.substringBefore("//").trim()

                // Real-time Missing Semicolon Detector
                if (codeOnly.isNotEmpty() && requiresTrailingSemicolon(codeOnly)) {
                    syntaxErrCount++
                    val lastNonSpaceIdx = lineText.substringBefore("//").indexOfLast { !it.isWhitespace() }
                    if (lastNonSpaceIdx >= 0) {
                        val tokenStart = (lineOffsetCursor + (lastNonSpaceIdx - 4).coerceAtLeast(0))
                        val tokenEnd = lineOffsetCursor + lastNonSpaceIdx
                        errorHighlightRanges.add(tokenStart..tokenEnd)
                    }
                    diagnostics.add(
                        VersionSyntaxDiagnostic(
                            line = lineNum,
                            token = ";",
                            category = DiagnosticCategory.SYNTAX_ERROR,
                            introducedInJdk = jdkVersion,
                            message = "Syntax Error: ';' expected at end of statement on line $lineNum."
                        )
                    )
                }

                // Check Varargs '...' (JDK 5+)
                if (codeOnly.contains("...") && targetRank < JdkCatalogAndSpecifications.versionRank("5")) {
                    unsupportedCount++
                    diagnostics.add(
                        VersionSyntaxDiagnostic(
                            line = lineNum,
                            token = "...",
                            category = DiagnosticCategory.UNSUPPORTED_IN_SELECTED_JDK,
                            introducedInJdk = "5",
                            message = "Varargs ('...') parameter syntax requires JDK 5+ (unsupported in JDK $jdkVersion)."
                        )
                    )
                }

                // Check Binary literals (0b...) and Numeric underscores (1_000) in < JDK 7
                if (targetRank < JdkCatalogAndSpecifications.versionRank("7")) {
                    if (Regex("""\b0[bB][01_]+\b""").containsMatchIn(codeOnly)) {
                        unsupportedCount++
                        diagnostics.add(
                            VersionSyntaxDiagnostic(
                                line = lineNum,
                                token = "0b...",
                                category = DiagnosticCategory.UNSUPPORTED_IN_SELECTED_JDK,
                                introducedInJdk = "7",
                                message = "Binary integer literals ('0b...') require JDK 7+ (Project Coin). Not supported in JDK $jdkVersion."
                            )
                        )
                    }
                    if (Regex("""\b\d+_\d+\b""").containsMatchIn(codeOnly)) {
                        unsupportedCount++
                        diagnostics.add(
                            VersionSyntaxDiagnostic(
                                line = lineNum,
                                token = "1_000",
                                category = DiagnosticCategory.UNSUPPORTED_IN_SELECTED_JDK,
                                introducedInJdk = "7",
                                message = "Underscores in numeric literals require JDK 7+ (unsupported in JDK $jdkVersion)."
                            )
                        )
                    }
                }

                // Check try-with-resources & multi-catch in < JDK 7
                if (targetRank < JdkCatalogAndSpecifications.versionRank("7")) {
                    if (Regex("""\btry\s*\(""").containsMatchIn(codeOnly)) {
                        unsupportedCount++
                        diagnostics.add(
                            VersionSyntaxDiagnostic(
                                line = lineNum,
                                token = "try (...)",
                                category = DiagnosticCategory.UNSUPPORTED_IN_SELECTED_JDK,
                                introducedInJdk = "7",
                                message = "try-with-resources requires JDK 7+ (unsupported in JDK $jdkVersion)."
                            )
                        )
                    }
                    if (Regex("""\bcatch\s*\([^)]*\|[^)]*\)""").containsMatchIn(codeOnly)) {
                        unsupportedCount++
                        diagnostics.add(
                            VersionSyntaxDiagnostic(
                                line = lineNum,
                                token = "catch (A | B)",
                                category = DiagnosticCategory.UNSUPPORTED_IN_SELECTED_JDK,
                                introducedInJdk = "7",
                                message = "Multi-catch exception blocks ('|') require JDK 7+ (unsupported in JDK $jdkVersion)."
                            )
                        )
                    }
                }

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
                if (codeOnly.contains("->")) {
                    val isSwitchArrow = codeOnly.startsWith("case ") || codeOnly.startsWith("default ")
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
                if (codeOnly.contains("<>")) {
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
                if (genericsRegex.containsMatchIn(codeOnly)) {
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
                annotationRegex.findAll(codeOnly).forEach { match ->
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
                if (Regex("""new\s+(Integer|Double|Boolean|Long|Float|Short|Byte|Character)\s*\(""").containsMatchIn(codeOnly)) {
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
                if (codeOnly.contains("non-sealed")) {
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
                identifierRegex.findAll(codeOnly).forEach { match ->
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
                            val shouldFlag = if (word in setOf("stop", "suspend", "resume", "destroy", "countStackFrames")) {
                                codeOnly.contains("Thread.$word") || codeOnly.contains(".$word(")
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

            lineOffsetCursor += lineText.length + 1
        }

        // Check compact source file without class declaration in < JDK 25
        val hasTypeDecl = Regex("""\b(class|interface|enum|record)\s+[A-Za-z_][A-Za-z0-9_]*""").containsMatchIn(sourceCode)
        val hasMainMethod = Regex("""\bvoid\s+main\s*\(""").containsMatchIn(sourceCode)
        if (!hasTypeDecl && hasMainMethod && targetRank < JdkCatalogAndSpecifications.versionRank("25")) {
            syntaxErrCount++
            diagnostics.add(
                VersionSyntaxDiagnostic(
                    line = 1,
                    token = "void main()",
                    category = DiagnosticCategory.SYNTAX_ERROR,
                    introducedInJdk = "25",
                    message = "Syntax Error: Implicit compact source files without an enclosing 'class' declaration require JDK 25+ (selected: JDK $jdkVersion)."
                )
            )
        }

        val annotated = buildHighlightedString(sourceCode, targetRank, isDarkTheme, errorHighlightRanges)
        val spec = JdkCatalogAndSpecifications.getSpec(jdkVersion)

        return SyntaxAnalysisReport(
            jdkVersion = jdkVersion,
            lexerProfileName = "QScintilla-JavaLexer [JDK $jdkVersion | Bytecode v${spec.bytecodeMajor}.${spec.bytecodeMinor}]",
            annotatedCode = annotated,
            diagnostics = diagnostics.distinctBy { "${it.line}:${it.token}:${it.message}" },
            syntaxErrorCount = syntaxErrCount,
            classicKeywordCount = classicCount,
            modernFeatureCount = modernCount,
            deprecatedFeatureCount = deprecatedCount,
            unsupportedFeatureCount = unsupportedCount
        )
    }

    /**
     * Determines whether a single cleaned line of Java code is a statement missing a trailing semicolon.
     */
    private fun requiresTrailingSemicolon(cleanLine: String): Boolean {
        if (cleanLine.isEmpty()) return false
        // Lines that validly end with structural punctuation or continuation operators
        val validEndings = charArrayOf(';', '{', '}', ':', ',', '+', '-', '*', '/', '=', '(', '|', '&', '?')
        if (validEndings.any { cleanLine.endsWith(it) }) return false
        if (cleanLine.endsWith("->") || cleanLine.endsWith("\"\"\"")) return false
        if (cleanLine.startsWith("@")) return false
        // Control/type headers that might put '{' on the next line
        val headerPrefixes = listOf(
            "public class ", "class ", "private class ", "protected class ", "final class ", "abstract class ", "sealed class ",
            "public interface ", "interface ", "public enum ", "enum ", "public record ", "record ",
            "public static ", "public void ", "private void ", "protected void ", "void main",
            "if ", "if(", "else", "for ", "for(", "while ", "while(", "do", "switch ", "switch(",
            "try", "catch ", "catch(", "finally", "case ", "default"
        )
        if (headerPrefixes.any { cleanLine.startsWith(it) } && !cleanLine.contains("=")) return false

        // Statements that definitely require a semicolon at the end of the line
        val statementPatterns = listOf(
            Regex("""^(package|import)\s+.+"""),
            Regex("""^(return|throw|break|continue|assert)\b.*"""),
            Regex("""^(System\.out\.|System\.err\.|Scanner\b|[A-Za-z_][A-Za-z0-9_.]*\s*\().*"""),
            Regex("""^(?:final\s+)?(?:int|long|double|float|boolean|char|byte|short|String|var|Vector|ArrayList|List|Map|Set|[A-Z][A-Za-z0-9_<>]*)\s+[A-Za-z_][A-Za-z0-9_]*\s*=.*"""),
            Regex("""^[A-Za-z_][A-Za-z0-9_]*\s*(?:=|\+=|-=|\*=|/=|\+\+|--).*""")
        )
        return statementPatterns.any { it.matches(cleanLine) }
    }

    private fun buildHighlightedString(
        code: String,
        targetRank: Int,
        isDarkTheme: Boolean,
        errorRanges: List<IntRange>
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

            // 6. Highlight Real-Time Syntax Errors (Missing Semicolons, Mismatched Braces, Unclosed Strings)
            errorRanges.forEach { range ->
                val safeStart = range.first.coerceIn(0, code.length)
                val safeEnd = (range.last + 1).coerceIn(safeStart, code.length)
                if (safeStart < safeEnd) {
                    addStyle(
                        SpanStyle(
                            color = SyntaxUnsupportedError,
                            fontWeight = FontWeight.ExtraBold,
                            textDecoration = TextDecoration.Underline,
                            background = SyntaxUnsupportedError.copy(alpha = 0.28f)
                        ),
                        safeStart,
                        safeEnd
                    )
                }
            }
        }
    }
}
