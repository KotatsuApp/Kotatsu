package org.koitharu.kotatsu.explore.ui.adapter

import android.view.View
import org.koitharu.kotatsu.list.ui.adapter.ListStateHolderListener

interface SourcesHeaderEventListener : ListStateHolderListener {

	fun onManageClick(view: View)

}