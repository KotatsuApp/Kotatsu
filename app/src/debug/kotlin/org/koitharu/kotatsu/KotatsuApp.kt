package org.koitharu.kotatsu

import android.content.Context
import android.os.StrictMode
import androidx.fragment.app.strictmode.FragmentStrictMode
import org.koitharu.kotatsu.core.BaseApp
import org.koitharu.kotatsu.local.data.LocalMangaRepository
import org.koitharu.kotatsu.local.data.PagesCache
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.reader.domain.PageLoader

class KotatsuApp : BaseApp() {

	override fun attachBaseContext(base: Context) {
		super.attachBaseContext(base)
		enableStrictMode()
	}

	private fun enableStrictMode() {
		StrictMode.setThreadPolicy(
			StrictMode.ThreadPolicy.Builder()
				.detectAll()
				.penaltyLog()
				.build(),
		)
		StrictMode.setVmPolicy(
			StrictMode.VmPolicy.Builder()
				.detectAll()
				.setClassInstanceLimit(LocalMangaRepository::class.java, 1)
				.setClassInstanceLimit(PagesCache::class.java, 1)
				.setClassInstanceLimit(MangaLoaderContext::class.java, 1)
				.setClassInstanceLimit(PageLoader::class.java, 1)
				.penaltyLog()
				.build(),
		)
		FragmentStrictMode.defaultPolicy = FragmentStrictMode.Policy.Builder()
			.penaltyDeath()
			.detectFragmentReuse()
			.detectWrongFragmentContainer()
			.detectRetainInstanceUsage()
			.detectSetUserVisibleHint()
			.detectFragmentTagUsage()
			.build()
	}
}
