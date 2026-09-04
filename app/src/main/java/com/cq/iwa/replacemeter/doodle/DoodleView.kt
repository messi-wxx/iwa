package com.cq.iwa.replacemeter.doodle

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.cq.iwa.core.media.PhotoOverlay
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class DoodleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    enum class GraphType { OVAL, ARROW }

    var onRevertChanged: ((Boolean) -> Unit)? = null

    private var origin: Bitmap? = null
    private var overlayText: String = ""
    private var graphType = GraphType.OVAL
    private var editable = false
    private val graphs = mutableListOf<Graph>()
    private var draft: Graph? = null
    private var selected: Graph? = null
    private var dragMode = DragMode.NONE
    private val start = PointF()
    private val move = PointF()
    private var deltaX = 0f
    private var deltaY = 0f
    private val click = PointF()
    private val originStart = PointF()
    private val originEnd = PointF()

    private val bitmapPaint = Paint(Paint.DITHER_FLAG)
    private val ovalHalo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = PhotoOverlay.haloStroke(dp(3f))
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val ovalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = PhotoOverlay.MARK
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val arrowHalo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = dp(5f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = PhotoOverlay.ARROW
        style = Paint.Style.FILL
        strokeWidth = dp(3f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = PhotoOverlay.MARK
        style = Paint.Style.STROKE
        strokeWidth = 1f
        pathEffect = DashPathEffect(floatArrayOf(dp(3.5f), dp(2.5f)), 0f)
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = PhotoOverlay.MARK
        style = Paint.Style.FILL
    }
    private val arrowPath = Path()
    private val validMove = dp(6f)
    private val clickPad = dp(8f)
    private val dotRadius = dp(8f)

    fun setOriginBitmap(bitmap: Bitmap) {
        origin?.takeIf { !it.isRecycled }?.recycle()
        origin = bitmap
        scaleOrigin()
        invalidate()
    }

    fun setOverlayText(text: String) {
        overlayText = text
        invalidate()
    }

    fun setGraphType(type: GraphType) {
        clearGraphFocus()
        graphType = type
        notifyRevert()
    }

    fun setEditable(value: Boolean) {
        editable = value
    }

    fun canRevert(): Boolean = selected != null

    fun revert() {
        val current = selected ?: return
        graphs.remove(current)
        selected = null
        notifyRevert()
        invalidate()
    }

    fun exportBitmap(): Bitmap {
        val out = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val keep = selected
        selected = null
        draw(Canvas(out))
        selected = keep
        return out
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        scaleOrigin()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bitmap = origin
        if (bitmap != null && !bitmap.isRecycled) {
            canvas.drawBitmap(bitmap, 0f, 0f, bitmapPaint)
            if (overlayText.isNotBlank()) {
                PhotoOverlay.drawCaption(canvas, overlayText, bitmap.width)
            }
        }
        graphs.forEach { drawGraph(canvas, it) }
        draft?.takeIf { it.passed }?.let { drawGraph(canvas, it) }
        selected?.let { graph ->
            val color = if (graph.type == GraphType.ARROW) PhotoOverlay.ARROW else PhotoOverlay.MARK
            framePaint.color = color
            dotPaint.color = color
            canvas.drawRect(graph.startX, graph.startY, graph.endX, graph.endY, framePaint)
            canvas.drawCircle(graph.startX, graph.startY, dotRadius, dotPaint)
            canvas.drawCircle(graph.endX, graph.endY, dotRadius, dotPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!editable) return super.onTouchEvent(event)
        parent?.requestDisallowInterceptTouchEvent(true)
        move.set(event.x, event.y)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                start.set(move)
                deltaX = 0f
                deltaY = 0f
                val current = selected
                if (current != null && hitGraph(current, move.x, move.y)) {
                    click.set(move)
                    originStart.set(current.startX, current.startY)
                    originEnd.set(current.endX, current.endY)
                    dragMode = when {
                        inDot(current.startX, current.startY, move.x, move.y) -> DragMode.START
                        inDot(current.endX, current.endY, move.x, move.y) -> DragMode.END
                        else -> DragMode.MOVE
                    }
                } else {
                    selected = null
                    dragMode = DragMode.NONE
                    draft = Graph(graphType, start.x, start.y, start.x, start.y)
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                deltaX += abs(move.x - start.x)
                deltaY += abs(move.y - start.y)
                val current = selected
                if (current != null && dragMode != DragMode.NONE) {
                    val dx = move.x - click.x
                    val dy = move.y - click.y
                    when (dragMode) {
                        DragMode.MOVE -> {
                            current.startX = originStart.x + dx
                            current.startY = originStart.y + dy
                            current.endX = originEnd.x + dx
                            current.endY = originEnd.y + dy
                        }
                        DragMode.START -> {
                            current.startX = originStart.x + dx
                            current.startY = originStart.y + dy
                        }
                        DragMode.END -> {
                            current.endX = originEnd.x + dx
                            current.endY = originEnd.y + dy
                        }
                        DragMode.NONE -> Unit
                    }
                } else {
                    val graph = draft ?: return true
                    if (deltaX > validMove || deltaY > validMove) {
                        graph.passed = true
                        graph.endX = move.x
                        graph.endY = move.y
                    }
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                if (deltaX < dp(2f) && deltaY < dp(2f)) {
                    draft = null
                    selectAt(move.x, move.y)
                } else if (selected == null) {
                    draft?.takeIf { it.passed }?.let {
                        graphs += it
                        selected = it
                    }
                    draft = null
                }
                dragMode = DragMode.NONE
                notifyRevert()
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        origin?.takeIf { !it.isRecycled }?.recycle()
        origin = null
    }

    private fun drawGraph(canvas: Canvas, graph: Graph) {
        if (!graph.passed) return
        when (graph.type) {
            GraphType.OVAL -> {
                val oval = RectF(graph.startX, graph.startY, graph.endX, graph.endY)
                canvas.drawOval(oval, ovalHalo)
                canvas.drawOval(oval, ovalPaint)
            }
            GraphType.ARROW -> {
                drawArrow(canvas, graph.startX, graph.startY, graph.endX, graph.endY, arrowHalo)
                drawArrow(canvas, graph.startX, graph.startY, graph.endX, graph.endY, arrowPaint)
            }
        }
    }

    private fun drawArrow(canvas: Canvas, sx: Float, sy: Float, ex: Float, ey: Float, paint: Paint) {
        val size = 8f
        val count = 30f
        val x = ex - sx
        val y = ey - sy
        val r = hypot(x.toDouble(), y.toDouble()).toFloat().takeIf { it > 0f } ?: return
        val zx = ex - count * x / r
        val zy = ey - count * y / r
        val xz = zx - sx
        val yz = zy - sy
        val zr = sqrt((xz * xz + yz * yz).toDouble()).toFloat().takeIf { it > 0f } ?: return
        arrowPath.reset()
        arrowPath.moveTo(sx, sy)
        arrowPath.lineTo(zx + size * yz / zr, zy - size * xz / zr)
        arrowPath.lineTo(zx + size * 2 * yz / zr, zy - size * 2 * xz / zr)
        arrowPath.lineTo(ex, ey)
        arrowPath.lineTo(zx - size * 2 * yz / zr, zy + size * 2 * xz / zr)
        arrowPath.lineTo(zx - size * yz / zr, zy + size * xz / zr)
        arrowPath.close()
        canvas.drawPath(arrowPath, paint)
    }

    private fun selectAt(x: Float, y: Float) {
        selected = graphs.asReversed().firstOrNull { hitGraph(it, x, y) }
        selected?.let {
            graphs.remove(it)
            graphs += it
        }
    }

    private fun hitGraph(graph: Graph, x: Float, y: Float): Boolean {
        val rect = RectF(
            min(graph.startX, graph.endX) - clickPad,
            min(graph.startY, graph.endY) - clickPad,
            max(graph.startX, graph.endX) + clickPad,
            max(graph.startY, graph.endY) + clickPad,
        )
        return rect.contains(x, y)
    }

    private fun inDot(cx: Float, cy: Float, x: Float, y: Float): Boolean {
        return RectF(cx - dotRadius, cy - dotRadius, cx + dotRadius, cy + dotRadius).contains(x, y)
    }

    private fun clearGraphFocus() {
        selected = null
        invalidate()
    }

    private fun notifyRevert() {
        onRevertChanged?.invoke(canRevert())
    }

    private fun scaleOrigin() {
        val bitmap = origin ?: return
        if (width <= 0 || height <= 0) return
        if (bitmap.width == width && bitmap.height == height) return
        origin = Bitmap.createScaledBitmap(bitmap, width, height, true)
        if (origin != bitmap && !bitmap.isRecycled) bitmap.recycle()
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private enum class DragMode { NONE, MOVE, START, END }

    private class Graph(
        val type: GraphType,
        var startX: Float,
        var startY: Float,
        var endX: Float,
        var endY: Float,
        var passed: Boolean = false,
    )
}
