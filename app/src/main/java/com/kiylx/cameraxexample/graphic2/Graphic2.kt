package com.kiylx.cameraxexample.graphic2

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.mlkit.vision.face.Face
import com.kiylx.camerax_lib.main.manager.imagedetection.face.Graphic
import com.kiylx.camerax_lib.main.manager.imagedetection.face.GraphicOverlay

class Graphic2(overlay: GraphicOverlay, val faces: Face) : Graphic(overlay) {
    private val boxPaint: Paint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 5.0f
    }

    override fun draw(canvas: Canvas?) {
        drawBox(canvas)
    }

    private fun drawBox(canvas: Canvas?) {
        val rect = faces.boundingBox
        canvas?.drawRect(rect, boxPaint)
    }
}