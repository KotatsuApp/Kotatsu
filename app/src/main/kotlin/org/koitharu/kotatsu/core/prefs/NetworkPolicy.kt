package org.koitharu.kotatsu.core.prefs

import android.net.ConnectivityManager

enum class NetworkPolicy(
	private val key: Int,
) {

	NEVER(0),
	ALWAYS(1),
	NON_METERED(2);

	fun isNetworkAllowed(cm: ConnectivityManager) = when (this) {
		NEVER -> false
		ALWAYS -> true
		NON_METERED -> !cm.isActiveNetworkMetered
	}

	companion object {

		fun from(key: String?, default: NetworkPolicy): NetworkPolicy {
			val intKey = key?.toIntOrNull() ?: return default
			return NetworkPolicy.entries.find { it.key == intKey } ?: default
		}
	}
}
