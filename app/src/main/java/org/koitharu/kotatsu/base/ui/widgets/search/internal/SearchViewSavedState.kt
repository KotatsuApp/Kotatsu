/*https://github.com/lapism/search*/

package org.koitharu.kotatsu.base.ui.widgets.search.internal

import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import android.view.View

internal class SearchViewSavedState(superState: Parcelable) : View.BaseSavedState(superState) {

	var query: CharSequence? = null
	var hasFocus: Boolean = false

	override fun writeToParcel(out: Parcel, flags: Int) {
		super.writeToParcel(out, flags)
		TextUtils.writeToParcel(query, out, flags)
		out.writeInt(if (hasFocus) 1 else 0)
	}

}