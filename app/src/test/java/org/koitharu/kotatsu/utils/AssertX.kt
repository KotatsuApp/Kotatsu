package org.koitharu.kotatsu.utils

import org.junit.Assert
import java.net.HttpURLConnection
import java.net.URL

object AssertX {

	fun assertContentType(url: String, vararg types: String) {
		Assert.assertFalse("URL is empty", url.isEmpty())
		val cn = URL(url).openConnection() as HttpURLConnection
		cn.requestMethod = "HEAD"
		cn.connect()
		when (val code = cn.responseCode) {
			HttpURLConnection.HTTP_MOVED_PERM,
			HttpURLConnection.HTTP_MOVED_TEMP -> {
				assertContentType(cn.getHeaderField("Location"), *types)
			}
			HttpURLConnection.HTTP_OK -> {
				val ct = cn.contentType.substringBeforeLast(';').split("/")
				Assert.assertTrue(types.any {
					val x = it.split('/')
					x[0] == ct[0] && (x[1] == "*" || x[1] == ct[1])
				})
			}
			else -> Assert.fail("Invalid response code $code at $url")
		}
	}

}