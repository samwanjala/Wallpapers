package com.example.socialapp.ui.detailactivity

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.widget.AppCompatImageView
import timber.log.Timber

class ZoomPanImageView : AppCompatImageView {
    private var imageMatrix = Matrix()
    private val matrixValues = FloatArray(9)
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private val accelerateDecelerateInterpolator = AccelerateDecelerateInterpolator()
    private var tapDetector: GestureDetector =
        GestureDetector(context, object : SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                Timber.tag("scale").d("current scale: $currentScale")
                animateZoom(
                    if (currentScale > 1f) 3f else 1f,
                    if (currentScale > 1f) 1f else 3f,
                    e.x,
                    e.y
                )
                return true
            }
        })

    private val scaleListener: SimpleOnScaleGestureListener =
        object : SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (detector.scaleFactor.isNaN() || detector.scaleFactor.isInfinite())
                    return false

                Timber.tag("scale").d("current scale factor: ${detector.scaleFactor}")

                if (currentScale > 3f && detector.scaleFactor > 1f || currentScale < 0.5f) return false

                matrixZoom(detector.scaleFactor, detector.focusX, detector.focusY)
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                super.onScaleEnd(detector)
                Timber.tag("scale").d("scale end")
                if (currentScale < 1f) {
                    animateZoom(currentScale, 1f, detector.focusX, detector.focusY)
                }
            }
        }

    private inline val currentScale: Float
        get() {
            imageMatrix.getValues(matrixValues)
            return matrixValues[Matrix.MSCALE_X]
        }

    private val scaleGestureDetector = ScaleGestureDetector(context, scaleListener)

    constructor(context: Context) : super(context, null, 0)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet, 0)

    constructor(context: Context, attributeSet: AttributeSet, defStyle: Int) : super(
        context,
        attributeSet,
        defStyle
    )

    init {
        setBounds()
        setMatrix()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.x
                    lastTouchY = event.y
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    if (dx < 50f && dy < 50) {
                        Timber.tag("translate").d("x: $dx, y: $dy")
                        matrixTranslate(dx, dy)
                    }
                    lastTouchX = event.x
                    lastTouchY = event.y
                }

                else -> {}
            }
        }
        event?.let { scaleGestureDetector.onTouchEvent(it) }
        event?.let { tapDetector.onTouchEvent(it) }
        if (scaleGestureDetector.isInProgress) return false
        return this.drawable != null
    }

    fun matrixZoom(zoomFactor: Float, fx: Float, fy: Float) {
        imageMatrix.postScale(zoomFactor, zoomFactor, fx, fy)
        setBounds()
        setMatrix()
    }

    private fun matrixTranslate(dx: Float, dy: Float) {
        imageMatrix.postTranslate(dx, dy)
        setBounds()
        setMatrix()
    }

    private fun setMatrix() {
        setImageMatrix(imageMatrix)
    }

    private fun animateZoom(startZoom: Float, endZoom: Float, fx: Float, fy: Float) {
        ValueAnimator.ofFloat(
            startZoom, endZoom
        ).apply {
            duration = 300L
            interpolator = accelerateDecelerateInterpolator
            addUpdateListener {
                val value = it.animatedValue as Float / currentScale
                matrixZoom(value, fx, fy)
            }
            start()
        }
    }

    private fun setBounds() {
        val rect = displayRect ?: return

        val height = rect.height()
        val width = rect.width()
        val viewHeight: Int = this.height
        var deltaX = 0f
        var deltaY = 0f

        when {
            height <= viewHeight -> {
                deltaY = (viewHeight - height) / 2 - rect.top
            }

            rect.top > 0 -> {
                deltaY = -rect.top
            }

            rect.bottom < viewHeight -> {
                deltaY = viewHeight - rect.bottom
            }
        }

        val viewWidth: Int = this.width
        when {
            width <= viewWidth -> {
                deltaX = (viewWidth - width) / 2 - rect.left
            }

            rect.left > 0 -> {
                deltaX = -rect.left
            }

            rect.right < viewWidth -> {
                deltaX = viewWidth - rect.right
            }
        }
        imageMatrix.postTranslate(deltaX, deltaY)
    }

    private val displayRect: RectF? = RectF()
        get() {
            drawable?.let {
                field?.set(
                    0f, 0f, it.intrinsicWidth.toFloat(), it.intrinsicHeight.toFloat()
                )
                imageMatrix.mapRect(field)
                return field
            }
            return null
        }
}
