package com.kiylx.cameraxexample

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updatePadding
import com.kiylx.camera.camerax_analyzer_mlkit.filevision.MyFileProcessor
import com.kiylx.camera.camerax_analyzer_tensorflow.facedetection.FaceDetection
import com.kiylx.camerax_lib.main.manager.util.setWindowEdgeToEdge
import com.kiylx.camerax_lib.main.manager.util.statusBarTheme
import com.kiylx.cameraxexample.databinding.ActivityTestFileDecBinding
import com.kiylx.store_lib.StoreX

class TestFileDecActivity : AppCompatActivity() {
    private lateinit var model: FaceDetection
    private lateinit var page: ActivityTestFileDecBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        page = ActivityTestFileDecBinding.inflate(layoutInflater)
        setContentView(page.root)
        setWindowEdgeToEdge {
            page.root.updatePadding(top = it.top, bottom = it.bottom)
        }
        statusBarTheme(true)

        findViewById<Button>(R.id.select).setOnClickListener {
            selectFileShow()
        }
        model = FaceDetection.create(
            this.assets,
            TF_OD_API_MODEL_FILE,
            TF_OD_API_LABELS_FILE,
            TF_OD_API_IS_QUANTIZED
        )
    }


    fun selectFileShow() {
        StoreX.with(this).safHelper.selectFile(fileType = "image/*") { uri ->
            val imageView = findViewById<ImageView>(R.id.imageView)
            MyFileProcessor.process(contentResolver, uri) {
                imageView.setImageBitmap(it)
                //处理bitmap,获取面部特征点
                it?.let { it1 ->
                    //将bitmap转换成特定尺寸bitmap
                    val tmp =
                        FaceDetection.convertBitmap(
                            it1
                        )
                    //获取特征点
                    val masks = model.detectionBitmap(tmp)
                    page.tvMasks.setText("特征点:\n" + masks.joinToString(), TextView.BufferType.NORMAL)
                }
            }
        }
    }

    companion object {
        const val TAG = "MAINACTIVITY"

        // MobileFaceNet
        private const val TF_OD_API_INPUT_SIZE = 112
        const val TF_OD_API_IS_QUANTIZED = false
        const val TF_OD_API_MODEL_FILE = "mobile_face_net.tflite"
        const val TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt"

    }
}