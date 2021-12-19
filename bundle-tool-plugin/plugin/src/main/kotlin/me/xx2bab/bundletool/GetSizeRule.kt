package me.xx2bab.bundletool

import org.gradle.api.Named
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

interface GetSizeRule : Named {

    val dimensions: SetProperty<String>
    val modules: Property<String>
    val instant: Property<Boolean>

}