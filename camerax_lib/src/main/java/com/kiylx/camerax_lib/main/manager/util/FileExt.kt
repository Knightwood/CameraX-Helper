package com.kiylx.camerax_lib.main.manager.util

import android.content.Context
import java.io.File


var globalRootPath = ""

val MyPhotoDir = "$globalRootPath/cameraX/images/"
val rootFolder =
    File(
        MyPhotoDir
    ).apply {
        if (!exists())
            mkdirs()
    }

fun makeTempFile(context: Context): File {
    if (globalRootPath.isEmpty()) {
        globalRootPath = context.getExternalFilesDir(null)!!.absolutePath
    }
    return File.createTempFile("${System.currentTimeMillis()}", ".png", rootFolder)
}