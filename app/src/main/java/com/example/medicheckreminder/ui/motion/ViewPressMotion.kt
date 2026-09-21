package com.example.medicheckreminder.ui.motion

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View

/**
 * Subtle press feedback that does not consume the touch, so child buttons still click.
 */
@SuppressLint("ClickableViewAccessibility")
fun View.applyPressScale(
    restAlpha: Float = 1f,
    pressedScale: Float = 0.97f,
    pressedAlpha: Float = 0.92f,
    durationMs: Long = 90L
) {
    setOnTouchListener { view, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> view.animate()
                .scaleX(pressedScale)
                .scaleY(pressedScale)
                .alpha(pressedAlpha)
                .setDuration(durationMs)
                .start()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(restAlpha)
                .setDuration(durationMs)
                .start()
        }
        false
    }
}

fun View.cancelPressScale(restAlpha: Float = 1f) {
    animate().cancel()
    scaleX = 1f
    scaleY = 1f
    alpha = restAlpha
}
