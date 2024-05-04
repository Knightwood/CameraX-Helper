package com.kiylx.camera.camerax_analyzer.filevision

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskExecutors
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.kiylx.camera.camerax_analyzer.bitmap.BitmapUtils
import kotlin.math.abs

/**
 * 处理图像文件，将人脸部分裁剪下来
 */
object MyFileProcessor {
    private var imageProcessor: FileVisionProcessor = FileVisionProcessor()

    fun stop() {
        imageProcessor.stop()
    }

    /**
     * 处理传入照片，获取照片中最大的人脸区域，返回裁剪后的bitmap
     */
    fun process(
        contentResolver: ContentResolver,
        uri: Uri,
        block: (bitmap: Bitmap?) -> Unit,
    ) {
        val bitmap = BitmapUtils.getBitmapFromContentUri(contentResolver, uri)
        process(bitmap,block)
    }

    /**
     * 处理传入照片，获取照片中最大的人脸区域，返回裁剪后的bitmap
     */
    fun process(bitmap: Bitmap?, block: (bitmap: Bitmap?) -> Unit) {
        if (bitmap == null)
            return
        else {
            imageProcessor.processBitmap(bitmap) { faces ->
                //这里将在另一个线程里被回调，所以，不能从这里给外面的变量赋值返回
                val result = bitmap.cropImage(faces)
//                Log.d(TAG, "process: bitmap不存在？：  ${result == null}")
                block(result)
            }
            return
        }
    }

    const val TAG = "单张图像处理"
}

class FileVisionProcessor {
    private val executor = ScopedExecutor(TaskExecutors.MAIN_THREAD)
    private val detector: FaceDetector

    init {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)//在检测人脸时更注重速度还是准确性，精确模式会检测到比快速模式更少的人脸
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)//轮廓检测
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)//面部特征点
            //.setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)//是否将人脸分为不同类别（例如“微笑”和“眼睛睁开”）。
            .setMinFaceSize(1.0f)//人脸最小占图片的百分比
            //.enableTracking() //disable when contour is enable https://developers.google.com/ml-kit/vision/face-detection/android
            .build()

        detector = FaceDetection.getClient(options)
        Log.v(TAG, "Face detector options: $options")

    }

    fun stop() {
        detector.close()
    }

    fun processBitmap(bitmap: Bitmap, listener: OnSuccessListener<List<Face>>) {
        detectInImage(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener(executor, listener)
            .addOnFailureListener(
                executor,
                OnFailureListener { e: Exception ->
                    val error = "Failed to process. Error: " + e.localizedMessage
                    Log.d(TAG, error)
                    e.printStackTrace()
                }
            )
    }

    private fun detectInImage(image: InputImage): Task<List<Face>> {
        return detector.process(image)
    }

    companion object {
        const val TAG = "文件处理"
    }
}

fun Bitmap.cropImage(faces: List<Face>): Bitmap? {
    var boxSize = 0
    val maxFace = if (faces.size == 1) {
        faces[0]
    } else {//筛除欧拉角度不符合的
        faces.filter {
            it.checkFace()
        }.findLast {
            val size = it.boundingBox.height() * it.boundingBox.width()
            if (size >= boxSize) {
                boxSize = size
                return@findLast true
            } else {
                return@findLast false
            }
        }
    }
    maxFace?.let {
        Log.d(
            "log",
            "face bounding box: " + it.boundingBox.flattenToString()
        )
        Log.d(
            "log",
            "face Euler Angle X: " + it.headEulerAngleX
        )
        Log.d(
            "log",
            "face Euler Angle Y: " + it.headEulerAngleY
        )
        Log.d(
            "log",
            "face Euler Angle Z: " + it.headEulerAngleZ
        )
        try {
            val reSizeRect = it.boundingBox.reSize(this.width, this.height)
            return Bitmap.createBitmap(this,
                reSizeRect.left,
                reSizeRect.top,
                reSizeRect.width(),
                reSizeRect.height())
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    return null
}

/**
 *
 * 根据传入的宽高限制（widthLimit，heightLimit）
 * 返回一个可以把整个面部裁剪下来的合理的宽高
 */
fun Rect.reSize(widthLimit: Int, heightLimit: Int): Rect {
    val rect = Rect()
    rect.left = if (left - 70 < 0) 0 else left - 70
    rect.right = if (right + 70 > widthLimit) widthLimit else right + 70
    rect.top = if (top - 60 < 0) 0 else top - 60
    rect.bottom = if (bottom + 60 > heightLimit) heightLimit else bottom + 60
    return rect
}
/**
 * 检测欧拉角度
 */
fun Face.checkFace(): Boolean {
    if (abs(this.headEulerAngleY) > 15) {
        return false
    }
    if (abs(this.headEulerAngleX) > 15) {
        return false
    }
    if (abs(this.headEulerAngleZ) > 15) {
        return false
    }
    return true
}
fun Bitmap.cropTest(faces: List<Face>) {
    val maxFace = faces[0]
    maxFace.let {
        Log.v(
            "log",
            "face bounding box: " + it.boundingBox.flattenToString()
        )
        Log.v(
            "log",
            "face Euler Angle X: " + it.headEulerAngleX
        )
        Log.v(
            "log",
            "face Euler Angle Y: " + it.headEulerAngleY
        )
        Log.v(
            "log",
            "face Euler Angle Z: " + it.headEulerAngleZ
        )
    }
}