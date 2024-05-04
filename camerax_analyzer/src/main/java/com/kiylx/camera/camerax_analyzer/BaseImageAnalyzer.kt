package com.kiylx.camera.camerax_analyzer

import android.annotation.SuppressLint
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.kiylx.camerax_lib.main.manager.analyer.graphic_view.GraphicOverlayView
import com.kiylx.camerax_lib.main.manager.util.*

abstract class BaseImageAnalyzer<T> : ImageAnalysis.Analyzer {

    abstract val graphicOverlayView: GraphicOverlayView
    abstract val cameraPreview: PreviewView

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        mediaImage?.let {
            detectInImage(InputImage.fromMediaImage(it, rotationDegrees))
                .addOnSuccessListener { results ->
                    //坐标转换
                    //MatrixPoint.matrix = getCorrectionMatrix(imageProxy, cameraPreview)//生成转换矩阵
                    //Log.e("面部","${rotationDegrees}")
                    onSuccess(
                        imageProxy,
                        results,
                        graphicOverlayView,
                    )
                }
                .addOnFailureListener {
                    graphicOverlayView.clear()
                    graphicOverlayView.postInvalidate()
                    onFailure(it)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    abstract fun stop()

    /**
     * 分析图像
     */
    protected abstract fun detectInImage(image: InputImage): Task<T>

    /**
     *@param rect :  imageProxy.image.cropRect: 获取与此帧关联的裁剪矩形。裁剪矩形使用最大分辨率平面中的坐标指定图像中有效像素的区域。
     */
    protected abstract fun onSuccess(
        imageProxy:ImageProxy,
        results: T,
        overlayView: GraphicOverlayView,
    )

    protected abstract fun onFailure(e: Exception)

}

/**
 * 全局的坐标转换矩阵
 */
object MatrixPoint {
    var matrix: Matrix? = null
}

/**
 * 谷歌示例代码
 */
fun getCorrectionMatrix(imageProxy: ImageProxy, previewView: PreviewView): Matrix {
    val cropRect = imageProxy.cropRect
    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
    val matrix = Matrix()

    // A float array of the source vertices (crop rect) in clockwise order.
    val source = floatArrayOf(
        cropRect.left.toFloat(),
        cropRect.top.toFloat(),
        cropRect.right.toFloat(),
        cropRect.top.toFloat(),
        cropRect.right.toFloat(),
        cropRect.bottom.toFloat(),
        cropRect.left.toFloat(),
        cropRect.bottom.toFloat()
    )

    // A float array of the destination vertices in clockwise order.
    val destination = floatArrayOf(
        0f,
        0f,
        previewView.width.toFloat(),
        0f,
        previewView.width.toFloat(),
        previewView.height.toFloat(),
        0f,
        previewView.height.toFloat()
    )

    // The destination vertexes need to be shifted based on rotation degrees. The
    // rotation degree represents the clockwise rotation needed to correct the image.

    // Each vertex is represented by 2 float numbers in the vertices array.
    val vertexSize = 2
    // The destination needs to be shifted 1 vertex for every 90° rotation.
    val shiftOffset = rotationDegrees / 90 * vertexSize;
    val tempArray = destination.clone()
    for (toIndex in source.indices) {
        val fromIndex = (toIndex + shiftOffset) % source.size
        destination[toIndex] = tempArray[fromIndex]
    }
    matrix.setPolyToPoly(source, 0, destination, 0, 4)
    return matrix
}