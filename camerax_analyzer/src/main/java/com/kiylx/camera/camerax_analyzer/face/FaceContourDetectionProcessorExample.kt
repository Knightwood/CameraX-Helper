package com.kiylx.camera.camerax_analyzer.face
//示例
/*

import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import com.zenglb.camerax.main.manager.imagedetection.base.AnalyzeResultListener
import com.zenglb.camerax.main.manager.imagedetection.base.BaseImageAnalyzer
import com.zenglb.camerax.main.manager.util.TimeInterval
import java.io.IOException
import kotlin.math.abs

class FaceContourDetectionProcessor(
    private val view: GraphicOverlayView,
    private val analyzeListener: AnalyzeResultListener? = null,
) :
    BaseImageAnalyzer<List<Face>>() {
    private var hasFace: TimeInterval = TimeInterval(10000)
    private var successNotify: TimeInterval = TimeInterval(1500)
    private var faceCountNotify: TimeInterval = TimeInterval(2000)

    /**
     * 注：某些选项不能同时使用
     * 面部分类不能和轮廓检测和特征点检测一起使用
     * 当同时设置 LANDMARK_MODE_NONE、CONTOUR_MODE_ALL、CLASSIFICATION_MODE_NONE 和 PERFORMANCE_MODE_FAST 时，
     * 不会报告检测到的人脸的欧拉 X、欧拉 Y 或欧拉 Z 角度
     */
    private val realTimeOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)//在检测人脸时更注重速度还是准确性，精确模式会检测到比快速模式更少的人脸
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)//轮廓检测
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)//面部特征点
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)//是否将人脸分为不同类别（例如“微笑”和“眼睛睁开”）。
        .setMinFaceSize(0.6f)//人脸最小占图片的百分比
        //.enableTracking() //disable when contour is enable https://developers.google.com/ml-kit/vision/face-detection/android
        .build()

    private val detector = FaceDetection.getClient(realTimeOpts)

    override val graphicOverlay: GraphicOverlayView
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
        graphicOverlay: GraphicOverlayView,
        rect: Rect,
        imageProxy: ImageProxy,
    ) {
        graphicOverlay.clear()
//        if (results.isNotEmpty()) {
            results.filterFace().run {
                if (this.isEmpty()) {
                    //如果没有面部数据，超过十秒，降低屏幕亮度
                    hasFace.then {
                        analyzeListener?.hasFace(false)
                    }
                } else {
                    for (face in this) {
                        val faceGraphic = FaceContourGraphic(graphicOverlay, face, rect)
                        graphicOverlay.add(faceGraphic)
                        graphicOverlay.postInvalidate()
                    }
                    faceCountNotify.then {//每隔几s通知人脸数量
                        analyzeListener?.faceCount(this.size)
                    }
                    this.findLastBiggest()?.checkFace(success = {
                        successNotify.then {
                            //val bitmap = BitmapUtils.getBitmap(imageProxy)//获取imageProxy中的bitmap
                            analyzeListener?.run {
                                isSuccess(null, null)
                                hasFace(true)
                            }
                        }
                    }, failed = {

                    })
                }
            }
//        } else {//如果没有面部数据，超过十秒，降低屏幕亮度
//            hasFace.then {
//                analyzeListener?.hasFace(false)
//            }
//        }
    }

    /**
     * 检查是否符合条件，若是符合条件，执行block块
     */
    private fun Face.checkFace(
        success: (face: Face) -> Unit,
        failed: (face: Face) -> Unit,
    ): Boolean {

        if (abs(headEulerAngleY) > 15) {
            DrawData.toast = "不要左顾右盼"
            failed(this)
            return false
        }
        if (abs(headEulerAngleX) > 15) {
            DrawData.toast = "不要仰头低头"
            failed(this)
            return false
        }
        if (abs(headEulerAngleZ) > 15) {
            DrawData.toast = "不要歪头"
            failed(this)
            return false
        }
        DrawData.toast = ""
        success(this)
        return true

    }

    private fun List<Face>.findLastBiggest(): Face? {
        if (this.size == 1)
            return this[0]
        else {
            var boxWidth = 0
            return findLast {
//                val left = it.getContour(FaceContour.LEFT_EYEBROW_TOP)!!.points[8]
//                val right = it.getContour(FaceContour.RIGHT_EYEBROW_TOP)!!.points[0]
//                val width = abs(left.x - right.x)
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

    override fun onFailure(e: Exception) {
        Log.w(TAG, "Face Detector failed.$e")
    }

    companion object {
        private const val TAG = "tty1-FaceDetectorProcessor"
    }

}

*/
