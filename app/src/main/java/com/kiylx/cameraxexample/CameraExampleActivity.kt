package com.kiylx.cameraxexample

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setMargins
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.SizeUtils
import com.google.mlkit.vision.face.Face
import com.kiylx.camera.camerax_analyzer_mlkit.face.AnalyzeResultListener
import com.kiylx.camera.camerax_analyzer_mlkit.face.FaceContourDetectionProcessor
import com.kiylx.camera.camerax_analyzer_mlkit.filevision.MyFileProcessor
import com.kiylx.camera.camerax_analyzer_mlkit.filevision.cropImage
import com.kiylx.camera.camerax_analyzer_tensorflow.faceantispoofing.FaceAntiSpoofingHolder
import com.kiylx.camerax_lib.R
import com.kiylx.camerax_lib.main.manager.CameraHolder
import com.kiylx.camerax_lib.main.manager.analyer.graphic_view.GraphicOverlayView
import com.kiylx.camerax_lib.main.manager.model.FlashModel
import com.kiylx.camerax_lib.main.manager.model.ManagerConfig
import com.kiylx.camerax_lib.main.manager.model.UseCaseMode
import com.kiylx.camerax_lib.main.manager.video.CameraRecordQuality
import com.kiylx.camerax_lib.main.store.ImageCaptureConfig
import com.kiylx.camerax_lib.main.store.SaveFileData
import com.kiylx.camerax_lib.main.store.VideoRecordConfig
import com.kiylx.camerax_lib.main.ui.BaseCameraXActivity
import com.kiylx.camerax_lib.view.ControllerPanelUseCaseMode
import com.kiylx.cameraxexample.graphic2.BitmapProcessor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class CameraExampleActivity : BaseCameraXActivity() {
    private lateinit var antiHelper: FaceAntiSpoofingHolder
    lateinit var tv: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        antiHelper = FaceAntiSpoofingHolder.getInstance(application)

        //实际上，你可以使用provideView方法提供自己的布局和控制面板，
        //这里只是为了简单的展示结果
        tv = TextView(this)
        val lp = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
        )
        lp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        lp.setMargins(SizeUtils.dp2px(55f))
        tv.textSize = 18f
        tv.layoutParams = lp
        (rootPage as ConstraintLayout).addView(tv)
    }

    /**
     * 这里直接构建了配置，我没有使用intent传入配置。
     */
    override fun configAll(intent: Intent): ManagerConfig {
        val useImageDetection = intent.getBooleanExtra(ImageDetection, false)
        //视频录制配置(可选)
        val videoRecordConfig = VideoRecordConfig(
            quality = CameraRecordQuality.HD,//设置视频拍摄质量
//            asPersistentRecording = true,//实验特性，保持长时间录制
//            fileSizeLimit=5.mb, //文件大限制,单位bytes
//            durationLimitMillis =1000*15, //录制时长限制，单位毫秒
        )
        //拍照配置(可选)
        val imageCaptureConfig = ImageCaptureConfig()
        //整体的配置
        return ManagerConfig().apply {
            this.recordConfig = videoRecordConfig
            //这里指定了用例组合，当然你也可以调用UseCaseMode.customGroup方法自定义用例组合
            this.useCaseMode =
                if (useImageDetection) UseCaseMode.imageAnalysis else UseCaseMode.takePhoto
            this.flashMode = FlashModel.CAMERA_FLASH_AUTO
            //android R以下时，在少数display为null的情况下，设置预览，拍照的默认分辨率
            this.size = Size(1920, 1080)
        }
    }

    override fun closeActivity(shouldInvokeFinish: Boolean) {
        cameraXF.stopRecord(0)

        if (shouldInvokeFinish) {
            mBaseHandler.postDelayed(Runnable {
                this.finish()
            }, 500)
        }
    }

    lateinit var analyzer: FaceContourDetectionProcessor
    override fun cameraHolderInitStart(cameraHolder: CameraHolder) {
        super.cameraHolderInitStart(cameraHolder)
        val cameraPreview = cameraHolder.cameraPreview

        //示例1
        //生成图像分析器
        analyzer = FaceContourDetectionProcessor(
            cameraPreview,
            findViewById<GraphicOverlayView>(R.id.graphicOverlay_finder),
        )
        setUpAnalyzer(analyzer)//设置分析器
        //监听分析结果
        (analyzer as FaceContourDetectionProcessor).analyzeListener =
            AnalyzeResultListener { it, faces: List<Face> ->
                MyFileProcessor.process(it){croped->

                }
                //裁剪图像，或许需要优化裁剪的尺寸
                it?.cropImage(faces)?.let { bitmap: Bitmap? ->
                    if (bitmap != null) {
                        // when analyze success
                        lifecycleScope.launch {
                            val score = antiHelper.anti(bitmap)
                            tv.setText("为假的可能性：$score")
                        }
                    }
                }
            }

        //示例2
        //加载tensorflow lite模型，这里仅做演示
//        val model = FaceDetection.create(
//            this.assets,
//            TestFileDecActivity.TF_OD_API_MODEL_FILE,
//            TestFileDecActivity.TF_OD_API_LABELS_FILE,
//            TestFileDecActivity.TF_OD_API_IS_QUANTIZED
//        )
//        //TensorFlowLink继承自ImageAnalysis.Analyzer，将其作为分析器设置给相机即可拿到分析流
//        val tensorFlowLink = TensorFlowLink {image: ImageProxy ->
//            //获取图像
//            val bitmap= image.toBitmap()
//            //这里使用了mlkit分析是否包含面部数据，如果包含，则将面部图像裁剪下来
//            MyFileProcessor.process(bitmap){
//                it?.let {
//                    //将裁剪后的面部图像转换成特定尺寸bitmap
//                    val tmp = FaceDetection.convertBitmap(it)
//                    //将处理好的面部图像交给模型处理，获取特征点
//                    val masks = model.detectionBitmap(tmp)
//                }
//            }
//
//        }
//        setUpAnalyzer(tensorFlowLink)//设置分析器

    }

    override fun cameraHolderInitFinish(cameraHolder: CameraHolder) {
        super.cameraHolderInitFinish(cameraHolder)
        if (cameraConfig.isUsingImageAnalyzer()) {//使用了图像分析
            if (cameraConfig.isUsingVideoRecorder()) {
                controllerPanel.switchBetweenCaptureAndRecord(ControllerPanelUseCaseMode.recordVideo)
            } else {
                if (!cameraConfig.isUsingImageCapture()) {
                    controllerPanel.showHideControllerButton(true)
                }
            }
            controllerPanel.showHideUseCaseSwitch(true)
        }
    }

    /**
     * 拍完照片
     */
    override fun onPhotoTaken(saveFileData: SaveFileData?) {
        super.onPhotoTaken(saveFileData)
        Log.d("CameraXFragment", "onPhotoTaken： $saveFileData")
        cameraXF.indicateTakePhoto()//拍照闪光
    }

    /**
     * 录完视频
     */
    override fun onVideoRecorded(saveFileData: SaveFileData?) {
        super.onVideoRecorded(saveFileData)
        saveFileData?.let {
            Log.d(TAG, "onVideoRecorded: $it")
        }
    }


    var stopAnalyzer = false

    /**
     * 每隔20ms从预览视图中获取bitmap 然后运行图像分析，绘制矩形框
     * 但是这种方式分析图象后，绘制框体会有延迟、卡顿感，不如直接使用图像分析流畅
     */
    suspend fun runFaceDetection(interval: Long = 20L) {
        if (cameraConfig.isUsingImageAnalyzer() || stopAnalyzer) {
            Log.d(TAG, "runFaceDetection: 已使用图像分析或stopAnalyzer==true")
            return
        } else {
            flow<Boolean> {
                while (true) {
                    delay(interval)
                    emit(stopAnalyzer)
                    if (stopAnalyzer) {
                        break
                    }
                }
            }.collect {
                cameraXF.provideBitmap()?.let { originalBitmap ->
                    //识别图像
                    BitmapProcessor.process(originalBitmap) { faces: List<Face> ->
                        //上面依据识别成功，得到了返回数据，我们在这里调用了一个普通方法来使用识别出来的数据
                        BitmapProcessor.onSuccess(
                            faces,
                            findViewById<GraphicOverlayView>(R.id.graphicOverlay_finder),
                        )
                    }
                }

            }
        }
    }


}
