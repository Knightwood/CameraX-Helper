package com.kiylx.cameraxexample

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.video.Quality
import androidx.core.view.updatePadding
import com.kiylx.camerax_lib.main.manager.util.setWindowEdgeToEdge
import com.kiylx.camerax_lib.main.manager.util.statusBarTheme
import com.kiylx.camerax_lib.main.store.CameraXStoreConfig
import com.kiylx.camerax_lib.main.store.IStore
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
        setWindowEdgeToEdge{
            page.root.updatePadding(top=it.top,bottom=it.bottom)
        }
        statusBarTheme(true)
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
        CameraXStoreConfig.prepare(application)//初始化存储配置
        initPhoto()
        initVideo()
    }


    fun initPhoto() {
        page.rg1.setOnCheckedChangeListener { group, checkedId ->
            var relativePath = page.relativePath1.text.toString()
            when (checkedId) {
                R.id.save_app_1 -> {
                    CameraXStoreConfig.configPhoto(
                        IStore.FileStoreConfig(
                            application.cacheDir.absolutePath, relativePath
                        )
                    )
                }
                R.id.save_dcim_1 -> {
                    //默认保存到相册
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {//android 10 及以上
                        CameraXStoreConfig.configPhoto(
                            IStore.MediaStoreConfig(
                                saveCollection = FileLocate.IMAGE.uri,
                                mediaFolder = Environment.DIRECTORY_DCIM,
                                targetFolder = relativePath
                            )
                        )
                    } else {
                        CameraXStoreConfig.configPhoto(
                            IStore.FileStoreConfig(
                                targetFolder = relativePath
                            )
                        )
                    }
                }

                R.id.save_other_1 -> {
                    Log.e(tag, "存储到其他位置")
                    StoreX.with(this).safHelper.requestOneFolder {
                        CameraXStoreConfig.configPhoto(
                            IStore.SAFStoreConfig(it)
                        )
                        page.path1.setText(it.path, TextView.BufferType.NORMAL)
                    }
                }
            }
        }
    }

    fun initVideo() {
        page.rg2.setOnCheckedChangeListener { group, checkedId ->
            val relativePath = page.relativePath2.text.toString()
            when (checkedId) {
                R.id.save_app_2 -> {
                    CameraXStoreConfig.configVideo(
                        IStore.FileStoreConfig(
                            application.cacheDir.absolutePath, relativePath
                        )
                    )
                }

                R.id.save_dcim_2 -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {//android 10 及以上
                        CameraXStoreConfig.configVideo(
                            IStore.MediaStoreConfig(
                                saveCollection = FileLocate.VIDEO.uri,
                                mediaFolder = Environment.DIRECTORY_DCIM,
                                targetFolder = relativePath
                            )
                        )
                    } else {
                        CameraXStoreConfig.configVideo(
                            IStore.FileStoreConfig(
                                targetFolder = relativePath
                            )
                        )
                    }

                }

                R.id.save_other_2 -> {
                    StoreX.with(this).safHelper.requestOneFolder {
                        CameraXStoreConfig.configVideo(
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