package com.kiylx.camerax_lib.main.store

import android.app.Application
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import androidx.camera.video.Quality
import com.kiylx.camerax_lib.main.manager.ManagerUtil
import com.kiylx.camerax_lib.main.manager.model.MediaType
import com.kiylx.camerax_lib.main.manager.video.LocationKind
import com.kiylx.camerax_lib.utils.Weak
import java.io.File

/**
 * 全局的(存储照片、视频)存储配置 用法：
 * 1. StorageConfig.prepare(application) 2.（可选）
 *    StorageConfig.configStorage()
 */
object StorageConfig {
    var quality: Quality?=null//视频拍摄质量
    var application by Weak<Application> { null }
    lateinit var imageStorage: Storage
    lateinit var videoStorage: Storage

    fun prepare(application: Application): StorageConfig {
        this.application = application
        this.imageStorage = Storage.default(MediaType.photo)
        this.videoStorage = Storage.default(MediaType.video)
        return this
    }

    /**
     * 用于Android10以下版本
     *
     * @param type [MediaType] 配置图片或视频的存储位置
     * @param locationKind [LocationKind] 存储于app内部、公共媒体文件夹、其他目录
     * @param path 要存储到的文件夹的绝对路径
     * @param relativePath 比如存储在DCIM文件夹里，指定relativePath则存储在DCIM/relativePath下
     */
    fun configStorage(
        type: Int,
        locationKind: LocationKind,
        path: String = "",
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
                this.parentAbsoluteFolder = path
            }
            if (relativePath.isNotEmpty()) {
                this.relativePath = relativePath
            }
        }
    }

    /**
     * 配置 Android10及以上，使用saf存储文件的情况
     *
     * @param type [MediaType] 配置图片或视频的存储位置
     * @param uri :saf获取文件夹权限后，将文件夹uri传入
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun configStorageOther(
        type: Int,
        uri: Uri,
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
            this.locationKind = LocationKind.OTHER
            this.parentUri = uri
        }
    }

    /**
     * @param type [MediaType] 配置图片或视频的存储位置 配置
     *     Android10及以上，使用mediaStore存储到相册的情况
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun configStorageDCIM(
        type: Int,
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
            this.locationKind = LocationKind.DCIM
            if (relativePath.isNotEmpty()) {
                this.relativePath = relativePath
            }
        }
    }

    /** @param type [MediaType] 配置图片或视频的存储位置 配置 Android10及以上，存储到app外部私有目录情况 */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun configStorageApp(
        type: Int,
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
            this.locationKind = LocationKind.APP
            if (relativePath.isNotEmpty()) {
                this.relativePath = relativePath
            }
        }
    }


}

/** android 10及以上使用MediaStore或者saf android 10以下使用File */
class Storage(
    var relativePath: String = "SimpleCameraX",//相对路径：在上面的父路径下建立的文件夹名称,用于在Android10以下，或者Android10以上中的私有目录、MediaStore
    var mediaType: Int = -1,//标识哪种媒体的存储配置
) {

    var locationKind: LocationKind = LocationKind.DCIM

    /** Android10及以上，放在非公共目录和私有目录下时 */
    var parentUri: Uri = Uri.EMPTY

    /** Android10及以上，使用mediaStore存储到相册 */
    var mediaPath: String = Environment.DIRECTORY_DCIM
    get() {
        return "$field${File.separator}$relativePath"
    }

    /** Android10及以上,文件夹绝对路径,用在app自身路径下 默认值是app外部私有目录 */
    var appSelfAbsolutePath: String =
        StorageConfig.application!!.getExternalFilesDir(null)!!.absolutePath
        get() {
            val b = ManagerUtil.createDir(field)
            if (!b) {
                throw Exception("文件夹创建失败")
            }
            return "$field${File.separator}$relativePath"
        }

    /** Android10以下,表示任何位置 默认值是相册DCIM目录 */
    var parentAbsoluteFolder: String =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath
        get() {
            val b = ManagerUtil.createDir(field)
            if (!b) {
                throw Exception("文件夹创建失败")
            }
            return "$field${File.separator}$relativePath"
        }

    companion object {
        fun default(type: Int): Storage {
            return Storage(mediaType = type)
        }
    }
}