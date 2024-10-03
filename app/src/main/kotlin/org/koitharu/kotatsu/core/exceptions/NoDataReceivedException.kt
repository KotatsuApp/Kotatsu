package org.koitharu.kotatsu.core.exceptions

import okio.IOException

class NoDataReceivedException(
	url: String,
) : IOException("No data has been received from $url")
