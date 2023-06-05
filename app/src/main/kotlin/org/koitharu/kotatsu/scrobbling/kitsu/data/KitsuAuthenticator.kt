package org.koitharu.kotatsu.scrobbling.kitsu.data

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import org.koitharu.kotatsu.scrobbling.common.data.ScrobblerStorage
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerService
import org.koitharu.kotatsu.scrobbling.common.domain.model.ScrobblerType
import javax.inject.Inject
import javax.inject.Provider

class KitsuAuthenticator @Inject constructor(
	@ScrobblerType(ScrobblerService.KITSU) private val storage: ScrobblerStorage,
	private val repositoryProvider: Provider<KitsuRepository>,
) : Authenticator {

	override fun authenticate(route: Route?, response: Response): Request? {
		TODO("Not yet implemented")
	}

}
