package com.kiylx.camerax_lib.main.manager.model

import android.os.Parcelable
import android.util.Size
import android.view.Surface
import androidx.camera.core.ImageCapture
import com.kiylx.camerax_lib.main.store.ImageCaptureConfig
import com.kiylx.camerax_lib.main.store.VideoRecordConfig
import kotlinx.parcelize.Parcelize


@Parcelize
data class ManagerConfig(
    var flashMode: Int = FlashModel.CAMERA_FLASH_OFF,
    /**
     * android R以下时，在少数display为null的情况下，设置预览，拍照的默认分辨率
     */
    var size: Size = Size(1280, 720),

    /**
     * 指定这次相机使用的用例，例如拍照，录像，图像识别等 查看[UseCaseMode]
     */
    var useCaseBundle: Int = UseCaseMode.takePhoto,

    /**
     * 指定图像分析、拍照、录制的旋转角度,默认可能为[Surface.ROTATION_0]。 默认值是根据display的旋转方向而定
     * 因此，如果在此指定值，默认值将不会使用
     */
    var rotation: Int = -1,

    /**
     * 视频录制配置
     */
    var recordConfig: VideoRecordConfig = VideoRecordConfig(),
    /**
     * 拍照配置
     */
    var imageCaptureConfig: ImageCaptureConfig = ImageCaptureConfig()

) : Parcelable {

    fun isUsingImageAnalyzer(): Boolean {
        return UseCaseHexStatus.canAnalyze(useCaseBundle)
    }
}

/**
 * 指定相机绑定何种用例
 */
class UseCaseMode {
    companion object {
        val takePhoto = customGroup(
            UseCaseHexStatus.USE_CASE_PREVIEW,
            UseCaseHexStatus.USE_CASE_IMAGE_CAPTURE,
        )
        val takeVideo = customGroup(
            UseCaseHexStatus.USE_CASE_PREVIEW,
            UseCaseHexStatus.USE_CASE_VIDEO_CAPTURE,
        )
        val imageAnalysis = customGroup(
            UseCaseHexStatus.USE_CASE_PREVIEW,
            UseCaseHexStatus.USE_CASE_IMAGE_ANALYZE,
            UseCaseHexStatus.USE_CASE_IMAGE_CAPTURE,
        )

        /**
         * 除了预览画面用例，不使用其他任何用例
         */
        const val onlyPreview = UseCaseHexStatus.USE_CASE_PREVIEW

//        /**
//         * 自定义用例组合，需要实现[IUseCaseHelper]接口，在[IUseCaseHelper.initCustomUseCaseList]方法中实现自定义用例组合的初始化
//         * 使用[IUseCaseHelper.provideCustomUseCaseList]方法提供自定义用例组合
//         *
//         * 且在相机初始化之前调用[UseCaseHolder.setInitImpl]方法，提供[IUseCaseHelper]接口实现
//         */
//        const val customUseCaseGroup = CUSTOM_USE_CASE_GROUP

        fun customGroup(vararg useCaseMode: Int): Int {
            var result: Int = useCaseMode[0]
            if (useCaseMode.size > 1) {
                for (i in 1 until  useCaseMode.size) {
                    result = UseCaseHexStatus.addUseCase(result, useCaseMode[i])
                }
            }
            return result
        }

    }
}

/**
 * 闪光灯模式
 */
class FlashModel {
    companion object {
        const val CAMERA_FLASH_AUTO = ImageCapture.FLASH_MODE_AUTO
        const val CAMERA_FLASH_ON = ImageCapture.FLASH_MODE_ON
        const val CAMERA_FLASH_OFF = ImageCapture.FLASH_MODE_OFF
        const val CAMERA_FLASH_ALL_ON = 777    //常亮模式
    }
}