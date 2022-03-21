package me.xx2bab.bundletool

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeAll
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class BuildApksTest {

    companion object {

        private const val testProjectPath = "../../"
        private const val outputs = "$testProjectPath/app/build/outputs/bundle/stagingDebug-bundletool"

        @BeforeAll
        @JvmStatic
        fun buildTest() {
            println("Building...")
            GradleRunner.create()
                .forwardOutput()
                .withArguments("clean", "transformApksFromBundleForStagingDebug", "--stacktrace")
                .withProjectDir(File(testProjectPath))
                .build()

            println("Testing...")
        }
    }

    @Test
    fun `Transform aab to apks and get apks size successfully`() {
        val aab = File(outputs, "app-final-bundle-stagingdebug-1.0.aab")
        val pixel4aApks = File(outputs, "pixel4a-stagingdebug-1.0.apks")
        val pixel4aCsv = File(outputs, "pixel4a-stagingdebug-1.0-size.csv")
        val universalApks = File(outputs, "/universal-stagingdebug-1.0.apks")
        val universalCsv = File(outputs, "universal-stagingdebug-1.0-size.csv")
        assertTrue(aab.exists())
        assertTrue(pixel4aApks.exists())
        assertTrue(pixel4aCsv.exists())
        assertTrue(universalApks.exists())
        assertTrue(universalCsv.exists())
    }

}