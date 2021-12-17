package me.xx2bab.bundletool

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.*
import com.android.build.gradle.internal.signing.SigningConfigDataProvider
import me.xx2bab.polyfill.ApplicationVariantPolyfill
import me.xx2bab.polyfill.agp.provider.BuildToolInfoProvider
import me.xx2bab.polyfill.agp.tool.toApkCreationConfigImpl
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import java.io.File
import java.util.*

class BundleToolPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val config = project.extensions.create<BundleToolExtension>("bundleTool").apply {
            buildApks.whenObjectAdded {
                overwriteOutput.set(false)
                connectedDevice.set(false)
                localTestingMode.set(false)
                buildMode.set("")
                deviceId.set("")
                deviceSpec.set("")
            }
        }
        val androidExtension = project.extensions.getByType<ApplicationAndroidComponentsExtension>()
        androidExtension.onVariants { variant ->
            if (!config.isFeatureEnabled(variant)) {
                return@onVariants
            }
            val polyfill = ApplicationVariantPolyfill(project, variant)
            val versionName = variant.outputs[0].versionName
            val variantName = variant.name.capitalize(Locale.ROOT)
            val finalBundle = variant.artifacts.get(SingleArtifact.BUNDLE)

            val buildApksTaskProvider = project.tasks.register<BundleToolTask>(
                "TransformApksFromBundleFor${variantName}"
            ) {
                projectName.set(project.name)
                this.variantName.set(variantName)
                this.versionName.set(versionName)
                finalBundleProperty.set(finalBundle)
                signingConfigData = SigningConfigDataProvider.create(
                    variant.toApkCreationConfigImpl().config
                )
                buildApksRules = config.buildApks
                outputDirProperty.fileProvider(
                    finalBundle.map { File(it.asFile.parentFile, "bundletool") }
                )
                buildToolInfo.set(polyfill.newProvider(BuildToolInfoProvider::class.java).obtain())
            }

        }
    }

}