package com.kiylx.camerax_lib.main.manager.model

import android.os.Parcelable
import androidx.camera.core.ImageCapture
import com.kiylx.camerax_lib.main.manager.ManagerUtil
import kotlinx.android.parcel.Parcelize

@Parcelize
class ManagerConfig : Parcelable {
    var flashMode: Int = FlashModel.CAMERA_FLASH_OFF
    var MyPhotoDir = ""
    var MyVideoDir = ""

    /**
     * true：使用camera-video库完成视频录制功能
     * 将来会使用camera-video库取代旧方式录制视频
     */
    var useNewVideoCapture = true

    /**
     * 如果不设置cacheMediaDir，将会使用系统图库路径
     */
    var cacheMediaDir: String = ""
        set(value) {
            field = value
            MyPhotoDir = "$value/images/"
            MyVideoDir = "$value/videos/"
        }

    /**
     * 查看[CaptureMode]
     */
    var captureMode: Int = CaptureMode.takePhoto

    fun isUsingImageAnalyzer(): Boolean {
        return captureMode == CaptureMode.imageAnalysis
    }
}

/**
 * 拍摄模式
 */
class CaptureMode {
    companion object {
        const val takePhoto = ManagerUtil.TAKE_PHOTO_CASE
        const val takeVideo = ManagerUtil.TAKE_VIDEO_CASE
        const val imageAnalysis = ManagerUtil.IMAGE_ANALYZER_CASE
    }
}

/**
 *闪光灯模式
 */
class FlashModel {
    companion object {
        const val CAMERA_FLASH_AUTO = ImageCapture.FLASH_MODE_AUTO
        const val CAMERA_FLASH_ON = ImageCapture.FLASH_MODE_ON
        const val CAMERA_FLASH_OFF = ImageCapture.FLASH_MODE_OFF
        const val CAMERA_FLASH_ALL_ON = 777    //常亮模式
    }
}

object MediaType {
    const val photo = 1
    const val video = 2
}