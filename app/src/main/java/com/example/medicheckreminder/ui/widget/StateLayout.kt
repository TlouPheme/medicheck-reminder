package com.example.medicheckreminder.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import com.example.medicheckreminder.R
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.button.MaterialButton

/**
 * FrameLayout that swaps between loading, empty, error, and content.
 *
 * XML children are the **content** state. Overlays are inflated internally.
 *
 * ```
 * <com.example.medicheckreminder.ui.widget.StateLayout
 *     android:id="@+id/state_layout"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
 *     app:stateLoadingLayout="@layout/layout_home_shimmer"
 *     app:stateLoadingAnnouncement="@string/home_loading">
 *
 *     <androidx.recyclerview.widget.RecyclerView ... />
 * </com.example.medicheckreminder.ui.widget.StateLayout>
 * ```
 *
 * Then in the fragment: `binding.stateLayout.setState(ViewState.Content)`.
 */
class StateLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    @LayoutRes
    private val loadingLayoutRes: Int
    private val loadingAnnouncement: CharSequence?
    @DrawableRes
    private val defaultEmptyIllustration: Int
    @DrawableRes
    private val defaultErrorIllustration: Int

    private val contentViews = mutableListOf<View>()
    private lateinit var loadingView: View
    private lateinit var emptyView: View
    private lateinit var errorView: View

    private val emptyIllustration: ImageView
        get() = emptyView.findViewById(R.id.image_state_empty)
    private val emptyTitle: TextView
        get() = emptyView.findViewById(R.id.text_state_empty_title)
    private val emptySubtitle: TextView
        get() = emptyView.findViewById(R.id.text_state_empty_subtitle)
    private val emptyCta: MaterialButton
        get() = emptyView.findViewById(R.id.btn_state_empty_cta)

    private val errorIllustration: ImageView
        get() = errorView.findViewById(R.id.image_state_error)
    private val errorTitle: TextView
        get() = errorView.findViewById(R.id.text_state_error_title)
    private val errorMessage: TextView
        get() = errorView.findViewById(R.id.text_state_error_message)
    private val errorRetry: MaterialButton
        get() = errorView.findViewById(R.id.btn_state_retry)

    private var overlaysAttached = false
    private var currentState: ViewState = ViewState.Content

    init {
        val typed = context.obtainStyledAttributes(attrs, R.styleable.StateLayout)
        loadingLayoutRes = typed.getResourceId(R.styleable.StateLayout_stateLoadingLayout, 0)
        loadingAnnouncement = typed.getText(R.styleable.StateLayout_stateLoadingAnnouncement)
        defaultEmptyIllustration = typed.getResourceId(
            R.styleable.StateLayout_stateEmptyIllustration,
            R.drawable.ic_empty_state
        )
        defaultErrorIllustration = typed.getResourceId(
            R.styleable.StateLayout_stateErrorIllustration,
            R.drawable.ic_cloud_off
        )
        typed.recycle()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (overlaysAttached) return
        for (index in 0 until childCount) {
            contentViews += getChildAt(index)
        }
        val inflater = LayoutInflater.from(context)
        loadingView = inflateLoading(inflater)
        emptyView = inflater.inflate(R.layout.layout_state_empty, this, false)
        errorView = inflater.inflate(R.layout.layout_state_error, this, false)
        val overlayParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(loadingView, overlayParams)
        addView(emptyView, overlayParams)
        addView(errorView, overlayParams)
        overlaysAttached = true
        applyState(currentState)
    }

    fun setState(state: ViewState) {
        currentState = state
        if (overlaysAttached) applyState(state)
    }

    private fun applyState(state: ViewState) {
        val showContent = state is ViewState.Content
        val showLoading = state is ViewState.Loading
        val showEmpty = state is ViewState.Empty
        val showError = state is ViewState.Error

        contentViews.forEach { it.isVisible = showContent }
        setLoadingVisible(showLoading)
        emptyView.isVisible = showEmpty
        errorView.isVisible = showError

        when (state) {
            is ViewState.Content -> Unit
            is ViewState.Loading -> announceLoading()
            is ViewState.Empty -> bindEmpty(state)
            is ViewState.Error -> bindError(state)
        }
    }

    private fun bindEmpty(state: ViewState.Empty) {
        emptyIllustration.setImageResource(
            state.illustrationRes.takeIf { it != 0 } ?: defaultEmptyIllustration
        )
        emptyTitle.setText(state.titleRes)
        if (state.subtitleRes != null) {
            emptySubtitle.isVisible = true
            emptySubtitle.setText(state.subtitleRes)
        } else {
            emptySubtitle.isVisible = false
        }
        val ctaRes = state.ctaRes
        val onCtaClick = state.onCtaClick
        if (ctaRes != null && onCtaClick != null) {
            emptyCta.isVisible = true
            emptyCta.setText(ctaRes)
            emptyCta.setOnClickListener { onCtaClick() }
        } else {
            emptyCta.isVisible = false
            emptyCta.setOnClickListener(null)
        }
    }

    private fun bindError(state: ViewState.Error) {
        errorIllustration.setImageResource(
            state.illustrationRes.takeIf { it != 0 } ?: defaultErrorIllustration
        )
        errorTitle.setText(state.titleRes)
        if (state.message != null) {
            errorMessage.text = state.message
        } else {
            errorMessage.setText(state.messageRes)
        }
        val onRetry = state.onRetry
        if (onRetry != null) {
            errorRetry.isVisible = true
            errorRetry.setText(state.retryRes)
            errorRetry.setOnClickListener { onRetry() }
        } else {
            errorRetry.isVisible = false
            errorRetry.setOnClickListener(null)
        }
    }

    private fun setLoadingVisible(visible: Boolean) {
        loadingView.isVisible = visible
        val shimmer = loadingView as? ShimmerFrameLayout
            ?: (loadingView as? FrameLayout)?.let { it.getChildAt(0) as? ShimmerFrameLayout }
        if (visible) {
            shimmer?.startShimmer()
        } else {
            shimmer?.stopShimmer()
        }
    }

    private fun announceLoading() {
        val announcement = loadingAnnouncement
            ?: context.getString(R.string.state_loading)
        loadingView.findViewById<TextView>(R.id.text_state_loading)?.text = announcement
        announceForAccessibility(announcement)
    }

    private fun inflateLoading(inflater: LayoutInflater): View {
        if (loadingLayoutRes == 0) {
            return inflater.inflate(R.layout.layout_state_loading, this, false)
        }
        val inner = inflater.inflate(loadingLayoutRes, this, false)
        if (inner is ShimmerFrameLayout) return inner
        return ShimmerFrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            isClickable = false
            isFocusable = false
            setShimmer(
                Shimmer.AlphaHighlightBuilder()
                    .setDuration(1200)
                    .setBaseAlpha(0.7f)
                    .setHighlightAlpha(1f)
                    .build()
            )
            addView(
                inner,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            )
        }
    }

    override fun onDetachedFromWindow() {
        if (overlaysAttached) {
            (loadingView as? ShimmerFrameLayout)?.stopShimmer()
        }
        super.onDetachedFromWindow()
    }
}
