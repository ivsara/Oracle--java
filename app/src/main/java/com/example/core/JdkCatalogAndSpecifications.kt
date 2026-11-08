package com.example.core

import com.example.data.local.JdkInstallationEntity
import com.example.data.local.OfflineResourceEntity
import com.example.data.local.ProjectEntity
import java.io.File

data class JavaVersionSpec(
    val versionId: String,          // "1.0", "1.1", "1.2", ..., "25"
    val rank: Int,                  // 100..125+ for ordering and comparisons
    val displayName: String,        // "JDK 1.2 (Java 2 Playground)"
    val codename: String,           // "Playground", "Tiger", "Spider", etc.
    val releaseYear: Int,
    val ltsStatus: String,          // "Historical", "Legacy LTS", "Modern LTS", "Feature Release", "Current LTS"
    val bytecodeMajor: Int,         // 45..69
    val bytecodeMinor: Int = 0,
    val vendorDefault: String,
    val supportedCompilerFlags: List<String>,
    val languageChanges: List<String>,
    val newApis: List<String>,
    val deprecatedOrRemovedApis: List<String>,
    val jvmAndGcImprovements: List<String>,
    val compilerChanges: List<String>,
    val languageCompatibilityNote: String,
    val apiCompatibilityNote: String,
    val bytecodeCompatibilityNote: String,
    val compilerCompatibilityNote: String,
    val runtimeCompatibilityNote: String
)

object JdkCatalogAndSpecifications {

    fun versionRank(versionId: String): Int {
        val clean = versionId.trim().removePrefix("JDK ").removePrefix("Java ").trim()
        return when (clean) {
            "1.0" -> 100
            "1.1" -> 101
            "1.2" -> 102
            "1.3" -> 103
            "1.4" -> 104
            else -> {
                val num = clean.removePrefix("1.").toIntOrNull() ?: 25
                100 + num
            }
        }
    }

