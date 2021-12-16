package me.xx2bab.bundletool

import com.android.build.api.variant.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
class BundleToolPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val androidExtension = project.extensions.getByType<ApplicationAndroidComponentsExtension>()
        androidExtension.onVariants { variant ->

        }
    }

}