package com.kiylx.camerax_lib.main.ui

import android.content.Intent
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.KeyEvent
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.blankj.utilcode.util.LogUtils
import com.kiylx.camerax_lib.R
import com.kiylx.camerax_lib.databinding.ActivityCameraExampleBinding
import com.kiylx.camerax_lib.main.*
import com.kiylx.camerax_lib.main.buttons.CaptureListener
import com.kiylx.camerax_lib.main.manager.CameraHolder
import com.kiylx.camerax_lib.main.manager.KEY_CAMERA_EVENT_ACTION
import com.kiylx.camerax_lib.main.manager.KEY_CAMERA_EVENT_EXTRA
import com.kiylx.camerax_lib.main.manager.imagedetection.base.AnalyzeResultListener
import com.kiylx.camerax_lib.main.manager.model.CameraEventListener
import com.kiylx.camerax_lib.main.manager.model.CaptureResultListener
import com.kiylx.camerax_lib.main.manager.model.FlashModel
import com.kiylx.camerax_lib.main.manager.model.ManagerConfig
import com.kiylx.camerax_lib.main.manager.ui.setWindowEdgeToEdge
import com.kiylx.camerax_lib.main.store.FileMetaData

abstract class BaseCameraXActivity : BasicActivity(),
    View.OnClickListener {
    lateinit var cameraXFragment: NewCameraXFragment
    lateinit var mOrientationListener: OrientationEventListener
    lateinit var cameraConfig: ManagerConfig

    lateinit var mBaseHandler: Handler
    lateinit var page: ActivityCameraExampleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        page = ActivityCameraExampleBinding.inflate(layoutInflater)
        setWindowEdgeToEdge(page.root,page.settingLayout.id,page.cameraControlLayout.id)
        setContentView(page.root)
        mBaseHandler = Handler(Looper.getMainLooper())
        cameraConfig = configAll(intent)
        setCameraFragment()
        page.flushBtn.setOnClickListener {
            if (page.flashLayout.visibility == View.VISIBLE) {
                page.flashLayout.visibility = View.INVISIBLE
                page.switchBtn.visibility = View.VISIBLE
            } else {
                page.flashLayout.visibility = View.VISIBLE
                page.switchBtn.visibility = View.INVISIBLE
            }
        }
        //切换摄像头
        page.switchBtn.setOnClickListener {
            //要保持闪光灯上一次的模式
            if (cameraXFragment.canSwitchCamera()) {
                cameraXFragment.switchCamera()
            }
        }
        page.flashOn.setOnClickListener {
            initFlashSelectColor()
            page.flashOn.setTextColor(resources.getColor(R.color.flash_selected))
            page.flushBtn.setImageResource(R.drawable.flash_on)
            cameraXFragment.setFlashMode(FlashModel.CAMERA_FLASH_ON)
        }
        page.flashOff.setOnClickListener {
            initFlashSelectColor()
            page.flashOff.setTextColor(resources.getColor(R.color.flash_selected))
            page.flushBtn.setImageResource(R.drawable.flash_off)
            cameraXFragment.setFlashMode(FlashModel.CAMERA_FLASH_OFF)
        }
        page.flashAuto.setOnClickListener {
            initFlashSelectColor()
            page.flashAuto.setTextColor(resources.getColor(R.color.flash_selected))
            page.flushBtn.setImageResource(R.drawable.flash_auto)
            cameraXFragment.setFlashMode(FlashModel.CAMERA_FLASH_AUTO)
        }
        page.flashAllOn.setOnClickListener {
            initFlashSelectColor()
            page.flashAllOn.setTextColor(resources.getColor(R.color.flash_selected))
            page.flushBtn.setImageResource(R.drawable.flash_all_on)
            cameraXFragment.setFlashMode(FlashModel.CAMERA_FLASH_ALL_ON)
        }

        page.closeBtn.setOnClickListener {
            closeActivity()
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
    }

    private fun setCameraFragment() {
        cameraXFragment = NewCameraXFragment.newInstance(cameraConfig)
            .apply {
                eventListener = object : CameraXFragmentEventListener {
                    override fun cameraHolderInited(cameraHolder: CameraHolder) {//holder初始化完成
                        setCameraEventListener(object : CameraEventListener {
                            //holder初始化完成后，相机也初始化完成了
                            override fun initCameraFinished() {
                                this@BaseCameraXActivity.initCameraFinished()//初始化其他内容
                            }
                        })
                        setAnalyzerResultListener(object : AnalyzeResultListener {
                            //图像分析成功时
                            override fun isSuccess() {
                                //captureFace()
                                /*Toast.makeText(applicationContext, "图像分析完成", Toast.LENGTH_SHORT)
                                    .show()*/
                            }
                        })
                        //拍照录视频操作结果通知回调
                        setCaptureResultListener(object : CaptureResultListener {
                            override fun onVideoRecorded(fileMetaData: FileMetaData?) {
                                // 视频拍摄后

                            }

                            override fun onPhotoTaken(filePath: Uri?) {
                                Log.d("CameraXFragment", "onPhotoTaken： $filePath")
                                //图片拍摄后
                            }
                        })
                    }

                }
            }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, cameraXFragment).commit()
    }

    /**
     * 使用intent初始化ManagerConfig
     */
    abstract fun configAll(intent: Intent): ManagerConfig

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
        page.flashOn.setTextColor(resources.getColor(R.color.white))
        page.flashOff.setTextColor(resources.getColor(R.color.white))
        page.flashAuto.setTextColor(resources.getColor(R.color.white))
        page.flashAllOn.setTextColor(resources.getColor(R.color.white))

        page.flashLayout.visibility = View.INVISIBLE
        page.switchBtn.visibility = View.VISIBLE
    }

    override fun onStop() {
        super.onStop()
        mBaseHandler.removeCallbacksAndMessages(null)
    }


    open fun closeActivity(shouldInvokeFinish: Boolean = true) {
        cameraXFragment.stopTakeVideo(0)
        if (shouldInvokeFinish) {
            mBaseHandler.postDelayed(Runnable {
                this.finish()
            }, 200)
        }
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
//    override fun onBeforePermissionRequest(permissions: Array<String>, requestCode: Int) {
//        if (permissions.isEmpty()) return
//
//        //提示可以根据自己的业务自行开展
//        PermissionTipsDialog(this@BaseRecordVideoActivity, permissions,
//            object : PermissionTipsDialog.PermissionCallBack {
//                override fun onContinue() {
//                    //cameraXFragment.onRequestPermission(permissions, requestCode)
//                }
//
//                override fun onCancel() {
//
//                }
//            }
//        ).show()
//
//    }


    //相机初始化完成
    open fun initCameraFinished() {
        //拍照，拍视频的UI 操作的各种状态处理
        page.captureBtn2.setCaptureListener(object : CaptureListener {
            override fun takePictures() {
                cameraXFragment.takePhoto()
            }

            //开始录制视频
            override fun recordStart() {

            }

            //录制视频结束
            override fun recordEnd(time: Long) {

            }

            //长按拍视频的时候，在屏幕滑动可以调整焦距缩放
            override fun recordZoom(zoom: Float) {
                val a = zoom
            }

            //录制视频错误（拍照也会有错误，先不处理了吧）
            override fun recordError(message: String) {

            }
        })
        page.captureVideoBtn.setCaptureListener(object : CaptureListener {
            override fun takePictures() {

            }

            //开始录制视频
            override fun recordStart() {
                page.captureBtn2.visibility = View.GONE
                LogUtils.dTag("录制activity", "开始")
                cameraXFragment.takeVideo()
            }

            //录制视频结束
            override fun recordEnd(time: Long) {
                page.captureBtn2.visibility = View.VISIBLE
                LogUtils.dTag("录制activity", "停止")
                cameraXFragment.stopTakeVideo(time)
            }

            //长按拍视频的时候，在屏幕滑动可以调整焦距缩放
            override fun recordZoom(zoom: Float) {
                val a = zoom
            }

            //录制视频错误（拍照也会有错误，先不处理了吧）
            override fun recordError(message: String) {
                LogUtils.dTag(tag, message)
            }
        })
        cameraFinishInited()
    }

    /**
     * 初始化完成，做其他操作
     */
    open fun cameraFinishInited() {}
    abstract fun captureFace()


    companion object {
        const val tag = "BaseRecordVideo"
    }
}