package org.koitharu.kotatsu.core.exceptions

import okio.IOException

class CloudFlareProtectedException(val url: String) : IOException("Protected by CloudFlare")