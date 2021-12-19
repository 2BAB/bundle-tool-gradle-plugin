package me.xx2bab.bundletool

import com.android.tools.build.bundletool.androidtools.Aapt2Command
import com.android.tools.build.bundletool.commands.BuildApksCommand
import com.android.tools.build.bundletool.commands.GetSizeCommand
import com.android.tools.build.bundletool.device.AdbServer
import com.android.tools.build.bundletool.model.GetSizeRequest
import com.google.common.collect.ImmutableSet
import io.mockk.mockk
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * To check the API compatibility of [BuildApksCommand].
 * Only potential used APIs will be tested.
 * @see .github/api_compatibility.yml
 */
class BundleToolCommanderTest {

    companion object {
        @TempDir
        lateinit var tempDir: File

        lateinit var inputBundle: File
        lateinit var outputApks: File
        lateinit var aapt2File: File
        lateinit var adbFile: File
        lateinit var specJsonFile: File

        @BeforeAll
        @JvmStatic
        fun setup() {
            inputBundle = File(tempDir, "bundle.aab").also { it.createNewFile() }
            outputApks = File(tempDir, "out.apks").also { it.createNewFile() }
            aapt2File = File(tempDir, "aapt2").also { it.createNewFile() }
            adbFile = File(tempDir, "adb").also { it.createNewFile() }
            specJsonFile = File(javaClass.classLoader.getResource("pixel4a.json")!!.file)
        }
    }

    val adbServer = mockk<AdbServer>()


    /////////////////////////// BuildApks Task Usage

    @Test
    fun `BuildApksCommand builds successfully with basic input&output`() {
        val command = BuildApksCommand.builder()
            .setBundlePath(inputBundle.toPath())
            .setOutputFile(outputApks.toPath())
            .build()
        assertNotNull(command.bundlePath)
    }

    @Test
    fun `BuildApksCommand builds successfully with setAapt2Command()`() {
        val command = BuildApksCommand.builder()
            .setBundlePath(inputBundle.toPath())
            .setOutputFile(outputApks.toPath())
            .setAapt2Command(Aapt2Command.createFromExecutablePath(aapt2File.toPath()))
            .build()
        assertTrue { command.aapt2Command.isPresent }
    }


    @Test
    fun `BuildApksCommand builds successfully with setOverwriteOutput`() {
        val command = BuildApksCommand.builder()
            .setBundlePath(inputBundle.toPath())
            .setOutputFile(outputApks.toPath())
            .setOverwriteOutput(true)
            .build()
        assertTrue { command.overwriteOutput }
    }

    @Test
    fun `BuildApksCommand builds successfully with setDeviceId() & setGenerateOnlyForConnectedDevice`() {
        val id = "5rfvt1"
        val command = BuildApksCommand.builder()
            .setBundlePath(inputBundle.toPath())
            .setOutputFile(outputApks.toPath())
            .setGenerateOnlyForConnectedDevice(true)
            .setDeviceId(id)
            .build()
        assertTrue { command.deviceId.isPresent && command.deviceId.get() == id }
    }

    @Test
    fun `BuildApksCommand builds successfully with setApkBuildMode`() {
        val command = BuildApksCommand.builder()
            .setBundlePath(inputBundle.toPath())
            .setOutputFile(outputApks.toPath())
            .setApkBuildMode(BuildApksCommand.ApkBuildMode.UNIVERSAL)
            .build()
        assertTrue { command.apkBuildMode == BuildApksCommand.ApkBuildMode.UNIVERSAL }
    }

    @Test
    fun `BuildApksCommand builds successfully with setLocalTestingMode`() {
        val command = BuildApksCommand.builder()
            .setBundlePath(inputBundle.toPath())
            .setOutputFile(outputApks.toPath())
            .setLocalTestingMode(true)
            .build()
        assertTrue { command.localTestingMode }
    }

    @Test
    fun `BuildApksCommand builds successfully with setDeviceSpec()`() {
        val command = BuildApksCommand.builder()
            .setBundlePath(inputBundle.toPath())
            .setOutputFile(outputApks.toPath())
            .setDeviceSpec(specJsonFile.toPath())
            .build()
        assertTrue { command.deviceSpec.isPresent }
    }


    /////////////////////////// GetSize Usage

    @Test
    fun `GetSizeCommand builds successfully with input`() {
        val command = GetSizeCommand.builder()
            .setApksArchivePath(outputApks.toPath())
            .setGetSizeSubCommand(GetSizeCommand.GetSizeSubcommand.TOTAL)
            .build()
        assertTrue { command.apksArchivePath == outputApks.toPath() }
    }

    @Test
    fun `GetSizeCommand builds successfully with setDeviceSpec`() {
        val command = GetSizeCommand.builder()
            .setApksArchivePath(outputApks.toPath())
            .setGetSizeSubCommand(GetSizeCommand.GetSizeSubcommand.TOTAL)
            .setDeviceSpec(specJsonFile.toPath())
            .build()
        assertTrue { command.deviceSpec != null }
    }

    @Test
    fun `GetSizeCommand builds successfully with setDimensions`() {
        val command = GetSizeCommand.builder()
            .setApksArchivePath(outputApks.toPath())
            .setGetSizeSubCommand(GetSizeCommand.GetSizeSubcommand.TOTAL)
            .setDimensions(ImmutableSet.of(GetSizeRequest.Dimension.SDK))
            .build()
        assertTrue { command.dimensions.contains(GetSizeRequest.Dimension.SDK) }
    }

    @Test
    fun `GetSizeCommand builds successfully with setInstant`() {
        val command = GetSizeCommand.builder()
            .setApksArchivePath(outputApks.toPath())
            .setGetSizeSubCommand(GetSizeCommand.GetSizeSubcommand.TOTAL)
            .setInstant(true)
            .build()
        assertTrue { command.instant }
    }

    @Test
    fun `GetSizeCommand builds successfully with setModules`() {
        val command = GetSizeCommand.builder()
            .setApksArchivePath(outputApks.toPath())
            .setGetSizeSubCommand(GetSizeCommand.GetSizeSubcommand.TOTAL)
            .setModules(ImmutableSet.of("app"))
            .build()
        assertTrue { command.modules.get().contains("app")  }
    }

}