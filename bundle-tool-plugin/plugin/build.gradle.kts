plugins {
    id("java-gradle-plugin")
    `kotlin-dsl`
    `github-release`
    `maven-central-publish`
}

project.group = "me.2bab"
project.version = BuildConfig.Versions.pluginVersion

java {
    withSourcesJar()
    withJavadocJar()
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_17
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}
tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        create("bundle-tool-plugin") {
            id = "me.2bab.bundletool"
            implementationClass = "me.xx2bab.bundletool.BundleToolPlugin"
            displayName = "me.2bab.bundletool"
        }
    }
}

dependencies {
    implementation(gradleApi())

    implementation(deps.kotlin.std)
    implementation(deps.guava)

    compileOnly(deps.android.gradle.plugin)
    compileOnly(deps.android.tools.sdkcommon)
    compileOnly(deps.android.tools.sdklib)
    compileOnly(deps.android.tools.bundletool)

    implementation(deps.polyfill.main)

    testImplementation(gradleTestKit())
    testImplementation(deps.kotlin.test)
    testImplementation(deps.mockk)
    testImplementation(deps.junit5)
    testImplementation(deps.android.gradle.plugin)
    testImplementation(deps.android.tools.sdkcommon)
    testImplementation(deps.android.tools.sdklib)
    testImplementation(deps.android.tools.bundletool)
    testImplementation(deps.protobuf.java)
}