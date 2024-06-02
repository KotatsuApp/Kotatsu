package org.koitharu.kotatsu.core.network

import okhttp3.Cache
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.util.ext.printStackTraceDebug
import java.net.InetAddress
import java.net.UnknownHostException

class DoHManager(
	cache: Cache,
	private val settings: AppSettings,
) : Dns {

	private val bootstrapClient = OkHttpClient.Builder().cache(cache).build()

	private var cachedDelegate: Dns? = null
	private var cachedProvider: DoHProvider? = null

	override fun lookup(hostname: String): List<InetAddress> {
		return try {
			getDelegate().lookup(hostname)
		} catch (e: UnknownHostException) {
			// fallback
			Dns.SYSTEM.lookup(hostname)
		}
	}

	@Synchronized
	private fun getDelegate(): Dns {
		var delegate = cachedDelegate
		val provider = settings.dnsOverHttps
		if (delegate == null || provider != cachedProvider) {
			delegate = createDelegate(provider)
			cachedDelegate = delegate
			cachedProvider = provider
		}
		return delegate
	}

	private fun createDelegate(provider: DoHProvider): Dns = when (provider) {
		DoHProvider.NONE -> Dns.SYSTEM
		DoHProvider.GOOGLE -> DnsOverHttps.Builder().client(bootstrapClient)
			.url("https://dns.google/dns-query".toHttpUrl())
			.resolvePrivateAddresses(true)
			.bootstrapDnsHosts(
				listOfNotNull(
					tryGetByIp("8.8.4.4"),
					tryGetByIp("8.8.8.8"),
					tryGetByIp("2001:4860:4860::8888"),
					tryGetByIp("2001:4860:4860::8844"),
				),
			).build()

		DoHProvider.CLOUDFLARE -> DnsOverHttps.Builder().client(bootstrapClient)
			.url("https://cloudflare-dns.com/dns-query".toHttpUrl())
			.resolvePrivateAddresses(true)
			.bootstrapDnsHosts(
				listOfNotNull(
					tryGetByIp("162.159.36.1"),
					tryGetByIp("162.159.46.1"),
					tryGetByIp("1.1.1.1"),
					tryGetByIp("1.0.0.1"),
					tryGetByIp("162.159.132.53"),
					tryGetByIp("2606:4700:4700::1111"),
					tryGetByIp("2606:4700:4700::1001"),
					tryGetByIp("2606:4700:4700::0064"),
					tryGetByIp("2606:4700:4700::6400"),
				),
			).build()

		DoHProvider.ADGUARD -> DnsOverHttps.Builder().client(bootstrapClient)
			.url("https://dns-unfiltered.adguard.com/dns-query".toHttpUrl())
			.resolvePrivateAddresses(true)
			.bootstrapDnsHosts(
				listOfNotNull(
					tryGetByIp("94.140.14.140"),
					tryGetByIp("94.140.14.141"),
					tryGetByIp("2a10:50c0::1:ff"),
					tryGetByIp("2a10:50c0::2:ff"),
				),
			).build()

		DoHProvider.ZERO_MS -> DnsOverHttps.Builder().client(bootstrapClient)
			.url("https://2ca4h4crra.cloudflare-gateway.com/dns-query".toHttpUrl())
			.resolvePublicAddresses(true)
			.build()
	}

	private fun tryGetByIp(ip: String): InetAddress? = try {
		InetAddress.getByName(ip)
	} catch (e: UnknownHostException) {
		e.printStackTraceDebug()
		null
	}
}
