package com.kiylx.camerax_lib.main.manager.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import com.kiylx.camerax_lib.main.manager.ManagerUtil
import com.kiylx.camerax_lib.main.manager.video.LocationKind.APP
import com.kiylx.camerax_lib.main.manager.video.LocationKind.DCIM
import com.kiylx.camerax_lib.main.manager.video.OnceRecorderHelper
import java.text.SimpleDateFormat
import java.util.*

class ImageCaptureHelper

/**
 * 提供图像的输出配置。
 * Android10以上，支持输出到相册和文件自身的私有目录
 * Android10以下，可以输出到任意位置
 */
fun getFileOutputOption(
    metadata: ImageCapture.Metadata,
    context: Context,
): ImageCapture.OutputFileOptions? {
    var outputOptions: ImageCapture.OutputFileOptions? = null
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {//android 10 及以上
        when (OnceRecorderHelper.locationKind) {
            APP -> {
                val photoFile = ManagerUtil.createMediaFile(OnceRecorderHelper.MyPhotoDir,
                    ManagerUtil.PHOTO_EXTENSION)
                // 创建输出选项，包含有图片文件和其中的元数据
                outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                    .setMetadata(metadata)
                    .build()
            }
            DCIM -> {
                val contentValues: ContentValues = ContentValues().apply {
                    val name = SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA).format(Date())
                        .toString() + ".mp4"
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                }
                outputOptions = ImageCapture.OutputFileOptions.Builder(
                    context.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ).build()
            }
            else -> {
                throw Exception("其他方式还未实现")
            }
        }

    } else {
        val photoFile = ManagerUtil.createMediaFile(OnceRecorderHelper.MyPhotoDir,
            ManagerUtil.PHOTO_EXTENSION)
        // 创建输出选项，包含有图片文件和其中的元数据
        outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
            .setMetadata(metadata)
            .build()
    }
    return outputOptions
}