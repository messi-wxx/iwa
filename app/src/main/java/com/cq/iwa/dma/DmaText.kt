package com.cq.iwa.dma

import com.cq.iwa.R
import com.cq.iwa.feature.dma.ui.DmaMetric

internal fun metricLabel(metric: DmaMetric): Int = when (metric) {
    DmaMetric.SUPPLY -> R.string.dma_metric_supply
    DmaMetric.NIGHT -> R.string.dma_metric_night
    DmaMetric.NIGHT_RATIO -> R.string.dma_metric_night_ratio
    DmaMetric.KL -> R.string.dma_metric_kl
    DmaMetric.LEAK -> R.string.dma_metric_leak
    DmaMetric.NRW -> R.string.dma_metric_nrw
    DmaMetric.MNF -> R.string.dma_metric_mnf
    DmaMetric.EST_LEAK -> R.string.dma_metric_est
}
