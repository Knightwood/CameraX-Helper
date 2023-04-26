package com.kiylx.camerax_lib.main.manager.imagedetection.face

import android.graphics.Rect
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
    private val view: GraphicOverlay,
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
        imageProxy: ImageProxy,
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
        //BitmapUtils.getBitmap(imageProxy) //从imageProxy中获取bitmap
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