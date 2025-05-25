package org.koitharu.kotatsu.core.network.webview.adblock

import androidx.annotation.CheckResult
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Very simple implementation of adblock list parser
 * Not all features are supported
 */
class RulesList {

	private val blockRules = ArrayList<Rule>()
	private val allowRules = ArrayList<Rule>()

	operator fun get(url: HttpUrl, baseUrl: HttpUrl?): Rule? {
		val rule = blockRules.find { x -> x(url, baseUrl) }
		return rule?.takeIf { allowRules.none { x -> x(url, baseUrl) } }
	}

	fun add(line: String) {
		val parts = line.lowercase().trim().split('$')
		parts.first().addImpl(isWhitelist = false, modifiers = parts.getOrNull(1))
	}

	fun trimToSize() {
		blockRules.trimToSize()
		allowRules.trimToSize()
	}

	private fun String.addImpl(isWhitelist: Boolean, modifiers: String?) {
		val list = if (isWhitelist) allowRules else blockRules

		when {
			startsWith('!') || startsWith('[') -> {
				// Comment, do nothing
			}

			startsWith("||") -> {
				// domain
				list += Rule.Domain(substring(2).substringBefore('^').trim()).withModifiers(modifiers)
			}

			startsWith('|') -> {
				val url = substring(1).substringBefore('^').trim().toHttpUrlOrNull()
				if (url != null) {
					list += Rule.ExactUrl(url).withModifiers(modifiers)
				}
			}

			startsWith("@@") -> {
				substring(2).substringBefore('^').trim().addImpl(!isWhitelist, modifiers)
			}

			startsWith("##") -> {
				// TODO css rules
			}

			else -> {
				if (endsWith('*')) {
					list += Rule.Path(this.dropLast(1), contains = true).withModifiers(modifiers)
				} else if (!contains('*')) { // wildcards is not supported yet
					list += Rule.Path(this, contains = false).withModifiers(modifiers)
				}
			}
		}
	}

	@CheckResult
	private fun Rule.withModifiers(options: String?): Rule {
		if (options.isNullOrEmpty()) {
			return this
		}
		var script: Boolean? = null
		var thirdParty: Boolean? = null
		options.split(',').forEach {
			val isNot = it.startsWith('~')
			when (it.removePrefix("~")) {
				"script" -> script = !isNot
				"third-party" -> thirdParty = !isNot
			}
		}
		return Rule.WithModifiers(
			baseRule = this,
			script = script,
			thirdParty = thirdParty,
			domains = null, //TODO
			domainsNot = null, //TODO
		)
	}
}
