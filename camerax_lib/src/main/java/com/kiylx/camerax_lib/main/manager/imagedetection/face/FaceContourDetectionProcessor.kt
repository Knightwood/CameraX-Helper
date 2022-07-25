package com.kiylx.camerax_lib.main.manager.imagedetection.face

import android.graphics.Rect
import android.util.Log
import com.blankj.utilcode.util.LogUtils
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.kiylx.camerax_lib.main.manager.imagedetection.base.AnalyzeResultListener
import com.kiylx.camerax_lib.main.manager.imagedetection.base.BaseImageAnalyzer
import java.io.IOException

class FaceContourDetectionProcessor(
    private val view: GraphicOverlay,
    private val analyzeListener: AnalyzeResultListener?=null,
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

    override fun onSuccess(
        results: List<Face>,
        graphicOverlay: GraphicOverlay,
        rect: Rect,
    ) {
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