package com.kiylx.camerax_lib.main.manager.model

import android.view.OrientationEventListener
import android.view.Surface
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.*
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.kiylx.camerax_lib.utils.Weak

/**
 * 监听传感器变化，当设备旋转时，变换角度
 */
class SensorRotation(context: FragmentActivity) : LifecycleEventObserver {
    var activity by Weak { context }
    var angle: Int = 0
        internal set(value) {
            field = value
            rotation = when (field) {
                in 45..134 -> Surface.ROTATION_270
                in 135..224 -> Surface.ROTATION_180
                in 225..314 -> Surface.ROTATION_90
                else -> Surface.ROTATION_0
            }
            listener?.angleChanged(rotation,field)
        }

    /**
     * 设备逆时针旋转90度（表现在这里就是270度），内容就应该顺时针旋转90度，此时应该设置为Surface.ROTATION_90
     */
    var rotation = Surface.ROTATION_0
        internal set(value) {
            if (value != field) {
                field = value
                listener?.rotationChanged(field)
            }
        }

    /**
     * 监听传感器的变化，旋转角度值在0-359度，方向是顺时针方向
     *
     */
   private val orientationEventListener: OrientationEventListener by lazy {
        object : OrientationEventListener(activity) {
            override fun onOrientationChanged(orientation: Int) {
                angle = orientation
            }
        }
    }

    init {
        context.lifecycle.addObserver(this)
    }

    var listener: RotationChangeListener? = null

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            ON_CREATE -> {
                activity?.let {
                    orientationEventListener.enable()
                }
            }
            ON_STOP -> {
                activity?.let {
                    orientationEventListener.disable()
                    activity = null
                }
            }
            else -> {}
        }
    }

    interface RotationChangeListener {
        fun angleChanged(rotation: Int,angle: Int){}
        fun rotationChanged(rotation: Int)
    }
}