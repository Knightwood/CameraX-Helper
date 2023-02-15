package com.kiylx.camerax_lib.main.manager.imagedetection.face

/*

这是一个利用face数据的示例，例如，可以筛选出宽度最大，欧拉角在15°以内的面部数据
class FaceContourDetectionProcessorExample(
    private val view: GraphicOverlay,
    private val analyzeListener: AnalyzeResultListener? = null,
) :
    BaseImageAnalyzer<List<Face>>() {
    private var hasFace: TimeInterval = TimeInterval(10000)
    private var successNotify: TimeInterval = TimeInterval(1500)

    //注：某些选项不能同时使用
    private val realTimeOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)//在检测人脸时更注重速度还是准确性，精确模式会检测到比快速模式更少的人脸
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)//轮廓检测
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)//面部特征点
        //.setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)//是否将人脸分为不同类别（例如“微笑”和“眼睛睁开”）。
        .setMinFaceSize(0.6f)//人脸最小占图片的百分比
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
        imageProxy: ImageProxy,
        results: List<Face>,
        graphicOverlay: GraphicOverlay,
        rect: Rect,
    ) {
        graphicOverlay.clear()
        if (results.isNotEmpty()) {
            results.findLastBiggest()?.checkFace(success = {
                val faceGraphic = FaceContourGraphic(graphicOverlay, it, rect)
                graphicOverlay.add(faceGraphic)
                graphicOverlay.postInvalidate()

                successNotify.then {
                    //val bitmap = BitmapUtils.getBitmap(imageProxy)//获取imageProxy中的bitmap
                    analyzeListener?.run {
                        isSuccess(null, null)
                        hasFace(true)
                    }
                }
            },  failed = {
                val faceGraphic = FaceContourGraphic(graphicOverlay, it, rect)
                graphicOverlay.add(faceGraphic)
                graphicOverlay.postInvalidate()
            }, distanceNoMatch = {
                //如果没有符合的面部数据，超过十秒，降低屏幕亮度
                hasFace.then {
                    analyzeListener?.hasFace(false)
                }
            })
        } else {//如果没有面部数据，超过十秒，降低屏幕亮度
            hasFace.then {
                analyzeListener?.hasFace(false)
            }
        }
    }

    */
/**
     * 检查是否符合条件，若是符合条件，执行block块
     *//*

    private fun Face.checkFace(
        success: (face: Face) -> Unit,
        failed: (face: Face) -> Unit,
         distanceNoMatch: (face: Face) -> Unit,
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
          if (boundingBox.width() > 70) {//限制距离
            DrawData.toast = ""
            success(this)
            return true
        } else {
            distanceNoMatch(this)
            return false
        }
    }

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

    override fun onFailure(e: Exception) {
        Log.w(TAG, "Face Detector failed.$e")
    }

    companion object {
        private const val TAG = "FaceDetectorProcessor"
    }

}*/
