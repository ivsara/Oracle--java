package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.core.JavaCompilerAndExecutionEngine
import com.example.core.JdkCatalogAndSpecifications
import com.example.data.local.JdkInstallationEntity
import com.example.ui.theme.IdeEmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily

private class UniversalJavaWebBridge(
    private val onSyncJdkAndCode: (String, String) -> Unit
) {
    @JavascriptInterface
    fun onSwitchJdk(jdkVersion: String) {
        onSyncJdkAndCode(jdkVersion, "")
    }

    @JavascriptInterface
    fun compileFromWeb(jdkVersion: String, code: String): String {
        onSyncJdkAndCode(jdkVersion, code)
        val spec = JdkCatalogAndSpecifications.getSpec(jdkVersion)
        val jdk = JdkInstallationEntity(
            jdkVersion = spec.versionId,
            displayName = spec.displayName,
            vendor = spec.vendorDefault,
            javaHome = "/opt/universal-java-studio/toolchains/jdk-${spec.versionId}",
            compilerPath = "/opt/universal-java-studio/toolchains/jdk-${spec.versionId}/bin/javac",
            runtimePath = "/opt/universal-java-studio/toolchains/jdk-${spec.versionId}/bin/java",
            compilerVersion = "javac ${spec.versionId}.0",
            runtimeVersion = "java ${spec.versionId}",
            bytecodeMajorVersion = spec.bytecodeMajor,
            architecture = "64-bit",
            status = "Available",
            compatibilityLevel = "Supported"
        )
        val className = Regex("""\bclass\s+([A-Za-z_][A-Za-z0-9_]*)""").find(code)?.groupValues?.get(1) ?: "HelloWorld"
        val res = JavaCompilerAndExecutionEngine.compileProject(jdk, mapOf("$className.java" to code))
        return res.rawOutput
    }

    @JavascriptInterface
    fun runFromWeb(jdkVersion: String, code: String): String {
        onSyncJdkAndCode(jdkVersion, code)
        val spec = JdkCatalogAndSpecifications.getSpec(jdkVersion)
        val jdk = JdkInstallationEntity(
            jdkVersion = spec.versionId,
            displayName = spec.displayName,
            vendor = spec.vendorDefault,
            javaHome = "/opt/universal-java-studio/toolchains/jdk-${spec.versionId}",
            compilerPath = "/opt/universal-java-studio/toolchains/jdk-${spec.versionId}/bin/javac",
            runtimePath = "/opt/universal-java-studio/toolchains/jdk-${spec.versionId}/bin/java",
            compilerVersion = "javac ${spec.versionId}.0",
            runtimeVersion = "java ${spec.versionId}",
            bytecodeMajorVersion = spec.bytecodeMajor,
            architecture = "64-bit",
            status = "Available",
            compatibilityLevel = "Supported"
        )
        val className = Regex("""\bclass\s+([A-Za-z_][A-Za-z0-9_]*)""").find(code)?.groupValues?.get(1) ?: "HelloWorld"
        val exec = JavaCompilerAndExecutionEngine.executeProgram(
            jdk = jdk,
            files = mapOf("$className.java" to code),
            mainClass = className
        )
        return buildString {
            appendLine("$ ${exec.runtimeCommand}")
            if (exec.stdout.isNotBlank()) appendLine(exec.stdout)
            if (exec.stderr.isNotBlank()) appendLine(exec.stderr)
            appendLine("[Exit Code: ${exec.exitCode} • Duration: ${exec.durationMs}ms • JDK ${exec.jdkVersion}]")
        }.trimEnd()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebAppPortalScreen(
    activeJdkVersion: String,
    onSyncFromWeb: (String, String) -> Unit,
    onSwitchToNativeIde: () -> Unit
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("web_app_portal_screen")
    ) {
        // Browser Chrome Address Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Language,
                    contentDescription = "Web App Mode",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Offline Secure Bundle",
                            tint = IdeEmeraldSuccess,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "app://universal-java-studio.local/web/index.html?jdk=$activeJdkVersion",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                IconButton(
                    onClick = { webViewRef?.reload() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reload Web App", modifier = Modifier.size(16.dp))
                }
                OutlinedButton(
                    onClick = onSwitchToNativeIde,
                    modifier = Modifier.testTag("switch_to_compose_ide_btn")
                ) {
                    Text("Native View", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Embedded Offline HTML5 Web Application
        AndroidView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()
                    addJavascriptInterface(
                        UniversalJavaWebBridge { jdk, code ->
                            onSyncFromWeb(jdk, code)
                        },
                        "AndroidStudioBridge"
                    )
                    loadUrl("file:///android_asset/web/index.html")
                    webViewRef = this
                }
            }
        )
    }
}
