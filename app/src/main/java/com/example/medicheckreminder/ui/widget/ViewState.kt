package com.example.medicheckreminder.ui.widget

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.medicheckreminder.R

/**
 * Visual mode for [StateLayout]. Pass this from a fragment instead of toggling
 * loading / empty / error views by hand.
 *
 * ```
 * binding.stateLayout.setState(
 *     when {
 *         state.isLoading -> ViewState.Loading
 *         state.error != null -> ViewState.Error(
 *             messageRes = R.string.error_generic_message,
 *             onRetry = viewModel::retry
 *         )
 *         state.items.isEmpty() -> ViewState.Empty(
 *             titleRes = R.string.empty_generic_title,
 *             subtitleRes = R.string.empty_generic_subtitle,
 *             ctaRes = R.string.add_medication,
 *             onCtaClick = { openEditor() }
 *         )
 *         else -> ViewState.Content
 *     }
 * )
 * ```
 */
sealed class ViewState {
    data object Content : ViewState()
    data object Loading : ViewState()

    data class Empty(
        @StringRes val titleRes: Int,
        @StringRes val subtitleRes: Int? = null,
        @DrawableRes val illustrationRes: Int = R.drawable.ic_empty_state,
        @StringRes val ctaRes: Int? = null,
        val onCtaClick: (() -> Unit)? = null
    ) : ViewState()

    data class Error(
        @StringRes val titleRes: Int = R.string.error_generic_title,
        @StringRes val messageRes: Int = R.string.error_generic_message,
        val message: CharSequence? = null,
        @DrawableRes val illustrationRes: Int = R.drawable.ic_cloud_off,
        @StringRes val retryRes: Int = R.string.action_retry,
        val onRetry: (() -> Unit)? = null
    ) : ViewState()
}
