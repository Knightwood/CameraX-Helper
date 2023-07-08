package com.kiylx.camerax_lib.main.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.PreviewView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.blankj.utilcode.util.LogUtils
import com.kiylx.camerax_lib.R
import com.kiylx.camerax_lib.databinding.ActivityCameraExampleBinding
import com.kiylx.camerax_lib.main.buttons.DefaultCaptureListener
import com.kiylx.camerax_lib.main.manager.CameraHolder
import com.kiylx.camerax_lib.main.manager.KEY_CAMERA_EVENT_ACTION
import com.kiylx.camerax_lib.main.manager.KEY_CAMERA_EVENT_EXTRA
import com.kiylx.camerax_lib.main.manager.imagedetection.base.AnalyzeUtils
import com.kiylx.camerax_lib.main.manager.imagedetection.face.FaceContourDetectionProcessor
import com.kiylx.camerax_lib.main.manager.imagedetection.face.GraphicOverlay
import com.kiylx.camerax_lib.main.manager.model.*
import com.kiylx.camerax_lib.main.manager.ui.setWindowEdgeToEdge
import com.kiylx.camerax_lib.main.store.FileMetaData

abstract class BaseCameraXActivity : BasicActivity(),
    View.OnClickListener {
    lateinit var cameraXFragment: NewCameraXFragment
    lateinit var cameraConfig: ManagerConfig

    lateinit var mBaseHandler: Handler
    lateinit var page: ActivityCameraExampleBinding

    fun getOverlay(): GraphicOverlay {
        return page.graphicOverlayFinder
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        page = ActivityCameraExampleBinding.inflate(layoutInflater)
        setWindowEdgeToEdge(page.root, page.settingLayout.id, page.cameraControlLayout.id)
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
    }

    private fun setCameraFragment() {
        cameraXFragment = NewCameraXFragment.newInstance(cameraConfig)
            .apply {
                //指定不同类型图像分析器，默认只有面部分析器
                eventListener = object : CameraXFragmentEventListener {
                    override fun cameraHolderInitStart(cameraHolder: CameraHolder) {
                        this@BaseCameraXActivity.initCameraStart(
                            cameraHolder,
                            page.cameraPreview
                        )//初始化其他内容
                    }

                    override fun cameraHolderInited(cameraHolder: CameraHolder) {//holder初始化完成
                        setCameraEventListener(object : CameraEventListener {
                            //holder初始化完成后，相机也初始化完成了
                            override fun initCameraFinished() {
                                this@BaseCameraXActivity.initCameraFinished(
                                    cameraHolder,
                                    page.cameraPreview
                                )//初始化其他内容
                            }
                        })
                        //拍照录视频操作结果通知回调
                        setCaptureResultListener(object : CaptureResultListener {
                            override fun onVideoRecorded(fileMetaData: FileMetaData?) {
                                // 视频拍摄后
                                this@BaseCameraXActivity.videoRecordEnd(fileMetaData)
                            }

                            override fun onPhotoTaken(filePath: Uri?) {
                                Log.d("CameraXFragment", "onPhotoTaken： $filePath")
                                //图片拍摄后
                                this@BaseCameraXActivity.photoTakeEnd(filePath)
                            }
                        })
                    }

                }
            }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, cameraXFragment).commit()
    }

    open fun initCameraStart(cameraHolder: CameraHolder, cameraPreview: PreviewView) {
        //指定接收屏幕反转信息的接口实现，例如这里是一个view实现的接口
        cameraHolder.graphicOverlay=page.graphicOverlayFinder
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


    //相机初始化完成
    open fun initCameraFinished(cameraHolder: CameraHolder, cameraPreview: PreviewView) {
        //拍照，拍视频的UI 操作的各种状态处理
        page.fullCaptureBtn.setCaptureListener(object : DefaultCaptureListener() {
            override fun takePictures() {
                cameraXFragment.takePhoto()
            }

            //开始录制视频
            override fun recordStart() {
                page.captureVideoBtn.visibility = View.GONE
                LogUtils.dTag("录制activity", "开始")
                cameraXFragment.takeVideo()
                //录制视频时隐藏摄像头切换
                page.switchBtn.visibility = View.GONE
            }

            //录制视频到达预定的时长，可以结束了
            override fun recordShouldEnd(time: Long) {
                page.captureVideoBtn.visibility = View.VISIBLE
                LogUtils.dTag("录制activity", "停止")
                cameraXFragment.stopTakeVideo(time)
                page.switchBtn.visibility = View.VISIBLE
            }
        })
        page.captureVideoBtn.setCaptureListener(object : DefaultCaptureListener() {
            //开始录制视频
            override fun recordStart() {
                page.fullCaptureBtn.visibility = View.GONE
                LogUtils.dTag("录制activity", "开始")
                cameraXFragment.takeVideo()
                //录制视频时隐藏摄像头切换
                page.switchBtn.visibility = View.GONE
            }

            //录制视频到达预定的时长，可以结束了
            override fun recordShouldEnd(time: Long) {
                page.fullCaptureBtn.visibility = View.VISIBLE
                LogUtils.dTag("录制activity", "停止")
                cameraXFragment.stopTakeVideo(time)
                page.switchBtn.visibility = View.VISIBLE
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

    }

    abstract fun captureFace()

    /**
     * 使用intent初始化ManagerConfig
     */
    abstract fun configAll(intent: Intent): ManagerConfig
    open fun photoTakeEnd(filePath: Uri?) {}
    open fun videoRecordEnd(fileMetaData: FileMetaData?) {}


    companion object {
        const val tag = "BaseRecordVideo"
    }
}