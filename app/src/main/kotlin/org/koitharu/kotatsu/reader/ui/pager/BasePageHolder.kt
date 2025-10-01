package org.koitharu.kotatsu.reader.ui.pager

import android.content.ComponentCallbacks2
import android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE
import android.content.Context
import android.content.res.Configuration
import android.graphics.RectF
import android.view.View
import androidx.annotation.CallSuper
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.davemorrissey.labs.subscaleview.DefaultOnImageEventListener
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.ExceptionResolver
import org.koitharu.kotatsu.core.os.NetworkState
import org.koitharu.kotatsu.core.ui.list.lifecycle.LifecycleAwareViewHolder
import org.koitharu.kotatsu.core.util.ext.getDisplayMessage
import org.koitharu.kotatsu.core.util.ext.isLowRamDevice
import org.koitharu.kotatsu.core.util.ext.isSerializable
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.databinding.LayoutPageInfoBinding
import org.koitharu.kotatsu.parsers.util.ifZero
import org.koitharu.kotatsu.reader.domain.PageLoader
import org.koitharu.kotatsu.reader.domain.panel.Panel
import org.koitharu.kotatsu.reader.domain.panel.PanelPreferences
import org.koitharu.kotatsu.reader.ui.config.ReaderSettings
import org.koitharu.kotatsu.reader.ui.panel.PanelHighlightListener
import org.koitharu.kotatsu.reader.ui.panel.PanelHighlightOverlay
import org.koitharu.kotatsu.reader.ui.panel.PanelZoomConfig
import org.koitharu.kotatsu.reader.ui.panel.PanelZoomController
import org.koitharu.kotatsu.reader.ui.pager.vm.PageState
import org.koitharu.kotatsu.reader.ui.pager.vm.PageViewModel
import org.koitharu.kotatsu.reader.ui.pager.webtoon.WebtoonHolder
import kotlin.math.roundToInt

