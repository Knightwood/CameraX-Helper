package com.kiylx.cameraxexample

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.video.Quality
import com.kiylx.camerax_lib.main.manager.model.MediaType
import com.kiylx.camerax_lib.main.manager.video.LocationKind
import com.kiylx.camerax_lib.main.store.StorageConfig
import com.kiylx.camerax_lib.main.store.VideoCaptureConfig
import com.kiylx.cameraxexample.databinding.ActivityMainBinding
import com.kiylx.store_lib.StoreX

const val ImageDetection = "imageDetection"

class MainActivity : AppCompatActivity() {
    lateinit var page: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        page = ActivityMainBinding.inflate(layoutInflater)
        setContentView(page.root)
        page.one.setOnClickListener {//普通
            val intent: Intent = Intent(this, CameraExampleActivity::class.java)
            intent.putExtra(ImageDetection, false)
            startActivity(intent)
        }

        page.second.setOnClickListener {//人脸识别拍摄
            val intent: Intent = Intent(this, CameraExampleActivity::class.java)
            intent.putExtra(ImageDetection, true)
            startActivity(intent)
        }
        page.third.setOnClickListener {
            val intent: Intent = Intent(this, TestFileDecActivity::class.java)
            intent.putExtra(ImageDetection, true)
            startActivity(intent)
        }
        Log.e(tag, "onCreate")
        StorageConfig.prepare(application)//灰常重要
        initPhoto()
        initVideo()
    }


    fun initPhoto() {
        page.rg1.setOnCheckedChangeListener { group, checkedId ->
            val relativePath = page.relativePath1.text.toString()
            when (checkedId) {
                R.id.save_app_1 -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        StorageConfig.configStorageApp(MediaType.photo, relativePath)
                    } else {
                        StorageConfig.configStorage(
                            MediaType.photo,
                            LocationKind.APP,
                            getExternalFilesDir(null)!!.absolutePath,
                            relativePath
                        )
                    }
                }
                R.id.save_dcim_1 -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        StorageConfig.configStorageDCIM(MediaType.photo, relativePath)
                    } else {
                        StorageConfig.configStorage(
                            MediaType.photo,
                            LocationKind.DCIM,
                            relativePath = relativePath
                        )
                    }
                }
                R.id.save_other_1 -> {
                    Log.e(tag, "存储到其他位置")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        StoreX.with(this).safHelper.requestOneFolder {
                            StorageConfig.configStorageOther(MediaType.photo, it)
                            page.path1.setText(it.path, TextView.BufferType.NORMAL)
                        }
                    } else {
                        StorageConfig.configStorage(
                            MediaType.photo,
                            LocationKind.OTHER,
                            path = page.path1.text.toString(),
                            relativePath = relativePath
                        )
                    }
                }
            }
        }
    }

    fun initVideo() {
        VideoCaptureConfig.run {
            quality = Quality.HD//设置视频 拍摄质量
//            fileSizeLimit=100000 //文件大限制,单位bytes
//            durationLimitMillis =1000*15 //录制时长限制，单位毫秒
        }
        page.rg2.setOnCheckedChangeListener { group, checkedId ->
            val relativePath = page.relativePath2.text.toString()
            when (checkedId) {
                R.id.save_app_2 -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        StorageConfig.configStorageApp(MediaType.video, relativePath)
                    } else {
                        StorageConfig.configStorage(
                            MediaType.video,
                            LocationKind.APP,
                            getExternalFilesDir(null)!!.absolutePath,
                            relativePath
                        )
                    }
                }
                R.id.save_dcim_2 -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        StorageConfig.configStorageDCIM(MediaType.video, relativePath)
                    } else {
                        StorageConfig.configStorage(
                            MediaType.video,
                            LocationKind.DCIM,
                            relativePath = relativePath
                        )
                    }
                }
                R.id.save_other_2 -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        StoreX.with(this).safHelper.requestOneFolder {
                            StorageConfig.configStorageOther(MediaType.video, it)
                            page.path2.setText(it.path, TextView.BufferType.NORMAL)
                        }
                    } else {
                        StorageConfig.configStorage(
                            MediaType.video,
                            LocationKind.OTHER,
                            path = page.path2.text.toString(),
                            relativePath = relativePath
                        )
                    }
                }
            }
        }
    }


    companion object {
        const val tag = "MainActivity"
    }
}