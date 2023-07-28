package org.koitharu.kotatsu.core.ui.widgets

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.Parcelable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristic
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.util.ext.draw
import org.koitharu.kotatsu.core.util.ext.getAnimationDuration
import org.koitharu.kotatsu.core.util.ext.resolveDp
import org.koitharu.kotatsu.core.util.ext.resolveSp

class PieChart @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), PieChartInterface {

	private var marginTextFirst: Float = context.resources.resolveDp(DEFAULT_MARGIN_TEXT_1)
	private var marginTextSecond: Float = context.resources.resolveDp(DEFAULT_MARGIN_TEXT_2)
	private var marginTextThird: Float = context.resources.resolveDp(DEFAULT_MARGIN_TEXT_3)
	private var marginSmallCircle: Float = context.resources.resolveDp(DEFAULT_MARGIN_SMALL_CIRCLE)
	private val marginText: Float = marginTextFirst + marginTextSecond
	private val circleRect = RectF()
	private var circleStrokeWidth: Float = context.resources.resolveDp(6f)
	private var circleRadius: Float = 0f
	private var circlePadding: Float = context.resources.resolveDp(8f)
	private var circlePaintRoundSize: Boolean = true
	private var circleSectionSpace: Float = 3f
	private var circleCenterX: Float = 0f
	private var circleCenterY: Float = 0f
	private var numberTextPaint: TextPaint = TextPaint()
	private var descriptionTextPain: TextPaint = TextPaint()
	private var amountTextPaint: TextPaint = TextPaint()
	private var textStartX: Float = 0f
	private var textStartY: Float = 0f
	private var textHeight: Int = 0
	private var textCircleRadius: Float = context.resources.resolveDp(4f)
	private var textAmountStr: String = ""
	private var textAmountY: Float = 0f
	private var textAmountXNumber: Float = 0f
	private var textAmountXDescription: Float = 0f
	private var textAmountYDescription: Float = 0f
	private var totalAmount: Int = 0
	private var pieChartColors: List<String> = listOf()
	private var percentageCircleList: List<PieChartModel> = listOf()
	private var textRowList: MutableList<StaticLayout> = mutableListOf()
	private var dataList: List<Pair<Int, String>> = listOf()
	private var animationSweepAngle: Int = 0

	init {
		var textAmountSize: Float = context.resources.resolveSp(22f)
		var textNumberSize: Float = context.resources.resolveSp(20f)
		var textDescriptionSize: Float = context.resources.resolveSp(14f)
		var textAmountColor: Int = Color.WHITE
		var textNumberColor: Int = Color.WHITE
		var textDescriptionColor: Int = Color.GRAY

		if (attrs != null) {
			val typeArray = context.obtainStyledAttributes(attrs, R.styleable.PieChart)

			val colorResId = typeArray.getResourceId(R.styleable.PieChart_pieChartColors, 0)
			pieChartColors = typeArray.resources.getStringArray(colorResId).toList()

			marginTextFirst = typeArray.getDimension(R.styleable.PieChart_pieChartMarginTextFirst, marginTextFirst)
			marginTextSecond = typeArray.getDimension(R.styleable.PieChart_pieChartMarginTextSecond, marginTextSecond)
			marginTextThird = typeArray.getDimension(R.styleable.PieChart_pieChartMarginTextThird, marginTextThird)
			marginSmallCircle =
				typeArray.getDimension(R.styleable.PieChart_pieChartMarginSmallCircle, marginSmallCircle)

			circleStrokeWidth =
				typeArray.getDimension(R.styleable.PieChart_pieChartCircleStrokeWidth, circleStrokeWidth)
			circlePadding = typeArray.getDimension(R.styleable.PieChart_pieChartCirclePadding, circlePadding)
			circlePaintRoundSize =
				typeArray.getBoolean(R.styleable.PieChart_pieChartCirclePaintRoundSize, circlePaintRoundSize)
			circleSectionSpace = typeArray.getFloat(R.styleable.PieChart_pieChartCircleSectionSpace, circleSectionSpace)

			textCircleRadius = typeArray.getDimension(R.styleable.PieChart_pieChartTextCircleRadius, textCircleRadius)
			textAmountSize = typeArray.getDimension(R.styleable.PieChart_pieChartTextAmountSize, textAmountSize)
			textNumberSize = typeArray.getDimension(R.styleable.PieChart_pieChartTextNumberSize, textNumberSize)
			textDescriptionSize =
				typeArray.getDimension(R.styleable.PieChart_pieChartTextDescriptionSize, textDescriptionSize)
			textAmountColor = typeArray.getColor(R.styleable.PieChart_pieChartTextAmountColor, textAmountColor)
			textNumberColor = typeArray.getColor(R.styleable.PieChart_pieChartTextNumberColor, textNumberColor)
			textDescriptionColor =
				typeArray.getColor(R.styleable.PieChart_pieChartTextDescriptionColor, textDescriptionColor)
			textAmountStr = typeArray.getString(R.styleable.PieChart_pieChartTextAmount) ?: ""

			typeArray.recycle()
		}

		circlePadding += circleStrokeWidth

		// Инициализация кистей View
		initPaints(amountTextPaint, textAmountSize, textAmountColor)
		initPaints(numberTextPaint, textNumberSize, textNumberColor)
		initPaints(descriptionTextPain, textDescriptionSize, textDescriptionColor, true)
	}