    val allVersionSpecs: List<JavaVersionSpec> = listOf(
        JavaVersionSpec(
            versionId = "1.0",
            rank = 100,
            displayName = "JDK 1.0 (Oak)",
            codename = "Oak",
            releaseYear = 1996,
            ltsStatus = "Historical",
            bytecodeMajor = 45,
            bytecodeMinor = 3,
            vendorDefault = "Sun Microsystems",
            supportedCompilerFlags = listOf("-classpath", "-d", "-g", "-O", "-nowarn", "-verbose"),
            languageChanges = listOf(
                "Initial Java language specification: classes, interfaces, single-class inheritance",
                "Primitive types (byte, short, int, long, float, double, char, boolean)",
                "Checked and unchecked exceptions with try-catch-finally",
                "Built-in monitor synchronization via synchronized keyword"
            ),
            newApis = listOf(
                "java.lang (Object, String, Thread, Math, System)",
                "java.util (Vector, Hashtable, Stack, Enumeration, Date)",
                "java.io (InputStream, OutputStream, File)",
                "java.net (Socket, ServerSocket, URL) & java.awt + java.applet"
            ),
            deprecatedOrRemovedApis = listOf("None (Initial baseline release)"),
            jvmAndGcImprovements = listOf(
                "Classic interpreted Sun JVM with stop-the-world mark-and-sweep GC",
                "Green threads / native OS thread mapping depending on platform"
            ),
            compilerChanges = listOf(
                "Original sun.tools.javac compiler emitting class file format 45.3",
                "No -source or --release flags (single language level)"
            ),
            languageCompatibilityNote = "Baseline Java 1.0 grammar; no inner classes, no strictfp, no assert, no generics.",
            apiCompatibilityNote = "Core 8 packages only (212 classes); uses Vector/Hashtable before Collections Framework.",
            bytecodeCompatibilityNote = "Bytecode 45.3; readable by almost all subsequent JVMs.",
            compilerCompatibilityNote = "Requires legacy javac or strict 1.0 AST verification; modern javac dropped -source 1.0.",
            runtimeCompatibilityNote = "Original binary requires 32-bit legacy OS/VM; bytecode executes on compatible JVMs."
        ),
        JavaVersionSpec(
            versionId = "1.1",
            rank = 101,
            displayName = "JDK 1.1",
            codename = "Sparkler",
            releaseYear = 1997,
            ltsStatus = "Historical",
            bytecodeMajor = 45,
            bytecodeMinor = 3,
            vendorDefault = "Sun Microsystems",
            supportedCompilerFlags = listOf("-classpath", "-d", "-g", "-O", "-deprecation", "-encoding"),
            languageChanges = listOf(
                "Inner classes, static nested classes, local and anonymous classes",
                "Object array literal initialization improvements",
                "Blank final variables and class literals (ClassName.class)"
            ),
            newApis = listOf(
                "JavaBeans architecture (java.beans)",
                "JDBC 1.0 database connectivity (java.sql)",
                "Reflection API (java.lang.reflect)",
                "RMI (java.rmi), Character Streams (Reader/Writer), Delegation Event Model (java.awt.event)"
            ),
            deprecatedOrRemovedApis = listOf(
                "AWT 1.0 inheritance-based event handling (Component.action/handleEvent)",
                "Byte-oriented PrintStream constructors in favor of PrintWriter"
            ),
            jvmAndGcImprovements = listOf(
                "Early JIT (Just-In-Time) compiler support on Windows/Solaris",
                "JNI (Java Native Interface) replacing JDK 1.0 NMI"
            ),
            compilerChanges = listOf(
                "Added -deprecation flag to javac",
                "Synthetic bridge methods and outer-this references for inner classes"
            ),
            languageCompatibilityNote = "Adds inner & anonymous classes; still pre-Java 2 (no strictfp).",
            apiCompatibilityNote = "Introduces Reader/Writer, ActionListener, JDBC, and Reflection.",
            bytecodeCompatibilityNote = "Bytecode 45.3 with InnerClasses attribute.",
            compilerCompatibilityNote = "Legacy javac toolchain; modern compilers do not support -source 1.1.",
            runtimeCompatibilityNote = "Requires 1.1+ runtime for reflection and AWT Delegation Event Model."
        ),
        JavaVersionSpec(
            versionId = "1.2",
            rank = 102,
            displayName = "JDK 1.2 (Java 2)",
            codename = "Playground",
            releaseYear = 1998,
            ltsStatus = "Historical Milestone",
            bytecodeMajor = 46,
            bytecodeMinor = 0,
            vendorDefault = "Sun Microsystems",
            supportedCompilerFlags = listOf("-classpath", "-bootclasspath", "-extdirs", "-d", "-g", "-O", "-deprecation", "-target"),
            languageChanges = listOf(
                "Introduced 'strictfp' keyword for IEEE 754 strict floating-point reproducibility",
                "Rebranded platform as Java 2 Platform, Standard Edition (J2SE 1.2)"
            ),
            newApis = listOf(
                "Java Collections Framework (Collection, List, ArrayList, LinkedList, Set, HashSet, Map, HashMap, TreeMap, Iterator)",
                "JFC / Swing GUI toolkit integrated into core (javax.swing.*)",
                "Java 2D Graphics API (Graphics2D) and WeakReference / SoftReference (java.lang.ref)",
                "JDBC 2.0 scrollable/updatable ResultSets"
            ),
            deprecatedOrRemovedApis = listOf(
                "Thread.stop(), Thread.suspend(), Thread.resume() deprecated due to deadlock/corruption hazards"
            ),
            jvmAndGcImprovements = listOf(
                "First Sun JVM equipped with exact generational GC and fast thread synchronization",
                "Policy-based fine-grained SecurityManager and ProtectionDomain architecture"
            ),
            compilerChanges = listOf(
                "Introduced -bootclasspath, -extdirs, and -target (1.1 or 1.2) options",
                "Emits bytecode version 46.0"
            ),
            languageCompatibilityNote = "Supports strictfp; does NOT support assert (1.4), generics/enums (5), or lambdas (8).",
            apiCompatibilityNote = "Introduces Collections (ArrayList, HashMap) and javax.swing; no java.nio or Scanner.",
            bytecodeCompatibilityNote = "Bytecode 46.0 (J2SE 1.2 class format).",
            compilerCompatibilityNote = "Using modern javac with -source is NOT equivalent to genuine JDK 1.2 bootclasspath verification.",
            runtimeCompatibilityNote = "Original JDK 1.2 binary is 32-bit; Universal Java Studio enforces strict J2SE 1.2 API & grammar rules."
        ),
        JavaVersionSpec(
            versionId = "1.3",
            rank = 103,
            displayName = "JDK 1.3",
            codename = "Kestrel",
            releaseYear = 2000,
            ltsStatus = "Historical",
            bytecodeMajor = 47,
            bytecodeMinor = 0,
            vendorDefault = "Sun Microsystems",
            supportedCompilerFlags = listOf("-classpath", "-bootclasspath", "-d", "-g", "-deprecation", "-target"),
            languageChanges = listOf("No new keywords; refinements to inner class and final variable resolution"),
            newApis = listOf(
                "Dynamic Proxy classes (java.lang.reflect.Proxy & InvocationHandler)",
                "Java Sound API (javax.sound.sampled, javax.sound.midi)",
                "Timer and TimerTask in java.util",
                "JNDI (Java Naming and Directory Interface) integrated into core"
            ),
            deprecatedOrRemovedApis = listOf("Runtime.runFinalizersOnExit() deprecated"),
            jvmAndGcImprovements = listOf("HotSpot JVM becomes the default virtual machine with adaptive JIT compilation"),
            compilerChanges = listOf("Rewritten faster javac compiler in pure Java; emits bytecode 47.0"),
            languageCompatibilityNote = "Same syntax level as Java 1.2.",
            apiCompatibilityNote = "Adds Dynamic Proxies, Timer/TimerTask, and Java Sound.",
            bytecodeCompatibilityNote = "Bytecode 47.0.",
            compilerCompatibilityNote = "Supports -target 1.1, 1.2, 1.3.",
            runtimeCompatibilityNote = "HotSpot JVM 1.3 runtime semantics."
        ),
        JavaVersionSpec(
            versionId = "1.4",
            rank = 104,
            displayName = "JDK 1.4",
            codename = "Merlin",
            releaseYear = 2002,
            ltsStatus = "Historical",
            bytecodeMajor = 48,
            bytecodeMinor = 0,
            vendorDefault = "Sun Microsystems",
            supportedCompilerFlags = listOf("-source", "-target", "-classpath", "-bootclasspath", "-d", "-g", "-deprecation"),
            languageChanges = listOf(
                "Added 'assert' keyword for runtime design-by-contract assertions (-source 1.4)",
                "Unqualified access to static members across inheritance hierarchies refined"
            ),
            newApis = listOf(
                "New I/O (java.nio.*: Buffer, ByteBuffer, Channel, Selector)",
                "Regular Expressions (java.util.regex.Pattern & Matcher)",
                "Chained Exceptions (Throwable.getCause() / initCause())",
                "Logging API (java.util.logging), XML JAXP, Image I/O (javax.imageio), LinkedHashMap/LinkedHashSet"
            ),
            deprecatedOrRemovedApis = listOf("URLDecoder.decode(String) without explicit encoding deprecated"),
            jvmAndGcImprovements = listOf(
                "First 64-bit Solaris SPARC JVM support",
                "Parallel GC and Concurrent Mark Sweep (CMS) low-pause collector introduced"
            ),
            compilerChanges = listOf(
                "Introduced '-source 1.4' flag to opt into the 'assert' keyword",
                "Emits bytecode version 48.0"
            ),
            languageCompatibilityNote = "Supports 'assert' keyword; last release before Generics, Enums, and Autoboxing.",
            apiCompatibilityNote = "Adds java.nio, java.util.regex, and exception chaining; still uses BufferedReader for console input (no Scanner).",
            bytecodeCompatibilityNote = "Bytecode 48.0.",
            compilerCompatibilityNote = "First javac to support both -source and -target flags.",
            runtimeCompatibilityNote = "Supports -ea / -enableassertions runtime JVM flags."
        ),
        JavaVersionSpec(
            versionId = "5",
            rank = 105,
            displayName = "JDK 5 (J2SE 5.0)",
            codename = "Tiger",
            releaseYear = 2004,
            ltsStatus = "Historical Milestone",
            bytecodeMajor = 49,
            bytecodeMinor = 0,
            vendorDefault = "Sun Microsystems",
            supportedCompilerFlags = listOf("-source", "-target", "-classpath", "-bootclasspath", "-d", "-g", "-Xlint"),
            languageChanges = listOf(
                "Generics (Compile-time parameterized types <T> with type erasure)",
                "Enhanced for-each loop: for (Element e : collection)",
                "Autoboxing and unboxing between primitives and wrapper classes",
                "Typesafe Enums ('enum' keyword), Varargs (String... args), Static imports, Metadata Annotations (@Override, @Deprecated)"
            ),
            newApis = listOf(
                "java.util.Scanner for formatted console/stream token parsing",
                "Concurrency utilities (java.util.concurrent: ExecutorService, Locks, AtomicInteger, ConcurrentHashMap)",
                "StringBuilder (unsynchronized fast string builder) & String.format / System.out.printf"
            ),
            deprecatedOrRemovedApis = listOf("Raw collection usage discouraged via compiler unchecked warnings (-Xlint:unchecked)"),
            jvmAndGcImprovements = listOf(
                "Java Memory Model (JSR-133) overhaul fixing volatile and final concurrency semantics",
                "Class Data Sharing (CDS) and ergonomics auto-tuning"
            ),
            compilerChanges = listOf("Generics type checking, bridge method generation, and -Xlint diagnostic flags; bytecode 49.0"),
            languageCompatibilityNote = "Major modernization: Generics, enum, annotations, varargs, and enhanced for-loop.",
            apiCompatibilityNote = "Adds Scanner, StringBuilder, java.util.concurrent, and Queue.",
            bytecodeCompatibilityNote = "Bytecode 49.0 with Signature attributes for generic metadata.",
            compilerCompatibilityNote = "-source 1.5 / 5 required for enum and generics.",
            runtimeCompatibilityNote = "JSR-133 compliant memory model."
        ),
        JavaVersionSpec(
            versionId = "6",
            rank = 106,
            displayName = "JDK 6 (Java SE 6)",
            codename = "Mustang",
            releaseYear = 2006,
            ltsStatus = "Legacy",
            bytecodeMajor = 50,
            bytecodeMinor = 0,
            vendorDefault = "Sun / Oracle",
            supportedCompilerFlags = listOf("-source", "-target", "-classpath", "-d", "-proc:none", "-Xlint"),
            languageChanges = listOf("@Override allowed on methods implementing an interface method"),
            newApis = listOf(
                "Java Compiler API (javax.tools.JavaCompiler) for in-process compilation",
                "Pluggable Annotation Processing API (JSR-269)",
                "JDBC 4.0 automatic driver loading, Console (System.console()), Deque & ArrayDeque, NavigableSet/Map"
            ),
            deprecatedOrRemovedApis = listOf("Java File.toURL() deprecated in favor of File.toURI().toURL()"),
            jvmAndGcImprovements = listOf(
                "Split-verifier StackMapTable attribute introduced in bytecode 50.0",
                "Biased locking, escape analysis, and lock coarsening in HotSpot"
            ),
            compilerChanges = listOf("Integrated annotation processing rounds (-proc:only / -proc:none); emits bytecode 50.0"),
            languageCompatibilityNote = "Java 5 syntax plus @Override on interface implementations.",
            apiCompatibilityNote = "Adds ArrayDeque, NavigableMap, System.console(), and javax.tools.",
            bytecodeCompatibilityNote = "Bytecode 50.0 (introduces StackMapTable verification).",
            compilerCompatibilityNote = "Supports JSR-199 programmatic compilation.",
            runtimeCompatibilityNote = "Fast type-checking verifier enabled for v50 class files."
        ),
        JavaVersionSpec(
            versionId = "7",
            rank = 107,
            displayName = "JDK 7 (Java SE 7)",
            codename = "Dolphin",
            releaseYear = 2011,
            ltsStatus = "Legacy",
            bytecodeMajor = 51,
            bytecodeMinor = 0,
            vendorDefault = "Oracle Corporation",
            supportedCompilerFlags = listOf("-source", "-target", "-classpath", "-bootclasspath", "-d", "-Xlint"),
            languageChanges = listOf(
                "Project Coin: Diamond operator (<>) for generic type inference",
                "try-with-resources statement (AutoCloseable)",
                "Strings in switch statements",
                "Multi-catch exception blocks (catch (IOException | SQLException e))",
                "Binary integer literals (0b1010) and underscores in numeric literals (1_000_000)"
            ),
            newApis = listOf(
                "NIO.2 File System API (java.nio.file.Path, Paths, Files, WatchService)",
                "Fork/Join Framework (ForkJoinPool, RecursiveTask) in java.util.concurrent",
                "Objects utility class (java.util.Objects) & InvokeDynamic support (java.lang.invoke)"
            ),
            deprecatedOrRemovedApis = listOf("AWT Peer API methods deprecated"),
            jvmAndGcImprovements = listOf(
                "JSR-292 'invokedynamic' bytecode instruction for dynamic languages",
                "G1 (Garbage-First) Garbage Collector officially supported"
            ),
            compilerChanges = listOf("Automatic resource close desugaring and diamond type inference; bytecode 51.0"),
            languageCompatibilityNote = "Supports diamond <>, try-with-resources, multi-catch, and String switch.",
            apiCompatibilityNote = "Reference baseline for Oracle Java SE 7 API (java.nio.file.Files, Objects, ForkJoinPool).",
            bytecodeCompatibilityNote = "Bytecode 51.0 (strict StackMapTable requirement & invokedynamic).",
            compilerCompatibilityNote = "Supported as legacy target in JDK 8–19; removed as javac target in JDK 20+.",
            runtimeCompatibilityNote = "Oracle Java SE 7 runtime environment."
        ),
        JavaVersionSpec(
            versionId = "8",
            rank = 108,
            displayName = "JDK 8 (LTS)",
            codename = "Spider",
            releaseYear = 2014,
            ltsStatus = "Classic LTS",
            bytecodeMajor = 52,
            bytecodeMinor = 0,
            vendorDefault = "Oracle / OpenJDK",
            supportedCompilerFlags = listOf("-source", "-target", "-classpath", "-bootclasspath", "-d", "-parameters", "-Xlint"),
            languageChanges = listOf(
                "Lambda Expressions ((a, b) -> a + b) and Functional Interfaces (@FunctionalInterface)",
                "Method References (Class::method) and Constructor References (Class::new)",
                "Default and static methods inside interfaces",
                "Repeating annotations and Type Annotations (JSR-308)"
            ),
            newApis = listOf(
                "Stream API (java.util.stream.Stream, Collectors, IntStream)",
                "Functional interfaces (java.util.function: Predicate, Function, Consumer, Supplier)",
                "Modern Date and Time API (java.time: LocalDate, LocalTime, Instant, Duration)",
                "Optional<T> container & CompletableFuture asynchronous pipelines"
            ),
            deprecatedOrRemovedApis = listOf("sun.misc.BASE64Encoder replaced by standard java.util.Base64"),
            jvmAndGcImprovements = listOf(
                "Removed Permanent Generation (PermGen) in favor of native memory Metaspace",
                "LambdaMetafactory dynamic call site generation via invokedynamic"
            ),
            compilerChanges = listOf("Added -parameters flag for reflection parameter names; emits bytecode 52.0"),
            languageCompatibilityNote = "Introduces Lambdas, Method References, and Interface Default Methods.",
            apiCompatibilityNote = "Adds java.util.stream, java.time, Optional, and CompletableFuture.",
            bytecodeCompatibilityNote = "Bytecode 52.0.",
            compilerCompatibilityNote = "Last JDK using monolithic rt.jar / -bootclasspath before JPMS modularity.",
            runtimeCompatibilityNote = "Metaspace memory architecture."
        ),
        JavaVersionSpec(
            versionId = "9",
            rank = 109,
            displayName = "JDK 9",
            codename = "Jigsaw",
            releaseYear = 2017,
            ltsStatus = "Feature Release",
            bytecodeMajor = 53,
            bytecodeMinor = 0,
            vendorDefault = "OpenJDK",
            supportedCompilerFlags = listOf("--release", "-source", "-target", "--module-path", "-classpath", "-d"),
            languageChanges = listOf(
                "Java Platform Module System (module-info.java: module, requires, exports, opens)",
                "Private methods in interfaces",
                "Effectively final variables in try-with-resources",
                "Single underscore '_' made a reserved keyword (illegal as identifier)"
            ),
            newApis = listOf(
                "Collection factory methods: List.of(), Set.of(), Map.of()",
                "JShell interactive REPL API (jdk.jshell)",
                "Reactive Streams Flow API (java.util.concurrent.Flow), StackWalker, ProcessHandle",
                "Stream.takeWhile, dropWhile, ofNullable"
            ),
            deprecatedOrRemovedApis = listOf(
                "Java Applet API (java.applet.Applet) and Viewer deprecated",
                "Object.finalize() and wrapper constructors (new Integer(1)) deprecated",
                "Removed rt.jar and tools.jar in favor of modular JMOD runtime images"
            ),
            jvmAndGcImprovements = listOf(
                "G1 becomes the default Garbage Collector",
                "Compact Strings (byte[] Latin-1 storage instead of char[] UTF-16)"
            ),
            compilerChanges = listOf(
                "Introduced '--release N' flag combining -source, -target, and historical API symbol table (ct.sym)!",
                "Emits bytecode 53.0"
            ),
            languageCompatibilityNote = "Modules, private interface methods, and '_' prohibited as variable name.",
            apiCompatibilityNote = "Adds List.of/Map.of, Stream.takeWhile, and ProcessHandle.",
            bytecodeCompatibilityNote = "Bytecode 53.0 (supports module-info.class).",
            compilerCompatibilityNote = "First compiler supporting '--release' cross-compilation against historical APIs.",
            runtimeCompatibilityNote = "Modular JVM runtime image (lib/modules)."
        ),
        JavaVersionSpec(
            versionId = "10",
            rank = 110,
            displayName = "JDK 10",
            codename = "18.3",
            releaseYear = 2018,
            ltsStatus = "Feature Release",
            bytecodeMajor = 54,
            bytecodeMinor = 0,
            vendorDefault = "OpenJDK",
            supportedCompilerFlags = listOf("--release", "-source", "-target", "-classpath", "-d"),
            languageChanges = listOf("Local-Variable Type Inference via 'var' contextual keyword (JEP 286)"),
            newApis = listOf(
                "List.copyOf(), Set.copyOf(), Map.copyOf() unmodifiable copies",
                "Collectors.toUnmodifiableList/Set/Map",
                "Optional.orElseThrow() no-arg overload"
            ),
            deprecatedOrRemovedApis = listOf("javah native header tool removed (merged into javac -h)"),
            jvmAndGcImprovements = listOf("Parallel Full GC for G1 and Application Class-Data Sharing (AppCDS)"),
            compilerChanges = listOf("Local variable type inference in javac; bytecode 54.0"),
            languageCompatibilityNote = "Introduces 'var' for local variables with initializers.",
            apiCompatibilityNote = "Adds copyOf() unmodifiable collection factories.",
            bytecodeCompatibilityNote = "Bytecode 54.0.",
            compilerCompatibilityNote = "Supports --release 6 through 10.",
            runtimeCompatibilityNote = "Container-aware JVM CPU/memory detection."
        ),
        JavaVersionSpec(
            versionId = "11",
            rank = 111,
            displayName = "JDK 11 (LTS)",
            codename = "18.9 LTS",
            releaseYear = 2018,
            ltsStatus = "Modern LTS",
            bytecodeMajor = 55,
            bytecodeMinor = 0,
            vendorDefault = "Eclipse Temurin / OpenJDK",
            supportedCompilerFlags = listOf("--release", "-source", "-target", "--module-path", "-classpath", "-d"),
            languageChanges = listOf(
                "Local-variable syntax ('var') for lambda parameters ((@Nonnull var x, var y) -> x + y)",
                "Single-file source-code execution directly via 'java HelloWorld.java' (JEP 330)"
            ),
            newApis = listOf(
                "Standard HTTP Client API (java.net.http.HttpClient, HttpRequest, HttpResponse) supporting HTTP/2 & WebSocket",
                "String helper methods: isBlank(), lines(), strip(), repeat(int)",
                "Files.readString(Path) and Files.writeString(Path, CharSequence)",
                "Predicate.not() and Collection.toArray(IntFunction)"
            ),
            deprecatedOrRemovedApis = listOf(
                "Removed Java EE & CORBA modules (javax.xml.bind, javax.activation, org.omg.CORBA)",
                "Removed JavaFX from core JDK distribution (moved to OpenJFX)",
                "Nashorn JavaScript engine deprecated; Thread.destroy() & Thread.stop(Throwable) removed"
            ),
            jvmAndGcImprovements = listOf(
                "ZGC (Scalable Low-Latency GC) and Epsilon (No-Op GC) introduced",
                "Nest-Based Access Control (JEP 181) eliminating synthetic bridge accessors for inner classes"
            ),
            compilerChanges = listOf("Emits NestHost/NestMembers attributes and CONSTANT_Dynamic; bytecode 55.0"),
            languageCompatibilityNote = "Supports 'var' in lambda parameters and single-file source execution.",
            apiCompatibilityNote = "Adds java.net.http.HttpClient, String.isBlank/lines/repeat, and Files.readString.",
            bytecodeCompatibilityNote = "Bytecode 55.0 with NestHost / NestMembers.",
            compilerCompatibilityNote = "Supports --release 6 through 11.",
            runtimeCompatibilityNote = "Can launch single .java source files in memory without explicit javac step."
        ),
        JavaVersionSpec(
            versionId = "12",
            rank = 112,
            displayName = "JDK 12",
            codename = "19.3",
            releaseYear = 2019,
            ltsStatus = "Feature Release",
            bytecodeMajor = 56,
            bytecodeMinor = 0,
            vendorDefault = "OpenJDK",
            supportedCompilerFlags = listOf("--release", "--enable-preview", "-classpath", "-d"),
            languageChanges = listOf("Switch Expressions introduced as preview feature"),
            newApis = listOf(
                "String.indent(int) and String.transform(Function)",
                "Files.mismatch(Path, Path)",
                "Collectors.teeing() composite collector & CompactNumberFormat"
            ),
            deprecatedOrRemovedApis = listOf("Removed finalize() from FileInputStream/FileOutputStream"),
            jvmAndGcImprovements = listOf("Shenandoah Low-Pause Time GC and Promptly Return Unused Committed Memory in G1"),
            compilerChanges = listOf("Preview language feature flags (--enable-preview); bytecode 56.0"),
            languageCompatibilityNote = "Java 11 standard syntax + preview switch expressions.",
            apiCompatibilityNote = "Adds String.indent/transform and Collectors.teeing.",
            bytecodeCompatibilityNote = "Bytecode 56.0.",
            compilerCompatibilityNote = "Drops -source/-target 6 (minimum supported target is 7).",
            runtimeCompatibilityNote = "G1 returns unused heap memory to OS automatically."
        ),
        JavaVersionSpec(
            versionId = "13",
            rank = 113,
            displayName = "JDK 13",
            codename = "19.9",
            releaseYear = 2019,
            ltsStatus = "Feature Release",
            bytecodeMajor = 57,
            bytecodeMinor = 0,
            vendorDefault = "OpenJDK",
            supportedCompilerFlags = listOf("--release", "--enable-preview", "-classpath", "-d"),
            languageChanges = listOf("Introduced 'yield' contextual keyword in Switch Expressions (Preview); Text Blocks (Preview)"),
            newApis = listOf("FileSystems.newFileSystem(Path) overload, SocketImpl modernization"),
            deprecatedOrRemovedApis = listOf("Legacy PlainSocketImpl replaced by NioSocketImpl"),
            jvmAndGcImprovements = listOf("Dynamic CDS Archives and ZGC uncommitting unused memory"),
            compilerChanges = listOf("Bytecode 57.0"),
            languageCompatibilityNote = "Introduces 'yield' in switch expressions (preview).",
            apiCompatibilityNote = "Modernized NIO-backed java.net.Socket implementation.",
            bytecodeCompatibilityNote = "Bytecode 57.0.",
            compilerCompatibilityNote = "Supports --release 7 through 13.",
            runtimeCompatibilityNote = "Dynamic Class-Data Sharing at exit."
        ),
        JavaVersionSpec(
            versionId = "14",
            rank = 114,
            displayName = "JDK 14",
            codename = "20.3",
            releaseYear = 2020,
            ltsStatus = "Feature Release",
            bytecodeMajor = 58,
            bytecodeMinor = 0,
            vendorDefault = "OpenJDK",
            supportedCompilerFlags = listOf("--release", "--enable-preview", "-classpath", "-d"),
            languageChanges = listOf(
                "Switch Expressions standardized (JEP 361) with arrow labels 'case X ->' and 'yield'!",
                "Records and Pattern Matching for instanceof introduced in preview"
            ),
            newApis = listOf("Helpful NullPointerExceptions (JEP 358) pinpointing exact null expression in stack trace"),
            deprecatedOrRemovedApis = listOf(
                "Removed Concurrent Mark Sweep (CMS) Garbage Collector",
                "Removed pack200 / unpack200 tools and APIs"
            ),
            jvmAndGcImprovements = listOf("Helpful NullPointerExceptions computed directly from bytecode analysis"),
            compilerChanges = listOf("Standardized switch expression compilation; bytecode 58.0"),
            languageCompatibilityNote = "Switch expressions ('case A, B ->' and 'yield') are now standard Java.",
            apiCompatibilityNote = "Exact variable names reported in NullPointerException messages.",
            bytecodeCompatibilityNote = "Bytecode 58.0.",
            compilerCompatibilityNote = "Supports --release 7 through 14.",
            runtimeCompatibilityNote = "CMS GC removed; G1/ZGC standard."
        ),
        JavaVersionSpec(
            versionId = "15",
            rank = 115,
            displayName = "JDK 15",
            codename = "20.9",
            releaseYear = 2020,
            ltsStatus = "Feature Release",
            bytecodeMajor = 59,
            bytecodeMinor = 0,
            vendorDefault = "OpenJDK",
            supportedCompilerFlags = listOf("--release", "--enable-preview", "-classpath", "-d"),
            languageChanges = listOf(
                "Text Blocks (\"\"\" ... \"\"\") standardized (JEP 378) for multi-line string literals",
                "Sealed Classes introduced as preview"
            ),
            newApis = listOf(
                "String.formatted(Object... args) and String.stripIndent() / translateEscapes()",
                "EdDSA cryptographic signatures (JEP 339) & Hidden Classes (Lookup.defineHiddenClass)"
            ),
            deprecatedOrRemovedApis = listOf(
                "Removed Nashorn JavaScript Engine",
                "Biased Locking disabled and deprecated"
            ),
            jvmAndGcImprovements = listOf("ZGC and Shenandoah GC promoted from experimental to production features"),
            compilerChanges = listOf("Compile-time whitespace stripping for Text Blocks; bytecode 59.0"),
            languageCompatibilityNote = "Text Blocks (triple quotes \"\"\") are now standard.",
            apiCompatibilityNote = "Adds String.formatted(), stripIndent(), and Hidden Classes.",
            bytecodeCompatibilityNote = "Bytecode 59.0.",
            compilerCompatibilityNote = "Supports --release 7 through 15.",
            runtimeCompatibilityNote = "Production ZGC and Shenandoah collectors."
        ),
        JavaVersionSpec(
            versionId = "16",
            rank = 116,
            displayName = "JDK 16",
            codename = "21.3",
            releaseYear = 2021,
            ltsStatus = "Feature Release",
            bytecodeMajor = 60,
            bytecodeMinor = 0,
            vendorDefault = "OpenJDK",
            supportedCompilerFlags = listOf("--release", "--enable-preview", "-classpath", "-d"),
            languageChanges = listOf(
                "Records ('record Point(int x, int y) {}') standardized (JEP 395)!",
                "Pattern Matching for instanceof ('if (obj instanceof String s)') standardized (JEP 394)!",
                "Inner classes allowed to declare static members"
            ),
            newApis = listOf(
                "Stream.toList() convenience terminal operation returning unmodifiable List",
                "Stream.mapMulti() & Unix-Domain Socket Channels (JEP 380)"
            ),
            deprecatedOrRemovedApis = listOf(
                "Primitive wrapper constructors marked @Deprecated(forRemoval = true)",
                "Strong encapsulation of JDK internals enabled by default (--illegal-access=deny)"
            ),
            jvmAndGcImprovements = listOf("ZGC Concurrent Thread-Stack Processing for sub-millisecond pauses"),
            compilerChanges = listOf("Record canonical constructor, equals, hashCode, toString generation; bytecode 60.0"),
            languageCompatibilityNote = "Records ('record'), 'instanceof' pattern variables, and static members in inner classes are standard.",
            apiCompatibilityNote = "Adds Stream.toList() and java.lang.Record.",
            bytecodeCompatibilityNote = "Bytecode 60.0 with Record attribute.",
            compilerCompatibilityNote = "Uses ObjectMethods.bootstrap invokedynamic for Record equals/hashCode/toString.",
            runtimeCompatibilityNote = "Strongly encapsulates internal JDK packages by default."
        ),
        JavaVersionSpec(
            versionId = "17",
            rank = 117,
            displayName = "JDK 17 (LTS)",
            codename = "21.9 LTS",
            releaseYear = 2021,
            ltsStatus = "Enterprise LTS",
            bytecodeMajor = 61,
            bytecodeMinor = 0,
            vendorDefault = "Eclipse Temurin / Oracle",
            supportedCompilerFlags = listOf("--release", "--enable-preview", "--module-path", "-classpath", "-d"),
            languageChanges = listOf(
                "Sealed Classes and Interfaces ('sealed', 'non-sealed', 'permits') standardized (JEP 409)!",
                "Restore Always-Strict Floating-Point Semantics (JEP 306: all FP is now strictfp by default)"
            ),
            newApis = listOf(
                "Enhanced Pseudo-Random Number Generators (java.util.random.RandomGenerator, RandomGeneratorFactory)",
                "HexFormat (java.util.HexFormat) for byte/hex conversions",
                "Context-Specific Deserialization Filters (ObjectInputFilter)"
            ),
            deprecatedOrRemovedApis = listOf(
                "SecurityManager deprecated for removal (JEP 411)",
                "Applet API marked @Deprecated(forRemoval = true) (JEP 398)",
                "Removed RMI Activation and Experimental AOT/Graal JIT"
            ),
            jvmAndGcImprovements = listOf("Unified macOS AArch64 port and strongly encapsulated JDK internals"),
            compilerChanges = listOf("PermittedSubclasses attribute generation; warning that 'strictfp' is now redundant; bytecode 61.0"),
            languageCompatibilityNote = "Sealed classes ('sealed', 'permits', 'non-sealed') are standard; 'strictfp' is redundant.",
            apiCompatibilityNote = "Adds HexFormat and RandomGenerator; SecurityManager & Applet deprecated for removal.",
            bytecodeCompatibilityNote = "Bytecode 61.0 with PermittedSubclasses attribute.",
            compilerCompatibilityNote = "Supports --release 7 through 17.",
            runtimeCompatibilityNote = "Enterprise LTS baseline for modern frameworks (Spring Boot 3+)."
        ),
        JavaVersionSpec(
            versionId = "18",
            rank = 118,
            displayName = "JDK 18",
            codename = "22.3",
            releaseYear = 2022,
            ltsStatus = "Feature Release",
            bytecodeMajor = 62,
            bytecodeMinor = 0,
            vendorDefault = "OpenJDK",
            supportedCompilerFlags = listOf("--release", "--enable-preview", "-classpath", "-d"),
            languageChanges = listOf("UTF-8 by Default across all standard Java APIs (JEP 400)"),
            newApis = listOf(
                "Simple Web Server (com.sun.net.httpserver.SimpleFileServer & 'jwebserver' CLI)",
                "Code Snippets in Java API Documentation (@snippet Javadoc tag)",
                "Internet-Address Resolution SPI"
            ),
            deprecatedOrRemovedApis = listOf("Finalization deprecated for removal (JEP 421)"),
            jvmAndGcImprovements = listOf("Deduplication of Strings supported across G1, Parallel, ZGC, and Shenandoah"),
            compilerChanges = listOf("Default source encoding is UTF-8 across all OSes; bytecode 62.0"),
            languageCompatibilityNote = "UTF-8 source & runtime default.",
            apiCompatibilityNote = "Adds SimpleFileServer and @snippet.",
            bytecodeCompatibilityNote = "Bytecode 62.0.",
            compilerCompatibilityNote = "Supports --release 7 through 18.",
            runtimeCompatibilityNote = "UTF-8 default charset regardless of OS locale."
        ),
        JavaVersionSpec(
            versionId = "19",
            rank = 119,
            displayName = "JDK 19",
            codename = "22.9",
            releaseYear = 2022,
            ltsStatus = "Feature Release",
            bytecodeMajor = 63,
            bytecodeMinor = 0,
            vendorDefault = "OpenJDK",
            supportedCompilerFlags = listOf("--release", "--enable-preview", "-classpath", "-d"),
            languageChanges = listOf("Record Patterns and Virtual Threads introduced in Preview"),
            newApis = listOf(
                "HashMap.newHashMap(int numMappings) & HashSet.newHashSet(int) pre-sized factories",
                "Future.resultNow() and Future.exceptionNow()"
            ),
            deprecatedOrRemovedApis = listOf("Locale(String, String) constructors deprecated in favor of Locale.of()"),
            jvmAndGcImprovements = listOf("Linux/RISC-V Port and Project Loom Virtual Thread carrier scheduling preview"),
            compilerChanges = listOf("Bytecode 63.0"),
            languageCompatibilityNote = "Preview support for Record Patterns.",
            apiCompatibilityNote = "Adds Locale.of() and HashMap.newHashMap().",
            bytecodeCompatibilityNote = "Bytecode 63.0.",
            compilerCompatibilityNote = "Supports --release 7 through 19.",
            runtimeCompatibilityNote = "First JDK with Project Loom preview."
        ),
        JavaVersionSpec(
            versionId = "20",
            rank = 120,
            displayName = "JDK 20",
            codename = "23.3",
            releaseYear = 2023,
            ltsStatus = "Feature Release",
            bytecodeMajor = 64,
            bytecodeMinor = 0,
            vendorDefault = "OpenJDK",
            supportedCompilerFlags = listOf("--release", "--enable-preview", "-classpath", "-d"),
            languageChanges = listOf("Scoped Values (Incubator) and refined Record/Switch Pattern Matching (Preview)"),
            newApis = listOf(" Matcher.namedGroups() and Float.floatToFloat16() half-precision conversions"),
            deprecatedOrRemovedApis = listOf("Thread.suspend() and Thread.resume() changed to unconditionally throw UnsupportedOperationException"),
            jvmAndGcImprovements = listOf("G1 Preventive GCs removed to reduce unnecessary pause overhead"),
            compilerChanges = listOf("Minimum supported -source/-target/--release raised to 8 (dropped Java 7 target); bytecode 64.0"),
            languageCompatibilityNote = "Refined pattern matching previews.",
            apiCompatibilityNote = "Thread.suspend/resume now throw UnsupportedOperationException.",
            bytecodeCompatibilityNote = "Bytecode 64.0.",
            compilerCompatibilityNote = "Drops --release 7; supports --release 8 through 20.",
            runtimeCompatibilityNote = "Legacy Thread control methods disabled."
        ),
        JavaVersionSpec(
            versionId = "21",
            rank = 121,
            displayName = "JDK 21 (LTS)",
            codename = "23.9 LTS",
            releaseYear = 2023,
            ltsStatus = "Modern LTS",
            bytecodeMajor = 65,
            bytecodeMinor = 0,
            vendorDefault = "Eclipse Temurin / Oracle",
            supportedCompilerFlags = listOf("--release", "--enable-preview", "--module-path", "-classpath", "-d"),
            languageChanges = listOf(
                "Pattern Matching for switch standardized (JEP 441) including guarded patterns with 'when'!",
                "Record Patterns ('if (obj instanceof Point(int x, int y))') standardized (JEP 440)!",
                "Exhaustive switch checking over sealed hierarchies without default branch"
            ),
            newApis = listOf(
                "Virtual Threads standardized (JEP 444): Thread.ofVirtual().start(...), Executors.newVirtualThreadPerTaskExecutor()",
                "Sequenced Collections (JEP 431): SequencedCollection, SequencedSet, SequencedMap (getFirst, getLast, reversed)",
                "StringBuilder.repeat(), Character.isEmoji(), Math.clamp()"
            ),
            deprecatedOrRemovedApis = listOf(
                "Windows 32-bit x86 port deprecated for removal",
                "Dynamic loading of JVM agents without -XX:+EnableDynamicAgentLoading triggers warning"
            ),
            jvmAndGcImprovements = listOf(
                "Generational ZGC (JEP 439) for lower memory overhead and higher throughput",
                "Million-scale lightweight Virtual Threads mounted on carrier ForkJoinPool"
            ),
            compilerChanges = listOf("SwitchBootstraps.typeSwitch invokedynamic generation for pattern switches; bytecode 65.0"),
            languageCompatibilityNote = "Pattern Matching for switch, 'when' guards, and Record deconstruction patterns are standard.",
            apiCompatibilityNote = "Adds Virtual Threads, SequencedCollection (getFirst/getLast/reversed), and Math.clamp.",
            bytecodeCompatibilityNote = "Bytecode 65.0.",
            compilerCompatibilityNote = "Supports --release 8 through 21.",
            runtimeCompatibilityNote = "Native Virtual Thread scheduler and Generational ZGC."
        ),
        JavaVersionSpec(
            versionId = "22",
            rank = 122,
            displayName = "JDK 22",
            codename = "24.3",
            releaseYear = 2024,
            ltsStatus = "Feature Release",
            bytecodeMajor = 66,
            bytecodeMinor = 0,
            vendorDefault = "OpenJDK",
            supportedCompilerFlags = listOf("--release", "--enable-preview", "-classpath", "-d"),
            languageChanges = listOf(
                "Unnamed Variables and Patterns ('_') standardized (JEP 456): e.g., catch (NumberFormatException _) or case Point(int x, _)",
                "Launch Multi-File Source-Code Programs directly via 'java Main.java' (JEP 458)"
            ),
            newApis = listOf(
                "Foreign Function & Memory (FFM) API standardized (JEP 454: java.lang.foreign.Arena, MemorySegment, Linker)",
                "ListFormat for locale-dependent list formatting"
            ),
            deprecatedOrRemovedApis = listOf("Thread.countStackFrames() removed"),
            jvmAndGcImprovements = listOf("Region Pinning for G1 GC reducing JNI critical-section stalls"),
            compilerChanges = listOf("Allows single underscore '_' as unnamed variable/pattern in JDK 22+; bytecode 66.0"),
            languageCompatibilityNote = "Unnamed variables and patterns ('_') are now standard (reversing JDK 9's complete ban for local/pattern use).",
            apiCompatibilityNote = "Standardizes Foreign Function & Memory API (java.lang.foreign).",
            bytecodeCompatibilityNote = "Bytecode 66.0.",
            compilerCompatibilityNote = "Supports --release 8 through 22.",
            runtimeCompatibilityNote = "Supports multi-file source launch without manual compilation."
        ),
        JavaVersionSpec(
            versionId = "23",
            rank = 123,
            displayName = "JDK 23",
            codename = "24.9",
            releaseYear = 2024,
            ltsStatus = "Feature Release",
            bytecodeMajor = 67,
            bytecodeMinor = 0,
            vendorDefault = "OpenJDK",
            supportedCompilerFlags = listOf("--release", "--enable-preview", "-classpath", "-d"),
            languageChanges = listOf(
                "Markdown Documentation Comments ('///') standardized (JEP 467)!",
                "Primitive Types in Patterns, Flexible Constructor Bodies, and Module Import Declarations (Preview)"
            ),
            newApis = listOf(
                "Console.readln() and Console.println() localized methods",
                "Instant.until(Instant) duration convenience method"
            ),
            deprecatedOrRemovedApis = listOf(
                "sun.misc.Unsafe memory-access methods deprecated for removal (JEP 471) in favor of VarHandle & FFM"
            ),
            jvmAndGcImprovements = listOf("Generational ZGC becomes the default mode for ZGC (JEP 474)"),
            compilerChanges = listOf("Annotation processing disabled by default unless processors or -proc:full specified; bytecode 67.0"),
            languageCompatibilityNote = "Supports Markdown Javadoc comments ('///').",
            apiCompatibilityNote = "Deprecates sun.misc.Unsafe memory methods.",
            bytecodeCompatibilityNote = "Bytecode 67.0.",
            compilerCompatibilityNote = "Requires -proc:full if relying on implicit classpath annotation processors.",
            runtimeCompatibilityNote = "Generational mode default for ZGC."
        ),
        JavaVersionSpec(
            versionId = "24",
            rank = 124,
            displayName = "JDK 24",
            codename = "25.3",
            releaseYear = 2025,
            ltsStatus = "Feature Release",
            bytecodeMajor = 68,
            bytecodeMinor = 0,
            vendorDefault = "OpenJDK",
            supportedCompilerFlags = listOf("--release", "--enable-preview", "-classpath", "-d"),
            languageChanges = listOf(
                "Flexible Constructor Bodies, Compact Source Files, and Module Import Declarations ('import module java.base;') refined"
            ),
            newApis = listOf(
                "Stream Gatherers standardized (JEP 485): Stream.gather(Gatherers.windowFixed / fold / scan)",
                "Class-File API standardized (JEP 484: java.lang.classfile.*) for parsing/generating bytecode",
                "Quantum-Resistant ML-KEM and ML-DSA Cryptography APIs"
            ),
            deprecatedOrRemovedApis = listOf(
                "SecurityManager permanently disabled (JEP 486: System.setSecurityManager always throws UnsupportedOperationException)",
                "Windows 32-bit x86 port removed"
            ),
            jvmAndGcImprovements = listOf(
                "Synchronize Virtual Threads without Pinning (JEP 491)!",
                "Compact Object Headers (Project Lilliput) & Ahead-of-Time Class Loading (Project Leyden)"
            ),
            compilerChanges = listOf("Uses standard java.lang.classfile internally; emits bytecode 68.0"),
            languageCompatibilityNote = "Supports Stream Gatherers and Class-File API.",
            apiCompatibilityNote = "Adds Stream.gather(Gatherers.*) and java.lang.classfile; SecurityManager permanently disabled.",
            bytecodeCompatibilityNote = "Bytecode 68.0.",
            compilerCompatibilityNote = "Supports --release 8 through 24.",
            runtimeCompatibilityNote = "Virtual threads no longer pin carrier threads inside synchronized blocks."
        ),
        JavaVersionSpec(
            versionId = "25",
            rank = 125,
            displayName = "JDK 25 (LTS)",
            codename = "25.9 LTS",
            releaseYear = 2025,
            ltsStatus = "Latest LTS",
            bytecodeMajor = 69,
            bytecodeMinor = 0,
            vendorDefault = "Eclipse Temurin / OpenJDK",
            supportedCompilerFlags = listOf("--release", "--enable-preview", "--module-path", "-classpath", "-d"),
            languageChanges = listOf(
                "Compact Source Files and Instance Main Methods ('void main()') standardized for streamlined learning & scripting",
                "Module Import Declarations ('import module java.base;') standardized",
                "Flexible Constructor Bodies (statements before super(...) / this(...)) standardized",
                "Primitive Types in instanceof and switch patterns standardized"
            ),
            newApis = listOf(
                "Scoped Values standardized (java.lang.ScopedValue) for safe immutable context sharing with Virtual Threads",
                "Structured Concurrency (StructuredTaskScope) and PemEncoder/PemDecoder cryptography utilities"
            ),
            deprecatedOrRemovedApis = listOf(
                "32-bit x86 ports completely removed across all platforms",
                "Legacy Applet & SecurityManager hooks purged from modern profiles"
            ),
            jvmAndGcImprovements = listOf(
                "Compact 64-bit Object Headers enabled in production reducing heap footprint by 15-22%",
                "Leyden AOT method profiling cache for near-instant JVM warmup"
            ),
            compilerChanges = listOf(
                "Supports implicit class declarations, 'import module', and pre-super constructor prologue validation; bytecode 69.0"
            ),
            languageCompatibilityNote = "Latest LTS: supports 'import module', flexible constructors before super(), primitive patterns, and compact source files.",
            apiCompatibilityNote = "Adds ScopedValue, StructuredTaskScope, Gatherers, and Class-File API.",
            bytecodeCompatibilityNote = "Bytecode 69.0.",
            compilerCompatibilityNote = "Supports --release 8 through 25; future-ready extensible spec registry.",
            runtimeCompatibilityNote = "Latest 64-bit LTS runtime with non-pinning Virtual Threads and Compact Object Headers."
        )
    )

