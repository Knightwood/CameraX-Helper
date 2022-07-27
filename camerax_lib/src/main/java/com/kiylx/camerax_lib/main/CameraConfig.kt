package com.kiylx.camerax_lib.main

import android.os.Parcelable
import androidx.camera.core.ImageCapture
import com.kiylx.camerax_lib.main.manager.model.CaptureMode
import com.kiylx.camerax_lib.main.manager.model.ManagerConfig
import kotlinx.android.parcel.Parcelize

/***
 * CameraX 相机配置
 *
 */
@Deprecated("使用manager中构建的新工具替代")
@Parcelize
open class CameraConfig private constructor(val builder: Builder) : Parcelable {

    companion object {
        //1.多媒体模式
        const val MEDIA_MODE_ALL = 0    //拍照视频都可以
        const val MEDIA_MODE_PHOTO = 1  //仅拍照
        const val MEDIA_MODE_VIDEO = 2  //仅拍视频

        //2.闪光灯模式
        const val CAMERA_FLASH_AUTO = ImageCapture.FLASH_MODE_AUTO
        const val CAMERA_FLASH_ON = ImageCapture.FLASH_MODE_ON
        const val CAMERA_FLASH_OFF = ImageCapture.FLASH_MODE_OFF
        const val CAMERA_FLASH_ALL_ON = 777    //常亮模式

    }

    var faceDetector: Boolean = false//是否开启人脸识别
    var flashMode: Int          //闪光灯常亮模式
    var MyPhotoDir = ""
    var MyVideoDir = ""
    var cacheMediaDir: String = ""
        set(value) {
            field = value
            MyPhotoDir = "$value/images/"
            MyVideoDir = "$value/videos/"
        }
    var mediaMode: Int
    var captureMode: Int = 0 // 0：拍照 1：拍视频 默认拍照

    init {
        flashMode = builder.flashMode
        cacheMediaDir = builder.cacheMediaDir
        mediaMode = builder.mediaMode
        captureMode = builder.captureMode
        faceDetector = builder.faceDetector
    }


    @Parcelize
    class Builder(var cacheMediaDir: String) : Parcelable {
        internal var faceDetector: Boolean = false
        internal var flashMode: Int = CAMERA_FLASH_OFF //Default Value
        internal var mediaMode: Int = MEDIA_MODE_PHOTO
        internal var captureMode: Int = 0

        /**
         * 自动检测人脸并抓拍
         * true：自动抓拍
         */
        fun setFace(b: Boolean): Builder {
            this.faceDetector = b
            return this
        }

        /**
         * 设置页面打开后的默认模式，0：拍照；1：录视频
         */
        fun setCaptureMode(mode: Int = 0): Builder {
            this.captureMode = mode
            return this
        }

        fun flashMode(flashMode: Int): Builder {
            this.flashMode = flashMode
            return this
        }

        fun mediaMode(mediaMode: Int): Builder {
            this.mediaMode = mediaMode
            return this
        }

        fun cacheMediasDir(dir: String): Builder {
            this.cacheMediaDir = dir
            return this
        }

        fun build(): CameraConfig {
            return CameraConfig(this)
        }
    }


}

/**
 * 为了兼容旧方式而提供的
 */
fun CameraConfig.convert(): ManagerConfig {
    val config = this
    return ManagerConfig().apply {
        this.flashMode = config.flashMode
        if (config.faceDetector)
            this.captureMode = CaptureMode.imageAnalysis
        else
            this.captureMode = config.captureMode
        this.cacheMediaDir = config.cacheMediaDir
    }
}
