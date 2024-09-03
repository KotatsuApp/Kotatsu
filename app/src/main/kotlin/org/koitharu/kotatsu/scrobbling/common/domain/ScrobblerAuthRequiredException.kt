package org.koitharu.kotatsu.scrobbling.common.domain

import okio.IOException
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerService

class ScrobblerAuthRequiredException(
	val scrobbler: ScrobblerService,
) : IOException()
