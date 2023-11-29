package com.kiylx.camerax_lib.main.store

import android.location.Location
import androidx.camera.video.Quality

object VideoCaptureConfig {
    /**
     * 设置用于录制的预期视频编码比特率。
     * 目标视频编码比特率尝试使实际视频编码比特率接近请求的比特率。比特率在录制过程中可能会有所不同，具体取决于录制的场景。
     * 将对请求的比特率进行额外的检查，以确保指定的比特率适用，有时传递的比特率会在内部更改，以确保视频录制可以根据平台的功能顺利进行。
     * 此 API 仅影响视频流，不应被视为整个录制的目标。音频流的比特率不受此 API 的影响。
     * 如果未调用此方法（此处不进行修改，还是0的状态下），则默认选择适当的比特率进行普通视频录制。仅当需要自定义比特率时才调用此方法。
     */
    var encodingBitRate = 0

    /**
     * 位置信息
     * Sets a Location object representing a geographic location where the video was recorded.
     * When use with Recorder, the geographic location is stored in udta box if the output format is MP4, and is ignored for other formats. The geographic location is stored according to ISO-6709 standard.
     * If null, no location information will be saved with the video. Default value is null.
     */
    var location: Location? = null

    /**
     * 录制时长
     * Sets the limit for the video duration in milliseconds.
     * When used to generate recording with Recorder, if the specified duration limit is reached while the recording is being recorded, the recording will be finalized with VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED.
     * If not set or set with zero, the duration will be unlimited. If set with a negative value, an IllegalArgumentException will be thrown.
     */
    var durationLimitMillis: Long = 0

    /**
     * 文件大小限制
     * Sets the limit for the file length in bytes.
     * When used with Recorder to generate recording, if the specified file size limit is reached while the recording is being recorded, the recording will be finalized with VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED.
     * If not set or set with zero, the file size will be unlimited. If set with a negative value, an IllegalArgumentException will be thrown.
     */
    var fileSizeLimit: Long = 0

    /**
     * 视频录制质量，查看[Quality]，当设置的视频拍摄质量不支持时，将会自动寻找支持的最高质量
     */
    var quality: Quality? = null

    /**
     * 实验特性
     * 持久性录制，若开启此特性，
     * 在切换摄像头时保持录制而不停止
     */
    var asPersistentRecording =false
}