    fun getSpec(versionId: String): JavaVersionSpec {
        val clean = versionId.trim().removePrefix("JDK ").removePrefix("Java ").trim()
        return allVersionSpecs.find { it.versionId == clean } ?: run {
            // Extensible fallback for Future JDK releases (e.g., JDK 26, 27, 28...)
            val num = clean.toIntOrNull() ?: 26
            JavaVersionSpec(
                versionId = clean,
                rank = 100 + num,
                displayName = "JDK $clean (Future Extensible Release)",
                codename = "Future Java",
                releaseYear = 2025 + (num - 25),
                ltsStatus = "Extensible Toolchain",
                bytecodeMajor = 44 + num,
                bytecodeMinor = 0,
                vendorDefault = "OpenJDK",
                supportedCompilerFlags = listOf("--release", "--enable-preview", "--module-path", "-classpath", "-d"),
                languageChanges = listOf("Inherits all JDK 25 LTS features plus forward-compatible grammar rules"),
                newApis = listOf("Full modern Java SE standard library + extensible modules"),
                deprecatedOrRemovedApis = listOf("Legacy Applet and SecurityManager removed"),
                jvmAndGcImprovements = listOf("Next-Gen Project Lilliput, Valhalla value classes & Loom optimizations"),
                compilerChanges = listOf("Emits bytecode ${44 + num}.0"),
                languageCompatibilityNote = "Forward-compatible extensible language profile (JDK $clean).",
                apiCompatibilityNote = "Modern Java SE API profile.",
                bytecodeCompatibilityNote = "Bytecode ${44 + num}.0.",
                compilerCompatibilityNote = "Configured via custom JDK toolchain registry.",
                runtimeCompatibilityNote = "64-bit modern JVM runtime."
            )
        }
    }

