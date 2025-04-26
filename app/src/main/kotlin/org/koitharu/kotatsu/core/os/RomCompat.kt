package org.koitharu.kotatsu.core.os

import kotlinx.coroutines.Dispatchers
import org.jetbrains.annotations.Blocking
import org.koitharu.kotatsu.parsers.util.suspendlazy.suspendLazy
import java.io.InputStreamReader

object RomCompat {

	val isMiui = suspendLazy(Dispatchers.IO) {
		getProp("ro.miui.ui.version.name").isNotEmpty()
	}

	@Blocking
	private fun getProp(propName: String) = Runtime.getRuntime().exec("getprop $propName").inputStream.use {
		it.reader().use(InputStreamReader::readText).trim()
	}
}
