package org.koitharu.kotatsu.reader.ui.pager.standard

import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.viewpager2.widget.ViewPager2
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import org.koitharu.kotatsu.core.util.ext.recyclerView

class PagerEventSupplier(private val pager: ViewPager2) : View.OnKeyListener {

	fun attach() {
		pager.recyclerView?.setOnKeyListener(this)
	}

	override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
		val rootView = pager.recyclerView?.findViewHolderForAdapterPosition(pager.currentItem)?.itemView as? ViewGroup
			?: return false
		return rootView.children.firstNotNullOfOrNull { x ->
			x as? SubsamplingScaleImageView
		}?.dispatchKeyEvent(event) == true
	}
}
