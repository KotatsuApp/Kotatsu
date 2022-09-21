package org.koitharu.kotatsu.core.github

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.parsers.util.await
import org.koitharu.kotatsu.parsers.util.byte2HexFormatted
import org.koitharu.kotatsu.parsers.util.json.mapJSONNotNull
import org.koitharu.kotatsu.parsers.util.parseJsonArray
import org.koitharu.kotatsu.utils.ext.asArrayList
import org.koitharu.kotatsu.utils.ext.printStackTraceDebug
import org.koitharu.kotatsu.utils.ext.runCatchingCancellable
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Singleton

private const val CERT_SHA1 = "2C:19:C7:E8:07:61:2B:8E:94:51:1B:FD:72:67:07:64:5D:C2:58:AE"

@Singleton
class AppUpdateRepository @Inject constructor(
	@ApplicationContext private val context: Context,
	private val okHttp: OkHttpClient,
) {

	private val availableUpdate = MutableStateFlow<AppVersion?>(null)

	fun observeAvailableUpdate() = availableUpdate.asStateFlow()

	suspend fun getAvailableVersions(): List<AppVersion> {
		val request = Request.Builder()
			.get()
			.url("https://api.github.com/repos/KotatsuApp/Kotatsu/releases?page=1&per_page=10")
		val jsonArray = okHttp.newCall(request.build()).await().parseJsonArray()
		return jsonArray.mapJSONNotNull { json ->
			val asset = json.optJSONArray("assets")?.optJSONObject(0) ?: return@mapJSONNotNull null
			AppVersion(
				id = json.getLong("id"),
				url = json.getString("html_url"),
				name = json.getString("name").removePrefix("v"),
				apkSize = asset.getLong("size"),
				apkUrl = asset.getString("browser_download_url"),
				description = json.getString("body"),
			)
		}
	}

	suspend fun fetchUpdate(): AppVersion? = withContext(Dispatchers.Default) {
		if (!isUpdateSupported()) {
			return@withContext null
		}
		runCatchingCancellable {
			val currentVersion = VersionId(BuildConfig.VERSION_NAME)
			val available = getAvailableVersions().asArrayList()
			available.sortBy { it.versionId }
			if (currentVersion.isStable) {
				available.retainAll { it.versionId.isStable }
			}
			available.maxByOrNull { it.versionId }
				?.takeIf { it.versionId > currentVersion }
		}.onFailure {
			it.printStackTraceDebug()
		}.onSuccess {
			availableUpdate.value = it
		}.getOrNull()
	}

	fun isUpdateSupported(): Boolean {
		return BuildConfig.DEBUG || getCertificateSHA1Fingerprint() == CERT_SHA1
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
}
