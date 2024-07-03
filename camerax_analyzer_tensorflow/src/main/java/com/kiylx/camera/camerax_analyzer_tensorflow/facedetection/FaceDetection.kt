package com.kiylx.camera.camerax_analyzer_tensorflow.facedetection

import android.app.Application
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.Trace
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import kotlin.math.sqrt

/**
 * 使用tensorflow list模型处理面部图像数据，得到192个面部特征点
 */
class FaceDetection private constructor(
    assetManager: AssetManager,
    modelFilename: String,
    labelFilename: String,
    val isModelQuantized: Boolean
) {

    // Number of threads in the java app
    private val NUM_THREADS = 4

    // Pre-allocated buffers.
//    private val labels = Vector<String>()
    private val intValues: IntArray

    // Only return this many results.
    private val NUM_DETECTIONS = 1

    private var imgData: ByteBuffer? = null

    private var tfLite: Interpreter? = null

    // Face Mask Detector Output
//    private val output: Array<FloatArray>

    // outputLocations: array of shape [Batchsize, NUM_DETECTIONS,4]
    // contains the location of detected boxes
    private val outputLocations: Array<Array<FloatArray>>

    // outputClasses: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the classes of detected boxes
    private val outputClasses: Array<FloatArray>

    // outputScores: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the scores of detected boxes
    private val outputScores: Array<FloatArray>

    // numDetections: array of shape [Batchsize]
    // contains the number of detected boxes
    private val numDetections: FloatArray

    init {
        //标签文件
//        val actualFilename =
//            labelFilename.split("file:///android_asset/".toRegex()).toTypedArray()[1]
//        val labelsInput = assetManager.open(actualFilename)
//        val br = BufferedReader(InputStreamReader(labelsInput))
//        var line: String?
//        while (br.readLine().also { line = it } != null) {
//            labels.add(line)
//        }
//        br.close()

        try {
            tfLite = Interpreter(
                loadModelFile(
                    assetManager,
                    modelFilename
                )
            )
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        // Pre-allocate buffers.
        val numBytesPerChannel: Int = if (isModelQuantized) {
            1 // Quantized
        } else {
            4 // Floating point
        }
        val tmp =
            ByteBuffer.allocateDirect(1 * TF_OD_API_INPUT_SIZE * TF_OD_API_INPUT_SIZE * 3 * numBytesPerChannel)
        tmp.order(ByteOrder.nativeOrder())
        imgData = tmp

        intValues = IntArray(TF_OD_API_INPUT_SIZE * TF_OD_API_INPUT_SIZE)

        outputLocations = Array(1) {
            Array(NUM_DETECTIONS) {
                FloatArray(
                    4
                )
            }
        }
        outputClasses =
            Array(1) { FloatArray(NUM_DETECTIONS) }
        outputScores =
            Array(1) { FloatArray(NUM_DETECTIONS) }
        numDetections = FloatArray(1)
    }


    /** Memory-map the model file in Assets.  */
    @Throws(IOException::class)
    private fun loadModelFile(assets: AssetManager, modelFilename: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(modelFilename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }


    fun detectionBitmap(bitmap: Bitmap): FloatArray {
        // Preprocess the image data from 0-255 int to normalized float based
        // on the provided parameters.
        bitmap.getPixels(
            intValues,
            0,
            bitmap.width,
            0,
            0,
            bitmap.width,
            bitmap.height
        )

        imgData!!.rewind()
        for (i in 0 until TF_OD_API_INPUT_SIZE) {
            for (j in 0 until TF_OD_API_INPUT_SIZE) {
                val pixelValue = intValues[i * TF_OD_API_INPUT_SIZE + j]
                if (isModelQuantized) {
                    // Quantized model
                    imgData!!.put((pixelValue shr 16 and 0xFF).toByte())
                    imgData!!.put((pixelValue shr 8 and 0xFF).toByte())
                    imgData!!.put((pixelValue and 0xFF).toByte())
                } else { // Float model
                    imgData!!.putFloat(((pixelValue shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    imgData!!.putFloat(((pixelValue shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    imgData!!.putFloat(((pixelValue and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                }
            }
        }

        val inputArray = with(imgData!!) {
            arrayOf<Any>(this)
        }

        // Here outputMap is changed to fit the Face Mask detector
        val outputMap: MutableMap<Int, Any> = HashMap()

        val embeedings: Array<FloatArray> = Array(1) { FloatArray(OUTPUT_SIZE) }
        outputMap[0] = embeedings

        // Run the inference call.
        Trace.beginSection("run")
        //tfLite.runForMultipleInputsOutputs(inputArray, outputMapBack);
        tfLite!!.runForMultipleInputsOutputs(inputArray, outputMap)
        Trace.endSection()

        //输出特征点
        var res = "[";
        for (i in 0 until embeedings[0].size) {
            res += embeedings[0][i];
            if (i < embeedings[0].size - 1) res += ", ";
        }
        res += "]";
        Log.d(TAG, "detectionBitmap: $res")
        return embeedings[0]
    }

    companion object {
        val TAG = FaceDetection::class.simpleName

        //输入图片尺寸
        val TF_OD_API_INPUT_SIZE = 112

        //private static final int OUTPUT_SIZE = 512;
        private val OUTPUT_SIZE = 192

        // Float model
        private val IMAGE_MEAN = 128.0f
        private val IMAGE_STD = 128.0f

        // MobileFaceNet
        const val TF_OD_API_IS_QUANTIZED = false
        const val TF_OD_API_MODEL_FILE = "mobile_face_net.tflite"
        const val TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt"

        @Volatile
        private var instance: FaceDetection? = null

        fun getInstance(ctx: Application):FaceDetection{
            return instance ?: synchronized(this) {
                instance ?: FaceDetection(
                    ctx.assets,
                    TF_OD_API_MODEL_FILE,
                    TF_OD_API_LABELS_FILE,
                    TF_OD_API_IS_QUANTIZED,
                ).also { instance = it }
            }
        }

        fun create(
            assetManager: AssetManager,
            modelFilename: String,
            labelFilename: String,
            isQuantized: Boolean,
        ) = instance ?: synchronized(this) {
            instance ?: FaceDetection(
                assetManager,
                modelFilename,
                labelFilename,
                isQuantized,
            ).also { instance = it }
        }

        /**
         * 把bitmap缩放到112*112
         */
        fun convertBitmap(bitmap: Bitmap): Bitmap {
            val matrix: Matrix = Matrix()
            matrix.postScale(
                (TF_OD_API_INPUT_SIZE / bitmap.width).toFloat(),
                (TF_OD_API_INPUT_SIZE / bitmap.height).toFloat()
            )
            //人脸图像，112*112大小尺寸的空白bitmap
            val faceBmp = Bitmap.createBitmap(
                TF_OD_API_INPUT_SIZE,
                TF_OD_API_INPUT_SIZE,
                Bitmap.Config.ARGB_8888
            )
            //使用canvas,把传入的bitmap绘制到faceBmp
            val canvas = Canvas(faceBmp)
            canvas.drawBitmap(bitmap, matrix, null)
            canvas.save()
            return faceBmp
        }

        /**
         * 计算两个特征点数组的相似度，返回值越小，越相似
         */
        fun distanceCacl(origin: FloatArray, snd: FloatArray): Float {
            var distance = 0f
            for (i in origin.indices) {
                val diff: Float = origin[i] - snd[i]
                distance += diff * diff
            }
            distance = sqrt(distance.toDouble()).toFloat()
            return distance
        }
    }

}
