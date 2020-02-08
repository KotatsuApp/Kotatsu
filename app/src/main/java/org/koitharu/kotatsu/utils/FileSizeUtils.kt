package org.koitharu.kotatsu.utils

object FileSizeUtils {

	@JvmStatic
	fun mbToBytes(mb: Int) = 1024L * 1024L * mb

	@JvmStatic
	fun kbToBytes(kb: Int) = 1024L * kb
}