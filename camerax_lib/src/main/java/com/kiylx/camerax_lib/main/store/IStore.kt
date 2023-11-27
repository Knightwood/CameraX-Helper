package com.kiylx.camerax_lib.main.store

import android.app.Application
import android.net.Uri
import android.os.Build
import android.os.Environment
import com.kiylx.camerax_lib.main.manager.ManagerUtil
import com.kiylx.camerax_lib.utils.Weak
import com.kiylx.store_lib.mediastore.FileLocate
import java.io.File

sealed class IStore(
    /**父路径下的存储文件的文件夹名称*/
    var targetFolder: String,
) {
    class MediaStoreConfig(
        /**MediaStore存储到哪个数据库表*/
        val saveCollection: Uri = FileLocate.IMAGE.uri,
        /**
         * 存储到哪个文件夹，
         * 例如
         * Environment.DIRECTORY_DCIM即sdcard下的DCIM文件夹,
         * Environment.DIRECTORY_MOVIES即sdcard下的Movies文件夹下
         * */
        val mediaFolder: String = Environment.DIRECTORY_DCIM,
        /**mediaFolder下的子目录名称*/
        targetFolder: String = "CameraX",//父文件夹
    ) : IStore(targetFolder = targetFolder) {
        var filePath=""
        fun getRelativePath() = "$mediaFolder${File.separator}$targetFolder"
    }

    class SAFStoreConfig(
        val parentUri: Uri
    ) : IStore(targetFolder = "") {}

    class FileStoreConfig(
        val parentPath: String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath,
        targetFolder: String = "CameraX",
    ) : IStore(targetFolder = targetFolder) {
        fun getFullPath(): String = genAndFullPath(parentPath, targetFolder)
    }

    companion object {
        /**
         * 片姐父路径和相对路径，并创建文件夹
         */
        fun genAndFullPath(
            parent: String,
            relativePath: String
        ): String {
            val b = ManagerUtil.createDir(parent)
            if (!b) {
                throw Exception("文件夹创建失败")
            }
            return "$parent{File.separator}$relativePath"
        }
    }
}

/**
 * 默认保存到相册文件夹
 */
object CameraStore {
    var application by Weak<Application> { null }
    internal lateinit var imageStorage: IStore
    internal lateinit var videoStorage: IStore

    fun prepare(application: Application): CameraStore {
        this.application = application
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {//android 10 及以上
            this.imageStorage = IStore.MediaStoreConfig()
            this.videoStorage = IStore.MediaStoreConfig()
        } else {
            this.imageStorage = IStore.FileStoreConfig(
                parentPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath
            )
            this.videoStorage = IStore.FileStoreConfig(
                parentPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath
            )
        }
        return this
    }

    fun configPhoto(storeConfig: IStore) {
        this.imageStorage = storeConfig
    }

    fun configVideo(storeConfig: IStore) {
        this.videoStorage = storeConfig
    }

}
