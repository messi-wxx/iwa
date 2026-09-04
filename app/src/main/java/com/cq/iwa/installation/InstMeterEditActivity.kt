package com.cq.iwa.installation

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.text.InputType
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cq.iwa.R
import com.cq.iwa.databinding.ActivityInstMeterBinding
import com.cq.iwa.feature.installation.network.InstAddMeterBody
import com.cq.iwa.feature.installation.network.InstMeterRecordDto
import com.cq.iwa.feature.installation.ui.InstMeterViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class InstMeterEditActivity : InstActivity<ActivityInstMeterBinding>() {

    private val viewModel: InstMeterViewModel by viewModels()
    private val excelPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        val dest = File(cacheDir, "meter_import.xlsx")
        contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { input.copyTo(it) }
        }
        viewModel.importExcel(dest.absolutePath)
    }

    override fun inflateBinding() = ActivityInstMeterBinding.inflate(layoutInflater)

    override fun initView(savedInstanceState: Bundle?) {
        observeUiEvents(viewModel)
        viewModel.projectId = intent.getIntExtra(InstNavigator.EXTRA_PROJECT_ID, 0)
        setupInstHeader(
            binding.toolBar,
            getString(R.string.inst_meter_edit),
            R.menu.menu_inst_edit_meter,
        ) { item ->
            when (item.itemId) {
                R.id.action_search -> {
                    showSearch()
                    true
                }
                R.id.action_add -> {
                    showEdit(null)
                    true
                }
                R.id.action_import -> {
                    excelPicker.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    true
                }
                else -> false
            }
        }
        val adapter = InstMeterAdapter(onItem = { showEdit(it) })
        binding.rvItems.layoutManager = LinearLayoutManager(this)
        binding.rvItems.adapter = adapter
        val deleteBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFEF4444.toInt() }
        val deleteTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 14 * resources.displayMetrics.scaledDensity
            textAlign = Paint.Align.CENTER
        }
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position == RecyclerView.NO_POSITION) return
                val record = adapter.itemAt(position) ?: return
                viewModel.delete(record.id)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean,
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView
                    val bgRect = RectF(
                        (itemView.right + dX).coerceAtLeast(itemView.left.toFloat()),
                        itemView.top.toFloat(),
                        itemView.right.toFloat(),
                        itemView.bottom.toFloat(),
                    )
                    c.drawRoundRect(bgRect, 36f, 36f, deleteBgPaint)
                    val textX = itemView.right - (itemView.right + dX).coerceAtLeast(0f) / 2
                    val textY = itemView.top + itemView.height / 2f -
                        (deleteTextPaint.descent() + deleteTextPaint.ascent()) / 2
                    c.drawText("删除", textX, textY, deleteTextPaint)
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }).attachToRecyclerView(binding.rvItems)
        lifecycleScope.launch {
            viewModel.ui.collect { ui ->
                adapter.submit(ui.items)
                binding.rvItems.isVisible = ui.items.isNotEmpty()
            }
        }
        viewModel.loadOptions()
        viewModel.load()
    }

    private fun showSearch() {
        val meterNo = InstDialogs.field(this, "表号")
        val userNo = InstDialogs.field(this, "户号")
        val address = InstDialogs.field(this, "地址")
        InstDialogs.form(this, getString(R.string.inst_search), buildContent = { box ->
            box.addView(meterNo)
            box.addView(userNo)
            box.addView(address)
        }) {
            viewModel.load(
                meterNo.text.toString().takeIf { it.isNotBlank() },
                userNo.text.toString().takeIf { it.isNotBlank() },
                address.text.toString().takeIf { it.isNotBlank() },
            )
            true
        }
    }

    private fun showEdit(record: InstMeterRecordDto?) {
        val ui = viewModel.ui.value
        fun dropdown(options: List<Pair<String, String>>, selected: String, hint: String) =
            InstDialogs.dropdown(this, hint, options, selected)
        val meterNo = InstDialogs.field(this, "水表编号", record?.meterNo.orEmpty())
        val userNo = InstDialogs.field(this, "用户编号", record?.userNo.orEmpty())
        val address = InstDialogs.field(this, "地址", record?.address.orEmpty())
        val initWater = InstDialogs.field(
            this,
            "初始读数",
            record?.initWater?.toString().orEmpty(),
            InputType.TYPE_CLASS_NUMBER,
        )
        val caliber = dropdown(ui.caliber, record?.caliber.orEmpty(), "请选择口径")
        val type = dropdown(ui.meterType, record?.type.orEmpty(), "请选择类型")
        val factory = dropdown(ui.factory, record?.factory.orEmpty(), "请选择供应商")
        val direction = dropdown(ui.direction, record?.direction.orEmpty(), "请选择方向")
        InstDialogs.form(
            this,
            if (record == null) getString(R.string.inst_add_meter) else "编辑仪表",
            confirmText = "保存",
            buildContent = { box ->
                box.addView(meterNo)
                box.addView(userNo)
                box.addView(address)
                box.addView(initWater)
                box.addView(InstDialogs.labeled(this, "口径", caliber.view))
                box.addView(InstDialogs.labeled(this, "类型", type.view))
                box.addView(InstDialogs.labeled(this, "供应商", factory.view))
                box.addView(InstDialogs.labeled(this, "方向", direction.view))
            },
        ) {
            val meterNoText = meterNo.text?.toString()?.trim()
            val initWaterText = initWater.text?.toString()?.trim()
            if (meterNoText.isNullOrEmpty()) {
                showToast("请输入仪表编号")
                return@form false
            }
            if (initWaterText.isNullOrEmpty()) {
                showToast("请输入初始读数")
                return@form false
            }
            viewModel.save(
                InstAddMeterBody(
                    address = address.text.toString().trim(),
                    caliber = caliber.value,
                    direction = direction.value,
                    factory = factory.value,
                    id = record?.id ?: 0,
                    initWater = initWaterText.toIntOrNull() ?: 0,
                    meterNo = meterNoText,
                    projectId = viewModel.projectId,
                    type = type.value,
                    userNo = userNo.text.toString().trim(),
                ),
                isNew = record == null,
            )
            true
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.projectId != 0) viewModel.load()
    }
}
