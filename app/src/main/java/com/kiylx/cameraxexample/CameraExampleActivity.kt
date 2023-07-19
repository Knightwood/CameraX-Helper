package com.kiylx.cameraxexample

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.util.Size
import android.view.View
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.face.Face
import com.kiylx.camerax_lib.main.manager.CameraHolder
import com.kiylx.camerax_lib.main.manager.imagedetection.base.AnalyzeResultListener
import com.kiylx.camerax_lib.main.manager.imagedetection.face.FaceContourDetectionProcessor
import com.kiylx.camerax_lib.main.manager.model.CaptureMode
import com.kiylx.camerax_lib.main.manager.model.FlashModel
import com.kiylx.camerax_lib.main.manager.model.ManagerConfig
import com.kiylx.camerax_lib.main.store.FileMetaData
import com.kiylx.camerax_lib.main.ui.BaseCameraXActivity
import com.kiylx.cameraxexample.graphic2.BitmapProcessor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

class CameraExampleActivity : BaseCameraXActivity() {
    var analyzeResultListener: AnalyzeResultListener? = null

    /**
     * 这里直接构建了配置，我没有使用intent传入配置。
     */
    override fun configAll(intent: Intent): ManagerConfig {
        val useImageDetection = intent.getBooleanExtra(ImageDetection, false)
        return ManagerConfig().apply {
            this.captureMode =
                if (useImageDetection) CaptureMode.imageAnalysis else CaptureMode.takePhoto
            this.flashMode = FlashModel.CAMERA_FLASH_AUTO
            this.size = Size(1920, 1080)//拍照，预览的分辨率，期望值，不一定会用这个值
        }
    }

    override fun closeActivity(shouldInvokeFinish: Boolean) {
        cameraXF.stopTakeVideo(0)

        if (shouldInvokeFinish) {
            mBaseHandler.postDelayed(Runnable {
                this.finish()
            }, 500)
        }
    }

    override fun initCameraStart(cameraHolder: CameraHolder, cameraPreview: PreviewView) {
        super.initCameraStart(cameraHolder, cameraPreview)
        //生成图像分析器
        val analyzer = FaceContourDetectionProcessor(
            cameraPreview,
            page.graphicOverlayFinder,
        ).also {
            cameraHolder.changeAnalyzer(it)//设置图像分析器
        }
        //监听分析结果
        (analyzer as FaceContourDetectionProcessor).analyzeListener =
            object : AnalyzeResultListener {
                override fun isSuccess() {

                }
            }.also { analyzeResultListener = it }
    }

    override fun initCameraFinished(cameraHolder: CameraHolder, cameraPreview: PreviewView) {
        super.initCameraFinished(cameraHolder, cameraPreview)
        if (cameraConfig.isUsingImageAnalyzer()) {//使用了图像分析
            page.cameraControlLayout.visibility = View.INVISIBLE
        }
    }

    /**
     * 启动后自动拍照或录像
     */
    private fun capture() {
        if (cameraConfig.captureMode == CaptureMode.takePhoto) {
            //拍照
            mBaseHandler.postDelayed(Runnable {
                cameraXF.takePhoto()
            }, 300)
        } else if (cameraConfig.captureMode == CaptureMode.takeVideo) {
            cameraXF.takeVideo()
        }
    }

    /**
     * 人脸识别后拍摄照片
     */
    override fun captureFace() {
        cameraXF.takePhoto()
        /*
        //还可以使用预览画面里的bitmap存储为图片
        mBaseHandler.post {
            val bitmap = cameraXF.provideBitmap()
            if (bitmap != null) {
                // TODO: 存储bitmap
            }
        }*/

    }

    override fun onClick(p0: View?) {
        TODO("Not yet implemented")
    }

    /**
     * 拍完照片
     */
    override fun photoTakeEnd(filePath: Uri?) {
        super.photoTakeEnd(filePath)
    }

    /**
     * 录完视频
     */
    override fun videoRecordEnd(fileMetaData: FileMetaData?) {
        super.videoRecordEnd(fileMetaData)
    }


    var stopAnalyzer = false

    /**
     * 每隔20ms从预览视图中获取bitmap
     * 然后运行图像分析，绘制矩形框
     * 但是这种方式分析图象后，绘制框体会有延迟、卡顿感，不如直接使用图像分析流畅
     */
    suspend fun runFaceDetection(interval: Long = 20L) {
        if (cameraConfig.isUsingImageAnalyzer() || stopAnalyzer) {
            Log.d(tag, "runFaceDetection: 已使用图像分析或stopAnalyzer==true")
            return
        } else {
            BitmapProcessor.analyzeListener = analyzeResultListener
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
                    BitmapProcessor.process(originalBitmap) { faces: List<Face> ->
                        BitmapProcessor.onSuccess(faces, page.graphicOverlayFinder)
                    }
                }

            }
        }
    }


}
