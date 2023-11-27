package com.kiylx.camerax_lib.main.manager.photo

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.documentfile.provider.DocumentFile
import com.kiylx.camerax_lib.main.manager.ManagerUtil
import com.kiylx.camerax_lib.main.store.CameraStore
import com.kiylx.camerax_lib.main.store.IStore
import com.kiylx.camerax_lib.main.store.SaveFileData
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
        ): Pair<ImageCapture.OutputFileOptions,SaveFileData> {
            val saveFileData =SaveFileData()
            val storageConfig = CameraStore.imageStorage//读取全局的图像存储配置
            var outputOptions: ImageCapture.OutputFileOptions? = null//根据不同的存储配置生成不同的OutputOptions
            when (storageConfig) {
                is IStore.SAFStoreConfig -> {
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
                    saveFileData.uri=uri
                }

                is IStore.FileStoreConfig -> {
                    val photoFile =
                        ManagerUtil.createMediaFile(
                            storageConfig.getFullPath(),
                            ManagerUtil.PHOTO_EXTENSION
                        )
                    // 创建输出选项，包含有图片文件和其中的元数据
                    outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                        .setMetadata(metadata)
                        .build()
                    saveFileData.path=photoFile.absolutePath
                    saveFileData.uri=DocumentFile.fromFile(photoFile).uri
                }

                is IStore.MediaStoreConfig -> {
                    val contentValues: ContentValues = ContentValues().apply {
                        val name = ManagerUtil.generateRandomName()
                        put(MediaStore.Images.Media.DISPLAY_NAME, name)
                        put(MediaStore.Images.Media.RELATIVE_PATH, storageConfig.getRelativePath())
                        put(MediaStore.Images.Media.MIME_TYPE, MimeTypeConsts.jpg)
                    }
                    outputOptions = ImageCapture.OutputFileOptions.Builder(
                        context.contentResolver,
                        storageConfig.saveCollection,
                        contentValues
                    ).build()
                }
            }

            return outputOptions to saveFileData
        }
    }
}