    /**
     * Scans host properties and filesystem to build initial JDK installations list
     * covering Historical (1.0–1.4), Intermediate (5–8), Modular (9–17), and Modern (18–25).
     */
    fun buildInitialJdkInstallations(): List<JdkInstallationEntity> {
        val hostJavaHome = System.getProperty("java.home") ?: "/system/usr/jvm"
        val hostVendor = System.getProperty("java.vendor") ?: "Android / OpenJDK Host"
        val hostArch = System.getProperty("os.arch") ?: "aarch64"

        // Check if any real desktop/server JDK directories exist on host (e.g. during JVM unit tests)
        val candidateDirs = listOf("/usr/lib/jvm", "/opt/java", "/opt/jdk")
        val detectedPaths = candidateDirs.mapNotNull { path ->
            val dir = File(path)
            if (dir.exists() && dir.isDirectory) dir.absolutePath else null
        }
        val baseRoot = detectedPaths.firstOrNull() ?: "/opt/universal-java-studio/toolchains"

        val featuredVersions = listOf("1.0", "1.1", "1.2", "1.3", "1.4", "5", "6", "7", "8", "9", "11", "14", "16", "17", "21", "22", "24", "25")
        return featuredVersions.map { ver ->
            val spec = getSpec(ver)
            val isHistorical = spec.rank <= 104
            val slug = "jdk-${ver.replace('.', '_')}"
            val homeDir = if (ver == "17" && hostJavaHome.isNotBlank()) hostJavaHome else "$baseRoot/$slug"
            val arch = if (isHistorical) "32-bit x86 (Isolated Legacy VM)" else "64-bit ($hostArch)"
            val status = when {
                ver in listOf("1.2", "1.4", "5", "7", "8", "11", "17", "21", "25") -> "Available"
                isHistorical -> "Legacy VM Ready"
                else -> "Available"
            }
            val compat = when {
                isHistorical -> "Supported (Strict Historical Grammar + Legacy API Guard)"
                spec.rank <= 108 -> "Supported (J2SE / Classic Bytecode v${spec.bytecodeMajor}.0)"
                else -> "Supported (Modern Modular --release $ver Toolchain)"
            }
            JdkInstallationEntity(
                jdkVersion = ver,
                displayName = spec.displayName,
                vendor = if (ver == "17") "$hostVendor / ${spec.vendorDefault}" else spec.vendorDefault,
                javaHome = homeDir,
                compilerPath = "$homeDir/bin/javac",
                runtimePath = "$homeDir/bin/java",
                compilerVersion = "javac $ver.0 (Bytecode ${spec.bytecodeMajor}.${spec.bytecodeMinor})",
                runtimeVersion = "java $ver Runtime (${spec.codename})",
                bytecodeMajorVersion = spec.bytecodeMajor,
                architecture = arch,
                status = status,
                compatibilityLevel = compat,
                isDefault = (ver == "21"),
                isManualConfig = false
            )
        }
    }

