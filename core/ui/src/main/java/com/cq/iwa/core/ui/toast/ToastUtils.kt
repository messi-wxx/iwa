package com.cq.iwa.core.ui.toast

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.cq.iwa.core.ui.R

object ToastUtils {

    private var toast: Toast? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun show(context: Context, message: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
        if (message.isBlank()) return
        val app = context.applicationContext
        val action = Runnable {
            runCatching {
                toast?.cancel()
                toast = createToast(app, message, duration).also { it.show() }
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run()
        } else {
            mainHandler.post(action)
        }
    }

    fun cancel() {
        mainHandler.post {
            toast?.cancel()
            toast = null
        }
    }

    @Suppress("DEPRECATION")
    private fun createToast(context: Context, message: CharSequence, duration: Int): Toast {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_toast, null)
        view.findViewById<TextView>(R.id.tvToastMessage).text = message
        val offsetY = (64 * context.resources.displayMetrics.density).toInt()
        return Toast(context).apply {
            setView(view)
            this.duration = duration
            setGravity(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM, 0, offsetY)
        }
    }
}
