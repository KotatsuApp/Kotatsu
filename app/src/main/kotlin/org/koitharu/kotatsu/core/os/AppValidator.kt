package org.koitharu.kotatsu.core.os

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.parsers.util.byte2HexFormatted
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppValidator @Inject constructor(
	@ApplicationContext private val context: Context,
) {

	val isOriginalApp by lazy {
		getCertificateSHA1Fingerprint() == CERT_SHA1
	}

	@Suppress("DEPRECATION")
	@SuppressLint("PackageManagerGetSignatures")
	private fun getCertificateSHA1Fingerprint(): String? = runCatching {
		val packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
		val signatures = requireNotNull(packageInfo?.signatures)
		val cert: ByteArray = signatures.first().toByteArray()
		val input: InputStream = ByteArrayInputStream(cert)
		val cf = CertificateFactory.getInstance("X509")
		val c = cf.generateCertificate(input) as X509Certificate
		val md: MessageDigest = MessageDigest.getInstance("SHA1")
		val publicKey: ByteArray = md.digest(c.encoded)
		return publicKey.byte2HexFormatted()
	}.onFailure { error ->
		error.printStackTraceDebug()
	}.getOrNull()

	private companion object {

		private const val CERT_SHA1 = "2C:19:C7:E8:07:61:2B:8E:94:51:1B:FD:72:67:07:64:5D:C2:58:AE"
	}
}
