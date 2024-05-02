package com.kiylx.cameraxexample

import android.content.Intent
import android.os.Bundle
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import com.kiylx.camerax_lib.main.manager.CameraHolder
import com.kiylx.camerax_lib.main.manager.model.CaptureMode
import com.kiylx.camerax_lib.main.manager.model.CaptureResultListener
import com.kiylx.camerax_lib.main.manager.model.FlashModel
import com.kiylx.camerax_lib.main.manager.model.ManagerConfig
import com.kiylx.camerax_lib.main.manager.video.CameraRecordQuality
import com.kiylx.camerax_lib.main.store.ImageCaptureConfig
import com.kiylx.camerax_lib.main.store.SaveFileData
import com.kiylx.camerax_lib.main.store.VideoRecordConfig
import com.kiylx.camerax_lib.main.ui.BaseCameraXFragment
import com.kiylx.camerax_lib.main.ui.CameraXFragmentEventListener

class CameraExampleActivity2 : AppCompatActivity(), CameraXFragmentEventListener,
    CaptureResultListener {
    lateinit var cameraConfig: ManagerConfig
    var cameraXFragment: BaseCameraXFragment ?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_container)
        cameraConfig = configAll(intent)
        cameraXFragment = BaseCameraXFragment.newInstance(
            cameraConfig,
            //设置初始化事件监听
            eventListener = this,
            //拍照录视频操作结果通知回调
            captureResultListener = this
        )
        supportFragmentManager.beginTransaction()
            .replace(com.kiylx.camerax_lib.R.id.fragment_container, cameraXFragment!!).commit()
    }

    /** 这里直接构建了配置，我没有使用intent传入配置。 */
    fun configAll(intent: Intent): ManagerConfig {
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

    override fun onVideoRecorded(saveFileData: SaveFileData?) {
        
    }

    override fun onPhotoTaken(saveFileData: SaveFileData?) {
        
    }

    override fun cameraHolderInitStart(cameraHolder: CameraHolder) {
        //可选
        cameraXFragment?.edge2edge(window)
    }

    override fun cameraHolderInitFinish(cameraHolder: CameraHolder) {
        //可选
    }
}