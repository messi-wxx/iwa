package com.cq.iwa.installation

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import com.cq.iwa.IwaBaseActivity
import com.cq.iwa.R
import com.cq.iwa.databinding.DialogInstFilterBinding

object InstFilterSheet {

    fun show(
        activity: IwaBaseActivity<*>,
        code: String?,
        address: String?,
        applicant: String?,
        type: List<Int>,
        state: List<Int>,
        beginTime: String? = null,
        endTime: String? = null,
        onApply: (String?, String?, String?, List<Int>, List<Int>, Long?, Long?) -> Unit,
    ) {
        val binding = DialogInstFilterBinding.inflate(LayoutInflater.from(activity))
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.setCancelable(true)

        binding.etCode.setText(code.orEmpty())
        binding.etAddress.setText(address.orEmpty())
        binding.etApplicant.setText(applicant.orEmpty())
        binding.chipType1.isChecked = 1 in type
        binding.chipType2.isChecked = 2 in type
        binding.chipType3.isChecked = 3 in type
        binding.chipState1.isChecked = -2 in state
        binding.chipState2.isChecked = -1 in state
        binding.chipState3.isChecked = 0 in state
        binding.chipState4.isChecked = 1 in state
        binding.chipState5.isChecked = 2 in state
        binding.chipState6.isChecked = 3 in state

        var startMillis = InstDateRange.parseDay(beginTime)
        var endMillis = InstDateRange.parseDay(endTime)
        InstDateRange.bind(binding.dateRangeBar.tvStartDate, binding.dateRangeBar.tvEndDate, startMillis, endMillis)

        val openRange = {
            InstDateRange.pick(
                activity,
                activity.getString(R.string.inst_filter_date),
                startMillis,
                endMillis,
            ) { start, end ->
                startMillis = start
                endMillis = end
                InstDateRange.bind(binding.dateRangeBar.tvStartDate, binding.dateRangeBar.tvEndDate, start, end)
            }
        }
        binding.dateRangeBar.tvStartDate.setOnClickListener { openRange() }
        binding.dateRangeBar.tvEndDate.setOnClickListener { openRange() }
        binding.btnReset.setOnClickListener {
            binding.etCode.setText("")
            binding.etAddress.setText("")
            binding.etApplicant.setText("")
            binding.chipType1.isChecked = false
            binding.chipType2.isChecked = false
            binding.chipType3.isChecked = false
            binding.chipState1.isChecked = false
            binding.chipState2.isChecked = false
            binding.chipState3.isChecked = false
            binding.chipState4.isChecked = false
            binding.chipState5.isChecked = false
            binding.chipState6.isChecked = false
            startMillis = null
            endMillis = null
            InstDateRange.bind(binding.dateRangeBar.tvStartDate, binding.dateRangeBar.tvEndDate, null, null)
        }
        binding.btnConfirm.setOnClickListener {
            val types = buildList {
                if (binding.chipType1.isChecked) add(1)
                if (binding.chipType2.isChecked) add(2)
                if (binding.chipType3.isChecked) add(3)
            }
            val states = buildList {
                if (binding.chipState1.isChecked) add(-2)
                if (binding.chipState2.isChecked) add(-1)
                if (binding.chipState3.isChecked) add(0)
                if (binding.chipState4.isChecked) add(1)
                if (binding.chipState5.isChecked) add(2)
                if (binding.chipState6.isChecked) add(3)
            }
            onApply(
                binding.etCode.text?.toString()?.takeIf { it.isNotBlank() },
                binding.etAddress.text?.toString()?.takeIf { it.isNotBlank() },
                binding.etApplicant.text?.toString()?.takeIf { it.isNotBlank() },
                types,
                states,
                startMillis,
                endMillis,
            )
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.setDimAmount(0.45f)
            val width = (activity.resources.displayMetrics.widthPixels * 0.92f).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setGravity(Gravity.CENTER)
        }
    }
}
