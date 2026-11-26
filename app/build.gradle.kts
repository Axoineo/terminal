import java.security.MessageDigest
import org.gradle.api.artifacts.component.ModuleComponentIdentifier

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.terminalworkspace"
    compileSdk {
        version = release(37) { minorApiLevel = 1 }
    }

    defaultConfig {
        applicationId = "dev.terminalworkspace"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0-dev"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.connectbot.termlib)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}

/** Reports resolved artifacts themselves, including AARs/JARs, without local cache paths. */
abstract class ExportRuntimeArtifacts : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val artifactFiles: ConfigurableFileCollection

    @get:Input
    abstract val componentsByFile: MapProperty<String, String>

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @TaskAction
    fun export() {
        val components = componentsByFile.get()
        val rows = artifactFiles.files.map { file ->
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            val coordinate = components.getValue(file.absolutePath)
            listOf(coordinate, file.name, digest.digest().joinToString("") {
                "%02x".format(it.toInt() and 0xff)
            }).joinToString("\t") { it.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ') }
        }.sorted()
        val destination = reportFile.get().asFile
        destination.parentFile.mkdirs()
        destination.writeText("coordinate\tfile\tsha256\n" + rows.joinToString("\n", postfix = "\n"))
    }
}

configurations.matching { name == "debugRuntimeClasspath" }.all {
    val resolved = incoming.artifacts.resolvedArtifacts
    tasks.register<ExportRuntimeArtifacts>("exportRuntimeArtifacts") {
        group = "verification"
        description = "Export resolved debug runtime artifact coordinates, names and SHA-256 hashes."
        artifactFiles.from(resolved.map { results -> results.map { it.file } })
        componentsByFile.set(resolved.map { results ->
            results.associate { artifact ->
                val component = artifact.id.componentIdentifier
                artifact.file.absolutePath to when (component) {
                    is ModuleComponentIdentifier -> "${component.group}:${component.module}:${component.version}"
                    else -> component.displayName
                }
            }
        })
        reportFile.set(layout.buildDirectory.file("reports/runtime-artifacts.tsv"))
    }
}
