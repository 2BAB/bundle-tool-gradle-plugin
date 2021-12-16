plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
}
dependencies {
    implementation("com.android.tools.build:gradle:7.0.4")
    implementation("com.android.tools:sdklib:30.0.4")
    implementation("com.android.tools.build:bundletool:1.6.0")
    kotlin("std-lib")
}

repositories {
    mavenCentral()
    google()
}


gradlePlugin {
    plugins {
        create("poc") {
            id = "poc"
            implementationClass ="ExtendMakeApkFromBundlePlugin"
        }
    }
}