	@RequiresApi(Build.VERSION_CODES.M)
	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		textRowList.clear()

		val initSizeWidth = resolveDefaultSize(widthMeasureSpec, DEFAULT_VIEW_SIZE_WIDTH)

		val textTextWidth = (initSizeWidth * TEXT_WIDTH_PERCENT)
		val initSizeHeight = calculateViewHeight(heightMeasureSpec, textTextWidth.toInt())

		textStartX = initSizeWidth - textTextWidth.toFloat()
		textStartY = initSizeHeight.toFloat() / 2 - textHeight / 2

		calculateCircleRadius(initSizeWidth, initSizeHeight)

		setMeasuredDimension(initSizeWidth, initSizeHeight)
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)

		drawCircle(canvas)
		drawText(canvas)
	}

	override fun onRestoreInstanceState(state: Parcelable?) {
		val pieChartState = state as? PieChartState
		super.onRestoreInstanceState(pieChartState?.superState ?: state)

		dataList = pieChartState?.dataList ?: listOf()
	}

	override fun onSaveInstanceState(): Parcelable {
		val superState = super.onSaveInstanceState()
		return PieChartState(superState, dataList)
	}

	override fun setDataChart(list: List<Pair<Int, String>>) {
		dataList = list
		calculatePercentageOfData()
	}

	override fun startAnimation() {
		val animator = ValueAnimator.ofInt(0, 360).apply {
			duration = context.getAnimationDuration(android.R.integer.config_longAnimTime)
			interpolator = FastOutSlowInInterpolator()
			addUpdateListener { valueAnimator ->
				animationSweepAngle = valueAnimator.animatedValue as Int
				invalidate()
			}
		}
		animator.start()
	}

	private fun drawCircle(canvas: Canvas) {
		for (percent in percentageCircleList) {
			if (animationSweepAngle > percent.percentToStartAt + percent.percentOfCircle) {
				canvas.drawArc(circleRect, percent.percentToStartAt, percent.percentOfCircle, false, percent.paint)
			} else if (animationSweepAngle > percent.percentToStartAt) {
				canvas.drawArc(
					circleRect,
					percent.percentToStartAt,
					animationSweepAngle - percent.percentToStartAt,
					false,
					percent.paint,
				)
			}
		}
	}

	private fun drawText(canvas: Canvas) {
		var textBuffY = textStartY
		textRowList.forEachIndexed { index, staticLayout ->
			if (index % 2 == 0) {
				staticLayout.draw(canvas, textStartX + marginSmallCircle + textCircleRadius, textBuffY)
				canvas.drawCircle(
					textStartX + marginSmallCircle / 2,
					textBuffY + staticLayout.height / 2 + textCircleRadius / 2,
					textCircleRadius,
					Paint().apply { color = Color.parseColor(pieChartColors[(index / 2) % pieChartColors.size]) },
				)
				textBuffY += staticLayout.height + marginTextFirst
			} else {
				staticLayout.draw(canvas, textStartX, textBuffY)
				textBuffY += staticLayout.height + marginTextSecond
			}
		}

		canvas.drawText(totalAmount.toString(), textAmountXNumber, textAmountY, amountTextPaint)
		canvas.drawText(textAmountStr, textAmountXDescription, textAmountYDescription, descriptionTextPain)
	}

	private fun initPaints(textPaint: TextPaint, textSize: Float, textColor: Int, isDescription: Boolean = false) {
		textPaint.color = textColor
		textPaint.textSize = textSize
		textPaint.isAntiAlias = true

		if (!isDescription) textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
	}

	private fun resolveDefaultSize(spec: Int, defValue: Int): Int {
		return when (MeasureSpec.getMode(spec)) {
			MeasureSpec.UNSPECIFIED -> resources.resolveDp(defValue)
			else -> MeasureSpec.getSize(spec)
		}
	}

	@RequiresApi(Build.VERSION_CODES.M)
	private fun calculateViewHeight(heightMeasureSpec: Int, textWidth: Int): Int {
		val initSizeHeight = resolveDefaultSize(heightMeasureSpec, DEFAULT_VIEW_SIZE_HEIGHT)
		textHeight = (dataList.size * marginText + getTextViewHeight(textWidth)).toInt()

		val textHeightWithPadding = textHeight + paddingTop + paddingBottom
		return if (textHeightWithPadding > initSizeHeight) textHeightWithPadding else initSizeHeight
	}

	private fun calculateCircleRadius(width: Int, height: Int) {
		val circleViewWidth = (width * CIRCLE_WIDTH_PERCENT)
		circleRadius = if (circleViewWidth > height) {
			(height.toFloat() - circlePadding) / 2
		} else {
			circleViewWidth.toFloat() / 2
		}

		with(circleRect) {
			left = circlePadding
			top = height / 2 - circleRadius
			right = circleRadius * 2 + circlePadding
			bottom = height / 2 + circleRadius
		}

		circleCenterX = (circleRadius * 2 + circlePadding + circlePadding) / 2
		circleCenterY = (height / 2 + circleRadius + (height / 2 - circleRadius)) / 2

		textAmountY = circleCenterY

		val sizeTextAmountNumber = getWidthOfAmountText(
			totalAmount.toString(),
			amountTextPaint,
		)

		textAmountXNumber = circleCenterX - sizeTextAmountNumber.width() / 2
		textAmountXDescription = circleCenterX - getWidthOfAmountText(textAmountStr, descriptionTextPain).width() / 2
		textAmountYDescription = circleCenterY + sizeTextAmountNumber.height() + marginTextThird
	}

	@RequiresApi(Build.VERSION_CODES.M)
	private fun getTextViewHeight(maxWidth: Int): Int {
		var textHeight = 0
		dataList.forEach {
			val textLayoutNumber = getMultilineText(
				text = it.first.toString(),
				textPaint = numberTextPaint,
				width = maxWidth,
			)
			val textLayoutDescription = getMultilineText(
				text = it.second,
				textPaint = descriptionTextPain,
				width = maxWidth,
			)
			textRowList.apply {
				add(textLayoutNumber)
				add(textLayoutDescription)
			}
			textHeight += textLayoutNumber.height + textLayoutDescription.height
		}

		return textHeight
	}

	private fun calculatePercentageOfData() {
		totalAmount = dataList.fold(0) { res, value -> res + value.first }

		var startAt = circleSectionSpace
		percentageCircleList = dataList.mapIndexed { index, pair ->
			var percent = pair.first * 100 / totalAmount.toFloat() - circleSectionSpace
			percent = if (percent < 0f) 0f else percent

			val resultModel = PieChartModel(
				percentOfCircle = percent,
				percentToStartAt = startAt,
				colorOfLine = Color.parseColor(pieChartColors[index % pieChartColors.size]),
				stroke = circleStrokeWidth,
				paintRound = circlePaintRoundSize,
			)
			if (percent != 0f) startAt += percent + circleSectionSpace
			resultModel
		}
	}

	private fun getWidthOfAmountText(text: String, textPaint: TextPaint): Rect {
		val bounds = Rect()
		textPaint.getTextBounds(text, 0, text.length, bounds)
		return bounds
	}

	@RequiresApi(Build.VERSION_CODES.M)
	private fun getMultilineText(
		text: CharSequence,
		textPaint: TextPaint,
		width: Int,
		start: Int = 0,
		end: Int = text.length,
		alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
		textDir: TextDirectionHeuristic = TextDirectionHeuristics.LTR,
		spacingMult: Float = 1f,
		spacingAdd: Float = 0f
	): StaticLayout {

		return StaticLayout.Builder
			.obtain(text, start, end, textPaint, width)
			.setAlignment(alignment)
			.setTextDirection(textDir)
			.setLineSpacing(spacingAdd, spacingMult)
			.build()
	}

	companion object {
		private const val DEFAULT_MARGIN_TEXT_1 = 2f
		private const val DEFAULT_MARGIN_TEXT_2 = 10f
		private const val DEFAULT_MARGIN_TEXT_3 = 2f
		private const val DEFAULT_MARGIN_SMALL_CIRCLE = 12f

		private const val TEXT_WIDTH_PERCENT = 0.40
		private const val CIRCLE_WIDTH_PERCENT = 0.50

		const val DEFAULT_VIEW_SIZE_HEIGHT = 150
		const val DEFAULT_VIEW_SIZE_WIDTH = 250
	}
}

