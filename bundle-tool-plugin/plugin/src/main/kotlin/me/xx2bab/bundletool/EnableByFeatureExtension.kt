package me.xx2bab.bundletool

import com.android.build.api.variant.Variant
import groovy.lang.Closure
import org.gradle.api.provider.Property

/**
 * Extract `enableByVariant(...)` function, can be reused in other plugins.
 * Currently the Lambda and Closure are defined by raw types, they can be encapsulated
 * by [Property] as well to fulfill "lazily produced/consumed" purpose.
 */
abstract class EnableByFeatureExtension<T> {

    var kotlinEnableByVariant: EnableByVariant<T>? = null

    var groovyEnableByVariant: Closure<Boolean>? = null

    // For Gradle Kotlin DSL
    fun enableByVariant(selector: EnableByVariant<T>) {
        kotlinEnableByVariant = selector
    }

    // For Gradle Groovy DSL
    fun enableByVariant(selector: Closure<Boolean>) {
        groovyEnableByVariant = selector.dehydrate()
    }

    internal fun isFeatureEnabled(variant: Variant, t: T): Boolean = when {
        kotlinEnableByVariant != null -> {
            kotlinEnableByVariant!!.invoke(variant, t)
        }
        groovyEnableByVariant != null -> {
            groovyEnableByVariant!!.call(variant, t)
        }
        else -> false
    }

}

internal typealias EnableByVariant<T> = (variant: Variant, t: T) -> Boolean