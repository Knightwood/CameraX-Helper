package com.kiylx.camerax_lib.main.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.kiylx.camerax_lib.databinding.FragmentCameraxBinding
import com.kiylx.camerax_lib.main.*
import com.kiylx.camerax_lib.main.manager.CAMERA_CONFIG
import com.kiylx.camerax_lib.main.manager.imagedetection.face.GraphicOverlay
import com.kiylx.camerax_lib.main.manager.CameraHolder
import com.kiylx.camerax_lib.main.manager.KEY_CAMERA_EVENT_ACTION
import com.kiylx.camerax_lib.main.manager.KEY_CAMERA_EVENT_EXTRA
import com.kiylx.camerax_lib.main.manager.imagedetection.base.AnalyzeResultListener
import com.kiylx.camerax_lib.main.manager.model.*

class NewCameraXFragment : Fragment(), CameraCommon {
    private var captureResultListener: CaptureResultListener? = null
    var eventListener: CameraXFragmentEventListener? = null
    lateinit var cameraHolder: CameraHolder
    lateinit var page: FragmentCameraxBinding

    //相机的配置：存储路径，闪光灯模式，
    private lateinit var cameraConfig: ManagerConfig
    private lateinit var broadcastManager: LocalBroadcastManager

    //音量下降按钮接收器用于触发快门
    private val volumeDownReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(KEY_CAMERA_EVENT_EXTRA, KeyEvent.KEYCODE_UNKNOWN)) {
                // When the volume down button is pressed, simulate a shutter button click
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    cameraHolder.takePhoto()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            cameraConfig = it.getParcelable(CAMERA_CONFIG)!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        page = FragmentCameraxBinding.inflate(layoutInflater, container, false)

        cameraHolder = CameraHolder(
            page.cameraPreview,
            page.graphicOverlayFinder,
            cameraConfig,
            page.root,
            this.captureResultListener
        ).apply {
            bindLifecycle(requireActivity())//非常重要，绝对不能漏了绑定生命周期
//使用方式 示例代码：
//            analyzerProvider=object :AnalyzerProvider{
//                override fun provider(verType: VisionType): ImageAnalysis.Analyzer {
//                    TODO("在这里可以提供其他类型的图像识别器")
//                }
//            }
        }
        //使用changeAnalyzer方法改变camerax使用的图像识别器
        // cameraHolder.changeAnalyzer(VisionType.Barcode)
        eventListener?.cameraHolderInited(cameraHolder)//通知外界holder初始化完成了，可以对holder做其他操作了
        return page.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        broadcastManager = LocalBroadcastManager.getInstance(view.context)//用于监测音量键触发

        // 设置意图过滤器，从我们的main activity接收事件
        val filter = IntentFilter().apply { addAction(KEY_CAMERA_EVENT_ACTION) }

        broadcastManager.registerReceiver(volumeDownReceiver, filter)
    }

    fun getOverlay(): GraphicOverlay {
        return page.graphicOverlayFinder
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Unregister the broadcast receivers and listeners
        broadcastManager.unregisterReceiver(volumeDownReceiver)
    }

    companion object {
        /**
         * @param cameraConfig [ManagerConfig]
         * @return A new instance of fragment NewCameraXFragment.
         */
        @JvmStatic
        fun newInstance(cameraConfig: ManagerConfig) =
            NewCameraXFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(CAMERA_CONFIG, cameraConfig)
                }
            }
    }

    /** manager中的事件监听 */
    override fun setCameraEventListener(cameraEventListener: CameraEventListener) {
        cameraHolder.cameraListener = cameraEventListener
    }

    override fun setCaptureResultListener(captureListener: CaptureResultListener) {
        this.captureResultListener = captureListener
    }

    override fun setAnalyzerResultListener(analyzerResultListener: AnalyzeResultListener) {
        cameraHolder.analyzerResultListener = analyzerResultListener
    }

    override fun canSwitchCamera(): Boolean {
        return cameraHolder.canSwitchCamera()
    }

    override fun switchCamera() {
        cameraHolder.switchCamera()
    }

    /** 视频拍摄没有闪光灯这个选项。 所以，在使用视频拍摄时，使用[openFlash]方法取代本方法 */
    override fun setFlashMode(mode: Int) {
        cameraHolder.setFlashMode(mode)
    }

    /**
     * 用于视频拍摄，打开或关闭常亮的闪光灯（手电筒）。
     *
     * @param true:打开手电筒。 false：关闭手电筒
     */
    override fun openFlash(open: Boolean) {
        cameraHolder.setFlashAlwaysOn(open)
    }

    override fun getCurrentStatus(): Int {
        return cameraHolder.currentStatus
    }

    override fun takeVideo() {
        cameraHolder.takeVideo()
    }

    override fun stopTakeVideo(time: Long) {
        cameraHolder.stopTakeVideo()
    }

    override fun takePhoto() {
        cameraHolder.takePhoto()
    }

    override fun getCameraPreview(): PreviewView {
        return cameraHolder.cameraPreview
    }

    override fun provideBitmap(): Bitmap? {
        return cameraHolder.provideBitmap()
    }

    /** 基于当前值缩放 */
    override fun zoom(delta: Float) {
        cameraHolder.zoomBasedOnCurrent(delta)
    }

    /** 直接按照给出的值缩放 */
    override fun zoom2(zoomValue: Float) {
        cameraHolder.zoomDirectly(zoomValue)
    }
}

interface CameraXFragmentEventListener {
    /** cameraXHolder初始化完成，可以对其进行其他操作 */
    fun cameraHolderInited(cameraHolder: CameraHolder)
}