package com.cq.iwa.core.media

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

/**
 * 叠在现场照片上的文字：白字 + 品牌海军蓝描边 + 半透明底条，
 * 避免纯红水印和锈表、红砖、反光金属撞色。
 */
object PhotoOverlay {

    const val NAVY = 0xFF12344D.toInt()
    const val BAR = 0xB312344D.toInt()
    const val FILL = Color.WHITE
    const val MARK = 0xFFE06B5C.toInt()
    const val ARROW = 0xFF2BA471.toInt()

    fun drawTimestamp(canvas: Canvas, imageWidth: Int, imageHeight: Int, text: String) {
        if (text.isBlank() || imageWidth <= 0 || imageHeight <= 0) return
        val textSize = (imageWidth / 22f).coerceIn(28f, 56f)
        val margin = (imageWidth * 0.04f).coerceAtLeast(16f)
        val maxWidth = (imageWidth - margin * 2).toInt().coerceAtLeast(80)
        val layout = outlinedLayout(text, textSize, maxWidth)
        val padH = textSize * 0.45f
        val padV = textSize * 0.28f
        val top = imageHeight - margin - layout.height - padV * 2
        val left = margin
        drawBar(
            canvas,
            left - padH,
            top,
            left + layout.width + padH,
            top + layout.height + padV * 2,
            textSize * 0.35f,
        )
        canvas.save()
        canvas.translate(left, top + padV)
        drawOutlined(canvas, text, textSize, maxWidth)
        canvas.restore()
    }

    fun drawCaption(canvas: Canvas, text: String, imageWidth: Int, padding: Float = 20f) {
        if (text.isBlank() || imageWidth <= 0) return
        val textSize = (imageWidth / 24f).coerceIn(26f, 42f)
        val maxWidth = (imageWidth - padding * 2 - textSize).toInt().coerceAtLeast(80)
        val layout = outlinedLayout(text, textSize, maxWidth)
        val padH = textSize * 0.45f
        val padV = textSize * 0.28f
        drawBar(
            canvas,
            padding - padH * 0.2f,
            padding - padV * 0.2f,
            padding + layout.width + padH,
            padding + layout.height + padV,
            textSize * 0.35f,
        )
        canvas.save()
        canvas.translate(padding, padding)
        drawOutlined(canvas, text, textSize, maxWidth)
        canvas.restore()
    }

    fun haloStroke(innerWidth: Float): Float = innerWidth * 1.7f

    private fun drawOutlined(canvas: Canvas, text: String, textSize: Float, maxWidth: Int) {
        outlinedLayout(text, textSize, maxWidth, stroke = true).draw(canvas)
        outlinedLayout(text, textSize, maxWidth, stroke = false).draw(canvas)
    }

    private fun outlinedLayout(
        text: String,
        textSize: Float,
        maxWidth: Int,
        stroke: Boolean = false,
    ): StaticLayout {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            isFakeBoldText = true
            if (stroke) {
                color = NAVY
                style = Paint.Style.STROKE
                strokeWidth = (textSize / 7f).coerceAtLeast(3f)
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            } else {
                color = FILL
                style = Paint.Style.FILL
            }
        }
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.1f)
            .setIncludePad(true)
            .build()
    }

    private fun drawBar(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, radius: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BAR }
        canvas.drawRoundRect(RectF(left, top, right, bottom), radius, radius, paint)
    }
}
