package com.kiylx.cameraxexample.graphic2

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.mlkit.vision.face.Face
import com.kiylx.camerax_lib.main.manager.analyer.graphic_view.Graphic
import com.kiylx.camerax_lib.main.manager.analyer.graphic_view.GraphicOverlayView

class Graphic2(overlay: GraphicOverlayView, val faces: Face) : Graphic(overlay) {
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