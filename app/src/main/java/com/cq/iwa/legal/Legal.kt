package com.cq.iwa.legal

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import androidx.annotation.ColorInt
import com.cq.iwa.R

object Legal {
    const val PRIVACY_URL = "https://wx.aql.cn/aql/app/home/Privacy"
    const val ICP_NO = "渝ICP备12002102号-3A"
    const val BEIAN_URL = "https://beian.miit.gov.cn/"

    fun openPrivacyPolicy(context: Context) {
        context.startActivity(
            WebPageActivity.intent(
                context = context,
                title = context.getString(R.string.privacy_policy_title),
                url = PRIVACY_URL,
            ),
        )
    }

    fun openBeian(context: Context) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BEIAN_URL)))
    }

    fun linkAll(
        text: CharSequence,
        link: String,
        @ColorInt color: Int,
        onClick: () -> Unit,
    ): SpannableString {
        val spannable = SpannableString(text)
        var index = text.toString().indexOf(link)
        while (index >= 0) {
            spannable.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) = onClick()

                    override fun updateDrawState(ds: TextPaint) {
                        ds.color = color
                        ds.isUnderlineText = true
                        ds.bgColor = Color.TRANSPARENT
                    }
                },
                index,
                index + link.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            index = text.toString().indexOf(link, index + link.length)
        }
        return spannable
    }
}
