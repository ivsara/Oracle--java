package com.example.core

data class TutorialLesson(
    val id: String,
    val chapterNumber: Int,
    val chapterTitle: String,
    val lessonTitle: String,
    val minJdkVersion: String,
    val compatibleJdkRange: String,
    val deprecationNote: String? = null,
    val topicIntroduction: String,
    val definition: String,
    val detailedExplanation: String,
    val syntaxBlock: String,
    val importantConcepts: List<String>,
    val java2ExampleCode: String,
    val modernJavaExampleCode: String,
    val programExplanation: String,
    val sampleOutput: String,
    val commonErrors: List<String>,
    val practiceQuestions: List<String>,
    val relatedApis: List<String>
)

data class ApiMemberDoc(
    val kind: String, // "CONSTRUCTOR", "METHOD", "FIELD"
    val signature: String,
    val sinceJdk: String,
    val deprecatedInJdk: String? = null,
    val description: String
)

data class ApiClassDoc(
    val id: String,
    val packageName: String,
    val className: String,
    val kind: String, // "class", "interface", "record", "enum"
    val sinceJdk: String,
    val deprecatedInJdk: String? = null,
    val removedInJdk: String? = null,
    val inheritanceHierarchy: String,
    val summary: String,
    val relatedClasses: List<String>,
    val members: List<ApiMemberDoc>
)

data class PracticeExercise(
    val id: String,
    val categoryNumber: Int,
    val categoryName: String,
    val title: String,
    val recommendedJdk: String,
    val compatibleJdkVersions: String,
    val problemStatement: String,
    val algorithmSteps: List<String>,
    val starterJavaCode: String,
    val expectedOutput: String,
    val explanation: String
)

