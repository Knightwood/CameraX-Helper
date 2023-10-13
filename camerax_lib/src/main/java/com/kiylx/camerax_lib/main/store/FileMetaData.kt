package com.kiylx.camerax_lib.main.store

import android.net.Uri
import android.os.Build
import com.kiylx.camerax_lib.main.manager.video.LocationKind

/**
 * 描述文件存储位置，使用的存储方式，Android版本
 * Android10以下，path都表示file的绝对路径,uri则一直是Uri.empty()
 * Android10以上，存储在app私有目录下时，path表示私有目录下文件的绝对路径，uri为空
 *              存储在相册或其他位置时，path为空，uri表示文件位置
 */
data class FileMetaData(
    val locationKind: LocationKind = LocationKind.DCIM,
    val path: String = "",
    val uri: Uri = Uri.EMPTY,
)