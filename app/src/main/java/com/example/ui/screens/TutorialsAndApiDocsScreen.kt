package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.ApiClassDoc
import com.example.core.JdkCatalogAndSpecifications
import com.example.core.KnowledgeBaseCatalog
import com.example.core.TutorialLesson
import com.example.data.local.LearningProgressEntity
import com.example.ui.theme.IdeEmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SyntaxDeprecated

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
                        text = "Structured curriculum organized from IIT Kharagpur Programming in Java, Oracle Java SE 7 API, and Oracle Java Tutorials. Every lesson preserves both genuine Java 2 (JDK 1.2) and Modern Java examples.",
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

        // Lesson 12-Part Structured Content Card
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

                    // 12. Compatible JDK Versions & Minimum Version Badge
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

                    // 6. Side-by-Side or Dual Java 2 (JDK 1.2) vs Modern Java Example Programs
                    Text(
                        text = "6. Example Programs (Historical Java 2 vs Modern Java)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Java 2 Example
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Historical Java 2 (JDK 1.2)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = selectedLesson.java2ExampleCode,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                OutlinedButton(
                                    onClick = {
                                        onOpenSnippetInEditor(
                                            "${selectedLesson.lessonTitle} (JDK 1.2)",
                                            "1.2",
                                            selectedLesson.java2ExampleCode
                                        )
                                    },
                                    modifier = Modifier.testTag("open_java2_example_btn")
                                ) {
                                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Load in JDK 1.2 Editor")
                                }
                            }
                        }

                        // Modern Java Example
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Modern Java (JDK 17–25)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = selectedLesson.modernJavaExampleCode,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                OutlinedButton(
                                    onClick = {
                                        onOpenSnippetInEditor(
                                            "${selectedLesson.lessonTitle} (Modern)",
                                            "25",
                                            selectedLesson.modernJavaExampleCode
                                        )
                                    },
                                    modifier = Modifier.testTag("open_modern_example_btn")
                                ) {
                                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Load in JDK 25 Editor")
                                }
                            }
                        }
                    }

                    TutorialSectionBlock("7. Program Explanation", selectedLesson.programExplanation)

                    Text("8. Sample Output", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
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
                                text = "Offline Java API Reference Browser",
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

        // Selected Class Detailed Javadoc Reference
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
