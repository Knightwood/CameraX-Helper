package com.kiylx.camerax_lib.main.manager.video

import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.video.*
import com.kiylx.camerax_lib.main.store.VideoCaptureConfig
import java.util.concurrent.ExecutorService

/**
 * 配置VideoCapture，不包含录制视频。
 * 使用CameraX 软件包内的 camera-video 库中实现
 * 使用此类获取videoCapture，之后绑定用例。
 *
 * 用法：1.  videoCapture = VideoCaptureHolder.getVideoCapture(cameraExecutor) 获取videoCapture。
 *      2.  camera = cameraProvider?.bindToLifecycle(lifeOwner,cameraSelector,preview,videoCapture) 绑定用例
 * 绑定用例之后，用此方法录制视频：
 * 用法：val onceRecorder = OnceRecorder(context).getFileOutputOption(videoFile)//获取一个一次性录制工具
 *
 *      var recording: Recording? = onceRecorder.getVideoRecording()//使用这个录制视频，暂停，恢复，停止录制
 *      recording?.start()//开始录制
 *      这个是一次性的，下一次录制视频，要获取一个新的onceRecorder
 */
object VideoCaptureHolder {
    internal var videoCapture: VideoCapture<Recorder>? = null
    internal var quality: QualitySelector = QualitySelector.from(Quality.LOWEST)//默认录制质量为最低画质

    /**
     * 选择“Quality”列表中一个比较好的
     */
    private fun setSupportedQuality() {
        //以下代码会请求支持的最高录制分辨率；如果所有请求分辨率都不受支持，则授权 CameraX 选择最接近 Quality.SD 分辨率的分辨率：
        quality = QualitySelector.fromOrderedList(
            listOf(
                Quality.UHD,
                Quality.FHD,
                Quality.HD,
                Quality.SD
            ),
            FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)//回退策略，当不支持任何所需质量时会选至少比这个好一点的。
        )
    }

    /**
     * videoCapture是由Recorder生成
     * 如果给出“quality”，就用它生成VideoCapture，
     * 否则，自动挑选一个比较合适的“quality”生成VideoCapture
     */
    fun getVideoCapture(
        executor: ExecutorService,
        quality: Quality? = null,
        rotation: Int? = null,
    ): VideoCapture<Recorder> {
        if (quality == null) {
            setSupportedQuality()
        } else {
            this.quality = QualitySelector.from(quality)
        }
        Recorder.Builder()
            .setExecutor(executor)
            .setQualitySelector(this.quality)
            .apply {
                if (VideoCaptureConfig.encodingBitRate > 0) {
                    setTargetVideoEncodingBitRate(VideoCaptureConfig.encodingBitRate)
                }
            }
            .build()
            .also {
//                videoCapture = VideoCapture.withOutput(it)
                videoCapture = VideoCapture.Builder<Recorder>(it)
                    .apply {
                        rotation?.let { setTargetRotation(it) }
                    }
                    .build()
            }
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