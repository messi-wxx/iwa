package com.cq.iwa.core.media

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

/**
 * 现场照片上的文字卡片：左上角半透明浅色圆角底、深色字。
 * 涂鸦笔迹仍用 [MARK]、[ARROW]。
 */
object PhotoOverlay {

    const val MARK = 0xFFE06B5C.toInt()
    const val ARROW = 0xFF2BA471.toInt()
    private const val CHECK_IN_CARD = 0xB3F3EEE6.toInt()
    private const val CHECK_IN_TEXT = 0xFF241F1A.toInt()

    fun drawTimestamp(canvas: Canvas, imageWidth: Int, imageHeight: Int, text: String) {
        if (text.isBlank() || imageWidth <= 0 || imageHeight <= 0) return
        val textSize = (imageWidth / 18f).coerceIn(36f, 64f)
        val paint = cardPaint(textSize, bold = true)
        val margin = cardMargin(imageWidth)
        val padH = textSize * 0.55f
        val padV = textSize * 0.42f
        val fm = paint.fontMetrics
        val textWidth = paint.measureText(text)
        drawCard(
            canvas,
            margin,
            margin,
            margin + padH * 2 + textWidth,
            margin + padV * 2 + (fm.descent - fm.ascent),
            textSize * 0.22f,
        )
        canvas.drawText(text, margin + padH, margin + padV - fm.ascent, paint)
    }

    /** 抄表打卡：左上角浅色圆角卡片，标题一行、时间一行。 */
    fun drawCheckIn(canvas: Canvas, imageWidth: Int, imageHeight: Int, title: String, time: String) {
        if (title.isBlank() || imageWidth <= 0 || imageHeight <= 0) return
        val titleSize = (imageWidth / 16f).coerceIn(40f, 78f)
        val timeSize = titleSize * 0.58f
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CHECK_IN_TEXT
            textSize = titleSize
            isFakeBoldText = true
        }
        val timePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CHECK_IN_TEXT
            textSize = timeSize
        }
        val contentWidth = maxOf(titlePaint.measureText(title), timePaint.measureText(time))
        val margin = (imageWidth * 0.04f).coerceAtLeast(18f)
        val padH = titleSize * 0.55f
        val padV = titleSize * 0.42f
        val gap = titleSize * 0.18f
        val titleFm = titlePaint.fontMetrics
        val timeFm = timePaint.fontMetrics
        val titleHeight = titleFm.descent - titleFm.ascent
        val timeHeight = timeFm.descent - timeFm.ascent
        val left = margin
        val top = margin
        drawCard(
            canvas,
            left,
            top,
            left + contentWidth + padH * 2,
            top + padV * 2 + titleHeight + gap + timeHeight,
            titleSize * 0.22f,
        )
        canvas.drawText(title, left + padH, top + padV - titleFm.ascent, titlePaint)
        canvas.drawText(
            time,
            left + padH,
            top + padV + titleHeight + gap - timeFm.ascent,
            timePaint,
        )
    }

    fun drawCaption(canvas: Canvas, text: String, imageWidth: Int, padding: Float = 20f) {
        if (text.isBlank() || imageWidth <= 0) return
        val textSize = (imageWidth / 22f).coerceIn(32f, 52f)
        val margin = maxOf(padding, cardMargin(imageWidth))
        val padH = textSize * 0.55f
        val padV = textSize * 0.42f
        val maxWidth = (imageWidth - margin * 2 - padH * 2).toInt().coerceAtLeast(80)
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, cardPaint(textSize, bold = true), maxWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.1f)
            .setIncludePad(true)
            .build()
        drawCard(
            canvas,
            margin,
            margin,
            margin + padH * 2 + layout.width,
            margin + padV * 2 + layout.height,
            textSize * 0.22f,
        )
        canvas.save()
        canvas.translate(margin + padH, margin + padV)
        layout.draw(canvas)
        canvas.restore()
    }

    fun haloStroke(innerWidth: Float): Float = innerWidth * 1.7f

    private fun cardMargin(imageWidth: Int): Float = (imageWidth * 0.04f).coerceAtLeast(18f)

    private fun cardPaint(textSize: Float, bold: Boolean): TextPaint {
        return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CHECK_IN_TEXT
            this.textSize = textSize
            isFakeBoldText = bold
        }
    }

    private fun drawCard(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, radius: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CHECK_IN_CARD }
        canvas.drawRoundRect(RectF(left, top, right, bottom), radius, radius, paint)
    }
}
