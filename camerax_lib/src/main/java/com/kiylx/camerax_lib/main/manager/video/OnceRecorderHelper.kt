package com.kiylx.camerax_lib.main.manager.video

import android.content.Context
import android.view.Surface
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.documentfile.provider.DocumentFile
import com.kiylx.camerax_lib.main.manager.ManagerUtil
import com.kiylx.camerax_lib.main.store.CameraXStoreConfig
import com.kiylx.camerax_lib.main.store.SaveFileData
import com.kiylx.camerax_lib.main.store.IStore
import com.kiylx.camerax_lib.main.store.VideoCaptureConfig
import com.kiylx.store_lib.kit.MimeTypeConsts
import java.util.concurrent.ExecutorService

/**
 * 简化OnceRecorder使用，根据全局的存储配置直接生成OnceRecorder
 * 并生成文件信息，但MediaStore类型的没法提前得到文件路径，因此为空，要等到录制结束才可得到具体路径
 *
 * 1.通过这个单例，全局设置默认的文件输出位置。
 * 2.通过这个单例，获取一个新的OnceRecorder实例
 * 支持存储到相册，应用内部目录，其他目录。
 * 报错：
 * 例如设置存储到相册，但是文件位置却是自己在文件管理器建立的文件夹或是应用内部路径
 * 例如设置存储到应用内部，但是文件位置不是应用内部的路径
 */
object OnceRecorderHelper {
    /**
     * videoCapture是由Recorder生成
     * 如果[VideoCaptureConfig]给出“quality”，就用它生成VideoCapture，
     * 否则，自动挑选一个比较合适的“quality”生成VideoCapture
     *
     * 配置VideoCapture，不包含录制视频。
     * 使用CameraX 软件包内的 camera-video 库中实现
     * 使用此类获取videoCapture，之后绑定用例。
     *
     * 用法：1.获取videoCapture用例。
     *          videoCapture = OnceRecorderHelper.getVideoCapture(cameraExecutor,rotation) 获取videoCapture用例。
     *      2.绑定用例。
     *          camera = cameraProvider?.bindToLifecycle(lifeOwner,cameraSelector,preview,videoCapture)
     *
     * 绑定用例之后，用此方法录制视频：
     * //获取一个一次性录制工具，并配置输出信息
     * val onceRecorder = OnceRecorder(context).buildFileOutputOption(videoFile)
     * 可以使用[OnceRecorderHelper.newOnceRecorder]快速获取OnceRecorder，
     * [OnceRecorderHelper.newOnceRecorder]将根据[CameraXStoreConfig.videoStorage]自动配置好输出信息
     *
     * //传入videoCapture获取[VideoRecording]对象，用此对象进行录制
     * var recording: Recording? = onceRecorder.getVideoRecording(videoCapture)//使用这个录制视频，暂停，恢复，停止录制
     * recording?.start()//开始录制
     * 这个是一次性的，下一次录制视频，要获取一个新的onceRecorder
     */
    fun getVideoCapture(
        executor: ExecutorService,
        rotation: Int= Surface.ROTATION_0,
    ): VideoCapture<Recorder> {
        var qualitySelector: QualitySelector = QualitySelector.from(Quality.LOWEST)//默认录制质量为最低画质

        /**
         * 选择“Quality”列表中一个比较好的
         */
        fun setSupportedQuality() {
            //以下代码会请求支持的最高录制分辨率；如果所有请求分辨率都不受支持，则授权 CameraX 选择最接近 Quality.SD 分辨率的分辨率：
            qualitySelector = QualitySelector.fromOrderedList(
                listOf(
                    Quality.UHD,
                    Quality.FHD,
                    Quality.HD,
                    Quality.SD
                ),
                FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)//回退策略，当不支持任何所需质量时会选至少比这个好一点的。
            )
        }

        VideoCaptureConfig.quality?.let {
            qualitySelector = QualitySelector.from(it)
        } ?: setSupportedQuality()

        val recorder = Recorder.Builder()
            .setExecutor(executor)
            .setQualitySelector(qualitySelector)
            .apply {
                if (VideoCaptureConfig.encodingBitRate > 0) {
                    this.setTargetVideoEncodingBitRate(VideoCaptureConfig.encodingBitRate)
                }
            }
            .build()

        var videoCapture: VideoCapture<Recorder>? = null
        videoCapture = VideoCapture.Builder<Recorder>(recorder)
            .apply {
                setTargetRotation(rotation)
                setMirrorMode(VideoCaptureConfig.mirrorMode)
            }
            .build()
        return videoCapture

    }

    /**
     * 获取OnceRecorder，
     * 简化下面代码
     * val onceRecorder = OnceRecorder(context).getFileOutputOption(videoFile)//获取一个一次性录制工具
     */
    fun newOnceRecorder(context: Context): OnceRecorder {
        when (val storageConfig = CameraXStoreConfig.videoStorage) {
            is IStore.SAFStoreConfig -> {
                val name = ManagerUtil.generateRandomName()
                val uri = storageConfig.parentUri
                val df: DocumentFile? =
                    DocumentFile.fromTreeUri(context, uri)?.createFile(MimeTypeConsts.mp4, name)
                val uri2 = df!!.uri
                val fd = context.contentResolver.openFileDescriptor(uri2, "rw")
                return OnceRecorder(context).buildFileDescriptorOutput(fd!!)
                    .apply {
                        this.saveFileData = SaveFileData(uri = uri2)
                    }
            }

            is IStore.FileStoreConfig -> {
                val videoFile = ManagerUtil.createMediaFile(
                    storageConfig.getFullPath(),
                    ManagerUtil.VIDEO_EXTENSION
                )
                return OnceRecorder(context).buildFileOutput(videoFile)
                    .apply {
                        this.saveFileData = SaveFileData(
                            videoFile.absolutePath,
                            uri = DocumentFile.fromFile(videoFile).uri
                        )
                    }
            }

            is IStore.MediaStoreConfig -> {
                return OnceRecorder(context).buildMediaStoreOutput(storageConfig)
                    .apply {
                        this.saveFileData = SaveFileData()
                    }
            }
        }
    }
}

/**
 * 将Quality转换为字符串的辅助函数
 */
fun Quality.qualityToString(): String {
    return when (this) {
        Quality.UHD -> "UHD"
        Quality.FHD -> "FHD"
        Quality.HD -> "HD"
        Quality.SD -> "SD"
        else -> throw IllegalArgumentException()
    }
}