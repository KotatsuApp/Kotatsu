package org.koitharu.kotatsu.scrobbling.common.domain.model

import org.koitharu.kotatsu.list.ui.model.ListModel

enum class ScrobblingStatus : ListModel {

	PLANNED,
	READING,
	RE_READING,
	COMPLETED,
	ON_HOLD,
	DROPPED,
}