abstract class BasePageHolder<B : ViewBinding>(
    protected val binding: B,
    loader: PageLoader,
    readerSettingsProducer: ReaderSettings.Producer,
    networkState: NetworkState,
    exceptionResolver: ExceptionResolver,
    lifecycleOwner: LifecycleOwner,
) : LifecycleAwareViewHolder(binding.root, lifecycleOwner), DefaultOnImageEventListener, ComponentCallbacks2 {

    protected val viewModel = PageViewModel(
        loader = loader,
        settingsProducer = readerSettingsProducer,
        networkState = networkState,
        exceptionResolver = exceptionResolver,
        isWebtoon = this is WebtoonHolder,
    )
    protected val bindingInfo = LayoutPageInfoBinding.bind(binding.root)
    protected abstract val ssiv: SubsamplingScaleImageView

    private var panelController: PanelZoomController? = null
    private var panelCollectionJob: Job? = null
    private var panelConfig: PanelZoomConfig = PanelZoomConfig()
    private var isHighlightEnabled: Boolean = true
    private var panelJobHighlightEnabled: Boolean = true
    private val panelHighlightListener = object : PanelHighlightListener {
        override fun onPanelFocused(panel: Panel, viewRect: RectF) {
            panelOverlay()?.show(viewRect)
        }

        override fun onPanelCleared() {
            panelOverlay()?.hide()
        }
    }

    protected val settings: ReaderSettings
        get() = viewModel.settingsProducer.value

    val context: Context
        get() = itemView.context

    var boundData: ReaderPage? = null
        private set

    init {
        lifecycleScope.launch(Dispatchers.Main) {
            ssiv.bindToLifecycle(this@BasePageHolder)
            ssiv.isEagerLoadingEnabled = !context.isLowRamDevice()
            ssiv.addOnImageEventListener(viewModel)
            ssiv.addOnImageEventListener(this@BasePageHolder)
        }
        val clickListener = View.OnClickListener { v ->
            when (v.id) {
                R.id.button_retry -> viewModel.retry(
                    boundData ?: return@OnClickListener,
                    isFromUser = true,
                )

                R.id.button_error_details -> viewModel.showErrorDetails(boundData?.url)
            }
        }
        bindingInfo.buttonRetry.setOnClickListener(clickListener)
        bindingInfo.buttonErrorDetails.setOnClickListener(clickListener)
    }

    @CallSuper
    protected open fun onConfigChanged(settings: ReaderSettings) {
        settings.applyBackground(itemView)
        if (settings.applyBitmapConfig(ssiv)) {
            reloadImage()
        } else if (viewModel.state.value is PageState.Shown) {
            onReady()
        }
        ssiv.applyDownSampling(isResumed())
        configurePanelMode(settings)
    }

    fun reloadImage() {
        val source = (viewModel.state.value as? PageState.Shown)?.source ?: return
        ssiv.setImage(source)
    }

    fun bind(data: ReaderPage) {
        boundData = data
        viewModel.onBind(data)
        onBind(data)
    }

    @CallSuper
    protected open fun onBind(data: ReaderPage) = Unit

    override fun onCreate() {
        super.onCreate()
        context.registerComponentCallbacks(this)
        viewModel.state.observe(this, ::onStateChanged)
        viewModel.settingsProducer.observe(this, ::onConfigChanged)
    }

    override fun onResume() {
        super.onResume()
        ssiv.applyDownSampling(isForeground = true)
        if (viewModel.state.value is PageState.Error && !viewModel.isLoading()) {
            boundData?.let { viewModel.retry(it, isFromUser = false) }
        }
    }

    override fun onPause() {
        super.onPause()
        ssiv.applyDownSampling(isForeground = false)
    }

    override fun onDestroy() {
        context.unregisterComponentCallbacks(this)
        clearPanelMode()
        super.onDestroy()
    }

    open fun onAttachedToWindow() = Unit

    open fun onDetachedFromWindow() = Unit

    @CallSuper
    open fun onRecycled() {
        viewModel.onRecycle()
        clearPanelMode()
        ssiv.recycle()
    }

    override fun onTrimMemory(level: Int) {
        // TODO
    }

    override fun onConfigurationChanged(newConfig: Configuration) = Unit

    @Deprecated("Deprecated in Java")
    final override fun onLowMemory() = onTrimMemory(TRIM_MEMORY_COMPLETE)

    protected open fun onStateChanged(state: PageState) {
        bindingInfo.layoutError.isVisible = state is PageState.Error
        bindingInfo.layoutProgress.isGone = state.isFinalState()
        val progress = (state as? PageState.Loading)?.progress ?: -1
        if (progress in 0..100) {
            bindingInfo.progressBar.isIndeterminate = false
            bindingInfo.progressBar.setProgressCompat(progress, true)
            bindingInfo.textViewStatus.text = context.getString(R.string.percent_string_pattern, progress.toString())
        } else {
            bindingInfo.progressBar.isIndeterminate = true
            bindingInfo.textViewStatus.setText(R.string.loading_)
        }
        when (state) {
            is PageState.Converting -> {
                bindingInfo.textViewStatus.setText(R.string.processing_)
            }

            is PageState.Empty -> Unit

            is PageState.Error -> {
                val e = state.error
                bindingInfo.textViewError.text = e.getDisplayMessage(context.resources)
                bindingInfo.buttonRetry.setText(
                    ExceptionResolver.getResolveStringId(e).ifZero { R.string.try_again },
                )
                bindingInfo.buttonErrorDetails.isVisible = e.isSerializable()
                bindingInfo.layoutError.isVisible = true
                bindingInfo.progressBar.hide()
            }

            is PageState.Loaded -> {
                bindingInfo.textViewStatus.setText(R.string.preparing_)
                ssiv.setImage(state.source)
            }

            is PageState.Loading -> {
                if (state.preview != null && ssiv.getState() == null) {
                    ssiv.setImage(state.preview)
                }
            }

            is PageState.Shown -> Unit
        }
    }

    protected fun SubsamplingScaleImageView.applyDownSampling(isForeground: Boolean) {
        downSampling = when {
            isForeground || !settings.isReaderOptimizationEnabled -> 1
            BuildConfig.DEBUG -> 32
            context.isLowRamDevice() -> 8
            else -> 4
        }
    }

    protected open fun panelOverlay(): PanelHighlightOverlay? = null

    private fun configurePanelMode(settings: ReaderSettings) {
        val preferences = settings.panelPreferences
        val overlay = panelOverlay()
        overlay?.applyPreferences(preferences)

        val isPanelEnabled = settings.isPanelViewEnabled
        val highlightEnabled = isPanelEnabled && !preferences.disableFrame

        if (!highlightEnabled) {
            overlay?.hide()
        }

        ssiv.setPanLimit(
            if (isPanelEnabled && preferences.panBound) {
                SubsamplingScaleImageView.PAN_LIMIT_INSIDE
            } else {
                SubsamplingScaleImageView.PAN_LIMIT_OUTSIDE
            },
        )

        if (!isPanelEnabled) {
            clearPanelMode()
            return
        }

        val desiredConfig = buildPanelConfig(preferences)
        if (panelController == null || panelConfig != desiredConfig || isHighlightEnabled != highlightEnabled) {
            panelController?.dispose()
            panelController = PanelZoomController(
                imageView = ssiv,
                highlighter = if (highlightEnabled) panelHighlightListener else null,
                config = desiredConfig,
            )
            panelConfig = desiredConfig
            isHighlightEnabled = highlightEnabled
        }

        val shouldRestartJob = panelCollectionJob?.isActive != true || panelJobHighlightEnabled != highlightEnabled
        if (shouldRestartJob) {
            panelCollectionJob?.cancel()
            panelJobHighlightEnabled = highlightEnabled
            val overlayRef = overlay
            panelCollectionJob = lifecycleScope.launch(Dispatchers.Main.immediate) {
                viewModel.panelState.collectLatest { result ->
                    panelController?.setResult(result)
                    val hasPanels = result?.primary?.panels?.isNotEmpty() == true
                    if (hasPanels) {
                        panelController?.focus(0, animate = false)
                    } else if (highlightEnabled) {
                        overlayRef?.hide()
                    }
                }
            }
        }
    }

    private fun clearPanelMode() {
        panelCollectionJob?.cancel()
        panelCollectionJob = null
        panelController?.dispose()
        panelController = null
    }

    private fun buildPanelConfig(preferences: PanelPreferences): PanelZoomConfig {
        val density = context.resources.displayMetrics.density
        val paddingDp = if (preferences.fitToWidth) PANEL_PADDING_FIT_DP else PANEL_PADDING_DEFAULT_DP
        val paddingPx = maxOf(0, (density * paddingDp).roundToInt())
        val multiplier = if (preferences.fitToWidth) 1f else DEFAULT_SCALE_MULTIPLIER
        return PanelZoomConfig(
            viewportPaddingPx = paddingPx,
            scaleMultiplier = multiplier,
            animationDurationMs = PANEL_ANIMATION_DURATION_MS,
            minScale = null,
            maxScale = null,
        )
    }

    private companion object {
        private const val PANEL_PADDING_DEFAULT_DP = 24f
        private const val PANEL_PADDING_FIT_DP = 12f
        private const val DEFAULT_SCALE_MULTIPLIER = 1.05f
        private const val PANEL_ANIMATION_DURATION_MS = 220L
    }
}

