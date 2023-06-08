package org.koitharu.kotatsu.core.network

import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

class AppProxySelector(
	private val settings: AppSettings,
) : ProxySelector() {

	init {
		setDefault(this)
	}

	private var cachedProxy: Proxy? = null

	override fun select(uri: URI?): List<Proxy> {
		return listOf(getProxy())
	}

	override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
		ioe?.printStackTraceDebug()
	}

	private fun getProxy(): Proxy {
		val type = settings.proxyType
		val address = settings.proxyAddress
		val port = settings.proxyPort
		if (type == Proxy.Type.DIRECT || address.isNullOrEmpty() || port == 0) {
			return Proxy.NO_PROXY
		}
		cachedProxy?.let {
			val addr = it.address() as? InetSocketAddress
			if (addr != null && it.type() == type && addr.port == port && addr.hostString == address) {
				return it
			}
		}
		val proxy = Proxy(type, InetSocketAddress(address, port))
		cachedProxy = proxy
		return proxy
	}
}
