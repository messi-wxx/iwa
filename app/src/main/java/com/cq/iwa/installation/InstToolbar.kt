package com.cq.iwa.installation

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import com.cq.iwa.R
import com.cq.iwa.databinding.IncludeInstToolbarBinding

fun AppCompatActivity.setupInstHeader(
    header: IncludeInstToolbarBinding,
    title: CharSequence,
    menuRes: Int? = null,
    onMenuItem: ((MenuItem) -> Boolean)? = null,
) {
    header.tvTitle.text = title
    header.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    header.tvAction.isVisible = false
    header.btnAction.isVisible = false
    header.btnMore.isVisible = false
    if (menuRes == null || onMenuItem == null) return

    val menu = PopupMenu(this, header.btnMore).menu
    menuInflater.inflate(menuRes, menu)
    when (menu.size()) {
        0 -> Unit
        1 -> bindSingleAction(header, menu.getItem(0), onMenuItem)
        else -> {
            header.btnMore.isVisible = true
            header.btnMore.setOnClickListener { view ->
                showInstOverflow(view, menu, onMenuItem)
            }
        }
    }
}

private fun bindSingleAction(
    header: IncludeInstToolbarBinding,
    item: MenuItem,
    onMenuItem: (MenuItem) -> Boolean,
) {
    if (item.icon != null) {
        header.btnAction.isVisible = true
        header.btnAction.setImageDrawable(item.icon)
        header.btnAction.contentDescription = item.title
        header.btnAction.setOnClickListener { onMenuItem(item) }
    } else {
        header.tvAction.isVisible = true
        header.tvAction.text = item.title
        header.tvAction.setOnClickListener { onMenuItem(item) }
    }
}

private fun showInstOverflow(
    anchor: View,
    menu: Menu,
    onMenuItem: (MenuItem) -> Boolean,
) {
    val items = (0 until menu.size()).map { menu.getItem(it) }
    val content = LayoutInflater.from(anchor.context).inflate(R.layout.popup_inst_menu, null) as LinearLayout
    val popup = PopupWindow(
        content,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        true,
    )
    popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    popup.elevation = 0f
    popup.isOutsideTouchable = true
    popup.isFocusable = true
    items.forEachIndexed { index, item ->
        val row = LayoutInflater.from(anchor.context).inflate(R.layout.item_inst_popup_menu, content, false)
        row.findViewById<TextView>(R.id.tvMenuTitle).text = item.title
        row.findViewById<View>(R.id.divider).isVisible = index < items.lastIndex
        row.setOnClickListener {
            popup.dismiss()
            onMenuItem(item)
        }
        content.addView(row)
    }
    content.measure(
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
    )
    popup.width = content.measuredWidth
    popup.height = content.measuredHeight
    val xOff = anchor.width - popup.width
    popup.showAsDropDown(anchor, xOff, 4, Gravity.NO_GRAVITY)
}
