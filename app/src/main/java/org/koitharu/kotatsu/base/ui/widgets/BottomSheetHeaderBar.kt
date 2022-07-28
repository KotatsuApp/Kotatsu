package org.koitharu.kotatsu.base.ui.widgets

import android.animation.LayoutTransition
import android.content.Context
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.annotation.AttrRes
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.R as materialR
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.util.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.databinding.LayoutSheetHeaderBinding
import org.koitharu.kotatsu.utils.ext.getAnimationDuration
import org.koitharu.kotatsu.utils.ext.getThemeDrawable
import org.koitharu.kotatsu.utils.ext.parents

class BottomSheetHeaderBar @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = materialR.attr.appBarLayoutStyle,
) : AppBarLayout(context, attrs, defStyleAttr), MenuHost {

	private val binding = LayoutSheetHeaderBinding.inflate(LayoutInflater.from(context), this)
	private val closeDrawable = context.getThemeDrawable(materialR.attr.actionModeCloseDrawable)
	private val bottomSheetCallback = Callback()
	private var bottomSheetBehavior: BottomSheetBehavior<*>? = null
	private val locationBuffer = IntArray(2)
	private val expansionListeners = LinkedList<OnExpansionChangeListener>()
	private var fitStatusBar = false
	private var transition: AutoTransition? = null

	@Deprecated("")
	val toolbar: MaterialToolbar
		get() = binding.toolbar

	var title: CharSequence?
		get() = binding.toolbar.title
		set(value) {
			binding.toolbar.title = value
		}

	var subtitle: CharSequence?
		get() = binding.toolbar.subtitle
		set(value) {
			binding.toolbar.subtitle = value
		}

	init {
		setBackgroundResource(R.drawable.sheet_toolbar_background)
		layoutTransition = LayoutTransition().apply {
			setDuration(context.getAnimationDuration(R.integer.config_tinyAnimTime))
		}
		context.withStyledAttributes(attrs, R.styleable.BottomSheetHeaderBar, defStyleAttr) {
			binding.toolbar.title = getString(R.styleable.BottomSheetHeaderBar_title)
			fitStatusBar = getBoolean(R.styleable.BottomSheetHeaderBar_fitStatusBar, fitStatusBar)
			val menuResId = getResourceId(R.styleable.BottomSheetHeaderBar_menu, 0)
			if (menuResId != 0) {
				binding.toolbar.inflateMenu(menuResId)
			}
		}
		binding.toolbar.setNavigationOnClickListener(bottomSheetCallback)
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		setBottomSheetBehavior(findParentBottomSheetBehavior())
	}

	override fun onDetachedFromWindow() {
		setBottomSheetBehavior(null)
		super.onDetachedFromWindow()
	}

	override fun addView(child: View?, index: Int) {
		if (shouldAddView(child)) {
			super.addView(child, index)
		} else {
			binding.toolbar.addView(child, index)
		}
	}

	override fun addView(child: View?, width: Int, height: Int) {
		if (shouldAddView(child)) {
			super.addView(child, width, height)
		} else {
			binding.toolbar.addView(child, width, height)
		}
	}

	override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
		if (shouldAddView(child)) {
			super.addView(child, index, params)
		} else {
			binding.toolbar.addView(child, index, convertLayoutParams(params))
		}
	}

	override fun onApplyWindowInsets(insets: WindowInsets?): WindowInsets {
		dispatchInsets(if (insets != null) WindowInsetsCompat.toWindowInsetsCompat(insets) else null)
		return super.onApplyWindowInsets(insets)
	}

	override fun addMenuProvider(provider: MenuProvider) {
		binding.toolbar.addMenuProvider(provider)
	}

	override fun addMenuProvider(provider: MenuProvider, owner: LifecycleOwner) {
		binding.toolbar.addMenuProvider(provider, owner)
	}

	override fun addMenuProvider(provider: MenuProvider, owner: LifecycleOwner, state: Lifecycle.State) {
		binding.toolbar.addMenuProvider(provider, owner, state)
	}

	override fun removeMenuProvider(provider: MenuProvider) {
		binding.toolbar.removeMenuProvider(provider)
	}

	override fun invalidateMenu() {
		binding.toolbar.invalidateMenu()
	}

	fun setNavigationOnClickListener(onClickListener: OnClickListener) {
		binding.toolbar.setNavigationOnClickListener(onClickListener)
	}

	fun addOnExpansionChangeListener(listener: OnExpansionChangeListener) {
		expansionListeners.add(listener)
	}

	fun removeOnExpansionChangeListener(listener: OnExpansionChangeListener) {
		expansionListeners.remove(listener)
	}

	private fun setBottomSheetBehavior(behavior: BottomSheetBehavior<*>?) {
		bottomSheetBehavior?.removeBottomSheetCallback(bottomSheetCallback)
		bottomSheetBehavior = behavior
		if (behavior != null) {
			onBottomSheetStateChanged(behavior.state)
			behavior.addBottomSheetCallback(bottomSheetCallback)
		}
	}

	private fun onBottomSheetStateChanged(newState: Int) {
		val isExpanded = newState == BottomSheetBehavior.STATE_EXPANDED && isOnTopOfScreen()
		if (isExpanded == binding.dragHandle.isGone) {
			return
		}
		TransitionManager.beginDelayedTransition(this, getTransition())
		binding.toolbar.navigationIcon = (if (isExpanded) closeDrawable else null)
		binding.dragHandle.isGone = isExpanded
		expansionListeners.forEach { it.onExpansionStateChanged(this, isExpanded) }
		dispatchInsets(ViewCompat.getRootWindowInsets(this))
	}

	private fun dispatchInsets(insets: WindowInsetsCompat?) {
		if (!fitStatusBar) {
			return
		}
		val isExpanded = binding.dragHandle.isGone
		if (isExpanded) {
			val topInset = insets?.getInsets(WindowInsetsCompat.Type.systemBars())?.top ?: 0
			updatePadding(top = topInset)
		} else {
			updatePadding(top = 0)
		}
	}

	private fun findParentBottomSheetBehavior(): BottomSheetBehavior<*>? {
		for (p in parents) {
			val layoutParams = (p as? View)?.layoutParams
			if (layoutParams is CoordinatorLayout.LayoutParams) {
				val behavior = layoutParams.behavior
				if (behavior is BottomSheetBehavior<*>) {
					return behavior
				}
			}
		}
		return null
	}

	private fun isOnTopOfScreen(): Boolean {
		getLocationInWindow(locationBuffer)
		val topInset = ViewCompat.getRootWindowInsets(this)
			?.getInsets(WindowInsetsCompat.Type.systemBars())?.top ?: 0
		val zeroTop = (layoutParams as? MarginLayoutParams)?.topMargin ?: 0
		return (locationBuffer[1] - topInset) <= zeroTop
	}

	private fun dismissBottomSheet() {
		val behavior = bottomSheetBehavior ?: return
		if (behavior.isHideable) {
			behavior.state = BottomSheetBehavior.STATE_HIDDEN
		} else {
			behavior.state = BottomSheetBehavior.STATE_COLLAPSED
		}
	}

	private fun shouldAddView(child: View?): Boolean {
		if (child == null) {
			return true
		}
		val viewId = child.id
		return viewId == R.id.dragHandle || viewId == R.id.toolbar
	}

	private fun convertLayoutParams(params: ViewGroup.LayoutParams?): Toolbar.LayoutParams? {
		return when (params) {
			null -> null
			is MarginLayoutParams -> {
				val lp = Toolbar.LayoutParams(params)
				if (params is LayoutParams) {
					lp.gravity = params.gravity
				}
				lp
			}
			else -> Toolbar.LayoutParams(params)
		}
	}

	private fun getTransition(): AutoTransition {
		transition?.let { return it }
		val t = AutoTransition()
		t.duration = context.getAnimationDuration(R.integer.config_tinyAnimTime)
		// t.interpolator = AccelerateDecelerateInterpolator()
		transition = t
		return t
	}

	private inner class Callback : BottomSheetBehavior.BottomSheetCallback(), View.OnClickListener {

		override fun onStateChanged(bottomSheet: View, newState: Int) {
			onBottomSheetStateChanged(newState)
		}

		override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit

		override fun onClick(v: View?) {
			dismissBottomSheet()
		}
	}

	fun interface OnExpansionChangeListener {

		fun onExpansionStateChanged(headerBar: BottomSheetHeaderBar, isExpanded: Boolean)
	}
}
