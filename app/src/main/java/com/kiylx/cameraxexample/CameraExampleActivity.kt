package com.kiylx.cameraxexample

import android.content.Intent
import android.util.Log
import android.util.Size
import android.view.View
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.face.Face
import com.kiylx.camerax_lib.main.manager.CameraHolder
import com.kiylx.camerax_lib.main.manager.analyer.TensorFlowLink
import com.kiylx.camerax_lib.main.manager.imagedetection.base.AnalyzeResultListener
import com.kiylx.camerax_lib.main.manager.imagedetection.face.FaceContourDetectionProcessor
import com.kiylx.camerax_lib.main.manager.imagedetection.facedetection.FaceDetection
import com.kiylx.camerax_lib.main.manager.imagedetection.filevision.MyFileProcessor
import com.kiylx.camerax_lib.main.manager.model.CaptureMode
import com.kiylx.camerax_lib.main.manager.model.FlashModel
import com.kiylx.camerax_lib.main.manager.model.ManagerConfig
import com.kiylx.camerax_lib.main.manager.video.CameraRecordQuality
import com.kiylx.camerax_lib.main.store.ImageCaptureConfig
import com.kiylx.camerax_lib.main.store.SaveFileData
import com.kiylx.camerax_lib.main.store.VideoRecordConfig
import com.kiylx.camerax_lib.main.ui.BaseCameraXActivity
import com.kiylx.camerax_lib.utils.DataSize.Companion.mb
import com.kiylx.cameraxexample.graphic2.BitmapProcessor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration.Companion.minutes

class CameraExampleActivity : BaseCameraXActivity() {

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
            this.captureMode =
                if (useImageDetection) CaptureMode.imageAnalysis else CaptureMode.takePhoto
            this.flashMode = FlashModel.CAMERA_FLASH_AUTO
            this.size = Size(1920, 1080)//拍照，预览的分辨率，期望值，不一定会用这个值
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

    override fun cameraHolderInitStart(cameraHolder: CameraHolder) {
        super.cameraHolderInitStart(cameraHolder)
        val cameraPreview = cameraHolder.cameraPreview

        //示例1
        //生成图像分析器
        val analyzer = FaceContourDetectionProcessor(
            cameraPreview,
            page.graphicOverlayFinder,
        ).also {
            cameraHolder.changeAnalyzer(it)//设置图像分析器
        }
        //监听分析结果
        (analyzer as FaceContourDetectionProcessor).analyzeListener =
            AnalyzeResultListener {
                // when analyze success
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
//        }.also {
//            cameraHolder.changeAnalyzer(it)//设置图像分析器
//        }

    }

    override fun cameraHolderInitFinish(cameraHolder: CameraHolder) {
        super.cameraHolderInitFinish(cameraHolder)
        if (cameraConfig.isUsingImageAnalyzer()) {//使用了图像分析
            page.cameraControlLayout.visibility = View.INVISIBLE
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
         * 每隔20ms从预览视图中获取bitmap
         * 然后运行图像分析，绘制矩形框
         * 但是这种方式分析图象后，绘制框体会有延迟、卡顿感，不如直接使用图像分析流畅
         */
        suspend fun runFaceDetection(interval: Long = 20L) {
            if (cameraConfig.isUsingImageAnalyzer() || stopAnalyzer) {
                Log.d(TAG, "runFaceDetection: 已使用图像分析或stopAnalyzer==true")
                return
            } else {
                BitmapProcessor.analyzeListener = AnalyzeResultListener {
                    // when analyze success
                }
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
                            BitmapProcessor.onSuccess(faces, page.graphicOverlayFinder)
                        }
                    }

                }
            }
        }


    }
