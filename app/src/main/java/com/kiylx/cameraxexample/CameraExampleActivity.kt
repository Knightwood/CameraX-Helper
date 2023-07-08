package com.kiylx.cameraxexample

import android.content.Intent
import android.net.Uri
import android.util.Size
import android.view.View
import androidx.camera.view.PreviewView
import com.kiylx.camerax_lib.main.manager.CameraHolder
import com.kiylx.camerax_lib.main.manager.imagedetection.base.AnalyzeResultListener
import com.kiylx.camerax_lib.main.manager.imagedetection.face.FaceContourDetectionProcessor
import com.kiylx.camerax_lib.main.manager.model.CaptureMode
import com.kiylx.camerax_lib.main.manager.model.FlashModel
import com.kiylx.camerax_lib.main.manager.model.ManagerConfig
import com.kiylx.camerax_lib.main.store.FileMetaData
import com.kiylx.camerax_lib.main.ui.BaseCameraXActivity

class CameraExampleActivity : BaseCameraXActivity() {
    private var cacheMediasDir = "" //存储路径

    /**
     * 这里直接构建了配置，我没有使用intent传入配置。
     */
    override fun configAll(intent: Intent): ManagerConfig {
//        cacheMediasDir = "${application.getExternalFilesDir(null)}/dcim"//应用自身目录下
        val useImageDetection = intent.getBooleanExtra(ImageDetection, false)
        return ManagerConfig().apply {
            //this.cacheMediaDir = cacheMediasDir
            this.captureMode =
                if (useImageDetection) CaptureMode.imageAnalysis else CaptureMode.takePhoto
            this.flashMode = FlashModel.CAMERA_FLASH_AUTO
            this.size = Size(1920, 1080)//拍照，预览的分辨率，期望值，不一定会用这个值
        }
    }

    override fun closeActivity(shouldInvokeFinish: Boolean) {
        cameraXFragment.stopTakeVideo(0)

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
            }
    }

    override fun initCameraFinished(cameraHolder: CameraHolder, cameraPreview: PreviewView) {
        super.initCameraFinished(cameraHolder, cameraPreview)
        if (cameraConfig.isUsingImageAnalyzer()) {//人脸识别拍摄
            page.cameraControlLayout.visibility = View.INVISIBLE
        } else {
            //capture()//自动拍照或录像
        }
    }

    /**
     * 启动后自动拍照或录像
     */
    private fun capture() {
        if (cameraConfig.captureMode == CaptureMode.takePhoto) {
            //拍照
            mBaseHandler.postDelayed(Runnable {
                cameraXFragment.takePhoto()
            }, 300)
        } else if (cameraConfig.captureMode == CaptureMode.takeVideo) {
            cameraXFragment.takeVideo()
        }
    }

    /**
     * 人脸识别后拍摄照片
     */
    override fun captureFace() {
        mBaseHandler.post {
            cameraXFragment.takePhoto()
        }
        /*
        //还可以使用预览画面里的bitmap存储为图片
        mBaseHandler.post {
            val bitmap = cameraXFragment.provideBitmap()
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


}
