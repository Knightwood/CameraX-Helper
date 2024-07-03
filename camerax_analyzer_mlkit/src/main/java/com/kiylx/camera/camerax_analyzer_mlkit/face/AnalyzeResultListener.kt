package com.kiylx.camera.camerax_analyzer_mlkit.face

import android.graphics.Bitmap
import com.google.mlkit.vision.face.Face

/**
 * 图像识别的通知接口
 */
fun interface AnalyzeResultListener {
    fun isSuccess(bitmap: Bitmap?, results: List<Face>)
}
