package com.kiylx.camerax_lib.main.store

import android.app.Application
import android.os.Build
import android.os.Environment
import com.kiylx.camerax_lib.utils.Weak

/**
 * 默认保存到相册文件夹
 */
object CameraXStoreConfig {
    var application by Weak<Application> { null }
    internal lateinit var imageStorage: IStore
    internal lateinit var videoStorage: IStore

    fun prepare(application: Application): CameraXStoreConfig {
        this.application = application
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {//android 10 及以上
            this.imageStorage = IStore.MediaStoreConfig(saveCollection = FileLocate.IMAGE.uri)
			this.videoStorage = IStore.MediaStoreConfig(saveCollection = FileLocate.VIDEO.uri)
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