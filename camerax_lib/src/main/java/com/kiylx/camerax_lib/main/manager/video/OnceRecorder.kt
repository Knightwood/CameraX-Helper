package com.kiylx.camerax_lib.main.manager.video

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.annotation.RestrictTo
import androidx.camera.video.*
import com.kiylx.camerax_lib.main.manager.video.OutputKinds.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 配置输出选项，从videocapture中获取recorder，再从中获取pendingRecorder，用它录视频
 * 目前Android10以上，支持输出到相册和app私有目录
 * Android10一下，支持输出到任意位置
 *
 * 三种使用方式: 1. OnceRecorder(context).getMediaStoreOutput(contentValues)
 *             2. OnceRecorder(context).getFileOutputOption(file)
 *             3. OnceRecorder(context).getFileDescriptorOutput(fileDescriptor)
 *
 */
class OnceRecorder(
    var context: Context,
) {
    var outputKinds = MEDIA_STORE
    lateinit var outputOption: OutputOptions

    /**
     * 不使用MediaStore时，使用文件路径记录文件位置
     */
    var filePath = ""

    /**
     * 使用MediaStore时使用uri记录文件位置
     */
    var fileUri: Uri = Uri.EMPTY

    /**
     * 生成默认的输出位置,输出到相册
     * 默认使用mediastore，输出到相册
     */
    internal fun getDefaultOutputOptions() {
        val contentValues: ContentValues = ContentValues().apply {
            val name = SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA).format(Date())
                .toString() + ".mp4"
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
        }
        outputOption = MediaStoreOutputOptions.Builder(context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
    }

    @SuppressLint("MissingPermission")
    internal fun getVideoRecording(): PendingRecording? {
        if (VideoRecorderHolder.videoCapture == null || VideoRecorderHolder.recorder == null) {
            throw Exception("没有videoCapture")
        }
        val pendingRecording: PendingRecording? =
            when (outputKinds) {
                FILE_DESCRIPTOR -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        VideoRecorderHolder.videoCapture!!.output
                            .prepareRecording(
                                context,
                                outputOption as FileDescriptorOutputOptions)
                            .withAudioEnabled()

                    } else {
                        null
                    }
                }
                FILE -> {
                    VideoRecorderHolder.videoCapture!!.output
                        .prepareRecording(
                            context,
                            outputOption as FileOutputOptions)
                        .withAudioEnabled()
                }
                MEDIA_STORE -> {
                    VideoRecorderHolder.videoCapture!!.output
                        .prepareRecording(
                            context,
                            outputOption as MediaStoreOutputOptions)
                        .withAudioEnabled()

                }
            }
        return pendingRecording
    }


}

/**
 * 外部生成outputOptions，这里就直接赋值，记录输出种类就可以了
 */
fun OnceRecorder.getOutputOption(outputOptions: OutputOptions): OnceRecorder {
    this.outputOption = outputOptions
    when (outputOptions) {//记录输出配置种类
        is MediaStoreOutputOptions -> this.outputKinds = MEDIA_STORE
        is FileOutputOptions -> this.outputKinds = FILE
        else -> this.outputKinds = FILE_DESCRIPTOR
    }
    return this
}

/**
 * 生成mediaStore版本的输出配置。
 * 如果传入null，默认存储到相册
 */
fun OnceRecorder.getMediaStoreOutput(contentValues: ContentValues? = null): OnceRecorder {
    outputKinds = MEDIA_STORE//记录输出配置种类
    if (contentValues == null) {
        getDefaultOutputOptions()
    } else {
        outputOption = MediaStoreOutputOptions.Builder(context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
    }
    return this
}

/**
 * 生成FileDescriptor版本的输出配置
 * 要求Android8以上
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
fun OnceRecorder.getFileDescriptorOutput(
    fileDescriptor: ParcelFileDescriptor,
    limit: Long = -1,
): OnceRecorder {
    outputKinds = FILE_DESCRIPTOR
    val tmp = FileDescriptorOutputOptions.Builder(fileDescriptor)
    if (limit > 0)
        tmp.setFileSizeLimit(limit)
    outputOption = tmp.build()
    return this
}

/**
 * 生成File版本的输出配置
 * @param limit 文件大小限制
 */
fun OnceRecorder.getFileOutputOption(file: File, limit: Long = -1): OnceRecorder {
    outputKinds = FILE
    val tmp = FileOutputOptions.Builder(file)
    if (limit > 0)
        tmp.setFileSizeLimit(limit)
    outputOption = tmp.build()
    return this
}

/**
 * 描述使用哪种类型的[FileOutputOptions]
 * 不暴露给外界，外界也不应该关心这里使用那种类型去保存文件。
 *
 * 外界只应设置[LocationKind]及文件存储路径。
 */
enum class OutputKinds {
    /**
     * Android O及以上使用
     * 还未完全实现，未来也可能不实现
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    FILE_DESCRIPTOR,

    /**
     * 在Android Q以下会使用它
     */
    FILE,

    /**
     * Android Q 及以上会使用它
     */
    MEDIA_STORE
}