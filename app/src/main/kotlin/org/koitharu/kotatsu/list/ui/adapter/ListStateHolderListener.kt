package org.koitharu.kotatsu.list.ui.adapter

interface ListStateHolderListener {

	fun onRetryClick(error: Throwable)
	
	fun onEmptyActionClick()
}