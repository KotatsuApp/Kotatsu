package org.koitharu.kotatsu.list.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.list.ui.model.IndeterminateProgress

fun indeterminateProgressAD() = adapterDelegate<IndeterminateProgress, Any>(R.layout.item_progress) {
}