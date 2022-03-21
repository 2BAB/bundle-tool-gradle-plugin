package me.xx2bab.bundletool

import com.android.build.gradle.internal.signing.SigningConfigDataProvider
import com.android.build.gradle.internal.utils.toImmutableSet
import com.android.sdklib.BuildToolInfo
import com.android.tools.build.bundletool.androidtools.Aapt2Command
import com.android.tools.build.bundletool.commands.BuildApksCommand
import com.android.tools.build.bundletool.commands.GetSizeCommand
import com.android.tools.build.bundletool.commands.InstallApksCommand
import com.android.tools.build.bundletool.device.DdmlibAdbServer
import com.android.tools.build.bundletool.model.GetSizeRequest
import com.android.tools.build.bundletool.model.Password
import com.android.tools.build.bundletool.model.SigningConfiguration
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.io.PrintStream
import java.security.KeyStore
import java.util.Optional
import javax.inject.Inject

abstract class BundleToolTask : DefaultTask() {

    @get:Input
    var enableGetSizeFeature: Boolean = false

    @get:Input
    var enableInstallApksFeature: Boolean = false

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

    @get:Input
    var getSizeRules: NamedDomainObjectContainer<GetSizeRule>? = null

    @get:OutputDirectory
    abstract val outputDirProperty: DirectoryProperty

    @get:Internal
    abstract val buildToolInfo: Property<BuildToolInfo>

    @get:Internal
    abstract val adbFileProvider: RegularFileProperty

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @TaskAction
    fun transform() {
        val outDir = outputDirProperty.get().asFile
        val fileNameSuffix = "-${variantName.get()}-${versionName.get()}"

        // Put a copy of final aab to our /bundletool directory
        val inputAabFile = File(outDir, "${projectName.get()}-final-bundle-$fileNameSuffix.aab")
        if (inputAabFile.exists()) {
            inputAabFile.delete()
        }
        finalBundleProperty.get().asFile.copyTo(inputAabFile)

        // Fixed arguments
        val aapt2File = File(buildToolInfo.get().getPath(BuildToolInfo.PathId.AAPT2))
        val signature = signingConfigData.resolve()


        // Iterate all rules and run commands in parallel using WorkerAction
        val workQueue: WorkQueue = workerExecutor.noIsolation()
        val buildApksOutputs = mutableListOf<BuildApksOutput>()

        // For BuildApksCommand
        buildApksRules?.forEach { rule ->
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
//                connectedDevice = rule.connectedDevice.orNull
                localTestingMode = rule.localTestingMode.orNull
                buildMode = if (rule.buildMode.isPresent && rule.buildMode.get().isNotBlank()) {
                    rule.buildMode.get()
                } else {
                    null
                }
//                deviceId = if (rule.deviceId.isPresent && rule.deviceId.get().isNotBlank()) {
//                    rule.deviceId.get()
//                } else {
//                    null
//                }
                deviceSpecFile =
                    if (rule.deviceSpec.isPresent
                        && rule.deviceSpec.get().absolutePath.isNotBlank()
                        && rule.deviceSpec.get().extension == "json"
                    ) {
                        rule.deviceSpec.get()
                    } else {
                        null
                    }
                buildApksOutputs.add(
                    BuildApksOutput(
                        this.outputApks,
                        this.deviceSpecFile,
                        rule.deviceId.get()
                    )
                )
            }
        }
        workQueue.await()

        // For GetSizeCommand
        if (enableGetSizeFeature) {
            buildApksOutputs.forEach { buildApksOutput ->
                getSizeRules?.forEach { getSizeRule ->
                    workQueue.submit(GetSizeWorkAction::class.java) {
                        inputApks = buildApksOutput.outputApks
                        outputCsv =
                            File(
                                outDir,
                                "${buildApksOutput.outputApks.nameWithoutExtension}-size.csv"
                            )
                        deviceSpecFile = buildApksOutput.deviceSpecFile
                        dimensions = if (getSizeRule.dimensions.isPresent
                            && getSizeRule.dimensions.get().isNotEmpty()
                        ) {
                            getSizeRule.dimensions.get()
                        } else {
                            setOf()
                        }
                        isInstant = getSizeRule.instant.orNull
                        modules = if (getSizeRule.modules.isPresent
                            && getSizeRule.modules.get().isNotBlank()
                        ) {
                            getSizeRule.modules.get()
                        } else {
                            null
                        }
                    }

                }
            }
        }
        workQueue.await()

