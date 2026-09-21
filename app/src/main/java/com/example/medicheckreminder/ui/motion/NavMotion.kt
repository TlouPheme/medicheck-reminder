package com.example.medicheckreminder.ui.motion

import android.graphics.Color
import android.view.View
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.example.medicheckreminder.R
import com.example.medicheckreminder.ui.auth.ForgotPasswordFragment
import com.example.medicheckreminder.ui.auth.RegisterFragment
import com.example.medicheckreminder.ui.medications.AddEditMedicationFragment
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis

/**
 * Navigation motion for this graph.
 *
 * Material SharedAxis / FadeThrough are Fragment [android.transition.Transition]s, not
 * `app:enterAnim` resources. Wiring them on attach (before the view is created) keeps
 * [Fragment.returnTransition] and [Fragment.reenterTransition] in sync so
 * [androidx.navigation.NavController.navigateUp] and the system back button reverse
 * the same motion instead of popping with a blank cut.
 *
 * Do not also set `app:enterAnim` / `app:popEnterAnim` on the same actions — those are
 * view animations and would play on top of these transitions.
 */
object NavMotion {

    const val DURATION_MS = 300L
    const val POSTPONE_TIMEOUT_MS = 450L

    fun pillTransitionName(medicationId: String): String = "pill_icon_$medicationId"

    fun apply(fragment: Fragment) {
        when (fragment) {
            is AddEditMedicationFragment -> fragment.applyAddEditMotion()
            is RegisterFragment, is ForgotPasswordFragment ->
                fragment.applySharedAxis(MaterialSharedAxis.X)
            else -> fragment.applyFadeThrough()
        }
        fragment.allowEnterTransitionOverlap = true
        fragment.allowReturnTransitionOverlap = true
    }

    private fun Fragment.applyFadeThrough() {
        val enter = fadeThrough()
        val exit = fadeThrough()
        enterTransition = enter
        exitTransition = exit
        reenterTransition = fadeThrough()
        returnTransition = fadeThrough()
    }

    private fun Fragment.applySharedAxis(axis: Int) {
        enterTransition = MaterialSharedAxis(axis, true).configure()
        returnTransition = MaterialSharedAxis(axis, false).configure()
        exitTransition = MaterialSharedAxis(axis, true).configure()
        reenterTransition = MaterialSharedAxis(axis, false).configure()
    }

    private fun Fragment.applyAddEditMotion() {
        applySharedAxis(MaterialSharedAxis.Z)
        sharedElementEnterTransition = containerTransform()
        sharedElementReturnTransition = containerTransform()
    }

    private fun fadeThrough() = MaterialFadeThrough().configure()

    private fun MaterialFadeThrough.configure() = apply {
        duration = DURATION_MS
        interpolator = FastOutSlowInInterpolator()
    }

    private fun MaterialSharedAxis.configure() = apply {
        duration = DURATION_MS
        interpolator = FastOutSlowInInterpolator()
    }

    private fun containerTransform() = MaterialContainerTransform().apply {
        drawingViewId = R.id.nav_host_fragment
        duration = DURATION_MS
        scrimColor = Color.TRANSPARENT
        fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
        interpolator = FastOutSlowInInterpolator()
        setAllContainerColors(Color.TRANSPARENT)
    }
}

/**
 * Pairs [Fragment.postponeEnterTransition] with a guaranteed start so a missed
 * [androidx.core.view.doOnPreDraw] cannot freeze the back stack or shared-element return.
 */
fun Fragment.postponeEnterUntilDrawn(
    target: View,
    timeoutMs: Long = NavMotion.POSTPONE_TIMEOUT_MS
) {
    postponeEnterTransition()
    var started = false
    val start = Runnable {
        if (started) return@Runnable
        started = true
        startPostponedEnterTransition()
    }
    target.doOnPreDraw { start.run() }
    target.postDelayed(start, timeoutMs)
}