interface PieChartInterface {

	fun setDataChart(list: List<Pair<Int, String>>)

	fun startAnimation()
}

data class PieChartModel(
	var percentOfCircle: Float = 0f,
	var percentToStartAt: Float = 0f,
	var colorOfLine: Int = 0,
	var stroke: Float = 0f,
	var paint: Paint = Paint(),
	var paintRound: Boolean = true
) {

	init {
		if (percentOfCircle < 0 || percentOfCircle > 100) {
			percentOfCircle = 100f
		}

		percentOfCircle = 360 * percentOfCircle / 100

		if (percentToStartAt < 0 || percentToStartAt > 100) {
			percentToStartAt = 0f
		}

		percentToStartAt = 360 * percentToStartAt / 100

		if (colorOfLine == 0) {
			colorOfLine = Color.parseColor("#000000")
		}

		paint = Paint()
		paint.color = colorOfLine
		paint.isAntiAlias = true
		paint.style = Paint.Style.STROKE
		paint.strokeWidth = stroke
		paint.isDither = true

		if (paintRound) {
			paint.strokeJoin = Paint.Join.ROUND
			paint.strokeCap = Paint.Cap.ROUND
			paint.pathEffect = CornerPathEffect(8f)
		}
	}
}

class PieChartState(
	superSavedState: Parcelable?,
	val dataList: List<Pair<Int, String>>
) : View.BaseSavedState(superSavedState), Parcelable
