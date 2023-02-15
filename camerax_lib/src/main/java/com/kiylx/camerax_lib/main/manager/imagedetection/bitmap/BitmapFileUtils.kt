package com.kiylx.camerax_lib.main.manager.imagedetection.bitmap

import android.graphics.Bitmap
import android.text.TextUtils
import android.util.Log
import com.blankj.utilcode.util.FileUtils
import com.kiylx.store_lib.kit.ext.generateFileName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


/**
 * Save Bitmap
 * don't forget call in thread
 *
 * @param fileName file name
 * @param bm       picture to save
 * @param parentFolder which folder to save image
 */
suspend fun saveBitmap(fileName: String?, bm: Bitmap, parentFolder:String): File? =
    withContext(Dispatchers.IO) {
        if (bm.width == 0 || bm.isRecycled) {
            return@withContext null
        }
        var name = ""

        if (TextUtils.isEmpty(name)) {
            name = generateFileName() + ".jpg"
        } else {
            if (!fileName!!.contains(".jpg")) name = "$fileName.jpg"
        }
        Log.d("Save Bitmap", "Ready to save picture")
        val saveFile = File(parentFolder, name)
        Log.d("Save Bitmap", "Save Path=$parentFolder")
        //判断指定文件夹的路径是否存在
        if (!FileUtils.isFileExists(parentFolder)) {
            Log.d("Save Bitmap", "parentFolder isn't exist,will create it")
            val file = File(parentFolder)
            file.mkdirs()
        } else {
            //如果指定文件夹创建成功，那么我们则需要进行图片存储操作
            try {
                val saveImgOut = FileOutputStream(saveFile)
                // compress - 压缩的意思
                bm.compress(Bitmap.CompressFormat.JPEG, 100, saveImgOut)
                //存储完成后需要清除相关的进程
                saveImgOut.flush()
                saveImgOut.close()
                Log.d("Save Bitmap", "The picture is save to your phone!")
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        }
        return@withContext saveFile
    }

