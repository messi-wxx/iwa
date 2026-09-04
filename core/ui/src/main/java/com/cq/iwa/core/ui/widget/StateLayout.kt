package com.cq.iwa.core.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.cq.iwa.core.ui.R

class StateLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class State { CONTENT, LOADING, EMPTY, ERROR, OFFLINE }

    private val overlayViews = mutableListOf<View>()
    private var contentViews: List<View> = emptyList()
    private lateinit var loadingView: View
    private lateinit var emptyView: View
    private lateinit var errorView: View
    private lateinit var offlineView: View
    private var retryAction: (() -> Unit)? = null

    var state: State = State.CONTENT
        private set

    override fun onFinishInflate() {
        super.onFinishInflate()
        contentViews = (0 until childCount).map { getChildAt(it) }
        val inflater = LayoutInflater.from(context)
        loadingView = inflater.inflate(R.layout.layout_state_loading, this, false)
        emptyView = inflater.inflate(R.layout.layout_state_empty, this, false)
        errorView = inflater.inflate(R.layout.layout_state_error, this, false)
        offlineView = inflater.inflate(R.layout.layout_state_offline, this, false)
        overlayViews += listOf(loadingView, emptyView, errorView, offlineView)
        overlayViews.forEach { overlay ->
            addView(overlay, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            overlay.findViewById<View>(R.id.btnStateRetry)?.setOnClickListener {
                retryAction?.invoke()
            }
        }
        showContent()
    }

    fun setOnRetry(action: (() -> Unit)?) {
        retryAction = action
    }

    fun showContent() = render(State.CONTENT)

    fun showLoading(message: CharSequence? = null) {
        loadingView.findViewById<TextView>(R.id.tvStateMessage)?.text =
            message ?: context.getString(R.string.core_loading)
        render(State.LOADING)
    }

    fun showEmpty(
        message: CharSequence? = null,
        showRetry: Boolean = false,
        retryText: CharSequence? = null,
    ) {
        emptyView.findViewById<TextView>(R.id.tvStateMessage)?.text =
            message ?: context.getString(R.string.core_state_empty)
        emptyView.findViewById<TextView>(R.id.btnStateRetry)?.apply {
            isVisible = showRetry && retryAction != null
            text = retryText ?: context.getString(R.string.core_state_retry)
        }
        render(State.EMPTY)
    }

    fun showError(message: CharSequence? = null) {
        errorView.findViewById<TextView>(R.id.tvStateMessage)?.text =
            message ?: context.getString(R.string.core_state_error)
        errorView.findViewById<View>(R.id.btnStateRetry)?.isVisible = retryAction != null
        render(State.ERROR)
    }

    fun showOffline(message: CharSequence? = null) {
        offlineView.findViewById<TextView>(R.id.tvStateMessage)?.text =
            message ?: context.getString(R.string.core_state_offline)
        offlineView.findViewById<View>(R.id.btnStateRetry)?.isVisible = retryAction != null
        render(State.OFFLINE)
    }

    private fun render(next: State) {
        state = next
        contentViews.forEach { it.isVisible = next == State.CONTENT }
        loadingView.isVisible = next == State.LOADING
        emptyView.isVisible = next == State.EMPTY
        errorView.isVisible = next == State.ERROR
        offlineView.isVisible = next == State.OFFLINE
    }
}
