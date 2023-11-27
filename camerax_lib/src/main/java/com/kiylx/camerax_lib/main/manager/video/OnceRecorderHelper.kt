package com.kiylx.camerax_lib.main.manager.video

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.kiylx.camerax_lib.main.manager.ManagerUtil
import com.kiylx.camerax_lib.main.store.CameraStore
import com.kiylx.camerax_lib.main.store.SaveFileData
import com.kiylx.camerax_lib.main.store.IStore
import com.kiylx.store_lib.kit.MimeTypeConsts

/**
 * 1.通过这个单例，全局设置默认的文件输出位置。
 * 2.通过这个单例，获取一个新的OnceRecorder实例
 * 支持存储到相册，应用内部目录，其他目录。
 * 设置[LocationKind]后，文件位置也要正确，否则会报错。
 * 报错：
 * 例如设置存储到相册，但是文件位置却是自己在文件管理器建立的文件夹或是应用内部路径
 * 例如设置存储到应用内部，但是文件位置不是应用内部的路径
 *
 * 这个不适应于旧方法实现的视频录制，旧方法实现的视频录制不再被支持
 */
object OnceRecorderHelper {

    fun newOnceRecorder(context: Context): OnceRecorder {
        when (val storageConfig = CameraStore.videoStorage) {
            is IStore.SAFStoreConfig -> {
                val name = ManagerUtil.generateRandomName()
                val uri = storageConfig.parentUri
                val df: DocumentFile? =
                    DocumentFile.fromTreeUri(context, uri)?.createFile(MimeTypeConsts.mp4, name)
                val uri2 = df!!.uri
                val fd = context.contentResolver.openFileDescriptor(uri2, "rw")
                return OnceRecorder(context).getFileDescriptorOutput(fd!!)
                    .apply {
                        this.saveFileData = SaveFileData(uri = uri2)
                    }
            }

            is IStore.FileStoreConfig -> {
                val videoFile = ManagerUtil.createMediaFile(
                    storageConfig.getFullPath(),
                    ManagerUtil.VIDEO_EXTENSION
                )
                return OnceRecorder(context).getFileOutputOption(videoFile)
                    .apply {
                        this.saveFileData = SaveFileData(
                            videoFile.absolutePath,
                            uri = DocumentFile.fromFile(videoFile).uri
                        )
                    }
            }

            is IStore.MediaStoreConfig -> {
                return OnceRecorder(context).getMediaStoreOutput(storageConfig)
                    .apply {
                        this.saveFileData = SaveFileData()
                    }
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