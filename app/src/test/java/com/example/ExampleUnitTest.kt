package com.example

import com.example.core.DiagnosticCategory
import com.example.core.JavaCompilerAndExecutionEngine
import com.example.core.JdkCatalogAndSpecifications
import com.example.core.KnowledgeBaseCatalog
import com.example.core.VersionSyntaxHighlighter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `version syntax highlighter adjusts dynamically between JDK 1_2 and JDK 25`() {
        val modernCode = """
            import module java.base;
            public class Demo {
                public record Point(int x, int y) {}
                public static void main(String[] args) {
                    var p = new Point(3, 4);
                    System.out.println(p.x());
                }
            }
        """.trimIndent()

        // In JDK 1.2, 'import module', 'record', and 'var' must be flagged as UNSUPPORTED_IN_SELECTED_JDK
        val reportJdk12 = VersionSyntaxHighlighter.analyzeAndHighlight(modernCode, "1.2")
        assertTrue(reportJdk12.unsupportedFeatureCount >= 3)
        assertTrue(
            reportJdk12.diagnostics.any {
                it.token == "record" && it.category == DiagnosticCategory.UNSUPPORTED_IN_SELECTED_JDK
            }
        )

        // In JDK 25, all of them are supported modern features (0 unsupported)
        val reportJdk25 = VersionSyntaxHighlighter.analyzeAndHighlight(modernCode, "25")
        assertEquals(0, reportJdk25.unsupportedFeatureCount)
        assertTrue(reportJdk25.modernFeatureCount >= 3)
    }

    @Test
    fun `version syntax highlighter flags deprecated APIs and obsolete strictfp in JDK 17+`() {
        val codeWithDeprecated = """
            import java.applet.Applet;
            public class OldApp {
                public static strictfp void main(String[] args) {
                    SecurityManager sm = null;
                }
            }
        """.trimIndent()

        val reportJdk17 = VersionSyntaxHighlighter.analyzeAndHighlight(codeWithDeprecated, "17")
        assertTrue(reportJdk17.deprecatedFeatureCount >= 3)
        assertTrue(
            reportJdk17.diagnostics.any {
                it.token == "strictfp" && it.category == DiagnosticCategory.DEPRECATED_OR_REMOVED
            }
        )
    }

    @Test
    fun `compiler and execution engine compiles and runs Java 2 and JDK 25 and enforces version rules`() {
        val jdks = JdkCatalogAndSpecifications.buildInitialJdkInstallations()
        val jdk12 = jdks.first { it.jdkVersion == "1.2" }
        val jdk25 = jdks.first { it.jdkVersion == "25" }

        val java2Code = """
            import java.util.Vector;
            public class HelloWorld {
                public static strictfp void main(String args[]) {
                    Vector v = new Vector();
                    v.addElement("Java 2 OK");
                    System.out.println(v.elementAt(0));
                }
            }
        """.trimIndent()

        val compile12 = JavaCompilerAndExecutionEngine.compileProject(jdk12, mapOf("HelloWorld.java" to java2Code))
        assertTrue(compile12.success)
        assertEquals(46, compile12.generatedClasses.first().bytecodeMajor)

        val exec12 = JavaCompilerAndExecutionEngine.executeProgram(
            jdk = jdk12,
            files = mapOf("HelloWorld.java" to java2Code),
            mainClass = "HelloWorld"
        )
        assertTrue(exec12.success)
        assertEquals("Java 2 OK", exec12.stdout.trim())

        // Reject generics on JDK 1.2
        val genericsCode = """
            import java.util.ArrayList;
            public class GenTest {
                public static void main(String[] args) {
                    ArrayList<String> items = new ArrayList<String>();
                }
            }
        """.trimIndent()
        val failOn12 = JavaCompilerAndExecutionEngine.compileProject(jdk12, mapOf("GenTest.java" to genericsCode))
        assertFalse(failOn12.success)

        // Succeed on JDK 25
        val passOn25 = JavaCompilerAndExecutionEngine.compileProject(jdk25, mapOf("GenTest.java" to genericsCode))
        assertTrue(passOn25.success)
        assertEquals(69, passOn25.generatedClasses.first().bytecodeMajor)
    }

    @Test
    fun `java version explorer provides complete specs from JDK 1_0 to JDK 25`() {
        val spec8 = JdkCatalogAndSpecifications.getSpec("8")
        val spec17 = JdkCatalogAndSpecifications.getSpec("17")
        val spec25 = JdkCatalogAndSpecifications.getSpec("25")

        assertEquals(52, spec8.bytecodeMajor)
        assertEquals(61, spec17.bytecodeMajor)
        assertEquals(69, spec25.bytecodeMajor)
        assertTrue(spec17.languageChanges.any { it.contains("Sealed") })
        assertTrue(spec25.newApis.any { it.contains("ScopedValue") })
    }

    @Test
    fun `offline api documentation never leaks modern APIs into Java 2`() {
        val rank12 = JdkCatalogAndSpecifications.versionRank("1.2")
        val docsForJava2 = KnowledgeBaseCatalog.apiDocumentationCatalog.filter {
            JdkCatalogAndSpecifications.versionRank(it.sinceJdk) <= rank12
        }
        assertTrue(docsForJava2.any { it.className == "ArrayList" })
        assertFalse(docsForJava2.any { it.className == "Scanner" })
        assertFalse(docsForJava2.any { it.className == "Stream" })
        assertFalse(docsForJava2.any { it.className == "ScopedValue" })
    }

    @Test
    fun `JavaStudioViewModel exposes single-argument Application constructor for AndroidViewModelFactory`() {
        val ctor = com.example.ui.JavaStudioViewModel::class.java.getConstructor(android.app.Application::class.java)
        org.junit.Assert.assertNotNull(ctor)
    }

    @Test
    fun `real-time syntax error checker flags missing semicolons, mismatched braces, and version syntax errors`() {
        val brokenSyntaxCode = """
            public class SyntaxErrorDemo {
                public static void main(String[] args) {
                    int missingSemi = 42
                    System.out.println("Missing semicolon here too")
                    if (missingSemi > 10) {
                        System.out.println("Unclosed brace!");
                }
        """.trimIndent()

        val report = VersionSyntaxHighlighter.analyzeAndHighlight(brokenSyntaxCode, "17")
        assertTrue("Expected at least 3 syntax errors (2 missing semicolons + 1 unclosed brace)", report.syntaxErrorCount >= 3)
        assertTrue(
            report.diagnostics.any {
                it.category == DiagnosticCategory.SYNTAX_ERROR && it.message.contains("';' expected")
            }
        )
        assertTrue(
            report.diagnostics.any {
                it.category == DiagnosticCategory.SYNTAX_ERROR && it.message.contains("Unclosed '{'")
            }
        )

        // Verify JDK-version-specific syntax checks (varargs '...' in JDK 1.2)
        val varargsInJava2 = """
            public class VarargsLegacy {
                public static void logAll(String... items) {
                    System.out.println(items.length);
                }
            }
        """.trimIndent()
        val reportJdk12 = VersionSyntaxHighlighter.analyzeAndHighlight(varargsInJava2, "1.2")
        assertTrue(reportJdk12.unsupportedFeatureCount >= 1)
        assertTrue(reportJdk12.diagnostics.any { it.token == "..." })
    }
}

