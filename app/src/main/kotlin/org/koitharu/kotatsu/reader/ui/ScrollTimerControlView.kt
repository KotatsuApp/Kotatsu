package org.koitharu.kotatsu.reader.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.transition.Slide
import androidx.transition.TransitionManager
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ReaderMode
import org.koitharu.kotatsu.core.prefs.observeAsStateFlow
import org.koitharu.kotatsu.core.util.ext.isAnimationsEnabled
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.parentView
import org.koitharu.kotatsu.databinding.ViewScrollTimerBinding
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class ScrollTimerControlView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs), CompoundButton.OnCheckedChangeListener, Slider.OnChangeListener,
	View.OnClickListener, LabelFormatter {

	@Inject
	lateinit var settings: AppSettings

	var onVisibilityChangeListener: OnVisibilityChangeListener? = null

	private val binding = ViewScrollTimerBinding.inflate(LayoutInflater.from(context), this)

	private var scrollTimer: ScrollTimer? = null
	private var labelPattern = context.getString(R.string.speed_value)
	private var readerMode: ReaderMode = ReaderMode.STANDARD

	init {
		binding.switchScrollTimer.setOnCheckedChangeListener(this)
		binding.sliderTimer.addOnChangeListener(this)
		binding.buttonFab.setOnClickListener(this)
		binding.sliderTimer.setLabelFormatter(this)
		binding.buttonClose.setOnClickListener(this)
		binding.buttonFab.isGone = resources.getBoolean(R.bool.is_tablet)
		setPadding(0, 0, 0, context.resources.getDimensionPixelOffset(R.dimen.margin_normal))
	}

	fun attach(timer: ScrollTimer, lifecycleOwner: LifecycleOwner) {
		scrollTimer = timer
		timer.isActive.observe(lifecycleOwner) {
			binding.switchScrollTimer.setOnCheckedChangeListener(null)
			binding.switchScrollTimer.isChecked = it
			binding.switchScrollTimer.setOnCheckedChangeListener(this)
		}
		settings.observeAsStateFlow(
			scope = lifecycleOwner.lifecycleScope + Dispatchers.Default,
			key = AppSettings.KEY_READER_AUTOSCROLL_SPEED,
			valueProducer = { readerAutoscrollSpeed },
		).observe(lifecycleOwner) {
			if (abs(it - binding.sliderTimer.value) > 0.0001) {
				binding.sliderTimer.value = it.coerceIn(
					binding.sliderTimer.valueFrom,
					binding.sliderTimer.valueTo,
				)
			}
		}
		settings.observeAsStateFlow(
			scope = lifecycleOwner.lifecycleScope + Dispatchers.Default,
			key = AppSettings.KEY_READER_AUTOSCROLL_FAB,
			valueProducer = { isReaderAutoscrollFabVisible },
		).observe(lifecycleOwner) {
			binding.buttonFab.isChecked = it
		}
		updateDescription()
	}

	fun onReaderModeChanged(mode: ReaderMode) {
		readerMode = mode
		updateDescription()
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_close -> hide()
			R.id.button_fab -> settings.isReaderAutoscrollFabVisible = !settings.isReaderAutoscrollFabVisible
		}
	}

	override fun getFormattedValue(value: Float): String {
		val valueFrom = binding.sliderTimer.valueFrom
		val valueTo = binding.sliderTimer.valueTo
		val percent = (value - valueFrom) / (valueTo - valueFrom)
		return labelPattern.format(0.1 + percent * 10) // just something to display
	}

	override fun onValueChange(
		slider: Slider,
		value: Float,
		fromUser: Boolean
	) {
		if (fromUser) {
			settings.readerAutoscrollSpeed = value
		}
		updateDescription()
	}

	override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
		scrollTimer?.setActive(isChecked)
	}

	override fun setVisibility(visibility: Int) {
		super.setVisibility(visibility)
		onVisibilityChangeListener?.onVisibilityChanged(this, visibility)
	}

	fun show() {
		setupVisibilityTransition()
		isVisible = true
	}

	fun hide() {
		setupVisibilityTransition()
		isVisible = false
	}

	fun showOrHide() {
		setupVisibilityTransition()
		isVisible = !isVisible
	}

	private fun setupVisibilityTransition() {
		if (context.isAnimationsEnabled) {
			val sceneRoot = parentView ?: return
			val transition = Slide()
			transition.addTarget(this)
			TransitionManager.beginDelayedTransition(sceneRoot, transition)
		}
	}

	private fun updateDescription() {
		val timePerPage = scrollTimer?.pageSwitchDelay ?: 0L
		if (timePerPage <= 0L || readerMode == ReaderMode.WEBTOON) {
			binding.textViewDescription.isVisible = false
		} else {
			binding.textViewDescription.text = context.getString(
				R.string.page_switch_timer,
				TimeUnit.MILLISECONDS.toSeconds((scrollTimer ?: return).pageSwitchDelay),
			)
			binding.textViewDescription.isVisible = true
		}
	}

	fun interface OnVisibilityChangeListener {

		fun onVisibilityChanged(v: View, visibility: Int)
	}
}
