package org.koitharu.kotatsu.core.network.webview.adblock

import okhttp3.HttpUrl

sealed interface Rule {

	operator fun invoke(url: HttpUrl, baseUrl: HttpUrl?): Boolean

	data class Domain(private val domain: String) : Rule {

		override fun invoke(url: HttpUrl, baseUrl: HttpUrl?): Boolean = (url.topPrivateDomain() ?: url.host) == domain
	}

	data class ExactUrl(private val url: HttpUrl) : Rule {

		override operator fun invoke(url: HttpUrl, baseUrl: HttpUrl?): Boolean = url == this.url
	}

	data class Path(private val path: String, private val contains: Boolean) : Rule {

		override fun invoke(url: HttpUrl, baseUrl: HttpUrl?): Boolean {
			val fullPath = url.host + "/" + url.encodedPath
			return if (contains) {
				fullPath.contains(path)
			} else {
				fullPath.endsWith(path)
			}
		}
	}

	data class WithModifiers(
		private val baseRule: Rule,
		private val script: Boolean?,
		private val thirdParty: Boolean?,
		private val domains: Set<String>?,
		private val domainsNot: Set<String>?,
	) : Rule {

		override fun invoke(url: HttpUrl, baseUrl: HttpUrl?): Boolean {
			if (!baseRule.invoke(url, baseUrl)) {
				return false
			}
			if (baseUrl == null) {
				return true
			}
			thirdParty?.let {
				val isThirdPartyRequest =
					(url.topPrivateDomain() ?: url.host) != (baseUrl.topPrivateDomain() ?: baseUrl.host)
				if (isThirdPartyRequest != it) {
					return false
				}
			}
			// TODO check other modifiers
			return true
		}
	}
}
