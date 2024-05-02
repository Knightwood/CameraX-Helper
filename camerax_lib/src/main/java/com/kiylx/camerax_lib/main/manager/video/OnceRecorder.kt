package com.kiylx.camerax_lib.main.manager.video

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.camera.video.*
import com.kiylx.camerax_lib.main.manager.ManagerUtil
import com.kiylx.camerax_lib.main.manager.video.OutputKinds.*
import com.kiylx.camerax_lib.main.store.SaveFileData
import com.kiylx.camerax_lib.main.store.IStore
import com.kiylx.camerax_lib.main.store.VideoRecordConfig
import com.kiylx.store_lib.kit.MimeTypeConsts
import java.io.File
import java.util.*

/**
 *配置每一次的视频录制的输出位置，以及通过配置好的VideoCapture用例获得Recording来录制视频
 *
 * 使用过程：
 *1. 创建 Recorder，配置QualitySelector。
 *2. 使用创建好的Recorder，生成VideoCapture用例，并进行绑定。
 *3. 创建OutputOptions，这个配置了文件生成的名称，路径，输出到那个文件
 *4. 使用 videoCapture.output.prepareRecording(context,outputOption) 方法生成PendingRecording对象。
 *5. PendingRecording对象调用start开始录像并得到Recording对象 ，使用 pause()/resume()/stop() 来控制录制操作。
 *
 * 此类作用：
 * 配置输出选项(OutputOptions)，以及从VideoCapture中获取Recording，用来录视频。每次录制都要new一个实例
 *
 * 三种使用方式对OnceRecorder配置好OutputOptions。
 *             1. OnceRecorder(context).buildMediaStoreOutput(contentValues)
 *             2. OnceRecorder(context).buildFileOutputOption(file)
 *             3. OnceRecorder(context).buildFileDescriptorOutput(fileDescriptor)
 * 然后调用onceRecorder.getVideoRecording(videoCapture)获取PendingRecording对象，这个对象就用来拍视频了。
 *
 */
class OnceRecorder(
    var context: Context,
    var recordConfig: VideoRecordConfig,
) {
    var outputKinds = MEDIA_STORE
    lateinit var outputOption: OutputOptions
    lateinit var saveFileData: SaveFileData//描述文件存放信息

    /**
     * 生成默认的输出位置,输出到相册
     * 默认使用MediaStore，输出到相册
     */
    fun buildMediaStoreOutputOptions(
        storeConfig: IStore.MediaStoreConfig,
        contentValues: ContentValues? = null,
    ) {
        val innerContentValues: ContentValues = contentValues ?: ContentValues().apply {
            val name = ManagerUtil.generateRandomName()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    storeConfig.getRelativePath()
                )
            }
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, MimeTypeConsts.mp4)
        }
        outputOption = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            storeConfig.saveCollection
        ).setContentValues(innerContentValues)
            .apply {
                setFileSizeLimit(recordConfig.fileSizeLimit.inWholeBytes)
                setDurationLimitMillis(recordConfig.durationLimitMillis)
            }
            .setLocation(recordConfig.location)
            .build()
    }

    /**
     * 根据输出配置(outputOption)，从VideoCapture生成一个新的PendingRecording，此实例就能拿来录制了。
     */
    @SuppressLint("MissingPermission")
    fun getVideoRecording(videoCapture: VideoCapture<Recorder>): PendingRecording {
        if (!this::outputOption.isInitialized) {
            throw IllegalArgumentException("outputOption is not Initialized")
        }
        val pendingRecording: PendingRecording =
            when (outputKinds) {
                FILE_DESCRIPTOR -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        videoCapture.output
                            .prepareRecording(
                                context,
                                outputOption as FileDescriptorOutputOptions
                            )
                            .withAudioEnabled()

                    } else {
                        throw IllegalArgumentException(
                            "当使用FileDescriptorOutputOptions时，" +
                                    "prepareRecording方法@RequiresApi(26)"
                        )
                    }
                }

                FILE -> {
                    videoCapture.output
                        .prepareRecording(
                            context,
                            outputOption as FileOutputOptions
                        )
                        .withAudioEnabled()
                }

                MEDIA_STORE -> {
                    videoCapture.output
                        .prepareRecording(
                            context,
                            outputOption as MediaStoreOutputOptions
                        )
                        .withAudioEnabled()

                }
            }
        return pendingRecording
    }


}

/**
 * 外部生成outputOptions，这里就直接赋值，记录输出种类就可以了
 */
fun OnceRecorder.buildOutputOption(outputOptions: OutputOptions): OnceRecorder {
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
fun OnceRecorder.buildMediaStoreOutput(
    storeConfig: IStore.MediaStoreConfig,
    contentValues: ContentValues? = null
): OnceRecorder {
    outputKinds = MEDIA_STORE//记录输出配置种类
    buildMediaStoreOutputOptions(storeConfig, contentValues)
    return this
}

/**
 * 生成FileDescriptor版本的输出配置
 * 要求Android8以上
 */
fun OnceRecorder.buildFileDescriptorOutput(
    fileDescriptor: ParcelFileDescriptor,
): OnceRecorder {
    outputKinds = FILE_DESCRIPTOR
    outputOption = FileDescriptorOutputOptions.Builder(fileDescriptor)
        .setFileSizeLimit(recordConfig.fileSizeLimit.inWholeBytes)
        .setDurationLimitMillis(recordConfig.durationLimitMillis)
        .setLocation(recordConfig.location)
        .build()
    return this
}

/**
 * 生成File版本的输出配置
 */
fun OnceRecorder.buildFileOutput(file: File): OnceRecorder {
    outputKinds = FILE
    outputOption = FileOutputOptions.Builder(file)
        .setFileSizeLimit(recordConfig.fileSizeLimit.inWholeBytes)
        .setDurationLimitMillis(recordConfig.durationLimitMillis)
        .setLocation(recordConfig.location)
        .build()
    return this
}

/**
 * 描述使用哪种类型的[FileOutputOptions]
 * 不暴露给外界，外界也不应该关心这里使用那种类型去保存文件。
 *
 */
enum class OutputKinds {
    FILE_DESCRIPTOR,
    FILE,
    MEDIA_STORE
}