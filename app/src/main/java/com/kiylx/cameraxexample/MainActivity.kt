package com.kiylx.cameraxexample

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.video.Quality
import com.kiylx.camerax_lib.main.store.CameraStore
import com.kiylx.camerax_lib.main.store.IStore
import com.kiylx.camerax_lib.main.store.VideoCaptureConfig
import com.kiylx.cameraxexample.databinding.ActivityMainBinding
import com.kiylx.store_lib.StoreX
import com.kiylx.store_lib.mediastore.FileLocate

const val ImageDetection = "imageDetection"

class MainActivity : AppCompatActivity() {
    lateinit var page: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        page = ActivityMainBinding.inflate(layoutInflater)
        setContentView(page.root)
        page.one.setOnClickListener {
            val intent: Intent = Intent(this, CameraExampleActivity::class.java)
            intent.putExtra(ImageDetection, false)
            startActivity(intent)
        }

        page.second.setOnClickListener {
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
        CameraStore.prepare(application)//初始化存储配置
        initPhoto()
        initVideo()
    }


    fun initPhoto() {
        page.rg1.setOnCheckedChangeListener { group, checkedId ->
            var relativePath = page.relativePath1.text.toString()
            when (checkedId) {
                R.id.save_app_1 -> {
                    CameraStore.configPhoto(
                        IStore.FileStoreConfig(
                            application.cacheDir.absolutePath, relativePath
                        )
                    )
                }
                R.id.save_dcim_1 -> {
                    //默认保存到相册
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {//android 10 及以上
                        CameraStore.configPhoto(
                            IStore.MediaStoreConfig(
                                saveCollection = FileLocate.IMAGE.uri,
                                mediaFolder = Environment.DIRECTORY_DCIM,
                                targetFolder = relativePath
                            )
                        )
                    } else {
                        CameraStore.configPhoto(
                            IStore.FileStoreConfig(
                                targetFolder = relativePath
                            )
                        )
                    }
                }

                R.id.save_other_1 -> {
                    Log.e(tag, "存储到其他位置")
                    StoreX.with(this).safHelper.requestOneFolder {
                        CameraStore.configPhoto(
                            IStore.SAFStoreConfig(it)
                        )
                        page.path1.setText(it.path, TextView.BufferType.NORMAL)
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
                    CameraStore.configVideo(
                        IStore.FileStoreConfig(
                            application.cacheDir.absolutePath, relativePath
                        )
                    )
                }

                R.id.save_dcim_2 -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {//android 10 及以上
                        CameraStore.configVideo(
                            IStore.MediaStoreConfig(
                                saveCollection = FileLocate.VIDEO.uri,
                                mediaFolder = Environment.DIRECTORY_DCIM,
                                targetFolder = relativePath
                            )
                        )
                    } else {
                        CameraStore.configVideo(
                            IStore.FileStoreConfig(
                                targetFolder = relativePath
                            )
                        )
                    }

                }

                R.id.save_other_2 -> {
                    StoreX.with(this).safHelper.requestOneFolder {
                        CameraStore.configVideo(
                            IStore.SAFStoreConfig(it)
                        )
                        page.path2.setText(it.path, TextView.BufferType.NORMAL)
                    }
                }
            }
        }
    }


    companion object {
        const val tag = "MainActivity"
    }
}