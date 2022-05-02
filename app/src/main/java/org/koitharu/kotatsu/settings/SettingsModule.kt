package org.koitharu.kotatsu.settings

import android.net.Uri
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koitharu.kotatsu.core.backup.BackupRepository
import org.koitharu.kotatsu.core.backup.RestoreRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.settings.backup.BackupViewModel
import org.koitharu.kotatsu.settings.backup.RestoreViewModel
import org.koitharu.kotatsu.settings.newsources.NewSourcesViewModel
import org.koitharu.kotatsu.settings.onboard.OnboardViewModel
import org.koitharu.kotatsu.settings.protect.ProtectSetupViewModel
import org.koitharu.kotatsu.settings.sources.SourcesSettingsViewModel

val settingsModule
	get() = module {

		single { BackupRepository(get()) }
		single { RestoreRepository(get()) }
		single(createdAtStart = true) { AppSettings(androidContext()) }

		viewModel { BackupViewModel(get(), androidContext()) }
		viewModel { params ->
			RestoreViewModel(params.getOrNull(Uri::class), get(), androidContext())
		}
		viewModel { ProtectSetupViewModel(get()) }
		viewModel { OnboardViewModel(get()) }
		viewModel { SourcesSettingsViewModel(get()) }
		viewModel { NewSourcesViewModel(get()) }
	}