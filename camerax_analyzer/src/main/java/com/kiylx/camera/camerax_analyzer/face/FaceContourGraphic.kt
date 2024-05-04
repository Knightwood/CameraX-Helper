package com.kiylx.camera.camerax_analyzer.face

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import androidx.annotation.ColorInt
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.kiylx.camerax_lib.main.manager.analyer.graphic_view.Graphic
import com.kiylx.camerax_lib.main.manager.analyer.graphic_view.GraphicOverlayView

/**
 * 承担实际上的面部数据的绘，绘制到[GraphicOverlayView]
 * 每一个面部数据都会生成一个[FaceContourGraphic]实例
 */
class FaceContourGraphic(
    overlay: GraphicOverlayView,
    private val face: Face,
    private val imageRect: Rect,
) : Graphic(overlay) {

    private val facePositionPaint: Paint
    private val idPaint: Paint
    private val boxPaint: Paint

    init {
        val selectedColor = Color.GREEN

        facePositionPaint = Paint()
        facePositionPaint.color = selectedColor

        idPaint = Paint()
        idPaint.color = selectedColor
        idPaint.textSize = ID_TEXT_SIZE

        boxPaint = Paint()
        boxPaint.color = selectedColor
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = BOX_STROKE_WIDTH
    }

    override fun draw(canvas: Canvas?) {
        calculateRect(
            imageRect.height().toFloat(),
            imageRect.width().toFloat(),
        )//初始化缩放中心之类的
        drawBox(canvas)//画框
//        drawBox2(canvas)//画框
        drawContours(canvas)//画所有特征点
    }

    /**
     * 使用matrix来做坐标映射
     */
    private fun drawBox2(canvas: Canvas?) {
        val points = floatArrayOf(
            face.boundingBox.left.toFloat(),
            face.boundingBox.top.toFloat(),
            face.boundingBox.right.toFloat(),
            face.boundingBox.bottom.toFloat(),
        )
        overlay.scaleMatrix.mapPoints(points)
        val rect = RectF(
            points[0],
            points[1],
            points[2],
            points[3],
        )
        val centerX = overlay.width.toFloat() / 2
        //前置摄像头反转
        if (overlay.isFrontMode()) {
            rect.apply {
                left = centerX + (centerX - left)
                right = centerX - (right - centerX)
            }
        }
        canvas?.drawRect(rect, boxPaint)
    }


    /**
     * 画框
     */
    private fun drawBox(canvas: Canvas?) {
        val rect = translateBox(mScale!!, mOffsetX!!, mOffsetY!!, face.boundingBox)
        val point = PointF(
            ((rect.right - rect.left) / 2 + rect.left),
            ((rect.bottom - rect.top) / 2 + rect.top)
        )
        canvas?.let {
            it.save()
            it.rotate(-overlay.angle.toFloat(), point.x, point.y)
            it.drawRect(rect, boxPaint)
            it.restore()
        }
    }

    /**
     * 绘制面部所有位置的特征点
     */
    private fun drawContours(canvas: Canvas?) {
        val contours = face.allContours

        contours.forEach {
            it.points.forEach { point ->
                val px = translateX(point.x)
                val py = translateY(point.y)
                canvas?.drawCircle(px, py, FACE_POSITION_RADIUS, facePositionPaint)
            }
        }

        // face
        canvas?.drawFace(FaceContour.FACE, Color.BLUE)

        // left eye
        canvas?.drawFace(FaceContour.LEFT_EYEBROW_TOP, Color.RED)
        canvas?.drawFace(FaceContour.LEFT_EYE, Color.BLACK)
        canvas?.drawFace(FaceContour.LEFT_EYEBROW_BOTTOM, Color.CYAN)

        // right eye
        canvas?.drawFace(FaceContour.RIGHT_EYE, Color.DKGRAY)
        canvas?.drawFace(FaceContour.RIGHT_EYEBROW_BOTTOM, Color.GRAY)
        canvas?.drawFace(FaceContour.RIGHT_EYEBROW_TOP, Color.GREEN)

        // nose
        canvas?.drawFace(FaceContour.NOSE_BOTTOM, Color.LTGRAY)
        canvas?.drawFace(FaceContour.NOSE_BRIDGE, Color.MAGENTA)

        // rip
        canvas?.drawFace(FaceContour.LOWER_LIP_BOTTOM, Color.WHITE)
        canvas?.drawFace(FaceContour.LOWER_LIP_TOP, Color.YELLOW)
        canvas?.drawFace(FaceContour.UPPER_LIP_BOTTOM, Color.GREEN)
        canvas?.drawFace(FaceContour.UPPER_LIP_TOP, Color.CYAN)
    }

    /**
     * 绘制面部特定位置的特征点
     */
    private fun Canvas.drawFace(facePosition: Int, @ColorInt selectedColor: Int) {
        val contour = face.getContour(facePosition)//面部特征点
        val path = Path()
        contour?.points?.forEachIndexed { index, pointF ->
            if (index == 0) {
                path.moveTo(
                    translateX(pointF.x),
                    translateY(pointF.y)
                )
            }
            path.lineTo(
                translateX(pointF.x),
                translateY(pointF.y)
            )
        }
        val paint = Paint().apply {
            color = selectedColor
            style = Paint.Style.STROKE
            strokeWidth = BOX_STROKE_WIDTH
        }
        drawPath(path, paint)
    }

    companion object {
        private const val FACE_POSITION_RADIUS = 4.0f
        private const val ID_TEXT_SIZE = 30.0f
        private const val BOX_STROKE_WIDTH = 5.0f
    }

}