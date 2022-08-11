package com.kiylx.camerax_lib.main.manager.imagedetection.face

import android.graphics.Matrix
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import com.blankj.utilcode.util.LogUtils
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
    private val view: GraphicOverlay,
    private val analyzeListener: AnalyzeResultListener? = null,
) :
    BaseImageAnalyzer<List<Face>>() {
    /**
     * 1500毫秒的间隔
     */
    private var backPress: Int = 1000
    private var timeLast = System.currentTimeMillis()

    private val realTimeOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)//精确模式会检测到比快速模式更少的人脸
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)

        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)//检测多张人脸？
        //.setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.5f)//人脸最小占图片的百分比
        //.enableTracking() //disable when contour is enable https://developers.google.com/ml-kit/vision/face-detection/android
        .build()

    private val detector = FaceDetection.getClient(realTimeOpts)

    override val graphicOverlay: GraphicOverlay
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
     * @param graphicOverlay 叠加层
     * @param rect :  imageProxy.image.cropRect: 获取与此帧关联的裁剪矩形。裁剪矩形使用最大分辨率平面中的坐标指定图像中有效像素的区域。
     */
    override fun onSuccess(
        results: List<Face>,
        graphicOverlay: GraphicOverlay,
        rect: Rect,
    ) {
        //清空上一次识别的面部图像位置数据，
        //添加这一次的图像数据，并刷新叠加层以绘制面部数据
        graphicOverlay.clear()
        results.forEach {
            val faceGraphic = FaceContourGraphic(graphicOverlay, it, rect)
            graphicOverlay.add(faceGraphic)
        }
        LogUtils.d("人脸数量：${results.size}")
        graphicOverlay.postInvalidate()
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

    companion object {
        private const val TAG = "FaceDetectorProcessor"
    }

}