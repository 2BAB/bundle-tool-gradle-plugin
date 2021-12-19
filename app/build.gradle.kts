import me.xx2bab.bundletool.*
import me.xx2bab.bundletool.BundleToolExtension

plugins {
    id("com.android.application")
    kotlin("android")
//    id("poc")
    id("me.2bab.bundletool")
}

android {
    compileSdk = 31

    defaultConfig {
        applicationId = "me.xx2bab.sample.bundle"
        minSdk = 28
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("androidDebug") {
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storePassword = "android"
            storeFile = file("debug.keystore")
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("androidDebug")
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions += "server"
    productFlavors {
        create("staging") {
            dimension = "server"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
        }
        create("production") {
            dimension = "server"
            applicationIdSuffix = ".production"
            versionNameSuffix = "-production"
            versionCode = 2
        }
    }

    bundle {
        language {
            // Specifies that the app bundle should not support
            // configuration APKs for language resources. These
            // resources are instead packaged with each base and
            // feature APK.
            enableSplit = true
        }
        density {
            // This property is set to true by default.
            enableSplit = true
        }
        abi {
            // This property is set to true by default.
            enableSplit = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.4.0")
}


// Main configuration of the bundle-tool-gradle-plugin.
// Run `./gradlew TransformApksFromBundleForStagingDebug` for testing.
bundleTool {
    enableByVariant { variant -> variant.name.contains("debug", true) }

    buildApks {
        create("universal") {
            buildMode.set(ApkBuildMode.UNIVERSAL.name)
        }
        create("pixel4a") {
            deviceSpec.set(file("./pixel4a.json"))
        }
    }

    getSize {
        create("all") {
            dimensions.addAll(
                GetSizeDimension.SDK.name,
                GetSizeDimension.ABI.name,
                GetSizeDimension.SCREEN_DENSITY.name,
                GetSizeDimension.LANGUAGE.name)
        }
    }
}


// If you need to extend bundle-tool-gradle-plugin with all transformed ".apks" files,
// please leverage the `outputDirProperty` from BundleToolTask.
// Run `./gradlew UploadApksForStagingDebug` for testing.
androidComponents {
    // Pls use the same rule as `enableByVariant{...}` over
    onVariants(selector().withBuildType("debug")) { variant ->
        afterEvaluate {
            val taskProvider = (tasks.named("TransformApksFromBundleFor${variant.name.capitalize()}")
                    as TaskProvider<BundleToolTask>)
            val outputDirProperty = taskProvider.flatMap { it.outputDirProperty }
            tasks.register<UploadTask>("UploadApksFor${variant.name.capitalize()}") {
                this.outputDirProperty.set(outputDirProperty)
            }
        }
    }
}
abstract class UploadTask: DefaultTask() {

    @org.gradle.api.tasks.InputDirectory
    val outputDirProperty: DirectoryProperty = project.objects.directoryProperty()

    @org.gradle.api.tasks.TaskAction
    fun upload() {
        outputDirProperty.get().asFile.listFiles().forEach { artifact ->
            logger.lifecycle("Upload bundle-tool outputs: ${artifact.absolutePath}")
            // upload(artifact)
        }
    }
}