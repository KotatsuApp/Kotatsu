package org.koitharu.kotatsu.core.logs

import android.content.Context
import androidx.collection.arraySetOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import org.koitharu.kotatsu.core.prefs.AppSettings

@Module
@InstallIn(SingletonComponent::class)
object LoggersModule {

	@Provides
	@TrackerLogger
	fun provideTrackerLogger(
		@ApplicationContext context: Context,
		settings: AppSettings,
	) = FileLogger(context, settings, "tracker")

	@Provides
	@SyncLogger
	fun provideSyncLogger(
		@ApplicationContext context: Context,
		settings: AppSettings,
	) = FileLogger(context, settings, "sync")

	@Provides
	@ElementsIntoSet
	fun provideAllLoggers(
		@TrackerLogger trackerLogger: FileLogger,
		@SyncLogger syncLogger: FileLogger,
	): Set<@JvmSuppressWildcards FileLogger> = arraySetOf(
		trackerLogger,
		syncLogger,
	)
}
