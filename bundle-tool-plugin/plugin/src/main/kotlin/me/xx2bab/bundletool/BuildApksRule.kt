package me.xx2bab.bundletool

import org.gradle.api.Named
import org.gradle.api.provider.Property
import java.io.File

interface BuildApksRule : Named {

    // TODO: to use connectedDevice feature may require user to plug-in only 1 device,
    //   which is flaky to an automated build flow, I would like to guide everyone to
    //   take `deviceSpec` json file for robust experience.
    // val connectedDevice: Property<Boolean>
    val localTestingMode: Property<Boolean>
    val overwriteOutput: Property<Boolean>
    val buildMode: Property<String>
    val deviceId: Property<String> // deviceId will be used for INSTALL_APKS feature only
    val deviceSpec: Property<File>

}