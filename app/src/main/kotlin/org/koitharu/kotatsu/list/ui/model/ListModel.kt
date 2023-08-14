package org.koitharu.kotatsu.list.ui.model

interface ListModel {

	override fun equals(other: Any?): Boolean

	fun areItemsTheSame(other: ListModel): Boolean

	fun getChangePayload(previousState: ListModel): Any? = null
}
