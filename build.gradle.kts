plugins {
    kotlin("jvm") version "2.2.0"
    id("org.jetbrains.intellij.platform") version "2.6.0"
}

group = "com.mirkamalg"
version = "0.0.3"

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    testImplementation(kotlin("test"))

    intellijPlatform {
//        intellijIdeaCommunity("2025.1")
        androidStudio("2025.1.1.13")
    }
}

// Force Java 17 for compilation
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

//intellij {
//    version.set("2024.1.4")  // Updated to newer version
//    type.set("IC") // IC = IntelliJ Community, AI = Android Studio
//
//    // Remove Kotlin plugin to avoid K2 conflicts
//    plugins.set(listOf("java"))
//}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
//    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
//        kotlinOptions.jvmTarget = "17"
//    }

    // Skip buildSearchableOptions to avoid IDE conflicts
    buildSearchableOptions {
        enabled = false
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

tasks.test {
    useJUnitPlatform()
}