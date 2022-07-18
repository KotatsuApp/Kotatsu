package org.koitharu.kotatsu.core.github

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.headersContentLength
import org.junit.Assert
import org.junit.Test
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.parsers.util.await

class GithubRepositoryTest {

	private val okHttpClient = OkHttpClient()
	private val repository = GithubRepository(okHttpClient)

	@Test
	fun getLatestVersion() = runTest {
		val version = repository.getLatestVersion()
		val versionId = VersionId(version.name)

		val apkHead = okHttpClient.newCall(
			Request.Builder()
				.url(version.apkUrl)
				.head()
				.build()
		).await()

		Assert.assertTrue(versionId <= VersionId(BuildConfig.VERSION_NAME))
		Assert.assertTrue(apkHead.isSuccessful)
		Assert.assertEquals(version.apkSize, apkHead.headersContentLength())
	}
}