package org.koitharu.kotatsu.base.ui.util

import androidx.appcompat.view.ActionMode

interface ActionModeListener {

	fun onActionModeStarted(mode: ActionMode)

	fun onActionModeFinished(mode: ActionMode)
}