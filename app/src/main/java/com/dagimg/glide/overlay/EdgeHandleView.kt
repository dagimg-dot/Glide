package com.dagimg.glide.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View

/**
 * Edge handle view that appears on the screen edge.
 * User can tap or swipe on this handle to open the clipboard panel.
 */
@SuppressLint("ViewConstructor")
class EdgeHandleView(
    context: Context,
    private val onTap: () -> Unit,
) : View(context) {
    companion object {
        private const val HANDLE_WIDTH_DP = 8f
        private const val HANDLE_HEIGHT_DP = 100f
        private const val CORNER_RADIUS_DP = 4f
    }

    private val density = resources.displayMetrics.density
    private val handleWidth = (HANDLE_WIDTH_DP * density).toInt()
    private val handleHeight = (HANDLE_HEIGHT_DP * density).toInt()
    private val cornerRadius = CORNER_RADIUS_DP * density

    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#55FFFFFF") // Semi-transparent white
            style = Paint.Style.FILL
        }

    private val highlightPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#AAFFFFFF") // More visible on touch
            style = Paint.Style.FILL
        }

    private var isPressed = false
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    private val gestureDetector =
        GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    performHapticFeedback()
                    onTap()
                    return true
                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float,
                ): Boolean {
                    // Detect left swipe (fling towards left = opening panel from right edge)
                    if (velocityX < -500) {
                        performHapticFeedback()
                        onTap()
                        return true
                    }
                    return false
                }
            },
        )

    init {
        // Set view size
        minimumWidth = handleWidth
        minimumHeight = handleHeight
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        setMeasuredDimension(handleWidth, handleHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, if (isPressed) highlightPaint else paint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                invalidate()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                invalidate()
            }
        }
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    private fun performHapticFeedback() {
        vibrator?.let {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(30)
            }
        }
    }
}
