package me.xx2bab.bundletool

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.*
import com.android.build.gradle.internal.signing.SigningConfigDataProvider
import me.xx2bab.polyfill.getApplicationVariantImpl
import me.xx2bab.polyfill.getBuildToolInfo
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import java.io.File
import java.util.*

class BundleToolPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.apply(plugin = "me.2bab.polyfill")
        val config = project.extensions.create<BundleToolExtension>("bundleTool").apply {
            buildApks.whenObjectAdded {
                overwriteOutput.convention(false)
//                connectedDevice.convention(false)
                localTestingMode.convention(false)
                buildMode.convention("")
                deviceId.convention("")
                deviceSpec.convention(File(""))
            }
            getSize.whenObjectAdded {
                dimensions.convention(
                    setOf(
                        GetSizeDimension.SDK.name,
                        GetSizeDimension.ABI.name,
                        GetSizeDimension.SCREEN_DENSITY.name,
                        GetSizeDimension.LANGUAGE.name
                    )
                )
                modules.convention("")
                instant.convention(false)
            }
        }
        val androidExtension = project.extensions.getByType<ApplicationAndroidComponentsExtension>()
        androidExtension.beforeVariants { variantBuilder ->
            variantBuilder.registerExtension(
                BundleToolVariantExtension::class.java,
                BundleToolVariantExtension(project.objects)
            )
        }

        androidExtension.onVariants { variant ->
            if (!config.isFeatureEnabled(variant, BundleToolFeature.BUILD_APKS)) {
                return@onVariants
            }
            val featureGetSize = config.isFeatureEnabled(variant, BundleToolFeature.GET_SIZE)
            val featureInstallApks = config.isFeatureEnabled(variant, BundleToolFeature.INSTALL_APKS)
            val versionName = variant.outputs[0].versionName
            val variantName = variant.name.capitalize(Locale.ROOT)
            val finalBundle = variant.artifacts.get(SingleArtifact.BUNDLE)

            val buildApksTaskProvider = project.tasks.register<BundleToolTask>(
                "TransformApksFromBundleFor${variantName}"
            ) {
                enableGetSizeFeature = featureGetSize
                enableInstallApksFeature = featureInstallApks
                projectName.set(project.name)
                this.variantName.set(variantName)
                this.versionName.set(versionName)
                finalBundleProperty.set(finalBundle)
                signingConfigData = SigningConfigDataProvider.create(
                    variant.getApplicationVariantImpl()
                )
                buildApksRules = config.buildApks
                getSizeRules = config.getSize
                buildToolInfo.set(variant.getBuildToolInfo())
                adbFileProvider.set(androidExtension.sdkComponents.adb)
                outputDirProperty.fileProvider(finalBundle.map {
                    File(it.asFile.parentFile, "bundletool")
                })
            }
            variant.getExtension(BundleToolVariantExtension::class.java)!!
                .bundleToApksOutputDir
                .set(buildApksTaskProvider.flatMap { it.outputDirProperty })
        }

    }

}