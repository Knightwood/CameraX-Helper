package com.kiylx.camerax_lib.main

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.KeyEvent
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.kiylx.camerax_lib.main.CameraConfig
import com.kiylx.camerax_lib.R
import com.kiylx.camerax_lib.main.CameraConfig.Companion.CAMERA_FLASH_ALL_ON
import com.kiylx.camerax_lib.main.CameraConfig.Companion.CAMERA_FLASH_AUTO
import com.kiylx.camerax_lib.main.CameraConfig.Companion.CAMERA_FLASH_OFF
import com.kiylx.camerax_lib.main.CameraConfig.Companion.CAMERA_FLASH_ON
import com.kiylx.camerax_lib.main.buttons.CaptureListener
import com.kiylx.camerax_lib.main.manager.model.CaptureResultListener
import com.kiylx.camerax_lib.utils.PermissionTipsDialog
import kotlinx.android.synthetic.main.activity_camera_x.*
import java.io.File



class CameraXActivity : AppCompatActivity(), CameraXFragment.OnPermissionRequestListener {
    private var cacheMediasDir =
        Environment.getExternalStorageDirectory().toString() + "/cameraX/images/"
    private lateinit var cameraXFragment: CameraXFragment
    private lateinit var mOrientationListener: OrientationEventListener

