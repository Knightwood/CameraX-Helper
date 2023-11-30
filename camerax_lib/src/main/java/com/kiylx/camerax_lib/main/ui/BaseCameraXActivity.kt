package com.kiylx.camerax_lib.main.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import androidx.annotation.CallSuper
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.blankj.utilcode.util.LogUtils
import com.kiylx.camerax_lib.R
import com.kiylx.camerax_lib.databinding.ActivityCameraExampleBinding
import com.kiylx.camerax_lib.main.buttons.DefaultCaptureListener
import com.kiylx.camerax_lib.main.manager.CameraHolder
import com.kiylx.camerax_lib.main.manager.KEY_CAMERA_EVENT_ACTION
import com.kiylx.camerax_lib.main.manager.KEY_CAMERA_EVENT_EXTRA
import com.kiylx.camerax_lib.main.manager.model.*
import com.kiylx.camerax_lib.main.manager.ui.setWindowEdgeToEdge
import com.kiylx.camerax_lib.main.store.SaveFileData

abstract class BaseCameraXActivity : BasicActivity(),
   CameraXFragmentEventListener, CaptureResultListener {

    internal lateinit var cameraXFragment: CameraXFragment//相机功能实现者

    /**
     * 简化功能调用，复杂功能直接使用cameraHolder或cameraXFragment
     */
    val cameraXF: CameraXF by lazy { CameraXF(cameraXFragment) }//

    lateinit var cameraConfig: ManagerConfig
    lateinit var mBaseHandler: Handler
    lateinit var page: ActivityCameraExampleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        page = ActivityCameraExampleBinding.inflate(layoutInflater)
        setWindowEdgeToEdge(page.root, page.settingLayout.id, page.cameraControlLayout.id)
        setContentView(page.root)
        mBaseHandler = Handler(Looper.getMainLooper())
        cameraConfig = configAll(intent)
        setCameraFragment()
        //切换摄像头
        page.switchBtn.setOnClickListener {
            //要保持闪光灯上一次的模式
            if (cameraXFragment.canSwitchCamera()) {
                cameraXFragment.switchCamera()
            }
        }

        initFlashButton()
        page.closeBtn.setOnClickListener {
            closeActivity()
        }
    }


    private fun setCameraFragment() {
        cameraXFragment = CameraXFragment.newInstance(
            cameraConfig,
            //设置初始化事件监听
            eventListener = this,
            //拍照录视频操作结果通知回调
            captureResultListener = this
        )
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, cameraXFragment).commit()
    }


    //<editor-fold desc="相机初始化">

    //相机管理器初始化之前
    @CallSuper
    override fun cameraHolderInitStart(cameraHolder: CameraHolder) {}

    //相机管理器初始化之后
    @CallSuper
    override fun cameraHolderInitFinish(cameraHolder: CameraHolder) {
        //拍照，拍视频的UI 操作的各种状态处理
        page.fullCaptureBtn.setCaptureListener(object : DefaultCaptureListener() {
            override fun takePictures() {
                cameraXFragment.takePhoto()
            }

            //开始录制视频
            override fun recordStart() {
                page.captureVideoBtn.visibility = View.GONE
                LogUtils.dTag("录制activity", "开始")
                cameraXFragment.startRecord()
                //录制视频时隐藏摄像头切换
                page.switchBtn.visibility = View.GONE
            }

            //录制视频到达预定的时长，可以结束了
            override fun recordShouldEnd(time: Long) {
                page.captureVideoBtn.visibility = View.VISIBLE
                LogUtils.dTag("录制activity", "停止")
                cameraXFragment.stopRecord(time)
                page.switchBtn.visibility = View.VISIBLE
            }
        })

        page.captureVideoBtn.setCaptureListener(object : DefaultCaptureListener() {
            //开始录制视频
            override fun recordStart() {
                page.fullCaptureBtn.visibility = View.GONE
                LogUtils.dTag("录制activity", "开始")
                cameraXFragment.startRecord()
                //录制视频时隐藏摄像头切换
                if (!cameraConfig.recordConfig.asPersistentRecording) {
                    page.switchBtn.visibility = View.GONE
                }
            }

            //录制视频到达预定的时长，可以结束了
            override fun recordShouldEnd(time: Long) {
                page.fullCaptureBtn.visibility = View.VISIBLE
                LogUtils.dTag("录制activity", "停止")
                cameraXFragment.stopRecord(time)
                page.switchBtn.visibility = View.VISIBLE
            }

            //例如长按拍视频的时候，在屏幕滑动可以调整焦距缩放
            override fun recordZoom(zoom: Float) {
                val a = zoom
            }

        })

    }

    @CallSuper
    override fun cameraPreviewStreamStart() {}

    @CallSuper
    override fun switchCamera(lensFacing: Int) {
        page.graphicOverlayFinder.toggleSelector(lensFacing)
    }

    @CallSuper
    override fun cameraRotationChanged(rotation: Int, angle: Int) {
        page.graphicOverlayFinder.rotationChanged(rotation, angle)
    }

    //</editor-fold>

    //<editor-fold desc="拍照、录像结果">

    override fun onVideoRecorded(saveFileData: SaveFileData?) {}

    override fun onPhotoTaken(saveFileData: SaveFileData?) {}
    //</editor-fold>

   //<editor-fold desc="闪光设置">
   private fun initFlashButton() {
       page.flushBtn.setOnClickListener {
           if (page.flashLayout.visibility == View.VISIBLE) {
               page.flashLayout.visibility = View.INVISIBLE
               page.switchBtn.visibility = View.VISIBLE
           } else {
               page.flashLayout.visibility = View.VISIBLE
               page.switchBtn.visibility = View.INVISIBLE
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

   }
    private fun initFlashSelectColor() {
        page.flashOn.setTextColor(resources.getColor(R.color.white))
        page.flashOff.setTextColor(resources.getColor(R.color.white))
        page.flashAuto.setTextColor(resources.getColor(R.color.white))
        page.flashAllOn.setTextColor(resources.getColor(R.color.white))

        page.flashLayout.visibility = View.INVISIBLE
        page.switchBtn.visibility = View.VISIBLE
    }
   //</editor-fold>

    override fun onStop() {
        super.onStop()
        mBaseHandler.removeCallbacksAndMessages(null)
    }

    open fun closeActivity(shouldInvokeFinish: Boolean = true) {
        cameraXFragment.stopRecord(0)
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
     * 使用intent初始化ManagerConfig
     */
    abstract fun configAll(intent: Intent): ManagerConfig

    companion object {
        const val TAG = "BaseRecordVideo"
    }
}