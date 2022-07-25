package com.kiylx.cameraxexample

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

abstract class BasicActivity() : AppCompatActivity() {

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(setContent())
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        initView(savedInstanceState)
    }

    /**
     * 子类实现此方法以返回布局文件
     */
    abstract fun setContent(): View

    /**
     * onCreate中调用,以初始化数据
     */
    open fun initView(savedInstanceState: Bundle?) {}

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}