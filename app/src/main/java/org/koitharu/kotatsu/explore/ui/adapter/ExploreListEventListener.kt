package org.koitharu.kotatsu.explore.ui.adapter

import android.view.View
import org.koitharu.kotatsu.list.ui.adapter.ListStateHolderListener

interface ExploreListEventListener : ListStateHolderListener, View.OnClickListener {

	fun onManageClick(view: View)
}