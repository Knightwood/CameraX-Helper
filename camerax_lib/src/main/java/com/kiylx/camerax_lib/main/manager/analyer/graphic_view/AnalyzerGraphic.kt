package com.kiylx.camerax_lib.main.manager.analyer.graphic_view

import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.ceil

/**
 * 每一个人脸的面部数据都会转换成一个Graphic
 * 经由Graphic绘制到GraphicOverlay上
 * 此类承担着数据坐标的转换
 */
abstract class Graphic(protected val overlay: GraphicOverlayView) {
    var mScale: Float? = null
    var mOffsetX: Float? = null
    var mOffsetY: Float? = null

    abstract fun draw(canvas: Canvas?)

    /**
     * 计算坐标位置偏移和缩放值等
     */
    fun calculateRect(height: Float, width: Float) {
        /**
         * 是横屏的话，返回宽
         */
        fun whenLandScapeModeWidth(): Float {
            return when (overlay.isLandScapeMode()) {
                true -> width
                false -> height
            }
        }

        /**
         * 是横屏的话，返回高
         */
        fun whenLandScapeModeHeight(): Float {
            return when (overlay.isLandScapeMode()) {
                true -> height
                false -> width
            }
        }

        val scaleX = overlay.width.toFloat() / whenLandScapeModeWidth()
        val scaleY = overlay.height.toFloat() / whenLandScapeModeHeight()
        val scale = scaleX.coerceAtLeast(scaleY)
        mScale = scale

        // Calculate offset (we need to center the overlay on the target)
        val offsetX = (overlay.width.toFloat() - ceil(whenLandScapeModeWidth() * scale)) / 2.0f
        val offsetY = (overlay.height.toFloat() - ceil(whenLandScapeModeHeight() * scale)) / 2.0f

        mOffsetX = offsetX
        mOffsetY = offsetY
    }

    fun translateBox(scale: Float, offsetX: Float, offsetY: Float, boundingBoxT: Rect): RectF {
        var topLeft = Point(boundingBoxT.right, boundingBoxT.top)
        topLeft = calcNewPoint(topLeft, Point(), overlay.angle.toFloat())
        var bottomRight = Point(boundingBoxT.left, boundingBoxT.bottom)
        bottomRight = calcNewPoint(bottomRight, Point(), overlay.angle.toFloat())

        val mappedBox = RectF().apply {
            left = boundingBoxT.right * scale + offsetX
            top = boundingBoxT.top * scale + offsetY
            right = boundingBoxT.left * scale + offsetX
            bottom = boundingBoxT.bottom * scale + offsetY
        }
        // for front mode
        if (overlay.isFrontMode()) {
            val centerX = overlay.width.toFloat() / 2
            mappedBox.apply {
                left = centerX + (centerX - left)
                right = centerX - (right - centerX)
            }
        }
        return mappedBox
    }

    fun translateX(horizontal: Float): Float {
        return if (mScale != null && mOffsetX != null && !overlay.isFrontMode()) {
            (horizontal * mScale!!) + mOffsetX!!
        } else if (mScale != null && mOffsetX != null && overlay.isFrontMode()) {
            val centerX = overlay.width.toFloat() / 2
            centerX - ((horizontal * mScale!!) + mOffsetX!! - centerX)
        } else {
            horizontal
        }
    }

    fun translateY(vertical: Float): Float {
        return if (mScale != null && mOffsetY != null) {
            (vertical * mScale!!) + mOffsetY!!
        } else {
            vertical
        }
    }

}