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
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.postDelayed
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.snackbar.Snackbar
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.databinding.FadingSnackbarLayoutBinding
import org.koitharu.kotatsu.utils.ext.getThemeColorStateList
import com.google.android.material.R as materialR

private const val SHORT_DURATION_MS = 1_500L
private const val LONG_DURATION_MS = 2_750L

/**
 * A custom snackbar implementation allowing more control over placement and entry/exit animations.
 *
 * Xtimms: Well, my sufferings over the Snackbar in [DetailsActivity] will go away forever... Thanks, Google.
 *
 * https://github.com/google/iosched/blob/main/mobile/src/main/java/com/google/samples/apps/iosched/widget/FadingSnackbar.kt
 */
class FadingSnackbar @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

	private val binding = FadingSnackbarLayoutBinding.inflate(LayoutInflater.from(context), this)
	private val enterDuration = context.resources.getInteger(R.integer.config_defaultAnimTime).toLong()
	private val exitDuration = context.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

	init {
		binding.snackbarLayout.background = createThemedBackground()
	}

	fun dismiss() {
		if (visibility == VISIBLE && alpha == 1f) {
			animate()
				.alpha(0f)
				.withEndAction { visibility = GONE }
				.duration = exitDuration
		}
	}

	fun show(
		messageText: CharSequence?,
		@StringRes actionId: Int = 0,
		duration: Int = Snackbar.LENGTH_SHORT,
		onActionClick: (FadingSnackbar.() -> Unit)? = null,
		onDismiss: (() -> Unit)? = null,
	) {
		binding.snackbarText.text = messageText
		if (actionId != 0) {
			with(binding.snackbarAction) {
				visibility = VISIBLE
				text = context.getString(actionId)
				setOnClickListener {
					onActionClick?.invoke(this@FadingSnackbar) ?: dismiss()
				}
			}
		} else {
			binding.snackbarAction.visibility = GONE
		}
		alpha = 0f
		visibility = VISIBLE
		animate()
			.alpha(1f)
			.duration = enterDuration
		if (duration == Snackbar.LENGTH_INDEFINITE) {
			return
		}
		val durationMs = enterDuration + if (duration == Snackbar.LENGTH_LONG) LONG_DURATION_MS else SHORT_DURATION_MS
		postDelayed(durationMs) {
			dismiss()
			onDismiss?.invoke()
		}
	}

	private fun createThemedBackground(): Drawable {
		val backgroundColor = MaterialColors.layer(this, materialR.attr.colorSurface, materialR.attr.colorOnSurface, 1f)
		val shapeAppearanceModel = ShapeAppearanceModel.builder(
			context,
			materialR.style.ShapeAppearance_Material3_Corner_ExtraSmall,
			0
		).build()
		val background = createMaterialShapeDrawableBackground(
			backgroundColor,
			shapeAppearanceModel,
		)
		val backgroundTint = context.getThemeColorStateList(materialR.attr.colorSurfaceInverse)
		return if (backgroundTint != null) {
			val wrappedDrawable = DrawableCompat.wrap(background)
			DrawableCompat.setTintList(wrappedDrawable, backgroundTint)
			wrappedDrawable
		} else {
			DrawableCompat.wrap(background)
		}
	}

	private fun createMaterialShapeDrawableBackground(
		@ColorInt backgroundColor: Int,
		shapeAppearanceModel: ShapeAppearanceModel,
	): MaterialShapeDrawable {
		val background = MaterialShapeDrawable(shapeAppearanceModel)
		background.fillColor = ColorStateList.valueOf(backgroundColor)
		return background
	}
}