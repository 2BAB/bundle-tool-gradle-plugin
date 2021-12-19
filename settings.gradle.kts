enableFeaturePreview("VERSION_CATALOGS")

pluginManagement {
    val versions = file("deps.versions.toml").readText()
    val regexPlaceHolder = "%s\\s\\=\\s\\\"([A-Za-z0-9\\.\\-]+)\\\""
    val getVersion = { s: String -> regexPlaceHolder.format(s).toRegex().find(versions)!!.groupValues[1] }

    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }

    plugins {
        kotlin("android") version getVersion("kotlinVer") apply false
        id("com.android.application") version getVersion("agpVer") apply false
    }

    resolutionStrategy {
        eachPlugin {
            if(requested.id.id == "me.2bab.bundletool") {
                // It will be replaced by a local module using `includeBuild` below,
                // thus we just put a generic version (+) here.
                useModule("me.2bab:bundle-tool-plugin:+")
            }
        }
    }
}
dependencyResolutionManagement {
//    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
    versionCatalogs {
        create("deps") {
            from(files("./deps.versions.toml"))
        }
    }
}

include(":app")
includeBuild("bundle-tool-plugin"){
    dependencySubstitution {
        substitute(module("me.2bab:bundle-tool-plugin"))
            .using(project(":plugin"))
    }
}
