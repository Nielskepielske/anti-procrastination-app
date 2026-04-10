import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    kotlin("plugin.serialization") version "2.0.20"
    id("com.google.gms.google-services") version "4.4.4" apply false
}

repositories {
    google()
    mavenCentral()
}


kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm()

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)

            // Text recognition
            implementation(libs.text.recognition)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            // Room
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)

            //navigation
            // 1. The official JetBrains Compose Navigation port
            implementation(libs.navigation.compose)
            // 2. Kotlinx Serialization (for type-safe routing)
            implementation(libs.kotlinx.serialization.json)

            // Icons
            implementation(libs.material.icons.extended)

            // Constrainlayout
            implementation(libs.constraintlayout.compose.multiplatform)

            // Notifier
            api("io.github.mirzemehdi:kmpnotifier:1.6.1") // in iOS export this library
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            // desktopMain sourceSet
            implementation(libs.oshi.core)

            // Text recognition
            implementation(libs.net.tess4j)
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.jetbrains.kotlinx.coroutines.test)
        }
    }
}
room {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.example.procrastination_detection"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.example.procrastination_detection"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
    // commonMain sourceSet
    implementation(libs.lifecycle.viewmodel.compose.v280)

    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    //add("kspIosX64", libs.androidx.room.compiler)

    // ADD THIS LINE FOR DESKTOP/JVM:
    add("kspJvm", libs.androidx.room.compiler)
}

val myAppPackageName = "com.example.procrastination_detection"
compose.desktop {
    application {
        mainClass = "$myAppPackageName.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = myAppPackageName
            packageVersion = "1.0.0"
        }
    }
}

tasks.withType<JavaExec> {
    systemProperty("MY_APP_PROCESS_NAME", myAppPackageName)
    // Stops Java from taking a screenshot of the desktop
    environment("_JAVA_AWT_WM_NONREPARENTING", "1")

    // Encourages Compose/Skiko to bypass XWayland entirely if possible
    environment("GDK_BACKEND", "wayland")

//    // Fixes font anti-aliasing on transparent surfaces
//    systemProperty("awt.useSystemAAFontSettings", "on")
//    systemProperty("swing.aatext", "true")
}
