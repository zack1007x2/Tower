package org.droidplanner.android.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View

/**
 * Created by Fredia Huya-Kouadio on 8/14/15.
 */
public class TrackerView(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : View(context, attrs, defStyleAttr, defStyleRes) {

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet) :  this(context, attrs, 0)

    private val trackerPaint: Paint
    private val centerPaint: Paint

    init {
        trackerPaint = Paint()
        trackerPaint.setStyle(Paint.Style.STROKE)
        trackerPaint.setColor(Color.RED)

        centerPaint = Paint()
        centerPaint.setColor(Color.GREEN)
        centerPaint.setStyle(Paint.Style.FILL_AND_STROKE)
        centerPaint.setStrokeWidth(15f)
    }

    private var trackerHeight = 0f
    private var trackerWidth = 0f
    private var trackerPosition = PointF(0f, 0f)

    public fun updateTracker(position: PointF, width: Float, height: Float){
        trackerHeight = height
        trackerWidth = width
        trackerPosition = position
        invalidate()
    }

    override fun onDraw(canvas: Canvas){
        super.onDraw(canvas)

        val left = trackerPosition.x
        val top = trackerPosition.y
        val right = left + trackerWidth
        val bottom = top + trackerHeight
        val centerX = left + trackerWidth / 2
        val centerY = top + trackerHeight / 2
        canvas.drawRect(left, top, right, bottom, trackerPaint)
        canvas.drawPoint(centerX, centerY, centerPaint)
    }
}