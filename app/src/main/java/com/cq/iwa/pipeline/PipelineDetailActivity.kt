package com.cq.iwa.pipeline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.databinding.ActivityPipelineDetailBinding
import com.cq.iwa.databinding.ItemPipelineMetricBinding
import com.cq.iwa.feature.pipeline.data.PipelineFormat
import com.cq.iwa.feature.pipeline.network.PipelineMetricDto
import com.cq.iwa.feature.pipeline.ui.PipelineDetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PipelineDetailActivity : IwaBaseActivity<ActivityPipelineDetailBinding>() {

    private val viewModel: PipelineDetailViewModel by viewModels()

    override fun statusBarColorRes(): Int = R.color.main_background
    override fun inflateBinding() = ActivityPipelineDetailBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        val siteId = intent.getIntExtra(PipelineNavigator.EXTRA_SITE_ID, -1)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnAlarm.setOnClickListener {
            val site = viewModel.ui.value.site
            PipelineNavigator.openAlarm(this, site?.iotId, site?.siteType ?: -1)
        }
        val adapter = MetricAdapter(
            onFollow = { viewModel.toggleFollow(it) },
            onMetric = { metric ->
                PipelineChartSheet.show(
                    activity = this,
                    title = PipelineFormat.displayName(metric.name, metric.unit).ifBlank { metric.metricId },
                    start = PipelineFormat.beforeDays(1),
                    end = PipelineFormat.now("yyyy-MM-dd"),
                    onQuery = { start, end -> viewModel.loadChart(metric, start, end) },
                    chartFlow = viewModel.chart,
                )
            },
        )
        binding.rvMetrics.layoutManager = LinearLayoutManager(this)
        binding.rvMetrics.adapter = adapter
        binding.tvMoreInfo.setOnClickListener {
            val show = !binding.moreInfo.isVisible
            binding.moreInfo.isVisible = show
            binding.tvMoreInfo.text = getString(if (show) R.string.pipeline_less_info else R.string.pipeline_more_info)
        }
        binding.tvMoreMetric.setOnClickListener {
            val show = !viewModel.ui.value.showMoreMetrics
            viewModel.toggleMoreMetrics(show)
            binding.tvMoreMetric.text = getString(if (show) R.string.pipeline_less_metrics else R.string.pipeline_more_metrics)
        }
        lifecycleScope.launch {
            viewModel.ui.collect { ui ->
                val site = ui.site
                binding.tvSiteCode.text = getString(R.string.pipeline_label_code, site?.siteCode.orEmpty())
                binding.tvSiteName.text = getString(R.string.pipeline_label_name, site?.siteName.orEmpty())
                binding.tvAddress.text = getString(R.string.pipeline_label_address, site?.address.orEmpty())
                binding.tvType.text = getString(R.string.pipeline_label_type, site?.siteTypeDesc.orEmpty())
                binding.tvCreateTime.text = getString(R.string.pipeline_label_time, site?.displayCreateTime.orEmpty())
                binding.tvContact.text = getString(R.string.pipeline_label_contact, site?.contact.orEmpty())
                binding.tvMobile.text = getString(R.string.pipeline_label_mobile, site?.mobile.orEmpty())
                binding.tvPhone.text = getString(R.string.pipeline_label_phone, site?.phone.orEmpty())
                binding.tvRemark.text = getString(R.string.pipeline_label_remark, site?.remark.orEmpty())
                binding.tvProfile.text = getString(R.string.pipeline_label_profile, ui.profileName)
                binding.tvToken.text = getString(R.string.pipeline_label_token, ui.credential)
                binding.metricCard.isVisible = ui.metrics.isNotEmpty()
                binding.tvMoreMetric.isVisible = ui.hasHiddenMetrics
                adapter.submit(ui.visibleMetrics)
            }
        }
        viewModel.load(siteId)
    }

    override fun onResume() {
        super.onResume()
        viewModel.connectAndSubscribe()
    }

    private class MetricAdapter(
        private val onFollow: (PipelineMetricDto) -> Unit,
        private val onMetric: (PipelineMetricDto) -> Unit,
    ) : RecyclerView.Adapter<MetricAdapter.Holder>() {
        private var items: List<PipelineMetricDto> = emptyList()
        fun submit(value: List<PipelineMetricDto>) {
            items = value
            notifyDataSetChanged()
        }

        override fun getItemCount() = items.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemPipelineMetricBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            holder.binding.tvName.text = PipelineFormat.displayName(item.name, item.unit)
            holder.binding.tvDate.text = PipelineFormat.fromTimestamp(item.timestamp)
            holder.binding.tvValue.text = PipelineFormat.formatFloat(item.value, item.digit)
            holder.binding.ivFollow.setImageResource(if (item.follow) R.drawable.follow else R.drawable.follow_cancel)
            holder.binding.ivFollow.setOnClickListener { onFollow(item) }
            holder.binding.content.setOnClickListener { onMetric(item) }
        }

        class Holder(val binding: ItemPipelineMetricBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
