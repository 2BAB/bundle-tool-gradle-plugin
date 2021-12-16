import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.*
import com.android.build.api.variant.impl.ApplicationVariantImpl
import com.android.tools.build.bundletool.commands.BuildApksCommand
import com.android.build.gradle.internal.signing.SigningConfigDataProvider
import com.android.build.gradle.internal.tasks.*
import com.android.tools.build.bundletool.model.Password
import com.android.tools.build.bundletool.model.SigningConfiguration
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.gradle.workers.WorkerExecutor
import java.io.File
import com.android.sdklib.BuildToolInfo
import com.android.tools.build.bundletool.androidtools.Aapt2Command
import java.security.KeyStore
import java.util.Optional
import javax.inject.Inject

// This is a POC to quickly test some ideas and BundleTool APIs' compatibility
// For the plugin actual implementation please check rootDir/bundle-tool-gradle-plugin
class ExtendMakeApkFromBundlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.afterEvaluate {
            project.tasks.withType<PackageBundleTask>().configureEach {
                doFirst {
                    println("[PackageBundleTask][output]:" + bundleFile.asFile.get().absolutePath)
                }
            }

            project.tasks.withType<BundleToApkTask>().apply {
                val outputApks = File(project.buildDir, "${project.name}.apks")
                val isLocalTestingEnabled = false
                configureEach {
                    doFirst {
                        println("[BundleToApkTask][input]:" + bundle.asFile.get().absolutePath)
                        println("[BundleToApkTask][output]:" + outputFile.asFile.get().absolutePath)
                    }
                    this.outputFile.set(outputApks)
                    this.enableLocalTesting.set(isLocalTestingEnabled)
                }
            }

            project.tasks.withType<FinalizeBundleTask>().configureEach {
                doFirst {
                    println("[FinalizeBundleTask][input]:" + intermediaryBundleFile.asFile.get().absolutePath)
                    println("[FinalizeBundleTask][output]:" + finalBundleFile.asFile.get().absolutePath)
                }
            }
        }


        val androidExtension = project.extensions.getByType<ApplicationAndroidComponentsExtension>()

        androidExtension.onVariants { variant ->
            val variantImpl = (variant as ApplicationVariantImpl)
            val updateBundleTaskProvider = project.tasks.register<UpdateBundleFileTask>(
                "${variant.name}UpdateBundleFile"
            ) {
                initialBundleFileProperty.set(variant.artifacts.get(SingleArtifact.BUNDLE))
            }
            val finalBundleTaskProvider = project.tasks.register<ConsumeBundleFileTask>(
                "${variant.name}ConsumeBundleFile"
            ) {
                finalBundleProperty.set(variant.artifacts.get(SingleArtifact.BUNDLE))
                apksFileProperty.fileProvider(
                    variant.artifacts
                        .get(SingleArtifact.BUNDLE)
                        .map { bundle -> File(bundle.asFile.parentFile, "last.apks") }
                )
                buildToolInfo.set(variantImpl.globalScope.versionedSdkLoader.flatMap { it.buildToolInfoProvider })
                signingConfigData = SigningConfigDataProvider.create(variantImpl.delegate.config)
            }
            variant.artifacts.use(updateBundleTaskProvider).wiredWithFiles(
                UpdateBundleFileTask::initialBundleFileProperty, UpdateBundleFileTask::updatedBundleFileProperty
            ).toTransform(SingleArtifact.BUNDLE)
        }

    }


    abstract class UpdateBundleFileTask : DefaultTask() {

        @get: InputFiles
        abstract val initialBundleFileProperty: RegularFileProperty

        @get: OutputFile
        abstract val updatedBundleFileProperty: RegularFileProperty

        @TaskAction
        fun taskAction() {
            val initialBundle = initialBundleFileProperty.get().asFile
            val updatedBundle = updatedBundleFileProperty.get().asFile
            println(".toTransform(SingleArtifact.BUNDLE)")
            println("[UpdateBundleFileTask][input]:" + initialBundle.absolutePath)
            println("[UpdateBundleFileTask][output]:" + updatedBundle.absolutePath)
            initialBundle.copyTo(updatedBundle)
        }
    }

    abstract class ConsumeBundleFileTask : DefaultTask() {

        @get:InputFiles
        abstract val finalBundleProperty: RegularFileProperty

        @get:Internal
        abstract val buildToolInfo: Property<BuildToolInfo>

        @get:Nested
        lateinit var signingConfigData: SigningConfigDataProvider

        @get:OutputFile
        abstract val apksFileProperty: RegularFileProperty

        // Since this is just a POC, so we skip subtasks separation by workers
        @get:Inject
        abstract val workerExecutor: WorkerExecutor

        @TaskAction
        fun taskAction() {
            val aapt2Path = buildToolInfo.get().getPath(BuildToolInfo.PathId.AAPT2)
            println(".get(SingleArtifact.BUNDLE)")
            println("[ConsumeBundleFileTask][input]:" + finalBundleProperty.asFile.get().absolutePath)
            println("[ConsumeBundleFileTask][output]:" + apksFileProperty.asFile.get().absolutePath)
            val signingConfigData = signingConfigData.resolve()!!
            val command = BuildApksCommand.builder()
                .setBundlePath(finalBundleProperty.asFile.get().toPath())
                .setOutputFile(apksFileProperty.asFile.get().toPath())
                .setAapt2Command(
                    Aapt2Command.createFromExecutablePath(
                        File(aapt2Path).toPath()
                    )
                )
                .setSigningConfiguration2(
                    keystoreFile = signingConfigData.storeFile,
                    keystorePassword = signingConfigData.storePassword,
                    keyAlias = signingConfigData.keyAlias,
                    keyPassword = signingConfigData.keyPassword
                ).setLocalTestingMode(false)
            command.build().execute()
        }

        // below functions come from com.android.build.gradle.internal.tasks.BundleTaskUtil
        private fun toPassword(password: String?): Optional<Password> = Optional.ofNullable(password?.let {
            Password { KeyStore.PasswordProtection(it.toCharArray()) }
        })

        private fun BuildApksCommand.Builder.setSigningConfiguration2(
            keystoreFile: File?, keystorePassword: String?, keyAlias: String?, keyPassword: String?
        ): BuildApksCommand.Builder {
            if (keystoreFile == null) {
                return this
            }
            setSigningConfiguration(
                SigningConfiguration.extractFromKeystore(
                    keystoreFile.toPath(), keyAlias, toPassword(keystorePassword), toPassword(keyPassword)
                )
            )
            return this
        }
    }

}