package com.kiylx.camerax_lib.main.manager.model

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.Log
import android.view.Surface
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.*
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.blankj.utilcode.util.LogUtils
import com.kiylx.camerax_lib.utils.Weak

/**
 * 监听视图显示的旋转
 *
 * 设置了”android:configChanges="orientation|screenSize"
 * 不设置activity强制横屏或竖屏
 * 系统打开了 “自动旋转”或 关闭了“方向锁定”
 *
 * 此时，旋转手机，activity界面内容布局会跟着旋转，此时，DisplayListener就能监听到旋转。
 * 这个时候应该让相机的预览视图旋转。
 *
 * 如果限制activity竖屏或横屏，那这里就没用
 *
 * 使用 DisplayListener 可以让您在特定情况下更新相机用例的目标旋转角度，
 * 例如在设备旋转了 180 度后系统没有销毁并重新创建 Activity 的情况下。
 */
class DisplayRotation(context: FragmentActivity) : LifecycleEventObserver {
    var activity by Weak { context }
    var listener: DisplayRotationChangeListener? = null

    /**
     * 设备逆时针旋转90度（表现在这里就是270度），内容就应该顺时针旋转90度，此时应该设置为Surface.ROTATION_90
     */
    var rotation = Surface.ROTATION_0
        internal set(value) {
            field = value
            listener?.rotationChanged(field)
        }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayChanged(displayId: Int) {
            Log.e("旋转2", "DisplayChanged")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display?.let {
                    if (it.displayId == displayId) {
                        rotation = it.rotation
                    }
                }
            } else {
                val rootView = context.findViewById<ViewGroup>(android.R.id.content)
                if (rootView.display.displayId == displayId) {
                    rotation = rootView.display.rotation
                }
            }
        }

        override fun onDisplayAdded(displayId: Int) {
        }

        override fun onDisplayRemoved(displayId: Int) {
        }
    }

    init {
        context.lifecycle.addObserver(this)
    }


    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            ON_START -> {
                activity?.let {
                    val displayManager =
                        it.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                    displayManager.registerDisplayListener(displayListener, null)
                }
            }
            ON_STOP -> {
                activity?.let {
                    val displayManager =
                        it.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                    displayManager.unregisterDisplayListener(displayListener)
                    activity = null
                }
            }
            else -> {}
        }
    }

    interface DisplayRotationChangeListener {
        fun rotationChanged(rotation: Int)
    }
}