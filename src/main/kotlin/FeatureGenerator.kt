package com.mirkamalg

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.vfs.VirtualFile
import java.io.IOException

class FeatureGenerator(
    private val projectRoot: VirtualFile
) {

    fun generateFeature(featureName: String, tribe: String) {
        WriteAction.run<IOException> {
            val normalizedFeatureName = featureName.lowercase()
                .replace('_', '-')
                .replace(' ', '-')
            val featureCamel = toCamel(normalizedFeatureName)
            val basePackage = "iba.mobilbank.${normalizedFeatureName.replace("-", "")}"

            // Create tribe directory if it doesn't exist
            val featureDir = findOrCreateDirectory(projectRoot, "Feature")
            val tribeDir = findOrCreateDirectory(featureDir, tribe)

            // Check if modules already exist
            val apiModuleName = "feature-$normalizedFeatureName-api"
            val implModuleName = "feature-$normalizedFeatureName-impl"

            if (tribeDir.findChild(apiModuleName) != null) {
                throw IOException("Error: API module already exists.")
            }
            if (tribeDir.findChild(implModuleName) != null) {
                throw IOException("Error: Impl module already exists.")
            }

            // Create API and Impl modules
            val apiModule = createDirectory(tribeDir, apiModuleName)
            val implModule = createDirectory(tribeDir, implModuleName)

            // Generate API module
            generateApiModule(apiModule, basePackage, featureCamel)

            // Generate Impl module
            generateImplModule(implModule, basePackage, featureCamel, normalizedFeatureName)

            // Update settings.gradle.kts
            updateSettingsGradle(normalizedFeatureName, tribe)

            // Update app module dependencies
            updateAppModuleDependencies(normalizedFeatureName)
        }
    }

    private fun generateApiModule(apiModule: VirtualFile, basePackage: String, featureCamel: String) {
        // Create source directory structure
        val srcDir = createDirectory(apiModule, "src")
        val mainDir = createDirectory(srcDir, "main")
        val javaDir = createDirectory(mainDir, "java")

        // Create package directories
        val packageDir = createPackageDirectories(javaDir, "$basePackage.api")

        // Create build.gradle.kts
        createFile(apiModule, "build.gradle.kts", getApiBuildGradle(basePackage))

        // Create proguard files
        createFile(apiModule, "proguard-rules.pro", getProguardRules())
        createFile(apiModule, "consumer-rules.pro", "")

        // Create API interface files
        createFile(packageDir, "${featureCamel}Api.kt", getApiInterface(basePackage, featureCamel))
        createFile(packageDir, "${featureCamel}FeatureLauncher.kt", getFeatureLauncher(basePackage, featureCamel))
    }

    private fun generateImplModule(
        implModule: VirtualFile,
        basePackage: String,
        featureCamel: String,
        featureName: String
    ) {
        // Create source directory structure
        val srcDir = createDirectory(implModule, "src")
        val mainDir = createDirectory(srcDir, "main")
        val javaDir = createDirectory(mainDir, "java")

        // Create package directories
        val packageDir = createPackageDirectories(javaDir, "$basePackage.impl")

        // Create build.gradle.kts
        createFile(implModule, "build.gradle.kts", getImplBuildGradle(basePackage, featureName))

        // Create proguard files
        createFile(implModule, "proguard-rules.pro", getImplProguardRules(basePackage))
        createFile(implModule, "consumer-rules.pro", getImplConsumerRules(basePackage))

        // Create all the implementation directories and files
        createImplStructure(packageDir, basePackage, featureCamel)
    }

    private fun createImplStructure(packageDir: VirtualFile, basePackage: String, featureCamel: String) {
        // Create directory structure
        val diDir = createDirectory(packageDir, "di")
        val diModulesDir = createDirectory(diDir, "modules")
        val launcherDir = createDirectory(packageDir, "launcher")
        val dataDir = createDirectory(packageDir, "data")
        val dataRepoDir = createDirectory(dataDir, "repository")
        val dataMapperDir = createDirectory(dataDir, "mapper")
        val dataSourceDir = createDirectory(dataDir, "source")
        val dataSourceRemoteDir = createDirectory(dataSourceDir, "remote")
        val domainDir = createDirectory(packageDir, "domain")
        val domainRepoDir = createDirectory(domainDir, "repository")
        val domainModelDir = createDirectory(domainDir, "model")
        val domainUseCaseDir = createDirectory(domainDir, "usecase")
        val presentationDir = createDirectory(packageDir, "presentation")
        val presentationMapperDir = createDirectory(presentationDir, "mapper")
        val presentationUiDir = createDirectory(presentationDir, "ui")
        val presentationViewModelDir = createDirectory(presentationUiDir, "viewmodel")

        // Create DI files
        createFile(diDir, "${featureCamel}Component.kt", getComponentClass(basePackage, featureCamel))
        createFile(diDir, "${featureCamel}Dependencies.kt", getDependenciesInterface(basePackage, featureCamel))
        createFile(diModulesDir, "${featureCamel}ProviderModule.kt", getProviderModule(basePackage, featureCamel))
        createFile(diModulesDir, "${featureCamel}BindModule.kt", getBindModule(basePackage, featureCamel))

        // Create launcher
        createFile(
            launcherDir,
            "${featureCamel}FeatureLauncherImpl.kt",
            getFeatureLauncherImpl(basePackage, featureCamel)
        )

        // Create data layer files
        createFile(dataRepoDir, "${featureCamel}RepositoryImpl.kt", getRepositoryImpl(basePackage, featureCamel))
        createFile(dataMapperDir, "${featureCamel}DataMapper.kt", getDataMapper(basePackage, featureCamel))
        createFile(
            dataSourceRemoteDir,
            "${featureCamel}RemoteDataSource.kt",
            getRemoteDataSource(basePackage, featureCamel)
        )

        // Create domain layer files
        createFile(domainRepoDir, "${featureCamel}Repository.kt", getRepository(basePackage, featureCamel))
        createFile(domainModelDir, "${featureCamel}Model.kt", getModel(basePackage, featureCamel))
        createFile(domainUseCaseDir, "Get${featureCamel}UseCase.kt", getUseCase(basePackage, featureCamel))

        // Create presentation layer files
        createFile(presentationMapperDir, "${featureCamel}Mapper.kt", getPresentationMapper(basePackage, featureCamel))
        createFile(presentationViewModelDir, "${featureCamel}ViewModel.kt", getViewModel(basePackage, featureCamel))
    }

    private fun updateSettingsGradle(featureName: String, tribe: String) {
        val settingsGradle = projectRoot.findChild("settings.gradle.kts")
        if (settingsGradle != null) {
            val includeLine = "includeApiImplModules(\"feature-$featureName\",\"Feature\",\"$tribe\")"
            val content = String(settingsGradle.contentsToByteArray())
            if (!content.contains(includeLine)) {
                val newContent = content + "\n$includeLine\n"
                settingsGradle.setBinaryContent(newContent.toByteArray())
            }
        }
    }

    private fun findOrCreateDirectory(parent: VirtualFile, name: String): VirtualFile {
        return parent.findChild(name) ?: parent.createChildDirectory(this, name)
    }

    private fun createDirectory(parent: VirtualFile, name: String): VirtualFile {
        return parent.createChildDirectory(this, name)
    }

    private fun createPackageDirectories(parent: VirtualFile, packageName: String): VirtualFile {
        val parts = packageName.split(".")
        var current = parent
        for (part in parts) {
            current = findOrCreateDirectory(current, part)
        }
        return current
    }

    private fun createFile(parent: VirtualFile, name: String, content: String) {
        val file = parent.createChildData(this, name)
        file.setBinaryContent(content.toByteArray())
    }

    private fun toCamel(s: String): String {
        return s.split("-", "_", " ").joinToString("") {
            it.replaceFirstChar { c -> c.uppercase() }
        }
    }

    // Template methods for file contents
    private fun getApiBuildGradle(basePackage: String) = """
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.parcelize)
}

android {
    namespace = "$basePackage.api"
    compileSdk = libs.versions.projectCompileSdkVersion.get().toInt()

    defaultConfig {
        minSdk = libs.versions.projectMinSdkVersion.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }
    flavorDimensions += "default"
    productFlavors {
        create("regression") {
            dimension = "default"
        }
        create("prod") {
            dimension = "default"
        }
    }
    buildTypes {
        release {
            // In AGP 8.4.2 minify mechanism changed. Now minification is performed separately for each module immediately after its assembly.
            // Because of this, R8 thinks that the code in the module is not used by anyone, and simply deletes it!
            // FIX: minification enabled only in app:module. in that case it will work as before
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = libs.versions.projectJvmTarget.get()
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core-module-injector"))
    implementation(project(":core-remote-config-api"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.gson)
}
""".trimIndent()

    private fun getProguardRules() = """
# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
""".trimIndent()

    private fun getApiInterface(basePackage: String, featureCamel: String) = """
package $basePackage.api

import iba.mobilbank.core.moduleinjector.BaseAPI

interface ${featureCamel}Api : BaseAPI {
    val featureLauncher: ${featureCamel}FeatureLauncher
}
""".trimIndent()

    private fun getFeatureLauncher(basePackage: String, featureCamel: String) = """
package $basePackage.api

import android.content.Context
import android.content.Intent

interface ${featureCamel}FeatureLauncher {
    fun launch(context: Context)
    fun createIntent(context: Context): Intent
}
""".trimIndent()

    private fun getImplBuildGradle(basePackage: String, featureName: String) = """
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kapt)
    alias(libs.plugins.parcelize)
    alias(libs.plugins.safeargs.kotlin)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "$basePackage.impl"
    compileSdk = libs.versions.projectCompileSdkVersion.get().toInt()

    defaultConfig {
        minSdk = libs.versions.projectMinSdkVersion.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }
    flavorDimensions += "default"
    productFlavors {
        create("regression") {
            dimension = "default"
        }
        create("prod") {
            dimension = "default"
        }
    }
    buildTypes {
        release {
            // In AGP 8.4.2 minify mechanism changed. Now minification is performed separately for each module immediately after its assembly.
            // Because of this, R8 thinks that the code in the module is not used by anyone, and simply deletes it!
            // FIX: minification enabled only in app:module. in that case it will work as before
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = libs.versions.projectJvmTarget.get()
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }
}

dependencies {
    implementation(project(":feature-$featureName-api"))

    implementation(project(":ibam-design-system:xml-lib"))
    implementation(project(":ibam-design-system:compose-lib"))
    implementation(project(":ibam-design-system:icon-kit"))
    //TODO will be deleted after complete design-system development. Currently this dependency only used for AbbSnackBar
    implementation(project(":framework:ui-toolkit"))

    implementation(project(":core-module-injector"))
    implementation(project(":core-remote-config-api"))
    implementation(project(":core-network-api"))
    implementation(project(":core-wrappers-api"))
    implementation(project(":core-deeplink-api"))
    implementation(project(":core-ui"))
    implementation(project(":core-localization"))

    implementation(libs.timber)
    kapt(libs.dagger.compiler)
    implementation(libs.dagger)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.foundation)
}
""".trimIndent()

    private fun getImplProguardRules(basePackage: String) = """
# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn iba.mobilbank.common.R${'$'}color

# Keep all classes in a specific package
-keep class $basePackage.impl.data.model.** { *; }

# Keep any enums used in your API
-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class java.lang.ClassValue {
    *;
}
""".trimIndent()

    private fun getImplConsumerRules(basePackage: String) = """
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn iba.mobilbank.common.R${'$'}color

# Keep all classes in a specific package
-keep class $basePackage.impl.data.model.** { *; }

# Keep any enums used in your API
-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class java.lang.ClassValue {
    *;
}
""".trimIndent()

    private fun getComponentClass(basePackage: String, featureCamel: String) = """
package $basePackage.impl.di

import dagger.Component
import iba.mobilbank.core.moduleinjector.FeatureScope
import $basePackage.api.${featureCamel}Api
import $basePackage.impl.di.modules.${featureCamel}ProviderModule
import $basePackage.impl.di.modules.${featureCamel}BindModule
import $basePackage.impl.di.${featureCamel}Dependencies

@FeatureScope
@Component(
    modules = [
        ${featureCamel}ProviderModule::class,
        ${featureCamel}BindModule::class
    ],
    dependencies = [${featureCamel}Dependencies::class]
)
interface ${featureCamel}Component : ${featureCamel}Api {
    companion object {
        fun initAndGet(dependencies: ${featureCamel}Dependencies): ${featureCamel}Component {
            return Dagger${featureCamel}Component.builder()
                .${featureCamel.replaceFirstChar { it.lowercase() }}Dependencies(dependencies)
                .build()
        }
    }
}
""".trimIndent()

    private fun getDependenciesInterface(basePackage: String, featureCamel: String) = """
package $basePackage.impl.di

import iba.mobilbank.core.moduleinjector.BaseDependencies

interface ${featureCamel}Dependencies : BaseDependencies {
    // TODO: Define dependencies required from the host app or other modules
}
""".trimIndent()

    private fun getProviderModule(basePackage: String, featureCamel: String) = """
package $basePackage.impl.di.modules

import dagger.Module
import dagger.Provides
import iba.mobilbank.core.moduleinjector.FeatureScope
import $basePackage.api.${featureCamel}FeatureLauncher
import $basePackage.impl.launcher.${featureCamel}FeatureLauncherImpl
import $basePackage.impl.data.source.remote.${featureCamel}RemoteDataSource
import retrofit2.Retrofit

@Module
class ${featureCamel}ProviderModule {
    @Provides
    @FeatureScope
    fun provideFeatureLauncher(): ${featureCamel}FeatureLauncher = ${featureCamel}FeatureLauncherImpl()

    @Provides
    @FeatureScope
    fun provideRemoteDataSource(retrofit: Retrofit): ${featureCamel}RemoteDataSource {
        return retrofit.create(${featureCamel}RemoteDataSource::class.java)
    }
}
""".trimIndent()

    private fun getBindModule(basePackage: String, featureCamel: String) = """
package $basePackage.impl.di.modules

import dagger.Binds
import dagger.Module
import iba.mobilbank.core.moduleinjector.FeatureScope
import $basePackage.impl.data.repository.${featureCamel}RepositoryImpl
import $basePackage.impl.domain.repository.${featureCamel}Repository

@Module
abstract class ${featureCamel}BindModule {
    @Binds
    @FeatureScope
    abstract fun bindRepository(impl: ${featureCamel}RepositoryImpl): ${featureCamel}Repository
}
""".trimIndent()

    private fun getFeatureLauncherImpl(basePackage: String, featureCamel: String) = """
package $basePackage.impl.launcher

import android.content.Context
import android.content.Intent
import $basePackage.api.${featureCamel}FeatureLauncher

class ${featureCamel}FeatureLauncherImpl : ${featureCamel}FeatureLauncher {
    override fun launch(context: Context) {
        context.startActivity(createIntent(context))
    }

    override fun createIntent(context: Context): Intent {
        // TODO: Replace with actual Activity
        return Intent()
    }
}
""".trimIndent()

    private fun getRepositoryImpl(basePackage: String, featureCamel: String) = """
package $basePackage.impl.data.repository

import $basePackage.impl.data.source.remote.${featureCamel}RemoteDataSource
import $basePackage.impl.data.mapper.${featureCamel}DataMapper
import $basePackage.impl.domain.repository.${featureCamel}Repository
import javax.inject.Inject

class ${featureCamel}RepositoryImpl @Inject constructor(
    private val remoteDataSource: ${featureCamel}RemoteDataSource,
    private val mapper: ${featureCamel}DataMapper
) : ${featureCamel}Repository {
    // TODO: Implement repository methods
}
""".trimIndent()

    private fun getDataMapper(basePackage: String, featureCamel: String) = """
package $basePackage.impl.data.mapper

import javax.inject.Inject

class ${featureCamel}DataMapper @Inject constructor() {
    // TODO: Implement mapping logic
}
""".trimIndent()

    private fun getRemoteDataSource(basePackage: String, featureCamel: String) = """
package $basePackage.impl.data.source.remote

interface ${featureCamel}RemoteDataSource {
    // TODO: Define remote data source methods
}
""".trimIndent()

    private fun getRepository(basePackage: String, featureCamel: String) = """
package $basePackage.impl.domain.repository

interface ${featureCamel}Repository {
    // TODO: Define repository interface
}
""".trimIndent()

    private fun getModel(basePackage: String, featureCamel: String) = """
package $basePackage.impl.domain.model

data class ${featureCamel}Model(
    val id: String // TODO: Add fields
)
""".trimIndent()

    private fun getUseCase(basePackage: String, featureCamel: String) = """
package $basePackage.impl.domain.usecase

import $basePackage.impl.domain.repository.${featureCamel}Repository
import javax.inject.Inject

class Get${featureCamel}UseCase @Inject constructor(
    private val repository: ${featureCamel}Repository
) {
    // TODO: Implement use case logic
}
""".trimIndent()

    private fun getPresentationMapper(basePackage: String, featureCamel: String) = """
package $basePackage.impl.presentation.mapper

import javax.inject.Inject

class ${featureCamel}Mapper @Inject constructor() {
    // TODO: Implement presentation mapping
}
""".trimIndent()

    private fun getViewModel(basePackage: String, featureCamel: String) = """
package $basePackage.impl.presentation.ui.viewmodel

import androidx.lifecycle.ViewModel
import javax.inject.Inject

class ${featureCamel}ViewModel @Inject constructor() : ViewModel() {
    // TODO: Implement ViewModel logic
}
""".trimIndent()

    private fun updateAppModuleDependencies(featureName: String) {
        val appBuildFile = projectRoot.findFileByRelativePath("app/build.gradle.kts")
        if (appBuildFile == null || !appBuildFile.exists()) {
            println("Warning: Could not find app/build.gradle.kts file")
            return
        }

        val content = String(appBuildFile.contentsToByteArray())

        // Check if dependencies already exist
        if (content.contains("feature-$featureName-api") || content.contains("feature-$featureName-impl")) {
            println("Feature dependencies already exist in app module")
            return
        }

        // Add dependencies to the end of dependencies block
        val apiDependency = "    implementation(project(\":feature-$featureName-api\"))"
        val implDependency = "    implementation(project(\":feature-$featureName-impl\"))"
        val newDependencies = "\n$apiDependency\n$implDependency"

        val dependenciesPattern = Regex("""(dependencies\s*\{[^}]*)\}""", RegexOption.DOT_MATCHES_ALL)
        val newContent = dependenciesPattern.replace(content) { matchResult ->
            "${matchResult.groupValues[1]}$newDependencies\n}"
        }

        appBuildFile.setBinaryContent(newContent.toByteArray())
        println("Added feature dependencies to app module: feature-$featureName")
    }
}