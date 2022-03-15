package org.koitharu.kotatsu.core.exceptions

import androidx.annotation.StringRes
import okio.IOException
import org.koitharu.kotatsu.R

class CloudFlareProtectedException(
	val url: String
) : IOException("Protected by CloudFlare")