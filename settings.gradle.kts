pluginManagement {
    plugins {
        kotlin("android") version "1.5.31" apply false
        id("com.android.application") version "7.0.4" apply false
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
include(":app")
