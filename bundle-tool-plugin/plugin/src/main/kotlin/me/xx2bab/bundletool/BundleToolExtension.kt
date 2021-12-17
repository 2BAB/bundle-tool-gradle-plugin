package me.xx2bab.bundletool

import com.android.build.api.variant.Variant
import groovy.lang.Closure
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

abstract class BundleToolExtension {

    var kotlinEnableByVariant: EnableByVariant? = null

    var groovyEnableByVariant: Closure<Boolean>? = null

    // For Gradle Kotlin DSL
    fun enableByVariant(selector: EnableByVariant) {
        kotlinEnableByVariant = selector
    }

    // For Gradle Groovy DSL
    fun enableByVariant(selector: Closure<Boolean>) {
        groovyEnableByVariant = selector.dehydrate()
    }

    abstract val buildApks: NamedDomainObjectContainer<BuildApksRule>

    internal fun isFeatureEnabled(variant: Variant): Boolean = when {
        kotlinEnableByVariant != null -> {
            kotlinEnableByVariant!!.invoke(variant)
        }
        groovyEnableByVariant != null -> {
            groovyEnableByVariant!!.call(variant)
        }
        else -> false
    }


}

internal typealias EnableByVariant = (variant: Variant) -> Boolean

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