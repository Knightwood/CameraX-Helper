package com.kiylx.camerax_lib.main.manager.video

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.annotation.RestrictTo
import androidx.camera.video.MediaStoreOutputOptions
import com.kiylx.camerax_lib.main.manager.ManagerUtil
import com.kiylx.camerax_lib.main.manager.video.LocationKind.APP
import com.kiylx.camerax_lib.main.manager.video.LocationKind.DCIM
import com.kiylx.camerax_lib.main.manager.video.OutputKinds.MEDIA_STORE
import java.io.File

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
    var locationKind: LocationKind = DCIM//默认存储到相册

    private var MyVideoDir = ""
    private var cacheMediaDir: String = ""
        set(value) {
            field = value
            MyVideoDir = "$value/videos/"
        }

    /**
     * 获取一个OnceRecorder
     */
    fun newOnceRecorder(context: Context): OnceRecorder? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {//android 10 以上，使用mediastore
            when (locationKind) {
                APP -> {//存储到app私有目录
                    if (cacheMediaDir.isEmpty()) {
                        cacheMediaDir = context.getExternalFilesDir(null)!!.absolutePath
                    }
                    val videoFile = ManagerUtil.createMediaFile(MyVideoDir,
                        ManagerUtil.VIDEO_EXTENSION)
                    return OnceRecorder(context).getFileOutputOption(videoFile).apply {
                        this.filePath = videoFile.absolutePath
                    }
                }
                DCIM -> {
                    return OnceRecorder(context).getMediaStoreOutput().apply {
                        this.fileUri = (outputOption as MediaStoreOutputOptions).collectionUri
                    }
                }
                else -> {
                    return null
                }
            }
        } else {
            var videoPath = ""
            when (locationKind) {
                APP -> {
                    if (cacheMediaDir.isEmpty()) {
                        cacheMediaDir = context.getExternalFilesDir(null)!!.absolutePath
                    }
                    videoPath = MyVideoDir
                }
                DCIM -> {
                    videoPath =
                        Environment.getExternalStorageDirectory().absolutePath + File.separator +
                                Environment.DIRECTORY_PICTURES
                }
                else -> ""
            }
            if (videoPath.isEmpty())
                throw Exception("文件位置不存在")
            val videoFile = ManagerUtil.createMediaFile(videoPath,
                ManagerUtil.VIDEO_EXTENSION)
            return OnceRecorder(context).getFileOutputOption(videoFile).apply {
                this.filePath = videoPath
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