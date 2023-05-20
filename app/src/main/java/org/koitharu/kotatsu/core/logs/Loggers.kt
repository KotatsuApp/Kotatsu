package org.koitharu.kotatsu.core.logs

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TrackerLogger

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SyncLogger
