package me.xx2bab.bundletool

import org.gradle.api.NamedDomainObjectContainer

abstract class BundleToolExtension: EnableByFeatureExtension<BundleToolFeature>() {

    abstract val buildApks: NamedDomainObjectContainer<BuildApksRule>

    abstract val getSize: NamedDomainObjectContainer<GetSizeRule>

}

enum class BundleToolFeature {

    // It's currently a predecessor for GET_SIZE,
    // and the first job that plugin does,
    // disable it will cause the task registry got removed.
    // The work action that transforms .aab to .apks using `build-apks` command.
    BUILD_APKS,

    // The work action that gets the transformed .apks file size using `get-size total` command.
    GET_SIZE

}

/**
 * A copy of [com.android.tools.build.bundletool.commands.BuildApksCommand.ApkBuildMode],
 * so that users can directly use enums below instead of add BundleTool as dependency for build.gradle(.kts).
 */
enum class ApkBuildMode {
    DEFAULT,
    UNIVERSAL,
    SYSTEM,
    PERSISTENT,
    INSTANT
}

/**
 * A copy of [com.android.tools.build.bundletool.model.GetSizeRequest.Dimension],
 * so that users can directly use enums below instead of add BundleTool as dependency for build.gradle(.kts).
 */
enum class GetSizeDimension {
    SDK,
    ABI,
    SCREEN_DENSITY,
    LANGUAGE,
}