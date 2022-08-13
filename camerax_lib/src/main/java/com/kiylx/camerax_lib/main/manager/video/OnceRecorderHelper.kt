package com.kiylx.camerax_lib.main.manager.video

import android.content.Context
import android.os.Build
import androidx.camera.video.MediaStoreOutputOptions
import androidx.documentfile.provider.DocumentFile
import com.kiylx.camerax_lib.main.manager.ManagerUtil
import com.kiylx.camerax_lib.main.manager.video.LocationKind.*
import com.kiylx.camerax_lib.main.store.FileMetaData
import com.kiylx.camerax_lib.main.store.StorageConfig
import com.kiylx.store_lib.kit.MimeTypeConsts
import java.util.*

/**
 * 1.通过这个单例，全局设置默认的文件输出位置。
 * 2.通过这个单例，获取一个新的OnceRecorder实例
 * 支持存储到相册，应用内部目录，其他目录。
 * 设置[locationKind]后，文件位置也要正确，否则会报错。
 * 报错：
 * 例如设置存储到相册，但是文件位置却是自己在文件管理器建立的文件夹或是应用内部路径
 * 例如设置存储到应用内部，但是文件位置不是应用内部的路径
 *
 * 这个不适应于旧方法实现的视频录制，旧方法实现的视频录制不再被支持
 */
object OnceRecorderHelper {

    fun newOnceRecorder(context: Context): OnceRecorder {
        val storageConfig = StorageConfig.videoStorage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {//android 10 以上，使用mediastore
            when (storageConfig.locationKind) {
                APP -> {//存储到app私有目录
                    val videoFile = ManagerUtil.createMediaFile(storageConfig.appSelfAbsolutePath,
                        ManagerUtil.VIDEO_EXTENSION)
                    return OnceRecorder(context).getFileOutputOption(videoFile).apply {
                        this.fileMetaData = FileMetaData(APP, videoFile.absolutePath)
                    }
                }
                DCIM -> {
                    return OnceRecorder(context).getMediaStoreOutput().apply {
                        val tmp = (outputOption as MediaStoreOutputOptions).collectionUri
                        this.fileMetaData = FileMetaData(DCIM, uri = tmp)
                    }
                }
                OTHER -> {
                    val name = ManagerUtil.generateRandomName()
                    val uri = storageConfig.parentUri
                    val df: DocumentFile? =
                        DocumentFile.fromTreeUri(context, uri)?.createFile(MimeTypeConsts.mp4, name)
                    val uri2 = df!!.uri
                    val fd = context.contentResolver.openFileDescriptor(uri2, "rw")
                    return OnceRecorder(context).getFileDescriptorOutput(fd!!)
                        .apply {
                            this.fileMetaData = FileMetaData(OTHER, uri = uri2)
                        }
                }
            }
        } else {
            val videoFile = ManagerUtil.createMediaFile(storageConfig.parentAbsoluteFolder,
                ManagerUtil.VIDEO_EXTENSION)
            return OnceRecorder(context).getFileOutputOption(videoFile).apply {
                this.fileMetaData = FileMetaData(storageConfig.locationKind, videoFile.absolutePath)
            }
        }
    }
}

/**
 * 表示文件存储位置的三种类型
 * 分别是，应用自身的内部文件夹，系统相册，其他位置
 */
enum class LocationKind {
    APP,
    DCIM,
    OTHER
}