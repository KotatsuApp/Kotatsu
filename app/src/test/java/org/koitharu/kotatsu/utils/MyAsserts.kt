package org.koitharu.kotatsu.utils

import org.junit.Assert
import java.net.HttpURLConnection
import java.net.URL

object MyAsserts {

    private val VALID_RESPONSE_CODES = arrayOf(
        HttpURLConnection.HTTP_OK,
        HttpURLConnection.HTTP_MOVED_PERM,
        HttpURLConnection.HTTP_MOVED_TEMP
    )

    fun assertValidUrl(url: String) {
        val cn = URL(url).openConnection() as HttpURLConnection
        cn.connect()
        val code = cn.responseCode
        Assert.assertTrue("Invalid response code $code", code in VALID_RESPONSE_CODES)
    }

}