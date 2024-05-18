package com.kiylx.camerax_lib.main.manager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraProvider
import androidx.camera.core.CameraSelector
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.permissionx.guolindev.PermissionX
import com.permissionx.guolindev.callback.RequestCallback
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

const val KEY_CAMERA_EVENT_ACTION = "key_camera_event_action"
const val KEY_CAMERA_EVENT_EXTRA = "key_camera_event_extra"
const val CAMERA_CONFIG = "camera_config"   //相机的配置

class ManagerUtil {
    companion object {

        const val TAG = "CameraXFragment"
        const val PHOTO_EXTENSION = ".jpg"
        const val VIDEO_EXTENSION = ".mp4"

        const val TAKE_PHOTO_CASE = 0
        const val TAKE_VIDEO_CASE = 1
        const val IMAGE_ANALYZER_CASE = 2
        const val NONE_USE_CASE = -1
        const val CUSTOM_USE_CASE_GROUP =-2

        //图像的比例
        const val RATIO_4_3_VALUE = 4.0 / 3.0
        const val RATIO_16_9_VALUE = 16.0 / 9.0


        var REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )

        fun requestPerms(context: FragmentActivity, requestCallback: RequestCallback) {
            PermissionX.init(context).permissions(REQUIRED_PERMISSIONS.asList())
                .onForwardToSettings(null)
                .request(requestCallback)
        }

        /**
         * 需要添加的权限也一起申请了
         */
        fun addRequestPermission(permissions: Array<String>) {
            permissions.forEach {
                if (!REQUIRED_PERMISSIONS.contains(it)) {
                    REQUIRED_PERMISSIONS =
                        REQUIRED_PERMISSIONS.plus(permissions)
                }
            }
        }

        /** Convenience method used to check if all permissions required by this app are granted */
        fun hasAllPermissions(context: Context) = REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        /**
         * 以当前时间为名字
         */
        fun generateRandomName(): String {
            return SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        }

        /**
         * 产生的素材都将统一放在文件下
         *
         * @throws IOException
         */
        fun createMediaFile(baseFolder: String, format: String): File {
            val timeStamp = generateRandomName()
            createDir(baseFolder)
            return File(baseFolder + timeStamp + format)
        }

        /**
         * 创建图片目录
         *
         * @param dirPath
         * @return
         */
        fun createDir(dirPath: String): Boolean {
            //判断为空的目录的情况用默认目录。。。。
            val file = File(dirPath)
            return if (!file.exists() || !file.isDirectory) {
                file.mkdirs()
            } else
                true
        }

        /**
         * 检查设备是否有后置摄像头
         */
        fun CameraProvider.hasBackCamera(): Boolean {
            return hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
        }

        /**
         * 检查设备是否有前置摄像头
         */
        fun CameraProvider.hasFrontCamera(): Boolean {
            return hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
        }


        /**
         *  检测是4：3 还是16：9 好点
         *
         *  [androidx.camera.core.ImageAnalysis Config] requires enum value of
         *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
         *
         *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
         *  of preview ratio to one of the provided values.
         *
         *  @param width - preview width   预览的宽
         *  @param height - preview height 预览的高
         *  @return suitable aspect ratio 合适的比例
         */
        fun aspectRatio(width: Int, height: Int): Int {
            val previewRatio = max(width, height).toDouble() / min(width, height)
            if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
                return AspectRatio.RATIO_4_3
            }
            return AspectRatio.RATIO_16_9
        }

    }
}

/**
 * 将字符串列表中的字符串拼接起来
 * @param str 拼接后字符串的开头部分
 */
fun MutableList<String>.toStr(str: String): String {
    val sb = StringBuilder("$str : \n")
    this.forEach {
        sb.append("$it \n")
    }
    return sb.toString()
}