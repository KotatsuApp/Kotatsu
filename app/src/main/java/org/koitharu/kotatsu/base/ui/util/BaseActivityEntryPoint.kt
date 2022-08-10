package org.koitharu.kotatsu.base.ui.util

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.koitharu.kotatsu.base.ui.BaseActivity
import org.koitharu.kotatsu.core.prefs.AppSettings

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BaseActivityEntryPoint {
	val settings: AppSettings
}

// Hilt cannot inject into parametrized classes
fun BaseActivityEntryPoint.inject(activity: BaseActivity<*>) {
	activity.settings = settings
}
