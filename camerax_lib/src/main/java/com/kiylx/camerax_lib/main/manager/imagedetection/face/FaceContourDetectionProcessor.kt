package com.kiylx.camerax_lib.main.manager.imagedetection.face

import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.kiylx.camerax_lib.main.manager.imagedetection.base.AnalyzeResultListener
import com.kiylx.camerax_lib.main.manager.imagedetection.base.BaseImageAnalyzer
import java.io.IOException

/**
 * 处理面部数据
 */
class FaceContourDetectionProcessor(
    private val preview: PreviewView,
    private val view: GraphicOverlayView,
) : BaseImageAnalyzer<List<Face>>() {
    var analyzeListener: AnalyzeResultListener? = null

    /**
     * 1500毫秒的间隔
     */
    private var backPress: Int = 1000
    private var timeLast = System.currentTimeMillis()

    /**
     * 注：某些选项不能同时使用
     * 面部分类不能和轮廓检测和特征点检测一起使用
     * 当同时设置 LANDMARK_MODE_NONE、CONTOUR_MODE_ALL、CLASSIFICATION_MODE_NONE 和 PERFORMANCE_MODE_FAST 时，
     * 不会报告检测到的人脸的欧拉 X、欧拉 Y 或欧拉 Z 角度
     */
    private val realTimeOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)//在检测人脸时更注重速度还是准确性，精确模式会检测到比快速模式更少的人脸
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)//轮廓检测
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)//面部特征点
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)//是否将人脸分为不同类别（例如“微笑”和“眼睛睁开”）。
        .setMinFaceSize(0.6f)//人脸最小占图片的百分比
        //.enableTracking() //disable when contour is enable https://developers.google.com/ml-kit/vision/face-detection/android
        .build()

    private val detector = FaceDetection.getClient(realTimeOpts)

    override val graphicOverlayView: GraphicOverlayView
        get() = view
    override val cameraPreview: PreviewView
        get() = preview

    override fun detectInImage(image: InputImage): Task<List<Face>> {
        return detector.process(image)
    }

    override fun stop() {
        try {
            detector.close()
        } catch (e: IOException) {
            Log.e(TAG, "Exception thrown while trying to close Face Detector: $e")
        }
    }

    /**
     * @param results 人脸列表
     * @param overlayView 叠加层
     * @param rect :  imageProxy.image.cropRect: 获取与此帧关联的裁剪矩形。裁剪矩形使用最大分辨率平面中的坐标指定图像中有效像素的区域。
     */
    override fun onSuccess(
        imageProxy: ImageProxy,
        results: List<Face>,
        overlayView: GraphicOverlayView,
    ) {
        //清空上一次识别的面部图像位置数据，
        //添加这一次的图像数据，并刷新叠加层以绘制面部数据
        overlayView.clear()
        //可以用matrix来坐标映射
//        graphicOverlay.scaleMatrix=createMatrix(imageProxy)
        results.forEach {
            //FaceContourGraphic继承自Graphic并实现了自定义的绘制
            val faceGraphic = FaceContourGraphic(overlayView, it, imageProxy.cropRect)
            overlayView.add(faceGraphic)
        }
        //BitmapUtils.getBitmap(imageProxy) //从imageProxy中获取bitmap
        overlayView.postInvalidate()
        if (results.isNotEmpty()) {
            if (System.currentTimeMillis() - timeLast > backPress) {
                analyzeListener?.isSuccess()
                timeLast = System.currentTimeMillis()
            }
        }
    }

    override fun onFailure(e: Exception) {
        Log.w(TAG, "Face Detector failed.$e")
    }

    var matrix: Matrix? = null

    /**
     * 当置为true时，matrix会重建
     */
    var reCreateMatrix: Boolean = false
    /**
     * 来源：
     * https://developer.android.google.cn/training/camerax/transform-output?hl=zh-cn
     */
    fun createMatrix(imageProxy: ImageProxy): Matrix {
        return matrix?.let {
            if (reCreateMatrix) {
                reCreateMatrix=false
                genMatrixInner(imageProxy)
            } else {
                matrix
            }
        } ?: let {
            return@let genMatrixInner(imageProxy)
        }
    }

    /**
     * 生成一个将图像分析的坐标映射到预览视图的矩阵
     * 在使用前置摄像头获得图像的框体后，若是绘制到屏幕，x坐标需要再镜像一下
     * 示例：
     * ```
     * //视图的x轴中心
     * val centerX = overlay.width.toFloat() / 2
     * //前置摄像头反转
     * if (overlay.isFrontMode()) {
     *      rect.apply {
     *          left = centerX + (centerX - left)
     *          right = centerX - (right - centerX)
     *      }
     * }
     *···
     */
    fun genMatrixInner(imageProxy: ImageProxy): Matrix {
        return matrix ?: let {
            val matrix1 = Matrix()
            val cropRect = imageProxy.cropRect
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
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
                view.width.toFloat(),
                0f,
                view.width.toFloat(),
                view.height.toFloat(),
                0f,
                view.height.toFloat()
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
            matrix1.setPolyToPoly(source, 0, destination, 0, 4)
            matrix = matrix1
            return@let matrix1
        }
    }

    companion object {
        private const val TAG = "FaceDetectorProcessor"
    }

}