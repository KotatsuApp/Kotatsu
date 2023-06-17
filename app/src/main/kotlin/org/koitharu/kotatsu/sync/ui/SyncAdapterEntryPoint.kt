package org.koitharu.kotatsu.sync.ui

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.koitharu.kotatsu.sync.domain.SyncHelper

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncAdapterEntryPoint {
	val syncHelperFactory: SyncHelper.Factory
}
