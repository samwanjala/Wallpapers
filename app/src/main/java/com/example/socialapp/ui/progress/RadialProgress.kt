package com.example.socialapp.ui.progress

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import com.example.socialapp.R

class RadialProgressBar : ProgressBar {

    private val thickness = 14f
    private val halfThickness = thickness / 2
    private val startAngle = 270f
    private var boundsF: RectF? = null
    private lateinit var paint: Paint

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        paint = Paint()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = thickness
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = ContextCompat.getColor(context, R.color.ivory)

        progressDrawable = null
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        if (boundsF == null) {
            boundsF = RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
            boundsF?.inset(halfThickness, halfThickness)
        }

        canvas.drawArc(boundsF!!, startAngle, progress * 3.60f, false, paint)
    }
}