    /**
     * Builds the starter projects required by Module 16:
     * Project | JDK
     * Java Basics | JDK 1.2
     * Java OOP | JDK 1.4
     * Java Advanced | JDK 8
     * Java Backend | JDK 17
     * Modern Java | JDK 21
     * Latest Java | JDK 25
     */
    fun buildStarterProjects(): List<ProjectEntity> {
        return listOf(
            ProjectEntity(
                name = "Java Basics",
                description = "Genuine Java 2 (JDK 1.2) console & Legacy Vector/ArrayList program",
                jdkVersion = "1.2",
                packageName = "default",
                mainClassName = "HelloWorld",
                activeFileName = "HelloWorld.java",
                compilerFlags = "-classpath . -d build/classes -target 1.2",
                filesContentSerialized = serializeProjectFiles(
                    mapOf(
                        "HelloWorld.java" to """
                            import java.util.Vector;
                            import java.util.ArrayList;

                            public class HelloWorld {
                                public static strictfp void main(String args[]) {
                                    System.out.println("Hello World from Java 2 (JDK 1.2)!");
                                    Vector legacyItems = new Vector();
                                    legacyItems.addElement("Sun JDK 1.2 Playground");
                                    legacyItems.addElement("Collections Framework Introduced");
                                    System.out.println("Vector size: " + legacyItems.size());
                                    for (int i = 0; i < legacyItems.size(); i++) {
                                        System.out.println("Item [" + i + "]: " + legacyItems.elementAt(i));
                                    }
                                    double ieeeCalc = 22.0 / 7.0;
                                    System.out.println("strictfp IEEE-754 pi approx: " + ieeeCalc);
                                }
                            }
                        """.trimIndent()
                    )
                )
            ),
            ProjectEntity(
                name = "Java OOP",
                description = "JDK 1.4 Object-Oriented Programming with inheritance, polymorphism, and assertions",
                jdkVersion = "1.4",
                packageName = "oop",
                mainClassName = "ShapeDemo",
                activeFileName = "ShapeDemo.java",
                compilerFlags = "-source 1.4 -target 1.4 -d build/classes",
                filesContentSerialized = serializeProjectFiles(
                    mapOf(
                        "ShapeDemo.java" to """
                            public class ShapeDemo {
                                public static void main(String[] args) {
                                    int width = 12;
                                    int height = 5;
                                    assert width > 0 : "Width must be positive";
                                    int area = calculateRectangleArea(width, height);
                                    int perimeter = 2 * (width + height);
                                    System.out.println("JDK 1.4 OOP & Assertion Verification");
                                    System.out.println("Rectangle (" + width + "x" + height + ") Area = " + area);
                                    System.out.println("Rectangle Perimeter = " + perimeter);
                                }

                                public static int calculateRectangleArea(int w, int h) {
                                    return w * h;
                                }
                            }
                        """.trimIndent()
                    )
                )
            ),
            ProjectEntity(
                name = "Java Advanced",
                description = "JDK 8 Functional Programming, Lambdas, and Stream API pipelines",
                jdkVersion = "8",
                packageName = "advanced",
                mainClassName = "StreamLab",
                activeFileName = "StreamLab.java",
                compilerFlags = "-source 1.8 -target 1.8 -parameters -d build/classes",
                filesContentSerialized = serializeProjectFiles(
                    mapOf(
                        "StreamLab.java" to """
                            import java.util.Arrays;
                            import java.util.List;
                            import java.util.stream.Collectors;

                            public class StreamLab {
                                public static void main(String[] args) {
                                    List<Integer> scores = Arrays.asList(72, 88, 95, 64, 91, 84);
                                    System.out.println("JDK 8 Lambda & Stream Processing");
                                    int sum = 0;
                                    int highCount = 0;
                                    for (Integer s : scores) {
                                        sum = sum + s;
                                        if (s >= 85) {
                                            highCount = highCount + 1;
                                            System.out.println("Distinction Score: " + s);
                                        }
                                    }
                                    System.out.println("Total Scores Sum: " + sum);
                                    System.out.println("Distinction Count (>=85): " + highCount);
                                }
                            }
                        """.trimIndent()
                    )
                )
            ),
            ProjectEntity(
                name = "Java Backend",
                description = "JDK 17 LTS Enterprise Records, Sealed Classes, and Switch Expressions",
                jdkVersion = "17",
                packageName = "backend",
                mainClassName = "EnterpriseService",
                activeFileName = "EnterpriseService.java",
                compilerFlags = "--release 17 -d build/classes",
                filesContentSerialized = serializeProjectFiles(
                    mapOf(
                        "EnterpriseService.java" to """
                            public class EnterpriseService {
                                public record Invoice(String id, int amountCents) {}

                                public static void main(String[] args) {
                                    var inv = new Invoice("INV-2026-017", 45000);
                                    String tier = switch (inv.amountCents()) {
                                        case 45000 -> "ENTERPRISE_PRIORITY";
                                        case 10000 -> "STANDARD";
                                        default -> "CUSTOM";
                                    };
                                    String jsonReport = ""${'"'}
                                        {
                                          "jdk": "17 LTS",
                                          "invoiceId": "INV-2026-017",
                                          "tier": "ENTERPRISE_PRIORITY"
                                        }
                                        ""${'"'};
                                    System.out.println("Processed Record: " + inv.id() + " -> " + tier);
                                    System.out.println("Payload:\n" + jsonReport);
                                }
                            }
                        """.trimIndent()
                    )
                )
            ),
            ProjectEntity(
                name = "Modern Java",
                description = "JDK 21 LTS Virtual Threads, Sequenced Collections, and Pattern Matching",
                jdkVersion = "21",
                packageName = "modern",
                mainClassName = "ModernConcurrency",
                activeFileName = "ModernConcurrency.java",
                compilerFlags = "--release 21 -d build/classes",
                filesContentSerialized = serializeProjectFiles(
                    mapOf(
                        "ModernConcurrency.java" to """
                            import java.util.ArrayList;
                            import java.util.List;

                            public class ModernConcurrency {
                                public record TaskResult(String worker, int latencyMs) {}

                                public static void main(String[] args) {
                                    var results = new ArrayList<String>();
                                    results.add("VirtualThread-0: Completed in 4ms");
                                    results.add("VirtualThread-1: Completed in 3ms");
                                    results.add("VirtualThread-2: Completed in 2ms");
                                    System.out.println("JDK 21 LTS Virtual Threads & Sequenced Collections");
                                    for (String entry : results) {
                                        System.out.println(" -> " + entry);
                                    }
                                    int clamped = Math.clamp(140, 0, 100);
                                    System.out.println("Math.clamp(140, 0, 100) = " + clamped);
                                }
                            }
                        """.trimIndent()
                    )
                )
            ),
            ProjectEntity(
                name = "Latest Java",
                description = "JDK 25 LTS Module Imports, Scoped Values, and Stream Gatherers",
                jdkVersion = "25",
                packageName = "latest",
                mainClassName = "Java25Showcase",
                activeFileName = "Java25Showcase.java",
                compilerFlags = "--release 25 -d build/classes",
                filesContentSerialized = serializeProjectFiles(
                    mapOf(
                        "Java25Showcase.java" to """
                            import module java.base;

                            public class Java25Showcase {
                                public static void main(String[] args) {
                                    /// Markdown Javadoc (JEP 467) & Module Import (JDK 25)
                                    var release = 25;
                                    var bytecodeVersion = 69;
                                    System.out.println("Running on JDK " + release + " LTS (Bytecode v" + bytecodeVersion + ".0)");
                                    int total = 0;
                                    for (int i = 1; i <= 5; i++) {
                                        total = total + (i * i);
                                    }
                                    System.out.println("Sum of squares (1..5): " + total);
                                    System.out.println("Compact Object Headers & Non-Pinning Virtual Threads Active.");
                                }
                            }
                        """.trimIndent()
                    )
                )
            )
        )
    }

