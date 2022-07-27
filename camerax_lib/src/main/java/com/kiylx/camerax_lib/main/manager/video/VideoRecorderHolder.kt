package com.kiylx.camerax_lib.main.manager.video

import androidx.camera.video.*
import java.util.concurrent.ExecutorService

/**
 * 配置视频录制工具，不包含录制视频。
 * 使用CameraX 软件包内的 camera-video 库中实现
 * 使用此类获取videoCapture。
 * 用法： VideoRecorderHolder.getVideoCapture(cameraExecutor) 获取videoCapture。
 * 录制视频：
 * 用法：val onceRecorder = OnceRecorder(context).getFileOutputOption(videoFile)//获取一个一次性录制工具
 *
 *      var recording: Recording? = onceRecorder.getVideoRecording()//使用这个录制视频，暂停，恢复，停止录制
 *      recording?.start()//开始录制
 *      这个是一次性的，下一次录制视频，要获取一个新的onceRecorder
 */
object VideoRecorderHolder {
    internal var videoCapture: VideoCapture<Recorder>? = null
    internal var recorder: Recorder? = null
    internal var quality: QualitySelector = QualitySelector.from(Quality.LOWEST)//默认录制质量为最低画质

    fun setMostHighQuality() {
        quality = QualitySelector.from(Quality.HIGHEST)
    }

    fun setSupportedQuality() {
        //以下代码会请求支持的最高录制分辨率；如果所有请求分辨率都不受支持，则授权 CameraX 选择最接近 Quality.SD 分辨率的分辨率：
        quality = QualitySelector.fromOrderedList(listOf(
            Quality.UHD,
            Quality.FHD,
            Quality.HD,
            Quality.SD),
            FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)//回退策略，当不支持任何所需质量时会选至少比这个好一点的。
        )
    }

    private fun createCapture(executor: ExecutorService) {
        recorder = Recorder.Builder()
            .setExecutor(executor)
            .setQualitySelector(this.quality)
            .build()
            .also {
                videoCapture = VideoCapture.withOutput(it)
            }
    }

    fun getVideoCapture(
        executor: ExecutorService,
        quality: Quality? = null,
    ): VideoCapture<Recorder> {
        if (quality == null) {
            setSupportedQuality()
        } else {
            this.quality = QualitySelector.from(quality)
        }
        createCapture(executor)
        return videoCapture!!
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