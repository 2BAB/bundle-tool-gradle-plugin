package me.xx2bab.bundletool

import org.gradle.api.Named
import org.gradle.api.provider.Property

interface BuildApksRule : Named {

    val overwriteOutput: Property<Boolean>
    val connectedDevice: Property<Boolean>
    val localTestingMode: Property<Boolean>
    val buildMode: Property<String>
    val deviceId: Property<String>
    val deviceSpec: Property<String>

}