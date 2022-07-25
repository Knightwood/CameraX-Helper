package com.kiylx.camerax_lib.main.manager.model

import android.os.Parcelable
import androidx.camera.core.ImageCapture
import com.kiylx.camerax_lib.main.manager.ManagerUtil
import kotlinx.android.parcel.Parcelize

@Parcelize
class ManagerConfig : Parcelable {
    var flashMode: Int = FlashModel.CAMERA_FLASH_OFF
    var imageDetector: Boolean = false//是否开启图像识别
    var MyPhotoDir = ""
    var MyVideoDir = ""

    /**
     * 如果不设置cacheMediaDir，将会使用系统图库路径
     */
    var cacheMediaDir: String = ""
        set(value) {
            field = value
            MyPhotoDir = "$value/images/"
            MyVideoDir = "$value/videos/"
        }
    var captureMode: Int = CaptureMode.takePhoto // 0：拍照 1：拍视频 默认拍照
}

/**
 * 拍摄模式
 */
class CaptureMode {
    companion object {
        const val takePhoto = ManagerUtil.TAKE_PHOTO_CASE
        const val takeVideo = ManagerUtil.TAKE_VIDEO_CASE
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