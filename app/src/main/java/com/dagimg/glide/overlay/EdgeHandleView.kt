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
 * - Tap or swipe left to open the clipboard panel.
 * - Long press + drag to reposition vertically.
 */
@SuppressLint("ViewConstructor")
class EdgeHandleView(
    context: Context,
    private val onTap: () -> Unit,
    private val onDrag: (deltaY: Float) -> Unit,
    private val onDragEnd: () -> Unit,
) : View(context) {
    companion object {
        private const val HANDLE_WIDTH_DP = 8f
        private const val HANDLE_HEIGHT_DP = 100f
        private const val CORNER_RADIUS_DP = 4f
        private const val LONG_PRESS_TIMEOUT_MS = 300L
    }

    private val density = resources.displayMetrics.density
    private val handleWidth = (HANDLE_WIDTH_DP * density).toInt()
    private val handleHeight = (HANDLE_HEIGHT_DP * density).toInt()
    private val cornerRadius = CORNER_RADIUS_DP * density

    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#55FFFFFF")
            style = Paint.Style.FILL
        }

    private val highlightPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#AAFFFFFF")
            style = Paint.Style.FILL
        }

    private val dragPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF6C5CE7") // Purple when dragging
            style = Paint.Style.FILL
        }

    private var isPressed = false
    private var isDragging = false
    private var lastTouchY = 0f
    private var touchDownTime = 0L
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    private val gestureDetector =
        GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    if (!isDragging) {
                        performHapticFeedback()
                        onTap()
                        return true
                    }
                    return false
                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float,
                ): Boolean {
                    if (!isDragging && velocityX < -500) {
                        performHapticFeedback()
                        onTap()
                        return true
                    }
                    return false
                }
            },
        )

    init {
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
        val currentPaint =
            when {
                isDragging -> dragPaint
                isPressed -> highlightPaint
                else -> paint
            }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, currentPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                isDragging = false
                lastTouchY = event.rawY
                touchDownTime = System.currentTimeMillis()
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                val elapsed = System.currentTimeMillis() - touchDownTime
                val deltaY = event.rawY - lastTouchY

                // Start dragging after long press threshold OR significant vertical movement
                if (!isDragging && (elapsed > LONG_PRESS_TIMEOUT_MS || kotlin.math.abs(deltaY) > 20)) {
                    isDragging = true
                    performHapticFeedback()
                    invalidate()
                }

                if (isDragging) {
                    onDrag(deltaY)
                    lastTouchY = event.rawY
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                if (isDragging) {
                    isDragging = false
                    onDragEnd()
                    invalidate()
                    return true
                }
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
