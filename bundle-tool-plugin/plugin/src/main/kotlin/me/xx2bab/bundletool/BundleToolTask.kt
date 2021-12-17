package me.xx2bab.bundletool

import com.android.build.gradle.internal.signing.SigningConfigDataProvider
import com.android.sdklib.BuildToolInfo
import com.android.tools.build.bundletool.androidtools.Aapt2Command
import com.android.tools.build.bundletool.commands.BuildApksCommand
import com.android.tools.build.bundletool.model.Password
import com.android.tools.build.bundletool.model.SigningConfiguration
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.security.KeyStore
import java.util.Optional
import javax.inject.Inject

abstract class BundleToolTask : DefaultTask() {

    @get:Input
    abstract val projectName: Property<String>

    @get:Input
    abstract val variantName: Property<String>

    @get:Input
    abstract val versionName: Property<String>

    @get:InputFiles
    abstract val finalBundleProperty: RegularFileProperty

    @get:Nested
    lateinit var signingConfigData: SigningConfigDataProvider

    @get:Input
    var buildApksRules: NamedDomainObjectContainer<BuildApksRule>? = null

    @get:OutputDirectory
    abstract val outputDirProperty: DirectoryProperty

    @get:Internal
    abstract val buildToolInfo: Property<BuildToolInfo>

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @TaskAction
    fun transform() {

        val outDir = outputDirProperty.get().asFile
        val fileNameSuffix = "-${versionName.get()}"

        // Put a copy of final aab to our /bundletool directory
        val inputAabFile = File(outDir, "${projectName.get()}$fileNameSuffix.aab")
        if (inputAabFile.exists()) {
            inputAabFile.delete()
        }
        finalBundleProperty.get().asFile.copyTo(inputAabFile)

        // Fixed arguments
        val aapt2File = File(buildToolInfo.get().getPath(BuildToolInfo.PathId.AAPT2))
        val signature = signingConfigData.resolve()

        // Iterate all rules and run BuildApksCommand in parallel using WorkerAction
        val workQueue: WorkQueue = workerExecutor.noIsolation()
        buildApksRules?.asIterable()?.forEach { rule ->
            val outputApksFile = File(outDir, rule.name + fileNameSuffix + ".apks")
            workQueue.submit(BuildApksWorkAction::class.java) {
                inputAab = inputAabFile
                outputApks = outputApksFile
                aapt2 = aapt2File
                keyAlias = signature?.keyAlias
                keyPass = signature?.keyPassword
                storePass = signature?.storePassword
                storeFile = signature?.storeFile
                overwriteOutput = rule.overwriteOutput.orNull
                connectedDevice = rule.connectedDevice.orNull
                localTestingMode = rule.localTestingMode.orNull
                buildMode = if (rule.buildMode.isPresent && rule.buildMode.get().isNotBlank()) {
                    rule.buildMode.get()
                } else {
                    null
                }
                deviceId = if (rule.deviceId.isPresent && rule.deviceId.get().isNotBlank()) {
                    rule.deviceId.get()
                } else {
                    null
                }
                deviceSpecFile =
                    if (rule.deviceSpec.isPresent && rule.deviceSpec.get().isNotBlank()) {
                        File(rule.deviceSpec.get())
                    } else {
                        null
                    }
            }
        }
    }

}

private interface BuildApksWorkParam : WorkParameters {
    // Mandatory
    var inputAab: File
    var outputApks: File

    // Internal
    var aapt2: File?
    var keyAlias: String?
    var keyPass: String?
    var storePass: String?
    var storeFile: File?

    // External
    var overwriteOutput: Boolean?
    var connectedDevice: Boolean?
    var localTestingMode: Boolean?
    var buildMode: String?
    var deviceId: String?
    var deviceSpecFile: File?
}

private abstract class BuildApksWorkAction : WorkAction<BuildApksWorkParam> {

    override fun execute() {
        parameters.outputApks.let { if (it.exists()) it.delete() }
        val commandBuilder = BuildApksCommand.builder().apply {
            setBundlePath(parameters.inputAab.toPath())
            setOutputFile(parameters.outputApks.toPath())

            parameters.aapt2?.let { setAapt2Command(Aapt2Command.createFromExecutablePath(it.toPath())) }
            parameters.storeFile?.let {
                setSigningConfiguration2(
                    keystoreFile = it,
                    keystorePassword = parameters.storePass,
                    keyAlias = parameters.keyAlias,
                    keyPassword = parameters.keyPass
                )
            }

            parameters.overwriteOutput?.let { setOverwriteOutput(it) }
            parameters.connectedDevice?.let { setGenerateOnlyForConnectedDevice(it) }
            parameters.localTestingMode?.let { setLocalTestingMode(it) }
            parameters.buildMode?.let { setApkBuildMode(BuildApksCommand.ApkBuildMode.valueOf(it)) }
            parameters.deviceId?.let { setDeviceId(it) }
            parameters.deviceSpecFile?.let { setDeviceSpec(it.toPath()) }
        }
        commandBuilder.build().execute()
    }


    // below functions come from com.android.build.gradle.internal.tasks.BundleTaskUtil
    private fun toPassword(password: String?): Optional<Password> =
        Optional.ofNullable(password?.let {
            Password { KeyStore.PasswordProtection(it.toCharArray()) }
        })

    private fun BuildApksCommand.Builder.setSigningConfiguration2(
        keystoreFile: File?,
        keystorePassword: String?,
        keyAlias: String?,
        keyPassword: String?
    ): BuildApksCommand.Builder {
        if (keystoreFile == null) {
            return this
        }
        setSigningConfiguration(
            SigningConfiguration.extractFromKeystore(
                keystoreFile.toPath(),
                keyAlias,
                toPassword(keystorePassword),
                toPassword(keyPassword)
            )
        )
        return this
    }

}


