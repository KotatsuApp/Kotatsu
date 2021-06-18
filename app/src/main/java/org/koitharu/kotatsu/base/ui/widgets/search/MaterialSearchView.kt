/*https://github.com/lapism/search*/

package org.koitharu.kotatsu.base.ui.widgets.search

import android.animation.LayoutTransition
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.widgets.search.internal.SearchLayout

class MaterialSearchView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0
) : SearchLayout(context, attrs, defStyleAttr, defStyleRes), CoordinatorLayout.AttachedBehavior {

	// *********************************************************************************************
	private var mBehavior: CoordinatorLayout.Behavior<*> = SearchBehavior<MaterialSearchView>()
	private var mTransition: LayoutTransition? = null
	private var mStrokeWidth: Int = 0
	private var mRadius: Float = 0f
	private var mElevation: Float = 0f

	// *********************************************************************************************
	init {
		inflate(context, R.layout.layout_search_view, this)
		init()
		setTransition()

		val a = context.obtainStyledAttributes(
			attrs, R.styleable.MaterialSearchView, defStyleAttr, defStyleRes
		)

		if (a.hasValue(R.styleable.MaterialSearchView_search_navigationIconSupport)) {
			navigationIconSupport = a.getInt(
				R.styleable.MaterialSearchView_search_navigationIconSupport,
				NavigationIconSupport.NONE
			)
		}

		if (a.hasValue(R.styleable.MaterialSearchView_search_navigationIcon)) {
			setNavigationIconImageDrawable(a.getDrawable(R.styleable.MaterialSearchView_search_navigationIcon))
		}

		if (a.hasValue(R.styleable.MaterialSearchView_search_clearIcon)) {
			setClearIconImageDrawable(a.getDrawable(R.styleable.MaterialSearchView_search_clearIcon))
		} else {
			setClearIconImageDrawable(
				ContextCompat.getDrawable(
					context,
					R.drawable.ic_clear
				)
			)
		}

		if (a.hasValue(R.styleable.MaterialSearchView_search_micIcon)) {
			setMicIconImageDrawable(a.getDrawable(R.styleable.MaterialSearchView_search_micIcon))
		} else {
			setMicIconImageDrawable(
				ContextCompat.getDrawable(
					context,
					R.drawable.ic_mic_none
				)
			)
		}

		if (a.hasValue(R.styleable.MaterialSearchView_search_menuIcon)) {
			setMicIconImageDrawable(a.getDrawable(R.styleable.MaterialSearchView_search_menuIcon))
		} else {
			setMicIconImageDrawable(
				ContextCompat.getDrawable(
					context,
					R.drawable.ic_more
				)
			)
		}

		if (a.hasValue(R.styleable.MaterialSearchView_search_dividerColor)) {
			setDividerColor(a.getInt(R.styleable.MaterialSearchView_search_dividerColor, 0))
		}

		val defaultShadowColor = ContextCompat.getColor(context, R.color.shadow)
		setShadowColor(
			a.getInt(
				R.styleable.MaterialSearchView_search_shadowColor,
				defaultShadowColor
			)
		)

		if (a.hasValue(R.styleable.MaterialSearchView_search_textHint)) {
			setTextHint(a.getText(R.styleable.MaterialSearchView_search_textHint))
		}

		if (a.hasValue(R.styleable.MaterialSearchView_search_strokeColor)) {
			setBackgroundStrokeColor(a.getInt(R.styleable.MaterialSearchView_search_strokeColor, 0))
		}

		if (a.hasValue(R.styleable.MaterialSearchView_search_strokeWidth)) {
			setBackgroundStrokeWidth(a.getInt(R.styleable.MaterialSearchView_search_strokeWidth, 0))
		}

		val defaultTransitionDuration =
			context.resources.getInteger(R.integer.search_animation_duration)
		setTransitionDuration(
			a.getInt(
				R.styleable.MaterialSearchView_search_transitionDuration,
				defaultTransitionDuration
			).toLong()
		)

		val defaultRadius = context.resources.getDimensionPixelSize(R.dimen.search_radius)
		setBackgroundRadius(
			a.getInt(R.styleable.MaterialSearchView_search_radius, defaultRadius).toFloat()
		)

		val defaultElevation = context.resources.getDimensionPixelSize(R.dimen.search_elevation)
		elevation =
			a.getInt(R.styleable.MaterialSearchView_android_elevation, defaultElevation).toFloat()

		val imeOptions = a.getInt(R.styleable.MaterialSearchView_android_imeOptions, -1)
		if (imeOptions != -1) {
			setTextImeOptions(imeOptions)
		}

		val inputType = a.getInt(R.styleable.MaterialSearchView_android_inputType, -1)
		if (inputType != -1) {
			setTextInputType(inputType)
		}

		a.recycle()
	}

	// *********************************************************************************************
	override fun addFocus() {
		mOnFocusChangeListener?.onFocusChange(true)
		showKeyboard()

		mStrokeWidth = getBackgroundStrokeWidth()
		mRadius = getBackgroundRadius()
		mElevation = elevation

		setBackgroundStrokeWidth(context.resources.getDimensionPixelSize(R.dimen.search_stroke_width_focus))
		setBackgroundRadius(resources.getDimensionPixelSize(R.dimen.search_radius_focus).toFloat())
		elevation =
			context.resources.getDimensionPixelSize(R.dimen.search_elevation_focus).toFloat()

		val left = context.resources.getDimensionPixelSize(R.dimen.search_dp_16)
		val params = mSearchEditText?.layoutParams as LinearLayout.LayoutParams
		params.setMargins(left, 0, 0, 0)
		mSearchEditText?.layoutParams = params

		margins = Margins.FOCUS
		setLayoutHeight(context.resources.getDimensionPixelSize(R.dimen.search_layout_height_focus))

		mViewShadow?.visibility = View.VISIBLE

		mViewDivider?.visibility = View.VISIBLE
		mViewAnim?.visibility = View.VISIBLE
		mRecyclerView?.visibility = View.VISIBLE

		// layoutTransition = null
	}

	override fun removeFocus() {
		// layoutTransition = mTransition

		mOnFocusChangeListener?.onFocusChange(false)
		hideKeyboard()

		val params = mSearchEditText?.layoutParams as LinearLayout.LayoutParams
		params.setMargins(0, 0, 0, 0)
		mSearchEditText?.layoutParams = params

		setBackgroundStrokeWidth(mStrokeWidth)
		setBackgroundRadius(mRadius)
		elevation = mElevation

		setLayoutHeight(context.resources.getDimensionPixelSize(R.dimen.search_layout_height))
		margins = Margins.NO_FOCUS

		mViewShadow?.visibility = View.GONE

		mRecyclerView?.visibility = View.GONE
		mViewAnim?.visibility = View.GONE
		mViewDivider?.visibility = View.GONE
	}

	override fun getBehavior(): CoordinatorLayout.Behavior<*> {
		return mBehavior
	}

	fun setBehavior(behavior: CoordinatorLayout.Behavior<*>) {
		mBehavior = behavior
	}

	fun setTransitionDuration(duration: Long) {
		mTransition?.setDuration(duration)
		layoutTransition = mTransition
	}

	private fun setTransition() {
		mTransition = LayoutTransition()
		mTransition?.enableTransitionType(LayoutTransition.CHANGING)
		mTransition?.addTransitionListener(object : LayoutTransition.TransitionListener {
			override fun startTransition(
				transition: LayoutTransition?,
				container: ViewGroup?,
				view: View?,
				transitionType: Int
			) {

			}

			override fun endTransition(
				transition: LayoutTransition?,
				container: ViewGroup?,
				view: View?,
				transitionType: Int
			) {

			}
		})
	}

}