data class QuizQuestionItem(
    val id: String,
    val chapterOrVersion: String,
    val questionType: String, // "MCQ", "FILL_BLANK", "OUTPUT_PREDICTION", "FIND_ERROR"
    val targetJdk: String,
    val prompt: String,
    val codeSnippet: String? = null,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

object KnowledgeBaseCatalog {

    val tutorialLessons: List<TutorialLesson> = listOf(
        TutorialLesson(
            id = "tut_ch1_fundamentals",
            chapterNumber = 1,
            chapterTitle = "Chapter 1 – Fundamentals of Java",
            lessonTitle = "JDK, JRE, JVM Architecture & First Java Program",
            minJdkVersion = "1.0",
            compatibleJdkRange = "JDK 1.0 – JDK 25",
            topicIntroduction = "Java was conceived by James Gosling at Sun Microsystems (released as JDK 1.0 in 1996) around the 'Write Once, Run Anywhere' (WORA) architecture powered by bytecode verification and the Java Virtual Machine.",
            definition = "The Java Development Kit (JDK) provides the compiler (javac) and tooling; the Java Runtime Environment (JRE) provides core libraries; the Java Virtual Machine (JVM) loads, verifies, and executes .class bytecode.",
            detailedExplanation = "When javac compiles a .java source file, it produces a platform-neutral .class binary beginning with magic bytes 0xCAFEBABE and a version stamp (from 45.3 in JDK 1.0/1.1 and 46.0 in Java 2 up to 69.0 in JDK 25). Historical Java requires an explicit public class and static main(String args[]) entry point, whereas JDK 25 standardizes compact source files.",
            syntaxBlock = "// Classic Java 2 Entry Point (JDK 1.0–25)\npublic class HelloWorld {\n    public static void main(String args[]) {\n        System.out.println(\"Hello World\");\n    }\n}",
            importantConcepts = listOf(
                "Bytecode independence: .class files decouple source compilation from OS CPU architecture",
                "JDK vs JRE vs JVM responsibilities",
                "Strict difference between source compatibility (-source), bytecode target (-target), and API symbol table (--release)"
            ),
            java2ExampleCode = """
                public class HelloWorld {
                    public static void main(String args[]) {
                        System.out.println("Hello World from Java 2 (JDK 1.2)");
                        System.out.println("Bytecode Major Version: 46.0");
                    }
                }
            """.trimIndent(),
            modernJavaExampleCode = """
                public class HelloWorld {
                    public static void main(String[] args) {
                        var message = "Hello World from Modern Java (JDK 21 / 25)";
                        System.out.println(message);
                    }
                }
            """.trimIndent(),
            programExplanation = "The Java 2 example uses C-style array brackets 'String args[]' common in 1998 J2SE 1.2 textbooks (such as IIT Kharagpur notes), while the modern example uses 'String[] args' and JDK 10+ local type inference ('var').",
            sampleOutput = "Hello World from Java 2 (JDK 1.2)\nBytecode Major Version: 46.0",
            commonErrors = listOf(
                "Mismatch between public class name and .java filename",
                "Omitting 'static' in main(), causing NoSuchMethodError at runtime in JDK 1.0–24",
                "Using 'var' when project JDK is configured to JDK 1.2 or JDK 8"
            ),
            practiceQuestions = listOf(
                "Modify the program to print your JDK vendor and version.",
                "Switch the project JDK to 1.2 and observe what happens if you replace String with 'var'."
            ),
            relatedApis = listOf("java.lang.System", "java.lang.String", "java.io.PrintStream")
        ),
        TutorialLesson(
            id = "tut_ch2_programming",
            chapterNumber = 2,
            chapterTitle = "Chapter 2 – Programming in Java",
            lessonTitle = "Data Types, Operators, Control Flow & Arrays",
            minJdkVersion = "1.0",
            compatibleJdkRange = "JDK 1.0 – JDK 25",
            topicIntroduction = "Java is a strongly and statically typed language featuring 8 primitive data types, structured control statements, and bounds-checked arrays.",
            definition = "Primitives (byte, short, int, long, float, double, char, boolean) hold raw values on the stack/fields, whereas arrays and Strings are reference objects verified at runtime.",
            detailedExplanation = "In JDK 1.0–1.4, loops over arrays require indexed for-loops and switch statements only accept byte, short, char, or int. JDK 5 added the enhanced for-each loop and enum switch; JDK 7 added String switch and binary/underscore numeric literals (1_000_000); JDK 14 standardized Switch Expressions with arrow labels ('case X ->').",
            syntaxBlock = "// Classic Indexed Loop (JDK 1.0+)\nfor (int i = 0; i < arr.length; i++) { ... }\n\n// Enhanced For-Each (JDK 5+)\nfor (int item : arr) { ... }\n\n// Switch Expression (JDK 14+)\nString label = switch (code) { case 1 -> \"Active\"; default -> \"Idle\"; };",
            importantConcepts = listOf(
                "IEEE 754 floating-point precision and the 'strictfp' keyword (added in JDK 1.2, made default in JDK 17)",
                "Zero-indexed array bounds checking throwing ArrayIndexOutOfBoundsException",
                "Evolution of switch from C-style fallthrough to exhaustive pattern expressions"
            ),
            java2ExampleCode = """
                public class ArrayControlDemo {
                    public static strictfp void main(String args[]) {
                        int marks[] = {78, 85, 92, 67, 88};
                        int total = 0;
                        for (int i = 0; i < marks.length; i++) {
                            total = total + marks[i];
                        }
                        int average = total / marks.length;
                        System.out.println("Java 2 Total Marks: " + total);
                        System.out.println("Average: " + average);
                    }
                }
            """.trimIndent(),
            modernJavaExampleCode = """
                public class ArrayControlDemo {
                    public static void main(String[] args) {
                        int[] marks = {78, 85, 92, 67, 88};
                        int total = 0;
                        for (int m : marks) {
                            total += m;
                        }
                        var grade = switch (total / marks.length) {
                            case 82 -> "Distinction (A)";
                            default -> "Pass";
                        };
                        System.out.println("Modern Total: " + total + " | Grade: " + grade);
                    }
                }
            """.trimIndent(),
            programExplanation = "Java 2 iterates with an explicit index variable and declares 'strictfp', while Modern Java uses the enhanced for-loop (JDK 5+) and a concise switch expression (JDK 14+).",
            sampleOutput = "Java 2 Total Marks: 410\nAverage: 82",
            commonErrors = listOf(
                "Accessing marks[marks.length] causing ArrayIndexOutOfBoundsException",
                "Using underscores in numeric literals (e.g. 10_000) in JDK 1.4 or JDK 6"
            ),
            practiceQuestions = listOf(
                "Compute both minimum and maximum elements of the array.",
                "Test dividing by zero to inspect the runtime ArithmeticException trace."
            ),
            relatedApis = listOf("java.lang.Math", "java.util.Arrays")
        ),
        TutorialLesson(
            id = "tut_ch3_oop",
            chapterNumber = 3,
            chapterTitle = "Chapter 3 – Object-Oriented Programming",
            lessonTitle = "Classes, Constructors, Inheritance, Polymorphism & Records",
            minJdkVersion = "1.0",
            compatibleJdkRange = "JDK 1.0 – JDK 25",
            topicIntroduction = "Object-Oriented Programming (OOP) models software around state (fields) and behavior (methods) using encapsulation, inheritance, abstraction, and dynamic method dispatch.",
            definition = "A class is a blueprint for objects; constructors initialize state; 'extends' establishes single inheritance; polymorphism resolves overridden methods at runtime via virtual method tables (invokevirtual).",
            detailedExplanation = "Traditional Java (JDK 1.0–15) requires explicit fields, constructors, getters, equals(), hashCode(), and toString() for data carrier classes. JDK 16 introduced 'record' classes and JDK 17 standardized 'sealed' class hierarchies ('sealed class Shape permits Circle, Rectangle').",
            syntaxBlock = "// Classic Class (JDK 1.0+)\npublic class Account {\n    private int balance;\n    public Account(int b) { this.balance = b; }\n}\n\n// Modern Record & Sealed Hierarchy (JDK 16 / 17+)\npublic record AccountRecord(String owner, int balance) {}",
            importantConcepts = listOf(
                "Constructor overloading and 'super(...)' chaining (flexible statements before super() added in JDK 25)",
                "Dynamic Method Dispatch: runtime type determines which overridden method executes",
                "Immutable data modeling with JDK 16 Records and JDK 17 Sealed Classes"
            ),
            java2ExampleCode = """
                public class OopClassicDemo {
                    public static void main(String args[]) {
                        int baseSalary = 40000;
                        int bonus = 8500;
                        int netPay = calculateCompensation(baseSalary, bonus);
                        System.out.println("Employee Base Salary: " + baseSalary);
                        System.out.println("Total Compensation: " + netPay);
                    }

                    public static int calculateCompensation(int base, int bonus) {
                        return base + bonus;
                    }
                }
            """.trimIndent(),
            modernJavaExampleCode = """
                public class OopModernDemo {
                    public record Employee(String name, int salary) {}

                    public static void main(String[] args) {
                        var emp = new Employee("Aarav Sharma", 48500);
                        System.out.println("Employee Record: " + emp.name());
                        System.out.println("Compensation: " + emp.salary());
                    }
                }
            """.trimIndent(),
            programExplanation = "Compares classic procedural/class helper encapsulation in Java 2 with an immutable 'record' in JDK 16+ that automatically synthesizes canonical constructor and component accessors.",
            sampleOutput = "Employee Base Salary: 40000\nTotal Compensation: 48500",
            commonErrors = listOf(
                "Attempting to mutate a field of a Java 'record' (all record components are implicitly final)",
                "Using '@Override' on interface methods in JDK 5 (only allowed on superclass methods in JDK 5; fixed in JDK 6)"
            ),
            practiceQuestions = listOf(
                "Add a tax deduction calculation method and print net take-home pay.",
                "Compile the Modern Record example under JDK 8 to see the compiler compatibility diagnostic."
            ),
            relatedApis = listOf("java.lang.Object", "java.lang.Record")
        ),
        TutorialLesson(
            id = "tut_ch4_interfaces_packages",
            chapterNumber = 4,
            chapterTitle = "Chapter 4 – Interfaces and Packages",
            lessonTitle = "Interfaces, Default Methods, Packages & Module System",
            minJdkVersion = "1.0",
            compatibleJdkRange = "JDK 1.0 – JDK 25",
            topicIntroduction = "Interfaces specify contracts for multiple inheritance of type, while packages and modules organize namespaces and enforce encapsulation boundaries.",
            definition = "An interface defines abstract behavioral contracts implemented via 'implements'; packages group related types into directory namespaces.",
            detailedExplanation = "In JDK 1.0–7, interfaces could only contain public static final constants and public abstract methods. JDK 8 introduced default and static interface methods to evolve the Collections API for Streams without breaking existing implementations. JDK 9 added private interface methods and the Java Platform Module System (JPMS), and JDK 25 added 'import module java.base;'.",
            syntaxBlock = "// JDK 1.0–7 Interface\npublic interface Printable { void print(); }\n\n// JDK 8+ Default Method\npublic interface Loggable {\n    default void log(String msg) { System.out.println(msg); }\n}",
            importantConcepts = listOf(
                "Multiple interface implementation vs single class inheritance",
                "JDK 8 Default Methods & Functional Interfaces (@FunctionalInterface)",
                "JDK 9 JPMS module-info.java vs JDK 25 'import module'"
            ),
            java2ExampleCode = """
                import java.util.Vector;

                public class PackageInterfaceDemo {
                    public static void main(String args[]) {
                        Vector modules = new Vector();
                        modules.addElement("java.lang");
                        modules.addElement("java.util");
                        modules.addElement("java.io");
                        System.out.println("J2SE 1.2 Core Packages Count: " + modules.size());
                        for (int i = 0; i < modules.size(); i++) {
                            System.out.println("Package: " + modules.elementAt(i));
                        }
                    }
                }
            """.trimIndent(),
            modernJavaExampleCode = """
                import java.util.List;

                public class PackageInterfaceDemo {
                    public static void main(String[] args) {
                        List<String> modules = List.of("java.base", "java.sql", "java.net.http");
                        System.out.println("Modular JDK Packages: " + modules.size());
                        for (String mod : modules) {
                            System.out.println("Module: " + mod);
                        }
                    }
                }
            """.trimIndent(),
            programExplanation = "Java 2 imports individual classes from java.util and stores items in a Vector, while JDK 9+ provides immutable List.of() factories inside the foundational java.base module.",
            sampleOutput = "J2SE 1.2 Core Packages Count: 3\nPackage: java.lang\nPackage: java.util\nPackage: java.io",
            commonErrors = listOf(
                "Reducing visibility of an interface method from public to package-private when implementing",
                "Calling List.of() when compiling against JDK 8 or earlier"
            ),
            practiceQuestions = listOf(
                "Add 'javax.swing' to the package list and print the updated size.",
                "Test 'import module java.base;' under JDK 25 vs JDK 17."
            ),
            relatedApis = listOf("java.lang.Package", "java.lang.Module", "java.util.List")
        ),
        TutorialLesson(
            id = "tut_ch5_exceptions",
            chapterNumber = 5,
            chapterTitle = "Chapter 5 – Exception Handling",
            lessonTitle = "Checked vs Unchecked Exceptions, Multi-Catch & Helpful NPEs",
            minJdkVersion = "1.0",
            compatibleJdkRange = "JDK 1.0 – JDK 25",
            topicIntroduction = "Java's structured exception handling separates error-recovery logic from normal program flow using Throwable, Exception, RuntimeException, and Error hierarchies.",
            definition = "Exceptions represent abnormal runtime conditions caught via try-catch-finally blocks or declared via 'throws'.",
            detailedExplanation = "JDK 1.4 added chained exceptions (getCause()). JDK 7 introduced try-with-resources (AutoCloseable) and multi-catch ('catch (IOException | SQLException e)'). JDK 14 added Helpful NullPointerExceptions (JEP 358) that pinpoint which variable in a chained expression was null, and JDK 22 allowed unnamed catch variables ('catch (Exception _)').",
            syntaxBlock = "try {\n    int ratio = 100 / divisor;\n} catch (ArithmeticException e) {\n    System.out.println(\"Handled: \" + e.getMessage());\n} finally {\n    System.out.println(\"Cleanup complete\");\n}",
            importantConcepts = listOf(
                "Checked exceptions (must be caught or declared) vs RuntimeExceptions",
                "Guaranteed execution of 'finally' blocks for resource cleanup",
                "JDK 7 try-with-resources and JDK 22 unnamed catch parameter '_'"
            ),
            java2ExampleCode = """
                public class ExceptionDemo {
                    public static void main(String args[]) {
                        try {
                            System.out.println("Attempting division...");
                            int numerator = 50;
                            int denominator = 0;
                            int result = numerator / denominator;
                            System.out.println("Result: " + result);
                        } catch (ArithmeticException e) {
                            System.out.println("Caught Exception: " + e.getMessage());
                        } finally {
                            System.out.println("Finally block executed safely.");
                        }
                    }
                }
            """.trimIndent(),
            modernJavaExampleCode = """
                public class ExceptionDemo {
                    public static void main(String[] args) {
                        try {
                            int[] values = {10, 20, 30};
                            System.out.println("Element 1: " + values[1]);
                            System.out.println("Accessing out of bounds: " + values[5]);
                        } catch (ArrayIndexOutOfBoundsException e) {
                            System.out.println("Diagnostic Caught: " + e);
                        } finally {
                            System.out.println("Modern verification complete.");
                        }
                    }
                }
            """.trimIndent(),
            programExplanation = "Demonstrates real runtime exception trapping and recovery in both arithmetic division by zero and array bounds violations.",
            sampleOutput = "Attempting division...\nCaught Exception: / by zero\nFinally block executed safely.",
            commonErrors = listOf(
                "Placing a broad 'catch (Exception e)' before a specific 'catch (ArithmeticException e)' (unreachable catch block)",
                "Swallowing exceptions with an empty catch block"
            ),
            practiceQuestions = listOf(
                "Change denominator from 0 to 5 and observe the normal execution path.",
                "Use 'throw new IllegalArgumentException(\"Invalid input\");' inside the try block."
            ),
            relatedApis = listOf("java.lang.Throwable", "java.lang.Exception", "java.lang.ArithmeticException", "java.lang.AutoCloseable")
        ),
        TutorialLesson(
            id = "tut_ch6_multithreading",
            chapterNumber = 6,
            chapterTitle = "Chapter 6 – Multithreading",
            lessonTitle = "OS Platform Threads, Synchronization & JDK 21 Virtual Threads",
            minJdkVersion = "1.0",
            compatibleJdkRange = "JDK 1.0 – JDK 25",
            deprecationNote = "Thread.stop(), Thread.suspend(), and Thread.resume() were deprecated in JDK 1.2 due to monitor deadlock hazards and throw UnsupportedOperationException in JDK 20+.",
            topicIntroduction = "Java was one of the first mainstream languages to build multithreading and memory synchronization primitives directly into the language and runtime.",
            definition = "A thread is an independent path of execution within a process, created via Thread or Runnable, and coordinated via synchronized monitors, Locks, or Virtual Thread executors.",
            detailedExplanation = "In JDK 1.0–1.4, threads were managed directly via new Thread(Runnable) and wait()/notify(). JDK 1.2 deprecated unsafe Thread.stop()/suspend(). JDK 5 introduced java.util.concurrent (ExecutorService, Future, Locks). JDK 21 standardized Project Loom Virtual Threads (JEP 444), and JDK 24 eliminated carrier thread pinning inside synchronized blocks (JEP 491).",
            syntaxBlock = "// Classic Thread (JDK 1.0+)\nThread t = new Thread(runnable);\nt.start();\n\n// Virtual Thread (JDK 21+)\nThread.ofVirtual().start(() -> System.out.println(\"Lightweight Loom Thread\"));",
            importantConcepts = listOf(
                "Why Thread.stop() and Thread.suspend() are deprecated since Java 2 (JDK 1.2)",
                "Platform threads (1:1 OS kernel threads) vs JDK 21 Virtual Threads (M:N user-mode continuations)",
                "JEP 491 (JDK 24): Synchronizing virtual threads without carrier pinning"
            ),
            java2ExampleCode = """
                public class ThreadLifecycleDemo {
                    public static void main(String args[]) {
                        System.out.println("Main Thread Starting (JDK 1.2 Safe Cooperative Pattern)");
                        for (int step = 1; step <= 3; step++) {
                            System.out.println("Worker Task Tick #" + step + " [RUNNABLE]");
                        }
                        System.out.println("Worker Task Completed Cleanly [TERMINATED]");
                    }
                }
            """.trimIndent(),
            modernJavaExampleCode = """
                public class ThreadLifecycleDemo {
                    public static void main(String[] args) {
                        var tasks = 4;
                        System.out.println("JDK 21+ Virtual Thread Scheduler Simulation");
                        for (int i = 1; i <= tasks; i++) {
                            System.out.println("VirtualThread[#" + (20 + i) + "]/runnable@ForkJoinPool-1-worker-1");
                        }
                        System.out.println("All " + tasks + " virtual threads completed with zero pinning.");
                    }
                }
            """.trimIndent(),
            programExplanation = "Highlights safe cooperative thread termination in Java 2 (avoiding deprecated Thread.stop) alongside modern JDK 21+ Virtual Thread scheduling.",
            sampleOutput = "Main Thread Starting (JDK 1.2 Safe Cooperative Pattern)\nWorker Task Tick #1 [RUNNABLE]\nWorker Task Tick #2 [RUNNABLE]\nWorker Task Tick #3 [RUNNABLE]\nWorker Task Completed Cleanly [TERMINATED]",
            commonErrors = listOf(
                "Calling thread.run() instead of thread.start() (executes synchronously on current thread)",
                "Calling deprecated Thread.stop() or Thread.suspend()"
            ),
            practiceQuestions = listOf(
                "Add 'Thread.suspend()' to the code and observe the Version Syntax Highlighter deprecation warning.",
                "Compare JDK 1.2 and JDK 21 in the Version Explorer to see threading evolution."
            ),
            relatedApis = listOf("java.lang.Thread", "java.lang.Runnable", "java.util.concurrent.Executors", "java.lang.ScopedValue")
        ),
        TutorialLesson(
            id = "tut_ch7_9_10_gui_multimedia",
            chapterNumber = 7,
            chapterTitle = "Chapters 7, 9 & 10 – AWT, Applets & Swing GUI",
            lessonTitle = "Heavyweight AWT, Legacy Applets & Lightweight Swing Architecture",
            minJdkVersion = "1.0",
            compatibleJdkRange = "JDK 1.0 – JDK 25 (Applets removed in modern JDKs)",
            deprecationNote = "java.applet.Applet and javax.swing.JApplet were deprecated in JDK 9 (JEP 289), marked for removal in JDK 17 (JEP 398), and are obsolete in modern Java.",
            topicIntroduction = "Java's graphical toolkit evolved from JDK 1.0 Heavyweight AWT (Abstract Window Toolkit) and browser Applets to Java 2 (JDK 1.2) Lightweight Swing components (JFrame, JButton, JPanel).",
            definition = "AWT delegates rendering to native OS peer widgets, whereas Swing paints components purely in Java using Java 2D and the Delegation Event Model (ActionListener).",
            detailedExplanation = "In JDK 1.0, AWT used inheritance-based event handling. JDK 1.1 introduced event listeners (java.awt.event.ActionListener). JDK 1.2 bundled javax.swing into the core platform. While AWT and Swing remain supported in desktop JDKs, browser Applets (java.applet.Applet) were deprecated in JDK 9 and marked for removal in JDK 17.",
            syntaxBlock = "// Java 2 Swing Component Hierarchy (JDK 1.2+)\nJFrame frame = new JFrame(\"Calculator\");\nJButton btn = new JButton(\"Compute\");",
            importantConcepts = listOf(
                "AWT Heavyweight OS Peers vs Swing Lightweight MVC Architecture",
                "Event Dispatch Thread (EDT) single-threaded UI rule",
                "Deprecation and removal lifecycle of java.applet.Applet in JDK 9–25"
            ),
            java2ExampleCode = """
                import java.awt.Frame;
                import java.awt.Button;
                import javax.swing.JFrame;
                import javax.swing.JButton;

                public class SwingGuiDemo {
                    public static void main(String args[]) {
                        String title = "Universal Java Studio - JFC Swing Frame";
                        int width = 640;
                        int height = 480;
                        System.out.println("Initializing JFrame: " + title);
                        System.out.println("Viewport Bounds: " + width + "x" + height);
                        System.out.println("Added JButton('Compile') & JLabel('Status: Ready')");
                    }
                }
            """.trimIndent(),
            modernJavaExampleCode = """
                import javax.swing.JFrame;
                import javax.swing.JPanel;

                public class SwingGuiDemo {
                    public record WindowConfig(String title, int width, int height) {}

                    public static void main(String[] args) {
                        var cfg = new WindowConfig("Modern Swing HiDPI Surface", 1280, 720);
                        System.out.println("Window: " + cfg.title() + " [" + cfg.width() + "x" + cfg.height() + "]");
                    }
                }
            """.trimIndent(),
            programExplanation = "Demonstrates JFC/Swing component setup introduced in J2SE 1.2 and highlights why standalone JFrame desktop apps replaced browser Applets.",
            sampleOutput = "Initializing JFrame: Universal Java Studio - JFC Swing Frame\nViewport Bounds: 640x480\nAdded JButton('Compile') & JLabel('Status: Ready')",
            commonErrors = listOf(
                "Importing javax.swing.* when targeting JDK 1.0 or JDK 1.1 (Swing was integrated into core in JDK 1.2)",
                "Extending java.applet.Applet in JDK 17+ projects (triggers deprecation-for-removal warnings)"
            ),
            practiceQuestions = listOf(
                "Switch the project JDK to 1.0 and try compiling 'import javax.swing.JFrame;' to test the API guard.",
                "Add 'Applet' to the code and observe the syntax highlighter deprecation indicator in JDK 17."
            ),
            relatedApis = listOf("java.awt.Frame", "java.awt.event.ActionListener", "javax.swing.JFrame", "javax.swing.JButton", "java.applet.Applet")
        ),
        TutorialLesson(
            id = "tut_ch8_11_io_net_jdbc",
            chapterNumber = 8,
            chapterTitle = "Chapters 8 & 11 – I/O, Networking, HTTP Client & JDBC",
            lessonTitle = "Streams, NIO.2 Files, Sockets, Modern HTTP/2 Client & JDBC",
            minJdkVersion = "1.0",
            compatibleJdkRange = "JDK 1.0 – JDK 25",
            topicIntroduction = "Java's I/O, networking, and database layers progressed from byte/character streams and TCP Sockets (JDK 1.0/1.1) to NIO channels (1.4), NIO.2 Path/Files (JDK 7), and async HTTP/2 Client (JDK 11).",
            definition = "InputStream/OutputStream process raw bytes; Reader/Writer handle character encodings; java.sql (JDBC) executes parameterized SQL queries against relational databases.",
            detailedExplanation = "In JDK 1.1–1.4, console input required wrapping System.in inside InputStreamReader and BufferedReader. JDK 5 added java.util.Scanner; JDK 7 added Files.readAllLines and try-with-resources for automatic JDBC Connection/PreparedStatement closing; JDK 11 added Files.readString() and java.net.http.HttpClient.",
            syntaxBlock = "// JDK 5+ Scanner Console Input\nScanner sc = new Scanner(System.in);\nint n = sc.nextInt();\n\n// JDK 11+ Standard HTTP Client\nHttpClient client = HttpClient.newHttpClient();",
            importantConcepts = listOf(
                "Buffered I/O vs NIO non-blocking channels and NIO.2 Path API",
                "Using PreparedStatement instead of string concatenation to prevent SQL Injection in JDBC",
                "Passing Standard Input (stdin) to Scanner(System.in) inside Universal Java Studio"
            ),
            java2ExampleCode = """
                public class IoJdbcDemo {
                    public static void main(String args[]) {
                        String jdbcUrl = "jdbc:sqlite:universal_java_studio.db";
                        int rowsUpdated = 3;
                        System.out.println("Connecting to JDBC URL: " + jdbcUrl);
                        System.out.println("Executed PreparedStatement: INSERT INTO Projects...");
                        System.out.println("Transaction committed. Rows affected: " + rowsUpdated);
                    }
                }
            """.trimIndent(),
            modernJavaExampleCode = """
                import java.util.Scanner;

                public class IoJdbcDemo {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        int inputVal = sc.nextInt();
                        System.out.println("Read integer from Standard Input: " + inputVal);
                        System.out.println("Squared output: " + (inputVal * inputVal));
                    }
                }
            """.trimIndent(),
            programExplanation = "The modern example reads an integer directly from the IDE's Standard Input console using java.util.Scanner (introduced in JDK 5) and prints its square.",
            sampleOutput = "Connecting to JDBC URL: jdbc:sqlite:universal_java_studio.db\nExecuted PreparedStatement: INSERT INTO Projects...\nTransaction committed. Rows affected: 3",
            commonErrors = listOf(
                "Importing java.util.Scanner when project JDK is set to 1.2 or 1.4",
                "Forgetting to close I/O streams or JDBC connections when not using JDK 7+ try-with-resources"
            ),
            practiceQuestions = listOf(
                "Load the Modern Scanner example into the Editor, type '15' into the Standard Input box, and click Run!",
                "Try compiling 'import java.net.http.HttpClient;' under JDK 8 vs JDK 11."
            ),
            relatedApis = listOf("java.io.BufferedReader", "java.util.Scanner", "java.nio.file.Files", "java.net.Socket", "java.net.http.HttpClient", "java.sql.Connection")
        ),
        TutorialLesson(
            id = "tut_modern_streams_records_25",
            chapterNumber = 12,
            chapterTitle = "Chapter 12 – Modern Java Evolution (JDK 8 to JDK 25)",
            lessonTitle = "Lambdas, Streams, Pattern Matching, Gatherers & Compact Source",
            minJdkVersion = "8",
            compatibleJdkRange = "JDK 8 – JDK 25",
            topicIntroduction = "From JDK 8 to JDK 25, Java embraced functional programming, data-oriented programming (Records, Sealed Classes, Pattern Matching), and high-throughput concurrency (Virtual Threads, Scoped Values).",
            definition = "Data-oriented programming combines immutable Records (JDK 16), Sealed Algebraic Types (JDK 17), and Exhaustive Pattern Matching in switch (JDK 21).",
            detailedExplanation = "Each modern feature has a precise minimum JDK version: Lambdas & Streams (JDK 8), Modules & List.of (JDK 9), 'var' (JDK 10), HttpClient (JDK 11), Switch Expressions (JDK 14), Text Blocks (JDK 15), Records & instanceof Patterns (JDK 16), Sealed Classes (JDK 17), Virtual Threads & Record Patterns (JDK 21), Unnamed Variables '_' & FFM (JDK 22), Markdown Javadoc '///' (JDK 23), Stream Gatherers & Class-File API (JDK 24), and Module Imports & Scoped Values (JDK 25).",
            syntaxBlock = "/// Markdown Javadoc (JDK 23+)\nimport module java.base; // JDK 25\n\npublic record SensorReading(String id, int celsius) {}",
            importantConcepts = listOf(
                "Exact version boundaries between JDK 8, 11, 17, 21, and 25 LTS releases",
                "Why modern javac '--release N' is superior to '-source N -target N' (verifies historical API symbol table)",
                "Recognizing preview vs standardized JEP features"
            ),
            java2ExampleCode = """
                import java.util.Vector;

                public class HistoricalVsModern {
                    public static void main(String args[]) {
                        Vector readings = new Vector();
                        readings.addElement(new Integer(24));
                        readings.addElement(new Integer(31));
                        readings.addElement(new Integer(19));
                        System.out.println("Java 2 Vector Readings Count: " + readings.size());
                    }
                }
            """.trimIndent(),
            modernJavaExampleCode = """
                import module java.base;

                public class HistoricalVsModern {
                    public record Reading(String sensor, int tempC) {}

                    public static void main(String[] args) {
                        var r = new Reading("Core-CPU-0", 31);
                        String status = switch (r.tempC()) {
                            case 31 -> "OPTIMAL_THERMAL";
                            default -> "NOMINAL";
                        };
                        System.out.println("Sensor " + r.sensor() + " (" + r.tempC() + "C): " + status);
                    }
                }
            """.trimIndent(),
            programExplanation = "Notice how the Java 2 program uses Vector and 'new Integer(24)' (which is valid in JDK 1.2 but flagged as deprecated in JDK 9+), while the JDK 25 program uses 'import module java.base;', 'record', 'var', and a switch expression.",
            sampleOutput = "Sensor Core-CPU-0 (31C): OPTIMAL_THERMAL",
            commonErrors = listOf(
                "Using 'new Integer(24)' in JDK 17+ (triggers @Deprecated(forRemoval=true) warning)",
                "Using 'record' or 'var' when project JDK is set to JDK 8"
            ),
            practiceQuestions = listOf(
                "Open the Java 2 example in the Editor and switch the project JDK from 1.2 to 17 to see the 'new Integer(...)' deprecation highlight!",
                "Open the Modern example and test compiling it under JDK 25 vs JDK 11."
            ),
            relatedApis = listOf("java.util.stream.Stream", "java.util.stream.Gatherers", "java.lang.Record", "java.lang.ScopedValue")
        )
    )

    val apiDocumentationCatalog: List<ApiClassDoc> = listOf(
        ApiClassDoc(
            id = "api_object",
            packageName = "java.lang",
            className = "Object",
            kind = "class",
            sinceJdk = "1.0",
            inheritanceHierarchy = "java.lang.Object",
            summary = "Class Object is the root of the class hierarchy. Every class has Object as a superclass. All objects, including arrays, implement the methods of this class.",
            relatedClasses = listOf("java.lang.Class", "java.lang.String", "java.util.Objects"),
            members = listOf(
                ApiMemberDoc("CONSTRUCTOR", "public Object()", "1.0", null, "Constructs a new object."),
                ApiMemberDoc("METHOD", "public boolean equals(Object obj)", "1.0", null, "Indicates whether some other object is 'equal to' this one."),
                ApiMemberDoc("METHOD", "public int hashCode()", "1.0", null, "Returns a hash code value for the object."),
                ApiMemberDoc("METHOD", "public String toString()", "1.0", null, "Returns a string representation of the object."),
                ApiMemberDoc("METHOD", "protected void finalize() throws Throwable", "1.0", "9", "Deprecated in JDK 9 & marked for removal in JDK 18 (JEP 421). Use Cleaner or try-with-resources instead.")
            )
        ),
        ApiClassDoc(
            id = "api_string",
            packageName = "java.lang",
            className = "String",
            kind = "class",
            sinceJdk = "1.0",
            inheritanceHierarchy = "java.lang.Object -> java.lang.String (implements Serializable, Comparable<String>, CharSequence)",
            summary = "The String class represents immutable character strings. JDK 9 introduced Compact Strings (byte[] storage); JDK 11 added strip/isBlank/lines/repeat; JDK 15 added formatted/stripIndent.",
            relatedClasses = listOf("java.lang.StringBuffer", "java.lang.StringBuilder", "java.lang.CharSequence"),
            members = listOf(
                ApiMemberDoc("METHOD", "public int length()", "1.0", null, "Returns the length of this string."),
                ApiMemberDoc("METHOD", "public String substring(int beginIndex, int endIndex)", "1.0", null, "Returns a string that is a substring of this string."),
                ApiMemberDoc("METHOD", "public static String format(String format, Object... args)", "5", null, "Returns a formatted string using the specified format string and arguments."),
                ApiMemberDoc("METHOD", "public boolean isBlank()", "11", null, "Returns true if the string is empty or contains only white space codepoints."),
                ApiMemberDoc("METHOD", "public String repeat(int count)", "11", null, "Returns a string whose value is the concatenation of this string repeated count times."),
                ApiMemberDoc("METHOD", "public String formatted(Object... args)", "15", null, "Formats using this string as the format string (supports Text Blocks).")
            )
        ),
        ApiClassDoc(
            id = "api_thread",
            packageName = "java.lang",
            className = "Thread",
            kind = "class",
            sinceJdk = "1.0",
            inheritanceHierarchy = "java.lang.Object -> java.lang.Thread (implements Runnable)",
            summary = "A thread is a thread of execution in a program. JDK 1.2 deprecated unsafe stop/suspend/resume; JDK 21 standardized lightweight Virtual Threads (Thread.ofVirtual()).",
            relatedClasses = listOf("java.lang.Runnable", "java.util.concurrent.Executors", "java.lang.ScopedValue"),
            members = listOf(
                ApiMemberDoc("CONSTRUCTOR", "public Thread(Runnable target)", "1.0", null, "Allocates a new platform Thread object."),
                ApiMemberDoc("METHOD", "public void start()", "1.0", null, "Causes this thread to begin execution; the JVM calls the run method."),
                ApiMemberDoc("METHOD", "public final void stop()", "1.0", "1.2", "Deprecated in JDK 1.2 (inherently unsafe monitor unlocking) and removed/disabled in modern Java."),
                ApiMemberDoc("METHOD", "public final void suspend()", "1.0", "1.2", "Deprecated in JDK 1.2 (deadlock prone); throws UnsupportedOperationException in JDK 20+."),
                ApiMemberDoc("METHOD", "public static Thread.Builder.OfVirtual ofVirtual()", "21", null, "Returns a builder for creating a Virtual Thread (Project Loom JEP 444).")
            )
        ),
        ApiClassDoc(
            id = "api_vector",
            packageName = "java.util",
            className = "Vector",
            kind = "class",
            sinceJdk = "1.0",
            inheritanceHierarchy = "java.lang.Object -> java.util.AbstractCollection -> java.util.AbstractList -> java.util.Vector",
            summary = "The Vector class implements a growable array of objects. Retrofitted into the Collections Framework in Java 2 (JDK 1.2) and parameterized with Generics in JDK 5. Synchronized on every operation.",
            relatedClasses = listOf("java.util.ArrayList", "java.util.Enumeration", "java.util.Stack"),
            members = listOf(
                ApiMemberDoc("CONSTRUCTOR", "public Vector()", "1.0", null, "Constructs an empty vector with initial capacity 10."),
                ApiMemberDoc("METHOD", "public synchronized void addElement(Object obj)", "1.0", null, "Adds the specified component to the end of this vector, increasing its size by one."),
                ApiMemberDoc("METHOD", "public synchronized Object elementAt(int index)", "1.0", null, "Returns the component at the specified index."),
                ApiMemberDoc("METHOD", "public synchronized boolean add(E e)", "1.2", null, "Appends the specified element to the end of this Vector (Collections Framework method).")
            )
        ),
        ApiClassDoc(
            id = "api_arraylist",
            packageName = "java.util",
            className = "ArrayList",
            kind = "class",
            sinceJdk = "1.2",
            inheritanceHierarchy = "java.lang.Object -> java.util.AbstractCollection -> java.util.AbstractList -> java.util.ArrayList (implements List, RandomAccess, SequencedCollection)",
            summary = "Resizable-array implementation of the List interface introduced in Java 2 (JDK 1.2) as the unsynchronized, high-performance replacement for Vector.",
            relatedClasses = listOf("java.util.List", "java.util.Vector", "java.util.LinkedList", "java.util.Collections"),
            members = listOf(
                ApiMemberDoc("CONSTRUCTOR", "public ArrayList()", "1.2", null, "Constructs an empty list with an initial capacity of ten."),
                ApiMemberDoc("METHOD", "public boolean add(E e)", "1.2", null, "Appends the specified element to the end of this list."),
                ApiMemberDoc("METHOD", "public E get(int index)", "1.2", null, "Returns the element at the specified position in this list."),
                ApiMemberDoc("METHOD", "public int size()", "1.2", null, "Returns the number of elements in this list."),
                ApiMemberDoc("METHOD", "public E getFirst()", "21", null, "Gets the first element of this SequencedCollection (JEP 431).")
            )
        ),
        ApiClassDoc(
            id = "api_hashmap",
            packageName = "java.util",
            className = "HashMap",
            kind = "class",
            sinceJdk = "1.2",
            inheritanceHierarchy = "java.lang.Object -> java.util.AbstractMap -> java.util.HashMap (implements Map, Cloneable, Serializable)",
            summary = "Hash table based implementation of the Map interface introduced in Java 2 (JDK 1.2). Permits null values and the null key. JDK 8 added red-black tree collision bins.",
            relatedClasses = listOf("java.util.Map", "java.util.Hashtable", "java.util.LinkedHashMap", "java.util.TreeMap"),
            members = listOf(
                ApiMemberDoc("CONSTRUCTOR", "public HashMap()", "1.2", null, "Constructs an empty HashMap with default initial capacity (16) and load factor (0.75)."),
                ApiMemberDoc("METHOD", "public V put(K key, V value)", "1.2", null, "Associates the specified value with the specified key in this map."),
                ApiMemberDoc("METHOD", "public V get(Object key)", "1.2", null, "Returns the value to which the specified key is mapped, or null."),
                ApiMemberDoc("METHOD", "public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction)", "8", null, "Computes value using functional mapping if key is absent."),
                ApiMemberDoc("METHOD", "public static <K, V> HashMap<K, V> newHashMap(int numMappings)", "19", null, "Creates a new HashMap pre-sized to accommodate numMappings without resizing.")
            )
        ),
        ApiClassDoc(
            id = "api_swing_jframe",
            packageName = "javax.swing",
            className = "JFrame",
            kind = "class",
            sinceJdk = "1.2",
            inheritanceHierarchy = "java.lang.Object -> java.awt.Component -> java.awt.Container -> java.awt.Window -> java.awt.Frame -> javax.swing.JFrame",
            summary = "An extended version of java.awt.Frame that adds support for the JFC/Swing component architecture, root panes, and pluggable Look-and-Feel.",
            relatedClasses = listOf("javax.swing.JButton", "javax.swing.JLabel", "javax.swing.JPanel", "java.awt.Frame"),
            members = listOf(
                ApiMemberDoc("CONSTRUCTOR", "public JFrame(String title)", "1.2", null, "Creates a new, initially invisible Frame with the specified title."),
                ApiMemberDoc("METHOD", "public Container getContentPane()", "1.2", null, "Returns the contentPane object for this frame."),
                ApiMemberDoc("METHOD", "public void setDefaultCloseOperation(int operation)", "1.2", null, "Sets the operation that will happen by default when the user initiates a 'close'.")
            )
        ),
        ApiClassDoc(
            id = "api_applet",
            packageName = "java.applet",
            className = "Applet",
            kind = "class",
            sinceJdk = "1.0",
            deprecatedInJdk = "9",
            removedInJdk = "25",
            inheritanceHierarchy = "java.lang.Object -> java.awt.Component -> java.awt.Container -> java.awt.Panel -> java.applet.Applet",
            summary = "Historical embeddable browser program class from JDK 1.0. Deprecated in JDK 9 (JEP 289) as browser NPAPI plugins were retired, marked @Deprecated(forRemoval=true) in JDK 17 (JEP 398).",
            relatedClasses = listOf("java.awt.Panel", "javax.swing.JFrame"),
            members = listOf(
                ApiMemberDoc("METHOD", "public void init()", "1.0", "9", "Called by the browser or applet viewer to inform this applet that it has been loaded."),
                ApiMemberDoc("METHOD", "public void start()", "1.0", "9", "Called by the browser or applet viewer to start execution."),
                ApiMemberDoc("METHOD", "public void paint(Graphics g)", "1.0", "9", "Paints the applet canvas.")
            )
        ),
        ApiClassDoc(
            id = "api_scanner",
            packageName = "java.util",
            className = "Scanner",
            kind = "class",
            sinceJdk = "5",
            inheritanceHierarchy = "java.lang.Object -> java.util.Scanner (implements Iterator<String>, Closeable)",
            summary = "A simple text scanner which can parse primitive types and strings using regular expressions. Introduced in J2SE 5.0 (not available in Java 1.2 or 1.4).",
            relatedClasses = listOf("java.io.BufferedReader", "java.util.regex.Pattern"),
            members = listOf(
                ApiMemberDoc("CONSTRUCTOR", "public Scanner(InputStream source)", "5", null, "Constructs a new Scanner that produces values scanned from the specified input stream."),
                ApiMemberDoc("METHOD", "public int nextInt()", "5", null, "Scans the next token of the input as an int."),
                ApiMemberDoc("METHOD", "public String nextLine()", "5", null, "Advances this scanner past the current line and returns the input that was skipped."),
                ApiMemberDoc("METHOD", "public Stream<String> findAll(Pattern pattern)", "9", null, "Returns a stream of match results from this scanner.")
            )
        ),
        ApiClassDoc(
            id = "api_nio_files",
            packageName = "java.nio.file",
            className = "Files",
            kind = "class",
            sinceJdk = "7",
            inheritanceHierarchy = "java.lang.Object -> java.nio.file.Files",
            summary = "Consists exclusively of static methods that operate on files, directories, or other types of files. Introduced in Oracle Java SE 7 (NIO.2).",
            relatedClasses = listOf("java.nio.file.Path", "java.nio.file.Paths", "java.io.File"),
            members = listOf(
                ApiMemberDoc("METHOD", "public static List<String> readAllLines(Path path)", "7", null, "Read all lines from a file."),
                ApiMemberDoc("METHOD", "public static Stream<String> lines(Path path)", "8", null, "Read all lines from a file as a lazy Stream."),
                ApiMemberDoc("METHOD", "public static String readString(Path path)", "11", null, "Reads all content from a file into a string using UTF-8 charset."),
                ApiMemberDoc("METHOD", "public static long mismatch(Path path, Path path2)", "12", null, "Finds and returns the position of the first mismatched byte in the content of two files.")
            )
        ),
        ApiClassDoc(
            id = "api_stream",
            packageName = "java.util.stream",
            className = "Stream",
            kind = "interface",
            sinceJdk = "8",
            inheritanceHierarchy = "java.util.stream.BaseStream -> java.util.stream.Stream<T>",
            summary = "A sequence of elements supporting sequential and parallel aggregate operations. Introduced in JDK 8; enhanced with takeWhile/dropWhile (JDK 9), toList() (JDK 16), and gather() (JDK 24).",
            relatedClasses = listOf("java.util.stream.Collectors", "java.util.stream.Gatherers", "java.util.Optional"),
            members = listOf(
                ApiMemberDoc("METHOD", "Stream<T> filter(Predicate<? super T> predicate)", "8", null, "Returns a stream consisting of the elements of this stream that match the given predicate."),
                ApiMemberDoc("METHOD", "<R> Stream<R> map(Function<? super T, ? extends R> mapper)", "8", null, "Returns a stream consisting of the results of applying the given function."),
                ApiMemberDoc("METHOD", "default List<T> toList()", "16", null, "Accumulates the elements of this stream into an unmodifiable List."),
                ApiMemberDoc("METHOD", "default <R> Stream<R> gather(Gatherer<? super T, ?, R> gatherer)", "24", null, "Transforms elements using a stateful intermediate Stream Gatherer (JEP 485).")
            )
        ),
        ApiClassDoc(
            id = "api_httpclient",
            packageName = "java.net.http",
            className = "HttpClient",
            kind = "class",
            sinceJdk = "11",
            inheritanceHierarchy = "java.lang.Object -> java.net.http.HttpClient",
            summary = "An HttpClient can be used to send requests and retrieve their responses. Supports HTTP/1.1, HTTP/2, and WebSockets synchronously and asynchronously. Standardized in JDK 11.",
            relatedClasses = listOf("java.net.http.HttpRequest", "java.net.http.HttpResponse", "java.net.URL"),
            members = listOf(
                ApiMemberDoc("METHOD", "public static HttpClient newHttpClient()", "11", null, "Returns a new HttpClient with default settings."),
                ApiMemberDoc("METHOD", "public abstract <T> HttpResponse<T> send(HttpRequest req, BodyHandler<T> handler)", "11", null, "Sends the given request using this client, blocking if necessary to get the response.")
            )
        ),
        ApiClassDoc(
            id = "api_scoped_value",
            packageName = "java.lang",
            className = "ScopedValue",
            kind = "class",
            sinceJdk = "25",
            inheritanceHierarchy = "java.lang.Object -> java.lang.ScopedValue<T>",
            summary = "A value that may be safely and efficiently shared to methods without using method parameters. Preferred over ThreadLocal, especially when using large numbers of Virtual Threads. Standardized in JDK 25.",
            relatedClasses = listOf("java.lang.Thread", "java.lang.ThreadLocal"),
            members = listOf(
                ApiMemberDoc("METHOD", "public static <T> ScopedValue<T> newInstance()", "25", null, "Creates a scoped value that is initially unbound for all threads."),
                ApiMemberDoc("METHOD", "public static <T> Carrier where(ScopedValue<T> key, T value)", "25", null, "Creates a new Carrier that binds the given ScopedValue to a value within a dynamic scope."),
                ApiMemberDoc("METHOD", "public T get()", "25", null, "Returns the value of the scoped value if bound in the current thread.")
            )
        )
    )

    val practiceExercises: List<PracticeExercise> = listOf(
        PracticeExercise(
            id = "ex_1_basic",
            categoryNumber = 1,
            categoryName = "1. Basic Java Programs",
            title = "Fahrenheit to Celsius & IEEE-754 strictfp Converter",
            recommendedJdk = "1.2",
            compatibleJdkVersions = "JDK 1.2 – JDK 25",
            problemStatement = "Write a Java 2 compatible program that converts a temperature from Fahrenheit (98.6F) to Celsius using integer and floating-point arithmetic.",
            algorithmSteps = listOf(
                "Declare integer Fahrenheit value or double reading.",
                "Apply conversion formula C = (F - 32) * 5 / 9.",
                "Print both input and converted output to System.out."
            ),
            starterJavaCode = """
                public class TempConverter {
                    public static strictfp void main(String args[]) {
                        int fahrenheit = 212;
                        int celsius = (fahrenheit - 32) * 5 / 9;
                        System.out.println("Fahrenheit: " + fahrenheit + "F");
                        System.out.println("Celsius: " + celsius + "C");
                    }
                }
            """.trimIndent(),
            expectedOutput = "Fahrenheit: 212F\nCelsius: 100C",
            explanation = "Demonstrates basic Java 2 compilation, strictfp modifier, and arithmetic operator precedence."
        ),
        PracticeExercise(
            id = "ex_2_decision",
            categoryNumber = 2,
            categoryName = "2. Decision Making",
            title = "Leap Year & Tax Slab Evaluator",
            recommendedJdk = "1.4",
            compatibleJdkVersions = "JDK 1.0 – JDK 25",
            problemStatement = "Determine whether a given year (2024) is a leap year using compound boolean expressions and conditional branches.",
            algorithmSteps = listOf(
                "Initialize year = 2024.",
                "Check if year is divisible by 400 OR (divisible by 4 and not by 100).",
                "Print the decision result."
            ),
            starterJavaCode = """
                public class LeapYearCheck {
                    public static void main(String[] args) {
                        int year = 2024;
                        int rem4 = year % 4;
                        if (rem4 == 0) {
                            System.out.println("Year " + year + " is a Leap Year.");
                        } else {
                            System.out.println("Year " + year + " is a Common Year.");
                        }
                    }
                }
            """.trimIndent(),
            expectedOutput = "Year 2024 is a Leap Year.",
            explanation = "Uses modulo (%) arithmetic and if-else decision branching."
        ),
        PracticeExercise(
            id = "ex_3_loops",
            categoryNumber = 3,
            categoryName = "3. Looping Statements",
            title = "Fibonacci Sequence & Factorial Generator",
            recommendedJdk = "5",
            compatibleJdkVersions = "JDK 1.0 – JDK 25",
            problemStatement = "Compute the factorial of N = 6 and print the running product at each iteration using a for-loop.",
            algorithmSteps = listOf(
                "Set n = 6 and fact = 1.",
                "Loop i from 1 to n, multiplying fact = fact * i.",
                "Print each step and final factorial."
            ),
            starterJavaCode = """
                public class FactorialLab {
                    public static void main(String[] args) {
                        int n = 6;
                        int fact = 1;
                        for (int i = 1; i <= n; i++) {
                            fact = fact * i;
                            System.out.println("Step " + i + "! = " + fact);
                        }
                        System.out.println("Final Factorial: " + fact);
                    }
                }
            """.trimIndent(),
            expectedOutput = "Step 1! = 1\nStep 2! = 2\nStep 3! = 6\nStep 4! = 24\nStep 5! = 120\nStep 6! = 720\nFinal Factorial: 720",
            explanation = "Tests iterative loop state updates and accumulator variables."
        ),
        PracticeExercise(
            id = "ex_4_arrays",
            categoryNumber = 4,
            categoryName = "4. Arrays",
            title = "Array Peak & Sum Analyzer",
            recommendedJdk = "5",
            compatibleJdkVersions = "JDK 1.0 – JDK 25",
            problemStatement = "Given an integer array of sensor readings, compute the sum and maximum reading.",
            algorithmSteps = listOf(
                "Initialize integer array readings.",
                "Traverse elements updating sum and max via Math.max.",
                "Display results."
            ),
            starterJavaCode = """
                public class ArrayAnalyzer {
                    public static void main(String[] args) {
                        int[] data = {14, 29, 42, 18, 37};
                        int sum = 0;
                        int peak = 0;
                        for (int i = 0; i < data.length; i++) {
                            int val = data[i];
                            sum = sum + val;
                            peak = Math.max(peak, val);
                        }
                        System.out.println("Array Length: " + data.length);
                        System.out.println("Sum: " + sum + " | Peak: " + peak);
                    }
                }
            """.trimIndent(),
            expectedOutput = "Array Length: 5\nSum: 140 | Peak: 42",
            explanation = "Combines array indexing, length property, and java.lang.Math.max."
        ),
        PracticeExercise(
            id = "ex_5_strings",
            categoryNumber = 5,
            categoryName = "5. Strings",
            title = "String Normalization & Case Inspector",
            recommendedJdk = "11",
            compatibleJdkVersions = "JDK 11 – JDK 25",
            problemStatement = "Normalize a title string, convert it to uppercase, and repeat a separator banner using JDK 11 String.repeat().",
            algorithmSteps = listOf(
                "Create a String variable.",
                "Invoke toUpperCase(), length(), and repeat().",
                "Print formatted output."
            ),
            starterJavaCode = """
                public class StringInspector {
                    public static void main(String[] args) {
                        String title = "Universal Java Studio";
                        String upper = title.toUpperCase();
                        String divider = "=".repeat(21);
                        System.out.println(divider);
                        System.out.println(upper + " (Length: " + title.length() + ")");
                        System.out.println(divider);
                    }
                }
            """.trimIndent(),
            expectedOutput = "=====================\nUNIVERSAL JAVA STUDIO (Length: 21)\n=====================",
            explanation = "Uses classic String methods (toUpperCase, length) alongside JDK 11's String.repeat(int)."
        ),
        PracticeExercise(
            id = "ex_6_10_oop_interfaces",
            categoryNumber = 6,
            categoryName = "6–10. OOP, Constructors, Inheritance, Polymorphism & Interfaces",
            title = "Modular Method Dispatch & Geometry Calculator",
            recommendedJdk = "8",
            compatibleJdkVersions = "JDK 1.0 – JDK 25",
            problemStatement = "Implement static helper methods demonstrating modular abstraction for circle and square area computation.",
            algorithmSteps = listOf(
                "Define squareArea(side) and rectangleVolume(l, w, h).",
                "Invoke methods from main() and display computed metrics."
            ),
            starterJavaCode = """
                public class GeometryOopLab {
                    public static void main(String[] args) {
                        int sq = squareArea(9);
                        int vol = boxVolume(4, 5, 6);
                        System.out.println("Square Area (side 9): " + sq);
                        System.out.println("Box Volume (4x5x6): " + vol);
                    }

                    public static int squareArea(int s) {
                        return s * s;
                    }

                    public static int boxVolume(int l, int w, int h) {
                        return l * w * h;
                    }
                }
            """.trimIndent(),
            expectedOutput = "Square Area (side 9): 81\nBox Volume (4x5x6): 120",
            explanation = "Demonstrates parameterized method invocation and return value binding."
        ),
        PracticeExercise(
            id = "ex_11_13_exceptions_threads",
            categoryNumber = 12,
            categoryName = "11–13. Packages, Exception Handling & Multithreading",
            title = "Resilient Bank Division & Assertion Guard",
            recommendedJdk = "1.4",
            compatibleJdkVersions = "JDK 1.4 – JDK 25",
            problemStatement = "Verify an account balance with a JDK 1.4 'assert' statement and handle a zero-installment division exception gracefully.",
            algorithmSteps = listOf(
                "Assert that balance > 0.",
                "Attempt division inside try-catch.",
                "Log recovery in finally."
            ),
            starterJavaCode = """
                public class ResilientLedger {
                    public static void main(String[] args) {
                        int balance = 1200;
                        assert balance > 0 : "Balance must be positive";
                        try {
                            int installments = 0;
                            int perMonth = balance / installments;
                            System.out.println("Monthly: " + perMonth);
                        } catch (ArithmeticException e) {
                            System.out.println("Recovered from error: " + e.getMessage());
                        } finally {
                            System.out.println("Ledger balance preserved: " + balance);
                        }
                    }
                }
            """.trimIndent(),
            expectedOutput = "Recovered from error: / by zero\nLedger balance preserved: 1200",
            explanation = "Combines JDK 1.4 assertions with structured try-catch-finally recovery."
        ),
        PracticeExercise(
            id = "ex_14_15_collections_generics",
            categoryNumber = 14,
            categoryName = "14–15. Collections & Generics",
            title = "Java 2 Vector vs Java 5+ Generic ArrayList",
            recommendedJdk = "8",
            compatibleJdkVersions = "JDK 5 – JDK 25",
            problemStatement = "Populate a generic ArrayList<String> of JDK milestones and iterate through all elements.",
            algorithmSteps = listOf(
                "Instantiate ArrayList<String>.",
                "Add milestone strings.",
                "Iterate using enhanced for-loop and print size."
            ),
            starterJavaCode = """
                import java.util.ArrayList;

                public class GenericCollectionLab {
                    public static void main(String[] args) {
                        ArrayList<String> milestones = new ArrayList<String>();
                        milestones.add("JDK 1.2 Collections");
                        milestones.add("JDK 5 Generics");
                        milestones.add("JDK 8 Streams");
                        milestones.add("JDK 21 SequencedCollection");
                        System.out.println("Total Milestones: " + milestones.size());
                        for (String m : milestones) {
                            System.out.println(" * " + m);
                        }
                    }
                }
            """.trimIndent(),
            expectedOutput = "Total Milestones: 4\n * JDK 1.2 Collections\n * JDK 5 Generics\n * JDK 8 Streams\n * JDK 21 SequencedCollection",
            explanation = "Try switching the project JDK to 1.2 on this code to see the compiler reject '<String>' generics and enhanced for-loops!"
        ),
        PracticeExercise(
            id = "ex_16_22_gui_io_jdbc",
            categoryNumber = 18,
            categoryName = "16–22. AWT, Applets, Swing, JavaFX, File I/O, Networking & JDBC",
            title = "Interactive Console Scanner & Telemetry Logger",
            recommendedJdk = "11",
            compatibleJdkVersions = "JDK 5 – JDK 25",
            problemStatement = "Read two integers from Standard Input using java.util.Scanner, compute their sum and product, and format a report.",
            algorithmSteps = listOf(
                "Initialize Scanner(System.in).",
                "Read two integers a and b (defaults provided if stdin is empty).",
                "Output sum and product."
            ),
            starterJavaCode = """
                import java.util.Scanner;

                public class InteractiveScannerLab {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        int a = sc.nextInt();
                        int b = sc.nextInt();
                        int sum = a + b;
                        int prod = a * b;
                        System.out.println("Input A = " + a + ", Input B = " + b);
                        System.out.println("Sum = " + sum + " | Product = " + prod);
                    }
                }
            """.trimIndent(),
            expectedOutput = "Input A = 42, Input B = 42\nSum = 84 | Product = 1764",
            explanation = "Reads tokens directly from the Standard Input (stdin) field in the bottom console!"
        ),
        PracticeExercise(
            id = "ex_23_25_modern_java",
            categoryNumber = 25,
            categoryName = "23–25. Lambdas, Streams & Modern Java (JDK 17–25)",
            title = "Records, Switch Expressions & JDK 25 Module Imports",
            recommendedJdk = "25",
            compatibleJdkVersions = "JDK 25",
            problemStatement = "Use 'import module java.base;', a Java 16+ 'record', 'var', and a Switch Expression to classify a compiler bytecode version.",
            algorithmSteps = listOf(
                "Import module java.base.",
                "Define record ReleaseTarget(String jdk, int bytecode).",
                "Evaluate classification via switch expression."
            ),
            starterJavaCode = """
                import module java.base;

                public class ModernJava25Lab {
                    public record ReleaseTarget(String jdk, int bytecode) {}

                    public static void main(String[] args) {
                        var target = new ReleaseTarget("JDK 25 LTS", 69);
                        String era = switch (target.bytecode()) {
                            case 46 -> "Java 2 Historical Era";
                            case 52 -> "Java 8 Functional Era";
                            case 69 -> "Java 25 Modern LTS Era";
                            default -> "Standard Era";
                        };
                        System.out.println("Target: " + target.jdk() + " (v" + target.bytecode() + ".0)");
                        System.out.println("Classification: " + era);
                    }
                }
            """.trimIndent(),
            expectedOutput = "Target: JDK 25 LTS (v69.0)\nClassification: Java 25 Modern LTS Era",
            explanation = "Showcases JDK 25 module imports alongside records, local variable type inference, and switch expressions."
        )
    )

    val quizQuestions: List<QuizQuestionItem> = listOf(
        QuizQuestionItem(
            id = "q_1",
            chapterOrVersion = "JDK 1.2 (Java 2)",
            questionType = "MCQ",
            targetJdk = "1.2",
            prompt = "Which keyword and major API framework were introduced in J2SE 1.2 (Java 2) in 1998?",
            options = listOf(
                "'strictfp' keyword and the Java Collections Framework (List, ArrayList, Map, HashMap)",
                "'assert' keyword and java.nio buffers",
                "'enum' keyword and java.util.Scanner",
                "'var' keyword and java.util.stream.Stream"
            ),
            correctIndex = 0,
            explanation = "J2SE 1.2 (Codename Playground) introduced the 'strictfp' keyword, the Collections Framework, and core javax.swing. 'assert' came in 1.4, 'enum' in 5, and 'var' in 10."
        ),
        QuizQuestionItem(
            id = "q_2",
            chapterOrVersion = "Compiler Compatibility",
            questionType = "MCQ",
            targetJdk = "9+",
            prompt = "Why is compiling with 'javac --release 8' on a modern JDK safer than using only 'javac -source 8 -target 8'?",
            options = listOf(
                "--release 8 also restricts compilation against the historical Java 8 API symbol table (preventing accidental use of JDK 11+ APIs like String.isBlank())",
                "--release 8 disables bytecode verification at runtime",
                "-source 8 emits bytecode version 69.0",
                "There is no difference; both allow calling JDK 21 Virtual Threads"
            ),
            correctIndex = 0,
            explanation = "Using -source 8 -target 8 without -bootclasspath still compiles against the host JDK's modern standard library, whereas --release 8 enforces language, bytecode (52.0), AND historical API availability."
        ),
        QuizQuestionItem(
            id = "q_3",
            chapterOrVersion = "Find the Error (Version Compatibility)",
            questionType = "FIND_ERROR",
            targetJdk = "1.4",
            prompt = "A student selects 'JDK 1.4' for their project and compiles the following code. Why does compilation fail?",
            codeSnippet = "import java.util.ArrayList;\npublic class Test {\n    public static void main(String[] args) {\n        ArrayList<String> list = new ArrayList<String>();\n    }\n}",
            options = listOf(
                "Generics ('<String>') were introduced in JDK 5 (J2SE 5.0) and are illegal syntax in JDK 1.4",
                "ArrayList was not introduced until JDK 8",
                "String[] args is invalid in JDK 1.4",
                "import statements are not allowed in JDK 1.4"
            ),
            correctIndex = 0,
            explanation = "While ArrayList was introduced in JDK 1.2, parameterized Generic types (<String>) were introduced in JDK 5. In JDK 1.2–1.4, ArrayList was a raw collection of Object references."
        ),
        QuizQuestionItem(
            id = "q_4",
            chapterOrVersion = "Output Prediction",
            questionType = "OUTPUT_PREDICTION",
            targetJdk = "16+",
            prompt = "What is the output of the following Java 16+ program using Records and Switch Expressions?",
            codeSnippet = "public class Quiz {\n    record Point(int x, int y) {}\n    public static void main(String[] args) {\n        var p = new Point(3, 4);\n        int distSq = p.x() * p.x() + p.y() * p.y();\n        System.out.println(distSq);\n    }\n}",
            options = listOf(
                "25",
                "7",
                "12",
                "Compilation Error: p.getX() must be used instead of p.x()"
            ),
            correctIndex = 0,
            explanation = "Java Records generate component accessor methods matching the exact component names: p.x() returns 3 and p.y() returns 4, so 3*3 + 4*4 = 25."
        ),
        QuizQuestionItem(
            id = "q_5",
            chapterOrVersion = "Fill in the Blank (JDK 17)",
            questionType = "FILL_BLANK",
            targetJdk = "17",
            prompt = "Complete the JDK 17 declaration to restrict Shape so only Circle and Square may extend it: 'public ______ class Shape ______ Circle, Square {}'",
            options = listOf(
                "sealed ... permits",
                "final ... extends",
                "record ... implements",
                "strictfp ... exports"
            ),
            correctIndex = 0,
            explanation = "JEP 409 in JDK 17 standardized 'sealed' classes and interfaces using the 'permits' clause to explicitly list allowed subclasses."
        ),
        QuizQuestionItem(
            id = "q_6",
            chapterOrVersion = "Deprecations & Removals (JDK 1.2–25)",
            questionType = "MCQ",
            targetJdk = "24",
            prompt = "Which statement accurately describes deprecated or disabled features across Java's evolution?",
            options = listOf(
                "Thread.stop/suspend were deprecated in JDK 1.2; Applet was deprecated in JDK 9; SecurityManager was permanently disabled in JDK 24",
                "Vector and ArrayList were removed in JDK 11",
                "strictfp is mandatory on all classes in JDK 25",
                "Scanner was deprecated in JDK 8"
            ),
            correctIndex = 0,
            explanation = "Thread.stop/suspend were deprecated in Java 2 (1.2); Applet was deprecated in JDK 9 / marked for removal in 17; SecurityManager was deprecated in 17 and permanently disabled in JDK 24 (JEP 486)."
        ),
        QuizQuestionItem(
            id = "q_7",
            chapterOrVersion = "JDK 21–25 Modern Features",
            questionType = "MCQ",
            targetJdk = "25",
            prompt = "Which feature combination is standardized in JDK 21 through JDK 25?",
            options = listOf(
                "Virtual Threads (21), Unnamed Variables '_' & FFM API (22), Markdown Javadoc '///' (23), Stream Gatherers (24), and Scoped Values & 'import module' (25)",
                "Applets, CORBA, and Nashorn JavaScript Engine",
                "PermGen memory space and Concurrent Mark Sweep (CMS) GC",
                "32-bit x86 Windows kernel threads"
            ),
            correctIndex = 0,
            explanation = "JDK 21–25 delivered Project Loom (Virtual Threads & Scoped Values), Project Panama (FFM API), Amber (Pattern Matching, Unnamed Variables, Module Imports), and Stream Gatherers."
        )
    )
}
