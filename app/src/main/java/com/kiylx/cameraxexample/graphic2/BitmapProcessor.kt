package com.kiylx.cameraxexample.graphic2

import android.graphics.Bitmap
import android.view.View
import com.google.android.gms.tasks.OnSuccessListener
import com.google.mlkit.vision.face.Face
import com.kiylx.camerax_lib.main.manager.imagedetection.base.AnalyzeResultListener
import com.kiylx.camerax_lib.main.manager.imagedetection.face.GraphicOverlayView
import com.kiylx.camerax_lib.main.manager.imagedetection.filevision.FileVisionProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

object BitmapProcessor {
    private val imageProcessor: FileVisionProcessor = FileVisionProcessor()
    var analyzeListener: AnalyzeResultListener? = null

    fun stop() {
        imageProcessor.stop()
    }

    /**
     * @param bitmap 待处理识别的图像
     * @param block 当处理成功时的回调
     */
    suspend fun process(bitmap: Bitmap, block: OnSuccessListener<List<Face>>) =
        withContext(Dispatchers.IO) {
            imageProcessor.processBitmap(bitmap, block)
        }

    /**
     * 这就是一个普通的方法，虽然叫onSuccess，但与图像识别处理没有一点关系
     *
     */
    fun onSuccess(
        results: List<Face>,
        graphicOverlay: GraphicOverlayView,
    ) {
        graphicOverlay.clear()
        results.filterFace().run {
            if (this.isNotEmpty()) {
                for (face in this) {
                    val faceGraphic = Graphic2(graphicOverlay, face)
                    graphicOverlay.add(faceGraphic)
                    graphicOverlay.postInvalidate()
                }
                analyzeListener?.isSuccess()
            }
        }
    }

    //检查面部的框体是否接近预览视图的中心，
    private fun checkRectF(view: View, face: Face): Boolean {
        val centerX = view.width.toFloat() / 2
        val rectCenterX = face.boundingBox.centerX()
        return abs(centerX - rectCenterX) < 300
    }

    /**
     * 查找最大的那个
     */
    private fun List<Face>.findLastBiggest(): Face? {
        if (this.size == 1)
            return this[0]
        else {
            var boxWidth = 0
            return findLast {
                val width = it.boundingBox.width()
                if (width > boxWidth) {
                    boxWidth = width
                    return@findLast true
                } else {
                    return@findLast false
                }
            }
        }
    }

    /**
     * 筛选出框体大于70的面部数据
     */
    private fun List<Face>.filterFace(): List<Face> {
        val result = this.filter {
            return@filter it.boundingBox.width() > 70
        }
        return result
    }
}