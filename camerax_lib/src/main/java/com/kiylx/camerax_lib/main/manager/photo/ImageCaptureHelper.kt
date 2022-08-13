package com.kiylx.camerax_lib.main.manager.photo

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.documentfile.provider.DocumentFile
import com.kiylx.camerax_lib.main.manager.ManagerUtil
import com.kiylx.camerax_lib.main.manager.video.LocationKind.*
import com.kiylx.camerax_lib.main.store.StorageConfig
import com.kiylx.store_lib.kit.MimeTypeConsts


class ImageCaptureHelper {
    companion object {
        /**
         * 提供图像的输出配置。
         * Android10以上，支持输出到相册和文件自身的私有目录,以及saf获取文件夹权限，输出到此文件夹
         * Android10以下，可以输出到任意位置
         */
        fun getFileOutputOption(
            metadata: ImageCapture.Metadata,
            context: Context,
        ): ImageCapture.OutputFileOptions {
            val storageConfig = StorageConfig.imageStorage//读取全局的图像存储配置
            var outputOptions: ImageCapture.OutputFileOptions? = null//根据不同的存储配置生成不同的OutputOptions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {//android 10 及以上
                when (storageConfig.locationKind) {
                    APP -> {//Android10及以上，私有目录，使用绝对路径
                        val photoFile =
                            ManagerUtil.createMediaFile(storageConfig.appSelfAbsolutePath,
                                ManagerUtil.PHOTO_EXTENSION)
                        // 创建输出选项，包含有图片文件和其中的元数据
                        outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                            .setMetadata(metadata)
                            .build()
                    }
                    DCIM -> {//Android10及以上，相册，使用MediaStore
                        val contentValues: ContentValues = ContentValues().apply {
                            val name = ManagerUtil.generateRandomName()
                            put(MediaStore.Images.Media.DISPLAY_NAME, name)
                            put(MediaStore.Audio.Media.RELATIVE_PATH, storageConfig.mediaPath)
                            put(MediaStore.Audio.Media.MIME_TYPE, MimeTypeConsts.jpg)
                        }
                        outputOptions = ImageCapture.OutputFileOptions.Builder(
                            context.contentResolver,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            contentValues
                        ).build()
                    }
                    OTHER -> {//Android10及以上，其他目录，saf，前提要有权限
                        val name = ManagerUtil.generateRandomName()
                        val df: DocumentFile? =
                            DocumentFile.fromTreeUri(context, storageConfig.parentUri)
                                ?.createFile(MimeTypeConsts.jpg, name)
                        val uri = df!!.uri
                        val outputOptionsBuilder =
                            context.contentResolver.openOutputStream(uri)
                                ?.let { outputStream ->
                                    ImageCapture.OutputFileOptions.Builder(outputStream)
                                } ?: throw Exception("fuck")
                        outputOptions = outputOptionsBuilder.build()
                    }
                }
            } else {
                val photoFile = ManagerUtil.createMediaFile(storageConfig.parentAbsoluteFolder,
                    ManagerUtil.PHOTO_EXTENSION)
                // 创建输出选项，包含有图片文件和其中的元数据
                outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                    .setMetadata(metadata)
                    .build()
            }
            return outputOptions
        }
    }
}