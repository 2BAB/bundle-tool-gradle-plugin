package me.xx2bab.bundletool

import java.io.*
import java.util.zip.ZipFile

@Throws(IOException::class)
fun extractFileByExtensionFromZip(
    zipFile: File,
    fileNameExtension: String,
    destExtractFile: File
) {
    ZipFile(zipFile).use { zip ->
        zip.entries().asSequence().forEach { entry ->
            zip.getInputStream(entry).use { input ->
                if (entry.name.endsWith(".$fileNameExtension")) {
                    destExtractFile.outputStream().use { input.copyTo(it) }
                }
            }
        }
    }
}