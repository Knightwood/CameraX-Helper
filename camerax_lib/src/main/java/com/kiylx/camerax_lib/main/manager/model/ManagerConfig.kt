package com.kiylx.camerax_lib.main.manager.model

import android.os.Parcelable
import android.util.Size
import androidx.camera.core.ImageCapture
import androidx.camera.extensions.ExtensionMode
import com.kiylx.camerax_lib.main.manager.ManagerUtil
import kotlinx.parcelize.Parcelize


@Parcelize
data class ManagerConfig(
    var flashMode: Int = FlashModel.CAMERA_FLASH_OFF,
    var size: Size = Size(1280, 720),//android R以下，设置预览，拍照的默认分辨率
    /**
     * true：使用camera-video库完成视频录制功能
     * 将来会使用camera-video库取代旧方式录制视频
     */
    var useNewVideoCapture: Boolean = true,
    /**
     * 查看[CaptureMode]
     */
    var captureMode: Int = CaptureMode.takePhoto,

    @Deprecated("新的录像方式已不再使用此字段")
    var MyVideoDir: String = "",

    ) : Parcelable {

    fun isUsingImageAnalyzer(): Boolean {
        return captureMode == CaptureMode.imageAnalysis
    }

    /**
     * 是否为拍照启用扩展功能
     *
     * 启用扩展后，只能保证能够使用 ImageCapture 和 Preview 的组合。
     * 根据 OEM 实现情况，可能无法同时添加 ImageAnalysis；无法为 VideoCapture 用例启用扩展。如需了解详情，请参阅([扩展参考文档](https://developer.android.google.cn/reference/androidx/camera/extensions/ExtensionsManager?hl=zh-cn))
     * 如需查看支持扩展的设备列表，请参阅([支持的设备](https://developer.android.google.cn/training/camera/supported-devices?hl=zh-cn))。
     */
    var useExtensionApi=false

    /**
     * 要使用的相机扩展，详情查看[ExtensionMode]
     */
    var extensionMode: Int=ExtensionMode.AUTO
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