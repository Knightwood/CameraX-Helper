package com.kiylx.camerax_lib.main.store
//
//import android.app.Application
//import android.net.Uri
//import android.os.Build
//import android.os.Environment
//import androidx.annotation.RequiresApi
//import com.kiylx.camerax_lib.main.manager.ManagerUtil
//import com.kiylx.camerax_lib.main.manager.model.MediaType
//import com.kiylx.camerax_lib.main.manager.video.LocationKind
//import com.kiylx.camerax_lib.utils.Weak
//import java.io.File
//
///**
// * 全局的(存储照片、视频)存储配置 用法：
// * 1. StorageConfig.prepare(application)
// * 2.（可选）StorageConfig.configStorage()
// */
//object StorageConfig {
//    var application by Weak<Application> { null }
//    lateinit var imageStorage: Storage
//    lateinit var videoStorage: Storage
//
//
//
//}
//
//
///** android 10及以上使用MediaStore或者saf android 10以下使用File */
//class Storage(
//    var relativePath: String = "SimpleCameraX",//相对路径：在上面的父路径下建立的文件夹名称,用于在Android10以下，或者Android10以上中的私有目录、MediaStore
//    var mediaType: Int = -1,//标识哪种媒体的存储配置
//) {
//
//    var locationKind: LocationKind = LocationKind.DCIM
//
//    /** Android10及以上，放在非公共目录和私有目录下时 */
//    var parentUri: Uri = Uri.EMPTY
//
//    /** Android10及以上，使用mediaStore存储到相册 */
//    var mediaPath: String = Environment.DIRECTORY_DCIM
//        get() {
//            return "$field${File.separator}$relativePath"
//        }
//
//    /** Android10及以上,文件夹绝对路径,用在app自身路径下 默认值是app外部私有目录 */
//    var appSelfAbsolutePath: String =
//        StorageConfig.application!!.getExternalFilesDir(null)!!.absolutePath
//        get() {
//            val b = ManagerUtil.createDir(field)
//            if (!b) {
//                throw Exception("文件夹创建失败")
//            }
//            return "$field${File.separator}$relativePath"
//        }
//
//    /** Android10以下,表示任何位置 默认值是相册DCIM目录 */
//    var parentAbsoluteFolder: String =
//        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath
//        get() {
//            val b = ManagerUtil.createDir(field)
//            if (!b) {
//                throw Exception("文件夹创建失败")
//            }
//            return "$field${File.separator}$relativePath"
//        }
//
//    companion object {
//        fun default(type: Int): Storage {
//            return Storage(mediaType = type)
//        }
//    }
//}