package com.kiylx.camerax_lib.main.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.CallSuper
import androidx.camera.core.ImageAnalysis.Analyzer
import androidx.core.view.updatePadding
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.blankj.utilcode.util.LogUtils
import com.kiylx.camerax_lib.R
import com.kiylx.camerax_lib.databinding.ActivityCameraExampleBinding
import com.kiylx.camerax_lib.databinding.BottomControllerPanelBinding
import com.kiylx.camerax_lib.main.buttons.DefaultCaptureListener
import com.kiylx.camerax_lib.main.manager.CameraHolder
import com.kiylx.camerax_lib.main.manager.KEY_CAMERA_EVENT_ACTION
import com.kiylx.camerax_lib.main.manager.KEY_CAMERA_EVENT_EXTRA
import com.kiylx.camerax_lib.main.manager.analyer.graphic_view.GraphicOverlayView
import com.kiylx.camerax_lib.main.manager.model.CaptureResultListener
import com.kiylx.camerax_lib.main.manager.model.ManagerConfig
import com.kiylx.camerax_lib.main.manager.util.navBarTheme
import com.kiylx.camerax_lib.main.manager.util.setWindowEdgeToEdge
import com.kiylx.camerax_lib.main.manager.util.statusBarTheme
import com.kiylx.camerax_lib.main.store.SaveFileData
import com.kiylx.camerax_lib.view.ControllerPanel
import com.kiylx.camerax_lib.view.FlashButton
import com.kiylx.camerax_lib.view.IControllerPanel
import com.kiylx.camerax_lib.view.IControllerPanelEventListener
import com.kiylx.camerax_lib.view.IFlashButtonState

abstract class BaseCameraXActivity : BasicActivity(),
    CameraXFragmentEventListener, CaptureResultListener {

    internal lateinit var cameraXFragment: CameraXFragment//相机功能实现者

    /** 简化功能调用，复杂功能直接使用cameraHolder或cameraXFragment */
    val cameraXF: CameraXF by lazy { CameraXF(cameraXFragment) }//

    lateinit var cameraConfig: ManagerConfig
    lateinit var mBaseHandler: Handler
    lateinit var controllerPanel: IControllerPanel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(provideView(savedInstanceState))
        mBaseHandler = Handler(Looper.getMainLooper())
        cameraConfig = configAll(intent)
        setCameraFragment()
        edge2edge()
        statusBarTheme(light = false)
        navBarTheme(light = false)
        configFlashButton()
    }

    var controllerPanelEventListener = object : IControllerPanelEventListener {
        override fun switchCamera() {
            //要保持闪光灯上一次的模式
            if (cameraXFragment.canSwitchCamera()) {
                cameraXFragment.switchCamera()
            }
        }

        override fun switchCaptureBtnType(type: Int) {
            //这里可以监听到切换按钮的模式
        }
    }

    open fun configFlashButton() {
        //闪光灯按钮
        findViewById<FlashButton>(R.id.btn_flush_switch).stateInference = IFlashButtonState {
            cameraXF.setFlashMode(it)
        }
    }

    open fun edge2edge() {
        setWindowEdgeToEdge {
            this.findViewById<LinearLayout>(R.id.top_container).updatePadding(top = it.top)
        }
    }

    /**
     * 重写此方法提供自定义布局，提供底部的控制面板交互功能
     *
     * @param savedInstanceState
     * @return
     */
    open fun provideView(
        savedInstanceState: Bundle?
    ): View {
        val page = ActivityCameraExampleBinding.inflate(layoutInflater)
        val controllerPanelBinding = BottomControllerPanelBinding.bind(page.root)//处理merge标签
        //底部控制面板交互功能
        controllerPanel = ControllerPanel(this, controllerPanelBinding)
        controllerPanel.initAll()
        controllerPanel.eventListener = controllerPanelEventListener
        //拍照，拍视频的UI 操作的各种状态处理
        (controllerPanel as ControllerPanel).setCaptureListener(
            object : DefaultCaptureListener() {
                override fun takePictures() {
                    cameraXFragment.takePhoto()
                }

                //开始录制视频
                override fun recordStart() {
                    LogUtils.dTag("录制activity", "开始")
                    cameraXFragment.startRecord()
                    controllerPanel.showHideCameraSwitch(true)
                    //录制视频时隐藏摄像头切换
                    if (!cameraConfig.recordConfig.asPersistentRecording) {
                        controllerPanel.showHideUseCaseSwitch(true)
                    }
                }

                //1. 录制视频到达预定的时长结束
                //2. 或者手动按下按钮录制结束
                override fun recordShouldEnd(time: Long) {
                    LogUtils.dTag("录制activity", "停止")
                    cameraXFragment.stopRecord(time)
                    controllerPanel.showHideCameraSwitch(false)
                    controllerPanel.showHideUseCaseSwitch(false)

                }
            })
        return page.root
    }

    fun setUpAnalyzer(analyzer: Analyzer) {
        cameraXFragment.cameraHolder.changeAnalyzer(analyzer)
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
    override fun cameraHolderInitStart(cameraHolder: CameraHolder) {
    }

    //相机管理器初始化之后
    @CallSuper
    override fun cameraHolderInitFinish(cameraHolder: CameraHolder) {
        //focusView初始化触摸对焦
        cameraXFragment.setupTouchFocus(findViewById(R.id.focus_view))
        if (cameraConfig.isUsingImageAnalyzer()) {//使用了图像分析
            controllerPanel.showHideAll(true)
        }
    }

    @CallSuper
    override fun cameraPreviewStreamStart() {
    }

    @CallSuper
    override fun switchCamera(lensFacing: Int) {
        findViewById<GraphicOverlayView>(R.id.graphicOverlay_finder).toggleSelector(lensFacing)
    }

    @CallSuper
    override fun cameraRotationChanged(rotation: Int, angle: Int) {
        findViewById<GraphicOverlayView>(R.id.graphicOverlay_finder).rotationChanged(
            rotation,
            angle
        )
    }

    //</editor-fold>

    //<editor-fold desc="拍照、录像结果">
    @CallSuper
    override fun onVideoRecorded(saveFileData: SaveFileData?) {

    }

    override fun onPhotoTaken(saveFileData: SaveFileData?) {}
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
     * When key down event is triggered, relay it via local broadcast so
     * fragments can handle it
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

    /** 使用intent初始化ManagerConfig */
    abstract fun configAll(intent: Intent): ManagerConfig

    companion object {
        const val TAG = "BaseRecordVideo"
    }
}