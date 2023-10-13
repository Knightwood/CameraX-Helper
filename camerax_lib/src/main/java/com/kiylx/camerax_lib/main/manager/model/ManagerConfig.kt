package com.kiylx.camerax_lib.main.manager.model

import android.os.Parcelable
import android.util.Size
import androidx.camera.core.ImageCapture
import com.kiylx.camerax_lib.main.manager.ManagerUtil
import kotlinx.parcelize.Parcelize
import android.view.Surface



@Parcelize
data class ManagerConfig(
    var flashMode: Int = FlashModel.CAMERA_FLASH_OFF,
    var size: Size = Size(1280, 720),//android R以下，设置预览，拍照的默认分辨率

    /**
     * 查看[CaptureMode]
     */
    var captureMode: Int = CaptureMode.takePhoto,

    /**
     * 指定图像分析和拍照的旋转角度,默认可能为[Surface.ROTATION_0]。
     * 默认值是根据display的旋转方向而定
     * 因此，如果在此指定值，默认值将不会使用
     */
    var rotation: Int = -1

    ) : Parcelable {

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