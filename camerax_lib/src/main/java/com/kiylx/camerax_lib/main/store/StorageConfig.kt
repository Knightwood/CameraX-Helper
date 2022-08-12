package com.kiylx.camerax_lib.main.store

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.os.EnvironmentCompat
import com.kiylx.camerax_lib.main.manager.model.MediaType
import com.kiylx.camerax_lib.main.manager.video.LocationKind
import com.kiylx.camerax_lib.utils.Weak
import java.io.File

/**
 * 全局的(存储照片、视频)存储配置
 * 用法： 先配置application
 *   1. StorageConfig.prepare(application)
 *   2. StorageConfig.configStorage()
 */
object StorageConfig {
    var application by Weak<Application> { null }
    var imageStorage: Storage = Storage.default(MediaType.photo)
    var videoStorage: Storage = Storage.default(MediaType.video)

    fun prepare(application: Application): StorageConfig {
        this.application = application
        return this
    }

    /**
     * @param type [MediaType] 配置图片或视频的存储位置
     * @param locationKind [LocationKind] 存储于app内部、公共媒体文件夹、其他目录
     * @param relativePath 比如存储在DCIM文件夹里，指定relativePath则存储在DCIM/relativePath下
     */
    fun configStorage(
        type: Int,
        locationKind: LocationKind,
        relativePath: String = "",
    ) {
        val config = when (type) {
            MediaType.video -> {
                videoStorage
            }
            MediaType.photo -> {
                imageStorage
            }
            else -> {
                throw Exception("???怎么可能有其他类型")
            }
        }
        config.apply {
            this.locationKind = locationKind
            if (relativePath.isNotEmpty()) {
                this.relativePath = relativePath
            }
        }
    }

}

/**
 * android 10及以上使用MediaStore或者saf
 * android 10以下使用File
 */
class Storage() {
    /**
     * 相对路径：在上面的父路径下建立的文件夹名称
     * 用于在Android10以下，或者Android10以上中的私有目录、MediaStore
     */
    var relativePath: String = "SimpleCameraX"
    var mediaType: Int = -1//标识哪种媒体的存储配置
    var locationKind: LocationKind = LocationKind.DCIM

    /**
     * Android10及以上，放在非公共目录和私有目录下时
     */
    var parentUri: Uri = Uri.EMPTY

    /**
     * Android10及以上，使用mediaStore存储到相册
     */
    var mediaPath: String = "${Environment.DIRECTORY_DCIM}${File.separator}$relativePath"

    /**
     * Android10及以上,文件夹绝对路径,用在app自身路径下
     */
    var parentAbsolutePath: String =
        "${StorageConfig.application!!.getExternalFilesDir(null)!!.absolutePath}${File.separator}$relativePath"

    /**
     * Android10以下,表示任何位置
     */
    var parentAbsoluteFolder: String =
        "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath}${File.separator}$relativePath"


    companion object {
        fun default(type: Int): Storage {
            return Storage().apply {
                mediaType = type
            }
        }
    }
}