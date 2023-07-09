package com.kiylx.cameraxexample

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.kiylx.camerax_lib.main.manager.imagedetection.facedetection.FaceDetection
import com.kiylx.camerax_lib.main.manager.imagedetection.filevision.MyFileProcessor
import com.kiylx.store_lib.StoreX

class TestFileDecActivity : AppCompatActivity() {
    private val model = FaceDetection.create(
        this.assets,
        TF_OD_API_MODEL_FILE,
        TF_OD_API_LABELS_FILE,
        TF_OD_API_IS_QUANTIZED
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_file_dec)
        findViewById<Button>(R.id.select).setOnClickListener {
            selectFileShow()
        }
    }


    fun selectFileShow() {
        StoreX.with(this).safHelper.selectFile(fileType = "image/*") { uri ->
            val imageView = findViewById<ImageView>(R.id.imageView)
            MyFileProcessor.process(contentResolver, uri) {
                imageView.setImageBitmap(it)
                //处理bitmap,获取面部特征点
                it?.let { it1 ->
                    //将bitmap转换成特定尺寸bitmap
                    val tmp = FaceDetection.convertBitmap(it1)
                    //获取特征点
                    val masks = model.detectionBitmap(tmp)
                }
            }
        }
    }

    companion object {
        const val TAG = "MAINACTIVITY"

        // MobileFaceNet
        private const val TF_OD_API_INPUT_SIZE = 112
        private const val TF_OD_API_IS_QUANTIZED = false
        private const val TF_OD_API_MODEL_FILE = "mobile_face_net.tflite"
        private const val TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt"

    }
}