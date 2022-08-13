package com.kiylx.cameraxexample

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import com.kiylx.camerax_lib.main.manager.ManagerUtil
import com.kiylx.camerax_lib.main.manager.model.MediaType
import com.kiylx.camerax_lib.main.store.StorageConfig
import com.kiylx.cameraxexample.databinding.ActivityMainBinding
import com.kiylx.store_lib.StoreX
import com.kiylx.store_lib.kit.MimeTypeConsts
import com.kiylx.store_lib.mediastore.FileLocate

/**
 * 最初是github上的CameraxFragment项目和一个演示如何使用谷歌机器学习的库给了我启发，
 * 我吸取了两个项目思想，构建出来这个项目。
 */
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
        Log.e(tag, "onCreate")
        StorageConfig.prepare(application)//灰常重要
        StoreX.with(this).safHelper.requestOneFolder {
            StorageConfig.configStorageOther(MediaType.video,it)//配置视频输出位置，图片使用默认输出位置
        }
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.e(tag, "onConfigurationChanged")
    }

    override fun onRestart() {
        super.onRestart()
        Log.e(tag, "onRestart")
    }

    override fun onStart() {
        super.onStart()
        /*if (StorageConfig.uri== Uri.EMPTY){
            StoreX.initSaf(this).requestOneFolder {
                StorageConfig.uri=it
            }
        }*/
        Log.e(tag, "onStart")
        Log.e(tag,
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath)
    }

    override fun onResume() {
        super.onResume()
        Log.e(tag, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.e(tag, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.e(tag, "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(tag, "onDestroy")
    }

    companion object {
        const val tag = "MainActivity"
    }
}