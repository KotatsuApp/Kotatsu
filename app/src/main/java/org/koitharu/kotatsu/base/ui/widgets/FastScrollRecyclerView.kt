/*
 * Copyright 2022 Randy Webster. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.koitharu.kotatsu.base.ui.widgets

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView
import org.koitharu.kotatsu.R

class FastScrollRecyclerView : RecyclerView {

	private val fastScroller: FastScroller

	constructor(context: Context) : super(context) {
		fastScroller = context.layout()
		layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
	}

	@JvmOverloads
	constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr) {
		fastScroller = context.layout(attrs)
	}

	override fun setAdapter(adapter: Adapter<*>?) = super.setAdapter(adapter).also {
		when (adapter) {
			is FastScroller.SectionIndexer -> fastScroller.setSectionIndexer(adapter)
			null -> fastScroller.setSectionIndexer(null)
		}
	}

	override fun setVisibility(visibility: Int) = super.setVisibility(visibility).also {
		fastScroller.visibility = visibility
	}

	fun setFastScrollListener(fastScrollListener: FastScroller.FastScrollListener?) =
		fastScroller.setFastScrollListener(fastScrollListener)

	fun setFastScrollEnabled(enabled: Boolean) {
		fastScroller.isEnabled = enabled
	}

	fun setHideScrollbar(hideScrollbar: Boolean) = fastScroller.setHideScrollbar(hideScrollbar)

	fun setTrackVisible(visible: Boolean) = fastScroller.setTrackVisible(visible)

	fun setTrackColor(@ColorInt color: Int) = fastScroller.setTrackColor(color)

	fun setHandleColor(@ColorInt color: Int) = fastScroller.setHandleColor(color)

	@JvmOverloads
	fun setBubbleVisible(visible: Boolean, always: Boolean = false) = fastScroller.setBubbleVisible(visible, always)

	fun setBubbleColor(@ColorInt color: Int) = fastScroller.setBubbleColor(color)

	fun setBubbleTextColor(@ColorInt color: Int) = fastScroller.setBubbleTextColor(color)

	fun setBubbleTextSize(size: Int) = fastScroller.setBubbleTextSize(size)

	override fun onAttachedToWindow() = super.onAttachedToWindow().also {
		fastScroller.attachRecyclerView(this)
	}

	override fun onDetachedFromWindow() {
		fastScroller.detachRecyclerView()
		super.onDetachedFromWindow()
	}

	private fun Context.layout(attrs: AttributeSet? = null) =
		FastScroller(this, attrs).apply { id = R.id.fast_scroller }
}