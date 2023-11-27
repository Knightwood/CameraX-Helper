package com.kiylx.camerax_lib.main.store

import android.net.Uri
import com.kiylx.camerax_lib.main.manager.video.LocationKind

/**
 * 描述文件存储位置
 * 若使用了mediastore，path为空，uri为content开头
 * 若使用文件，path不为空，uri为file开头
 * 若使用SAF，path为空，uri为content开头
 */
data class SaveFileData(
    var path: String = "",
    var uri: Uri = Uri.EMPTY,
)