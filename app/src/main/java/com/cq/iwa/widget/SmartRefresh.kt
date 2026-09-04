package com.cq.iwa.widget

import com.scwang.smart.refresh.layout.SmartRefreshLayout

fun SmartRefreshLayout.bindSmartRefresh(
    enableLoadMore: Boolean = false,
    onRefresh: () -> Unit,
    onLoadMore: (() -> Unit)? = null,
) {
    setEnableRefresh(true)
    setEnableLoadMore(enableLoadMore)
    setOnRefreshListener { onRefresh() }
    if (enableLoadMore && onLoadMore != null) {
        setOnLoadMoreListener { onLoadMore() }
    }
}

fun SmartRefreshLayout.finishSmart(hasMore: Boolean = true, enableLoadMore: Boolean = false) {
    if (isRefreshing) finishRefresh()
    if (!enableLoadMore) {
        setEnableLoadMore(false)
        return
    }
    setEnableLoadMore(true)
    if (isLoading) {
        if (hasMore) {
            setNoMoreData(false)
            finishLoadMore()
        } else {
            finishLoadMoreWithNoMoreData()
        }
    } else {
        setNoMoreData(!hasMore)
    }
}
