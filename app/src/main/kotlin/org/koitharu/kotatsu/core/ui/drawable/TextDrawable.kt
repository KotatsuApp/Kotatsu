package org.koitharu.kotatsu.core.ui.drawable

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.annotation.StyleRes
import androidx.core.graphics.withTranslation
import com.google.android.material.resources.TextAppearance
import com.google.android.material.resources.TextAppearanceFontCallback
import org.koitharu.kotatsu.core.util.ext.getThemeColor

class TextDrawable(
	val text: CharSequence,
) : Drawable() {

	private val paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
	private var cachedLayout: StaticLayout? = null

	@SuppressLint("RestrictedApi")
	constructor(context: Context, text: CharSequence, @StyleRes textAppearanceId: Int) : this(text) {
		val ta = TextAppearance(context, textAppearanceId)
		paint.color = ta.textColor?.defaultColor ?: context.getThemeColor(android.R.attr.textColorPrimary, Color.BLACK)
		paint.typeface = ta.fallbackFont
		ta.getFontAsync(
			context, paint,
			object : TextAppearanceFontCallback() {
				override fun onFontRetrieved(typeface: Typeface?, fontResolvedSynchronously: Boolean) = Unit
				override fun onFontRetrievalFailed(reason: Int) = Unit
			},
		)
		paint.letterSpacing = ta.letterSpacing
	}

	var alignment = Layout.Alignment.ALIGN_NORMAL

	var lineSpacingMultiplier = 1f

	@Px
	var lineSpacingExtra = 0f

	@get:ColorInt
	var textColor: Int
		get() = paint.color
		set(@ColorInt value) {
			paint.color = value
		}

	override fun draw(canvas: Canvas) {
		val b = bounds
		if (b.isEmpty) {
			return
		}
		canvas.withTranslation(x = b.left.toFloat(), y = b.top.toFloat()) {
			obtainLayout().draw(canvas)
		}
	}

	override fun setAlpha(alpha: Int) {
		paint.alpha = alpha
	}

	override fun setColorFilter(colorFilter: ColorFilter?) {
		paint.setColorFilter(colorFilter)
	}

	@Suppress("DeprecatedCallableAddReplaceWith")
	@Deprecated("Deprecated in Java")
	override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

	private fun obtainLayout(): StaticLayout {
		val width = bounds.width()
		cachedLayout?.let {
			if (it.width == width) {
				return it
			}
		}
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
				.setAlignment(alignment)
				.setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
				.setIncludePad(true)
				.build()
		} else {
			@Suppress("DEPRECATION")
			StaticLayout(text, paint, width, alignment, lineSpacingMultiplier, lineSpacingExtra, true)
		}.also { cachedLayout = it }
	}
}
