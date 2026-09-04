package com.cq.iwa.feature.readmeter.ui

object MeterExtInfoDisplay {
    const val EMPTY = "暂无"
    const val DEBT_EMPTY = "暂无欠费"
    const val DEBT_LABEL = "欠费信息"

    fun displayValue(label: String, value: String?): String {
        return if (label == DEBT_LABEL) formatDebtAmount(value) else value.orEmpty().ifBlank { EMPTY }
    }

    fun isArrears(value: String?): Boolean {
        val amount = parseDebtAmount(value) ?: return false
        return amount.isNotBlank() && amount != "0"
    }

    fun formatDebtAmount(value: String?): String {
        val amount = parseDebtAmount(value)
        return if (amount != null && amount.isNotBlank() && amount != "0") "¥$amount" else DEBT_EMPTY
    }

    fun parseDebtDate(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            val parts = value.split("/")
            if (parts.size != 3) return null
            val date = parts[2].substringAfter("：")
                .substringAfter("(")
                .substringBefore(")")
                .replace("截止", "")
                .trim()
            date.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun parseDebtAmount(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            val parts = value.split("/")
            if (parts.size != 3) return null
            val amountPart = parts[2].split("：")
            if (amountPart.size < 2) return null
            amountPart[1].substringBefore("(").trim().takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
