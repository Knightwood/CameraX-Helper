package com.kiylx.cameraxexample

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.kiylx.camerax_lib.main.manager.ManagerUtil
import kotlinx.android.synthetic.main.activity_main.*

/**
 * 最初是github上的CameraxFragment项目和一个演示如何使用谷歌机器学习的库给了我启发，
 * 我吸取了两个项目思想，构建出来这个项目。
 */
const val ImageDetection="imageDetection"
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        one.setOnClickListener {
            ManagerUtil.requestPerms(this) { allGranted, grantedList, deniedList ->
                if (allGranted) {//普通
                    val intent: Intent = Intent(this, CameraExampleActivity::class.java)
                    intent.putExtra(ImageDetection,false)
                    startActivity(intent)
                }
                else {
                    throw Exception("没权限搞个屁啊")
                }
            }
        }
        second.setOnClickListener {//人脸识别拍摄
            ManagerUtil.requestPerms(this) { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    val intent: Intent = Intent(this, CameraExampleActivity::class.java)
                    intent.putExtra(ImageDetection,true)
                    startActivity(intent)
                }
                else {
                    throw Exception("没权限搞个屁啊")
                }
            }
        }

    }
}