        if (enableInstallApksFeature) {
            buildApksOutputs.forEach { buildApksOutput ->
                if (!buildApksOutput.deviceId.isNullOrBlank()) {
                    workQueue.submit(InstallApksWorkAction::class.java) {
                        outputApks = buildApksOutput.outputApks
                        deviceId = buildApksOutput.deviceId
                        adb = adbFileProvider.get().asFile
                        aapt2 = aapt2File
                    }
                }
            }
        }
    }
}


private data class BuildApksOutput(
    val outputApks: File,
    val deviceSpecFile: File?,
    val deviceId: String?
)

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

    // The Logger is not yet supported to be a build service injected by built-in DI.
    // https://github.com/gradle/gradle/issues/16991
    // Use below trick as a workaround.
    private val logger = Logging.getLogger(BundleToolTask::class.java)

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
            parameters.deviceId?.let {
                val conn = parameters.connectedDevice == null || parameters.connectedDevice!!
                if (conn) {
                    setDeviceId(it)
                }
            }
            parameters.deviceSpecFile?.let { setDeviceSpec(it.toPath()) }
        }
        commandBuilder.build().execute()
        logger.lifecycle("BuildApks command executed successfully, " +
                "generated ${parameters.outputApks.absolutePath}.")

        // Special case for extracting universal apk from apks
        if (parameters.buildMode != null
            && parameters.buildMode == ApkBuildMode.UNIVERSAL.name
        ) {
            val universalApk = File(
                parameters.outputApks.parentFile,
                parameters.outputApks.name.dropLast(1)
            )
            extractFileByExtensionFromZip(
                parameters.outputApks,
                "apk",
                universalApk
            )
        }
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


private interface GetSizeWorkParam : WorkParameters {
    // Mandatory
    var inputApks: File
    var outputCsv: File

    // External
    var deviceSpecFile: File?
    var dimensions: Set<String>?
    var isInstant: Boolean?
    var modules: String?

}

private abstract class GetSizeWorkAction : WorkAction<GetSizeWorkParam> {

    override fun execute() {
        val getSizeCommandBuilder = GetSizeCommand.builder().apply {
            setApksArchivePath(parameters.inputApks.toPath())
            setGetSizeSubCommand(GetSizeCommand.GetSizeSubcommand.TOTAL)

            parameters.deviceSpecFile?.let { setDeviceSpec(it.toPath()) }
            parameters.dimensions?.let { safeDimens ->
                val set = safeDimens.map { dimen ->
                    GetSizeRequest.Dimension.valueOf(dimen.trim())
                }
                    .toImmutableSet()
                setDimensions(set)
            }
            parameters.modules?.let { safeModules ->
                val set = safeModules.split(",")
                    .map { it.trim() }
                    .toImmutableSet()
                setModules(set)
            }
            parameters.isInstant?.let { setInstant(it) }
        }
        parameters.outputCsv.createNewFile()
        getSizeCommandBuilder.build()
            .getSizeTotal(PrintStream(parameters.outputCsv.outputStream()))
    }

}

private interface InstallApksWorkParam : WorkParameters {
    // Mandatory
    var outputApks: File

    // Internal
    var aapt2: File?
    var adb: File

    // External
    var deviceId: String
}

private abstract class InstallApksWorkAction : WorkAction<InstallApksWorkParam> {

    private val logger = Logging.getLogger(BundleToolTask::class.java)

    override fun execute() {
        try {
            InstallApksCommand.builder().apply {
                setDeviceId(parameters.deviceId)
                setAdbPath(parameters.adb.toPath())
                setAdbServer(DdmlibAdbServer.getInstance())
                setApksArchivePath(parameters.outputApks.toPath())
            }.build().execute()
            logger.lifecycle("Installation of ${parameters.outputApks.absolutePath} for ${parameters.deviceId} was successful.")
        } catch (e: Exception) {
            logger.warn("Installation of ${parameters.outputApks.absolutePath} for ${parameters.deviceId} was failed. ${e.message}")
        }
    }

}