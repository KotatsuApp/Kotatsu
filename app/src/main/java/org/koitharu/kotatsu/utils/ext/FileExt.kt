package org.koitharu.kotatsu.utils.ext

import java.io.File

fun File.sub(name: String) = File(this, name)

fun File.takeIfReadable() = takeIf { it.exists() && it.canRead() }