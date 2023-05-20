package org.koitharu.kotatsu.core.network

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class BaseHttpClient

@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class MangaHttpClient
