package org.koitharu.kotatsu

import android.content.Context
import android.os.Build
import android.os.StrictMode
import androidx.fragment.app.strictmode.FragmentStrictMode
import org.koitharu.kotatsu.core.BaseApp
import org.koitharu.kotatsu.local.data.LocalMangaRepository
import org.koitharu.kotatsu.local.data.PagesCache
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.ui.ReaderViewModel

class KotatsuApp : BaseApp() {

	override fun attachBaseContext(base: Context) {
		super.attachBaseContext(base)
		enableStrictMode()
	}

	private fun enableStrictMode() {
		val notifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			StrictModeNotifier(this)
		} else {
			null
		}
		StrictMode.setThreadPolicy(
			StrictMode.ThreadPolicy.Builder()
				.detectAll()
				.penaltyLog()
				.run {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && notifier != null) {
						penaltyListener(notifier.executor, notifier)
					} else {
						this
					}
				}.build(),
		)
		StrictMode.setVmPolicy(
			StrictMode.VmPolicy.Builder()
				.detectActivityLeaks()
				.detectLeakedSqlLiteObjects()
				.detectLeakedClosableObjects()
				.detectLeakedRegistrationObjects()
				.setClassInstanceLimit(LocalMangaRepository::class.java, 1)
				.setClassInstanceLimit(PagesCache::class.java, 1)
				.setClassInstanceLimit(MangaLoaderContext::class.java, 1)
				.setClassInstanceLimit(PageLoader::class.java, 1)
				.setClassInstanceLimit(ReaderViewModel::class.java, 1)
				.penaltyLog()
				.run {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && notifier != null) {
						penaltyListener(notifier.executor, notifier)
					} else {
						this
					}
				}.build(),
		)
		FragmentStrictMode.defaultPolicy = FragmentStrictMode.Policy.Builder()
			.penaltyDeath()
			.detectFragmentReuse()
			.detectWrongFragmentContainer()
			.detectRetainInstanceUsage()
			.detectSetUserVisibleHint()
			.detectFragmentTagUsage()
			.penaltyLog()
			.run {
				if (notifier != null) {
					penaltyListener(notifier)
				} else {
					this
				}
			}.build()
	}
}
