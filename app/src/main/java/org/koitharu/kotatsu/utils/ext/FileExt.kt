package org.koitharu.kotatsu.utils.ext

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

fun File.sub(name: String) = File(this, name)

fun File.takeIfReadable() = takeIf { it.exists() && it.canRead() }

fun ZipFile.readText(entry: ZipEntry) = getInputStream(entry).bufferedReader().use {
	it.readText()
}