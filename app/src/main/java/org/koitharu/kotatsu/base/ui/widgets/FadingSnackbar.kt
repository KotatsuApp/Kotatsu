/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
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
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.postDelayed
import org.koitharu.kotatsu.R

/**
 * A custom snackbar implementation allowing more control over placement and entry/exit animations.
 *
 * Xtimms: Well, my sufferings over the Snackbar in [DetailsActivity] will go away forever... Thanks, Google.
 *
 * https://github.com/google/iosched/blob/main/mobile/src/main/java/com/google/samples/apps/iosched/widget/FadingSnackbar.kt
 */
class FadingSnackbar @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet,
	defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

	private val message: TextView
	private val action: Button

	init {
		val view = LayoutInflater.from(context).inflate(R.layout.fading_snackbar_layout, this, true)
		message = view.findViewById(R.id.snackbar_text)
		action = view.findViewById(R.id.snackbar_action)
	}

	fun dismiss() {
		if (visibility == VISIBLE && alpha == 1f) {
			animate()
				.alpha(0f)
				.withEndAction { visibility = GONE }
				.duration = EXIT_DURATION
		}
	}

	fun show(
		messageText: CharSequence? = null,
		@StringRes actionId: Int? = null,
		longDuration: Boolean = true,
		actionClick: () -> Unit = { dismiss() },
		dismissListener: () -> Unit = { }
	) {
		message.text = messageText
		if (actionId != null) {
			action.run {
				visibility = VISIBLE
				text = context.getString(actionId)
				setOnClickListener {
					actionClick()
				}
			}
		} else {
			action.visibility = GONE
		}
		alpha = 0f
		visibility = VISIBLE
		animate()
			.alpha(1f)
			.duration = ENTER_DURATION
		val showDuration = ENTER_DURATION + if (longDuration) LONG_DURATION else SHORT_DURATION
		postDelayed(showDuration) {
			dismiss()
			dismissListener()
		}
	}

	companion object {
		private const val ENTER_DURATION = 300L
		private const val EXIT_DURATION = 200L
		private const val SHORT_DURATION = 1_500L
		private const val LONG_DURATION = 2_750L
	}
}