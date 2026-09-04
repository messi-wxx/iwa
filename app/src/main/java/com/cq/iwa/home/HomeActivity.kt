package com.cq.iwa.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cq.iwa.BuildConfig
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.calibration.MeterCalibrationNavigator
import com.cq.iwa.databinding.ActivityHomeBinding
import com.cq.iwa.databinding.ItemHomeFunctionBinding
import com.cq.iwa.diagnose.DiagnoseNavigator
import com.cq.iwa.readmeter.MeterNavigator
import com.cq.iwa.replacemeter.ReplaceMeterNavigator
import com.cq.iwa.installation.InstNavigator
import com.cq.iwa.pipeline.PipelineNavigator
import com.cq.iwa.sceneservice.SceneServiceNavigator
import com.cq.iwa.sdk.ThirdSdk
import com.cq.iwa.setting.SettingActivity
import com.cq.iwa.update.AppUpdateHelper
import com.cq.iwa.urgepayment.UrgePaymentNavigator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : IwaBaseActivity<ActivityHomeBinding>() {

    private val viewModel: HomeViewModel by viewModels()

    @Inject
    lateinit var appUpdateHelper: AppUpdateHelper

    private lateinit var adapter: HomeFunctionAdapter

    override fun statusBarColorRes(): Int? = R.color.home_status_bar

    override fun isLightStatusBar(): Boolean = true

    override fun inflateBinding(): ActivityHomeBinding =
        ActivityHomeBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        adapter = HomeFunctionAdapter(emptyList())
        binding.gridFunctions.adapter = adapter
        binding.gridFunctions.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val item = adapter.getItem(position)
                if (item.enabled) {
                    when (item.path) {
                        "readMeterTask" -> MeterNavigator.openBookList(this)
                        "changeMeterTask" -> ReplaceMeterNavigator.openBookList(this)
                        "meterCalibration" -> MeterCalibrationNavigator.open(this)
                        "urgePayment" -> UrgePaymentNavigator.open(this)
                        "diagnose" -> DiagnoseNavigator.open(this)
                        "sceneService" -> SceneServiceNavigator.open(this)
                        "pipelineNetworkMonitoring" -> PipelineNavigator.openFollow(this)
                        "installation" -> InstNavigator.openPending(this)
                        else -> showToast(getString(R.string.home_coming_soon) + ": ${item.title}")
                    }
                } else {
                    showToast(item.message.ifBlank { getString(R.string.home_no_permission) })
                }
            }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }

        collectPageState(
            stateFlow = viewModel.uiState,
            stateLayout = binding.stateLayout,
            onSuccess = { home ->
                binding.tvWelcome.text = getString(R.string.home_welcome, home.userName)
                adapter.update(home.functions)
            },
            onRetry = { viewModel.loadHome() },
        )
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.updateAvailable.collect { version ->
                    appUpdateHelper.promptUpdate(this@HomeActivity, version, showDontRemind = true)
                }
            }
        }
        ThirdSdk.initPush(this, BuildConfig.DEBUG)
        viewModel.loadHome()
        viewModel.checkApkVersion()
    }
}

private class HomeFunctionAdapter(
    private var items: List<HomeFunction>,
) : BaseAdapter() {

    fun update(newItems: List<HomeFunction>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): HomeFunction = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = if (convertView == null) {
            ItemHomeFunctionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        } else {
            ItemHomeFunctionBinding.bind(convertView)
        }
        val item = items[position]
        val ctx = parent.context
        binding.ivIcon.setImageResource(item.iconRes)
        binding.tvTitle.text = item.title
        binding.root.setBackgroundResource(
            if (item.enabled) R.drawable.bg_home_function else R.drawable.bg_home_function_disabled,
        )
        binding.root.elevation = if (item.enabled) {
            2f * ctx.resources.displayMetrics.density
        } else {
            0f
        }
        binding.tvTitle.setTextColor(
            ContextCompat.getColor(
                ctx,
                if (item.enabled) R.color.navy else R.color.icon_disabled,
            ),
        )
        ImageViewCompat.setImageTintList(binding.ivIcon, null)
        binding.root.alpha = if (item.enabled) 1f else 0.45f
        if (!item.enabled && item.message.isNotBlank()) {
            binding.tvHint.visibility = View.VISIBLE
            binding.tvHint.text = item.message
            binding.tvHint.setTextColor(ContextCompat.getColor(ctx, R.color.icon_disabled))
        } else {
            binding.tvHint.visibility = View.GONE
        }
        return binding.root
    }
}
