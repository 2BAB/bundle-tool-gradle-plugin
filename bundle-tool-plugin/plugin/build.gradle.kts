plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    `kotlin-dsl`
    `github-release`
    `maven-central-publish`
    `functional-test-setup`
}

project.group = "me.2bab"
project.version = BuildConfig.Versions.pluginVersion

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

gradlePlugin {
    plugins {
        create("bundle-tool-plugin") {
            id = "me.2bab.bundletool"
            implementationClass = "me.xx2bab.bundletool.BundleToolPlugin"
        }
    }
}

dependencies {
    implementation(gradleApi())

    implementation(deps.kotlin.std)

    compileOnly(deps.android.gradle.plugin)
    compileOnly(deps.android.tools.sdkcommon)
    compileOnly(deps.android.tools.sdklib)

    implementation(deps.polyfill.main)

    testImplementation(gradleTestKit())
    testImplementation(deps.junit)
    testImplementation(deps.mockito)
    testImplementation(deps.mockitoInline)
}