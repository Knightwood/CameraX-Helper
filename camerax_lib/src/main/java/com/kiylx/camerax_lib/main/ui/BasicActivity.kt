package com.kiylx.camerax_lib.main.ui

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity

/**
 * Manifest需要设置
 * android:configChanges="orientation|screenSize"
 * 以保证不销毁activity
 */
abstract class BasicActivity : AppCompatActivity() {

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //屏幕方向这个可选
        //this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setScreenOrientation()
        Log.d(tag,"onCreate")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(tag,"onConfigurationChanged")
    }
    override fun onRestart() {
        super.onRestart()
        Log.d(tag,"onRestart")
    }
    override fun onStart() {
        super.onStart()
        Log.d(tag,"onStart")
    }
    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        Log.d(tag,"onResume")
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        Log.d(tag,"onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(tag,"onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag,"onDestroy")
    }


    /**
     * 判断是否平板设备
     * @param context
     * @return true:平板,false:手机
     */
    private fun FragmentActivity.isTabletDevice(): Boolean {
        return resources
            .configuration
            .screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    //设置屏幕方向
    @SuppressLint("SourceLockedOrientationActivity")
    fun FragmentActivity.setScreenOrientation() {
        if (!isTabletDevice()) {
            //手机，竖屏
            this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            //平板，横屏
            this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    companion object{
        const val tag="基类Activity"
    }
}