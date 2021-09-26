package org.koitharu.kotatsu.core.parser

interface MangaRepositoryAuthProvider {

	val authUrl: String

	fun isAuthorized(): Boolean
}