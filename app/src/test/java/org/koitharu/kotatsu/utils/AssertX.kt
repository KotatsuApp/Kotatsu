package org.koitharu.kotatsu.utils

import org.junit.Assert
import java.net.HttpURLConnection
import java.net.URL

object AssertX {

    fun assertContentType(url: String, type: String, subtype: String? = null) {
        Assert.assertFalse("URL is empty", url.isEmpty())
        val cn = URL(url).openConnection() as HttpURLConnection
        cn.requestMethod = "HEAD"
        cn.connect()
        when (val code = cn.responseCode) {
            HttpURLConnection.HTTP_MOVED_PERM,
            HttpURLConnection.HTTP_MOVED_TEMP -> assertContentType(cn.getHeaderField("Location"), type, subtype)
            HttpURLConnection.HTTP_OK -> {
                val ct = cn.contentType.substringBeforeLast(';').split("/")
                Assert.assertEquals(type, ct.first())
                if (subtype != null) {
                    Assert.assertEquals(subtype, ct.last())
                }
            }
            else -> Assert.fail("Invalid response code $code")
        }
    }

}