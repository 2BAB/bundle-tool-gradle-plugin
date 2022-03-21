package me.xx2bab.bundletool

import com.android.build.api.variant.ApplicationVariant
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider

class BundleToolVariantExtension(objects: ObjectFactory) {
    internal val bundleToApksOutputDir = objects.directoryProperty()
}

fun ApplicationVariant.getBundleToApksOutputDir(): Provider<Directory> {
    val ext = this.getExtension(BundleToolVariantExtension::class.java)
        ?: throw BundleToolPluginUninitializedException()
    return ext.bundleToApksOutputDir
}

class BundleToolPluginUninitializedException : Exception(
    "bundle-tool-gradle-plugin is not yet initialized," +
            " please apply the plugin before calling any APIs following by the instruction."
)