import me.xx2bab.bundletool.*
import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    kotlin("android")
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
        }
        create("production") {
            dimension = "server"
            applicationIdSuffix = ".production"
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
        jvmTarget = "11"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.4.0")
}

val pixel4aId = if (project.file("../local.properties").exists()) {
    val p = Properties().apply { load(FileInputStream(project.file("../local.properties"))) }
    p["pixel4a.id"].toString()
} else {
    ""
}


// Main configuration of the bundle-tool-gradle-plugin.
// Run `./gradlew TransformApksFromBundleForStagingDebug` for testing all features.
bundleTool {
    // The plugin can be enabled by variant, for instance,
    // BundleToolFeature.GET_SIZE feature is disabled for "productionDebug" buildTypes,
    // BundleToolFeature.INSTALL_APKS feature is enabled for "debug" buildTypes only,
    // while other combinations are supported/enabled.
    enableByVariant { variant, feature ->
        when(feature) {
            BundleToolFeature.GET_SIZE -> {
                !(variant.buildType == "debug" && variant.flavorName == "production")
            }
            BundleToolFeature.INSTALL_APKS -> {
                variant.buildType == "debug"
            }
            else -> true
        }
    }

    // Each of them will create a work action with `build-apks` command
    buildApks {
        create("universal") {
            buildMode.set(ApkBuildMode.UNIVERSAL.name)
        }
        create("pixel4a") {
            deviceSpec.set(file("./pixel4a.json"))
            // `deviceId` will be used for INSTALL_APKS feature only,
            // set the `deviceId` to indicate that you want to install the apks after built
            deviceId.set(pixel4aId)
        }
        create("pixel6") {
            deviceSpec.set(file("./pixel6.json"))
        }
    }

    // Each of them will create a work action for above "buildApks" list items' output
    getSize {
        create("all") {
            dimensions.addAll(
                GetSizeDimension.SDK.name,
                GetSizeDimension.ABI.name,
                GetSizeDimension.SCREEN_DENSITY.name,
                GetSizeDimension.LANGUAGE.name
            )
        }
    }
}


// Run `./gradlew UploadApksForStagingDebug` for testing.
androidComponents {
    // Pls use the same rule as `enableByVariant{...}` over
    onVariants(selector().withBuildType("debug")) { variant ->
        tasks.register<UploadTask>("UploadApksFor${variant.name.capitalize()}") {
            this.outputDirProperty.set(variant.getBundleToApksOutputDir())
        }
    }
}

abstract class UploadTask : DefaultTask() {

    @get:org.gradle.api.tasks.InputDirectory
    abstract val outputDirProperty: DirectoryProperty

    @org.gradle.api.tasks.TaskAction
    fun upload() {
        outputDirProperty.get().asFile.listFiles().forEach { artifact ->
            logger.lifecycle("Uploading bundle-tool outputs: ${artifact.absolutePath}")
            // upload(artifact)
        }
    }
}