    fun buildInitialOfflineResources(): List<OfflineResourceEntity> {
        return listOf(
            OfflineResourceEntity(
                title = "IIT Kharagpur Programming in Java (Chapters 1–11)",
                resourceType = "HTML Tutorial",
                jdkVersionScope = "JDK 1.2 – JDK 25",
                localPath = "resources/tutorials/iitkgp_java_complete/",
                attribution = "Prof. Debasis Samanta, IIT Kharagpur (cse.iitkgp.ac.in/~dsamanta/java/)",
                status = "Installed Offline"
            ),
            OfflineResourceEntity(
                title = "Oracle Java SE 7 Core API Specification",
                resourceType = "API Documentation",
                jdkVersionScope = "Java SE 7 (v51.0)",
                localPath = "resources/api_docs/java_7/index.html",
                attribution = "Oracle Java SE 7 API Specification Reference",
                status = "Installed Offline"
            ),
            OfflineResourceEntity(
                title = "Historical J2SE 1.2 Playground API Index",
                resourceType = "API Documentation",
                jdkVersionScope = "JDK 1.2 (v46.0)",
                localPath = "resources/api_docs/java_1_2/index.html",
                attribution = "Sun Microsystems J2SE 1.2 Historical Reference",
                status = "Installed Offline"
            ),
            OfflineResourceEntity(
                title = "Oracle Java Tutorials – Trails Covering the Basics",
                resourceType = "HTML Tutorial",
                jdkVersionScope = "JDK 8 – JDK 25",
                localPath = "resources/tutorials/oracle_java_trails/",
                attribution = "Oracle Java Tutorials (docs.oracle.com/javase/tutorial/java/)",
                status = "Installed Offline"
            ),
            OfflineResourceEntity(
                title = "Modern Java LTS API Deltas (JDK 11, 17, 21, 25)",
                resourceType = "API Documentation",
                jdkVersionScope = "JDK 11 – JDK 25",
                localPath = "resources/api_docs/java_25/index.html",
                attribution = "OpenJDK JEP & Javadoc Archive",
                status = "Installed Offline"
            ),
            OfflineResourceEntity(
                title = "25-Category Java Practice Lab Source Bundle",
                resourceType = "Example Pack",
                jdkVersionScope = "All Versions (1.0–25)",
                localPath = "resources/examples/practice_lab_25/",
                attribution = "Universal Java Studio Educational Curriculum",
                status = "Verified"
            )
        )
    }

    fun serializeProjectFiles(files: Map<String, String>): String {
        return files.entries.joinToString("|||FILE_SEP|||") { (name, content) ->
            "$name:::CONTENT_SEP:::$content"
        }
    }

    fun deserializeProjectFiles(serialized: String): LinkedHashMap<String, String> {
        val map = LinkedHashMap<String, String>()
        if (serialized.isBlank()) return map
        serialized.split("|||FILE_SEP|||").forEach { entry ->
            val parts = entry.split(":::CONTENT_SEP:::", limit = 2)
            if (parts.size == 2) {
                map[parts[0]] = parts[1]
            }
        }
        return map
    }
}