    //有些应用希望能添加位置信息在水印上面
    private val perms = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_x)

        val cameraConfig = CameraConfig.Builder(cacheMediasDir)
            .flashMode(CameraConfig.CAMERA_FLASH_OFF)
            .mediaMode(CameraConfig.MEDIA_MODE_ALL)   //视频拍照都可以
            .build()

        cameraXFragment = CameraXFragment.newInstance(cameraConfig)
        cameraXFragment.addRequestPermission(perms) //添加需要多申请的权限，比如水印相机需要定位权限

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, cameraXFragment).commit()


        //拍照，拍视频的UI 操作的各种状态处理
        capture_btn.setCaptureListener(object : CaptureListener {
            override fun takePictures() {
                cameraXFragment.takePhoto()
            }

            //开始录制视频
            override fun recordStart() {
                cameraXFragment.takeVideo()
            }

            //录制视频结束
            override fun recordEnd(time: Long) {
                cameraXFragment.stopTakeVideo(time)
            }

            //长按拍视频的时候，在屏幕滑动可以调整焦距缩放
            override fun recordZoom(zoom: Float) {
                val a = zoom
            }

            //录制视频错误（拍照也会有错误，先不处理了吧）
            override fun recordError(message: String) {

            }
        })

        //拍照录视频操作结果通知回调
        cameraXFragment.setCaptureResultListener(object : CaptureResultListener {
            override fun onVideoRecorded(filePath: String) {
                Log.d("CameraXFragment", "onVideoRecorded：$filePath")

                val photoURI: Uri = FileProvider.getUriForFile(
                    baseContext,
                    baseContext.getApplicationContext().getPackageName().toString() + ".provider",
                    File(filePath)
                )

            }

            override fun onPhotoTaken(filePath: String) {
                Log.d("CameraXFragment", "onPhotoTaken： $filePath")

            }
        })

        flush_btn.setOnClickListener {
            if (flash_layout.visibility == View.VISIBLE) {
                flash_layout.visibility = View.INVISIBLE
                switch_btn.visibility = View.VISIBLE
            } else {
                flash_layout.visibility = View.VISIBLE
                switch_btn.visibility = View.INVISIBLE
            }
        }

        //切换摄像头
        switch_btn.setOnClickListener {
            //要保持闪光灯上一次的模式
            if (cameraXFragment.canSwitchCamera()) {
                cameraXFragment.switchCamera()
            }
        }


        flash_on.setOnClickListener {
            initFlashSelectColor()
            flash_on.setTextColor(resources.getColor(R.color.flash_selected))
            flush_btn.setImageResource(R.drawable.flash_on)
            cameraXFragment.setFlashMode(CAMERA_FLASH_ON)
        }
        flash_off.setOnClickListener {
            initFlashSelectColor()
            flash_off.setTextColor(resources.getColor(R.color.flash_selected))
            flush_btn.setImageResource(R.drawable.flash_off)
            cameraXFragment.setFlashMode(CAMERA_FLASH_OFF)
        }
        flash_auto.setOnClickListener {
            initFlashSelectColor()
            flash_auto.setTextColor(resources.getColor(R.color.flash_selected))
            flush_btn.setImageResource(R.drawable.flash_auto)
            cameraXFragment.setFlashMode(CAMERA_FLASH_AUTO)
        }
        flash_all_on.setOnClickListener {
            initFlashSelectColor()
            flash_all_on.setTextColor(resources.getColor(R.color.flash_selected))
            flush_btn.setImageResource(R.drawable.flash_all_on)
            cameraXFragment.setFlashMode(CAMERA_FLASH_ALL_ON)
        }

        close_btn.setOnClickListener {
            this@CameraXActivity.finish()
        }


        //相机的UI在横竖屏幕可以对应修改UI 啊
        mOrientationListener = object : OrientationEventListener(baseContext) {
            override fun onOrientationChanged(orientation: Int) {
                // Monitors orientation values to determine the target rotation value
                // 这个可以微调
                val rotation: Int = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }

                when (rotation) {
                    Surface.ROTATION_270 -> {

                    }
                    Surface.ROTATION_180 -> {

                    }
                    Surface.ROTATION_90 -> {

                    }
                    Surface.ROTATION_0 -> {

                    }
                }
            }
        }

        //还没有适配分区储存呢，估计短时间内没有办法了
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if(!Environment.isExternalStorageLegacy()){
                Toast.makeText(baseContext,"检测到你的应用以分区存储特性运行，但CameraXFragment库还没有适配分区储存",Toast.LENGTH_LONG).show()
            }
        }

    }

    override fun onResume() {
        super.onResume()
        if (mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable()
        } else {
            mOrientationListener.disable()
        }
    }


    override fun onPause() {
        super.onPause()
        mOrientationListener.disable();
    }


    private fun initFlashSelectColor() {
        flash_on.setTextColor(resources.getColor(R.color.white))
        flash_off.setTextColor(resources.getColor(R.color.white))
        flash_auto.setTextColor(resources.getColor(R.color.white))
        flash_all_on.setTextColor(resources.getColor(R.color.white))

        flash_layout.visibility = View.INVISIBLE
        switch_btn.visibility = View.VISIBLE
    }


    /**
     * 音量减按钮触发拍照，如果需要复制这份代码就可以
     *
     * When key down event is triggered,
     * relay it via local broadcast so fragments can handle it
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                val intent = Intent(KEY_CAMERA_EVENT_ACTION).apply {
                    putExtra(
                        KEY_CAMERA_EVENT_EXTRA,
                        keyCode
                    )
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    /**
     * 根据工信部的要求申请权限前需要向用户说明权限的明确具体用途，请根据业务和法务要求组织语言进行描述
     *
     *
     * @param permissions 需要申请的权限组合，如果为空说明底层CameraX 所需要的权限都申请好了
     * @param requestCode 有权限需要申请值为{@link CameraXFragment} 否则为0
     */
    override fun onBeforePermissionRequest(permissions: Array<String>, requestCode: Int) {
        if (permissions.isEmpty()) return

        //提示可以根据自己的业务自行开展
        PermissionTipsDialog(this@CameraXActivity, permissions,
            object : PermissionTipsDialog.PermissionCallBack {
                override fun onContinue() {
                    cameraXFragment.onRequestPermission(permissions,requestCode)
                }

                override fun onCancel() {

                }
            }
        ).show()

    }

    /**
     * 被拒绝的权限
     *
     */
    override fun onAfterPermissionDeny(permissions: Array<String>, requestCode: Int) {
        val a = 1;
    }

}