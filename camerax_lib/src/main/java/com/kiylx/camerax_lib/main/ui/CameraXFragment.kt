package com.kiylx.camerax_lib.main.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Range
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExposureState
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.FocusMeteringResult
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.common.util.concurrent.ListenableFuture
import com.kiylx.camerax_lib.databinding.FragmentCameraxBinding
import com.kiylx.camerax_lib.main.manager.CAMERA_CONFIG
import com.kiylx.camerax_lib.main.manager.CameraHolder
import com.kiylx.camerax_lib.main.manager.CameraXManager
import com.kiylx.camerax_lib.main.manager.KEY_CAMERA_EVENT_ACTION
import com.kiylx.camerax_lib.main.manager.KEY_CAMERA_EVENT_EXTRA
import com.kiylx.camerax_lib.main.manager.model.CameraManagerEventListener
import com.kiylx.camerax_lib.main.manager.model.CaptureResultListener
import com.kiylx.camerax_lib.main.manager.model.FlashModel
import com.kiylx.camerax_lib.main.manager.model.ICameraXF
import com.kiylx.camerax_lib.main.manager.model.ManagerConfig
import com.kiylx.camerax_lib.main.store.ImageCaptureConfig
import com.kiylx.camerax_lib.main.store.VideoRecordConfig
import com.kiylx.camerax_lib.utils.ANIMATION_SLOW_MILLIS
import com.kiylx.camerax_lib.view.CameraXPreviewViewTouchListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class CameraXF(private val cameraXF: CameraXFragment) : ICameraXF by cameraXF

/**  content 根布局，跟布局里面要包含预览、对焦、遮盖预览的图像视图等内容 */
class CameraXFragment : Fragment(), CameraManagerEventListener, ICameraXF {
    var eventListener: CameraXFragmentEventListener? = null
    lateinit var cameraHolder: CameraHolder
    lateinit var page: FragmentCameraxBinding
    private var mBitmapFlip: Bitmap? = null//临时存储bitmap

    //相机的配置：存储路径，闪光灯模式，
    private lateinit var cameraConfig: ManagerConfig
    private lateinit var broadcastManager: LocalBroadcastManager
    private var captureResultListener: CaptureResultListener? = null

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
            cameraConfig = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable(CAMERA_CONFIG, ManagerConfig::class.java)!!
            } else {
                it.getParcelable(CAMERA_CONFIG)!!
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        page = FragmentCameraxBinding.inflate(layoutInflater, container, false)
        return page.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCameraHolder()
        broadcastManager = LocalBroadcastManager.getInstance(view.context)//用于监测音量键触发

        // 设置意图过滤器，从我们的main activity接收事件
        val filter = IntentFilter().apply { addAction(KEY_CAMERA_EVENT_ACTION) }

        broadcastManager.registerReceiver(volumeDownReceiver, filter)
    }


    private fun initCameraHolder() {
        cameraHolder = CameraHolder(
            page.cameraPreview,
            cameraConfig,
            cameraManagerListener = this,
        )
        cameraHolder.run {
            eventListener?.cameraHolderInitStart(this)
            //拍照或录像结果监听
            this@CameraXFragment.captureResultListener?.let {
                this.captureResultListener = it
            }
            bindLifecycle(requireActivity())//非常重要，绝对不能漏了绑定生命周期
        }
    }

    //<editor-fold desc="CameraManagerEventListener">
    override fun initCameraStart(cameraXManager: CameraXManager) {
        super.initCameraStart(cameraXManager)
    }

    override fun initCameraFinished(cameraXManager: CameraXManager) {
        initTouchListener()
        eventListener?.cameraHolderInitFinish(cameraHolder)
        //通知holder初始化完成了，可以对holder做其他操作了
    }

    override fun switchCamera(lensFacing: Int) {
        eventListener?.switchCamera(lensFacing)
    }

    override fun cameraRotationChanged(rotation: Int, angle: Int) {
        eventListener?.cameraRotationChanged(rotation, angle)
    }

    override fun previewStreamStart() {
        flipImageViewRecycler()
        eventListener?.cameraPreviewStreamStart()
    }
    //</editor-fold>

    /** 标示拍照触发成功了 */
    override fun indicateTakePhoto(durationTime: Long) {
        if (CameraSelector.LENS_FACING_BACK == cameraHolder.lensFacing) {
            indicateSuccess(durationTime)
        } else {
            if (cameraConfig.flashMode == FlashModel.CAMERA_FLASH_ALL_ON || cameraConfig.flashMode == FlashModel.CAMERA_FLASH_ON) {
                indicateSuccess(durationTime)   //先不要柔白补光了 500
            }
        }
    }

    /** 拍照显示成功的提示 */
    private fun indicateSuccess(durationTime: Long) {
        // 显示一个闪光动画来告知用户照片已经拍好了。在华为等手机上好像有点问题啊 cameraPreview
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            page.root.let { cameraUIContainer ->
                cameraUIContainer.postDelayed({
                    cameraUIContainer.foreground = ColorDrawable(Color.WHITE)
                    cameraUIContainer.postDelayed(
                        { cameraUIContainer.foreground = null }, durationTime
                    )
                }, ANIMATION_SLOW_MILLIS)
            }
        }
    }

    /** 预览数据开始后，去掉假设的画面帧 */
    private fun flipImageViewRecycler() {
        page.lastPreview.let {
            viewLifecycleOwner.lifecycleScope.launch {
                delay(200)
                it.setImageBitmap(null)
                it.visibility = View.GONE
                if (mBitmapFlip != null) {
                    mBitmapFlip?.recycle()
                    mBitmapFlip = null
                }
            }/*handler.postDelayed(Runnable {
                it.setImageBitmap(null)
                it.visibility = View.GONE
                if (mBitmapFlip != null) {
                    mBitmapFlip?.recycle()
                    mBitmapFlip = null
                }
            }, 200)*/
        }

    }

    /** 相机点击等相关操作监听，焦距操作 */
    private fun initTouchListener() {
        cameraHolder.initZoomState()
        val cameraXPreviewViewTouchListener =
            CameraXPreviewViewTouchListener(this.requireContext()).apply {
                this.setCustomTouchListener(object :
                    CameraXPreviewViewTouchListener.CustomTouchListener {
                    // 放大缩小操作
                    override fun zoom(delta: Float) {
                        cameraHolder.zoomState?.value?.let {
                            val currentZoomRatio = it.zoomRatio
                            cameraHolder.camera?.cameraControl!!.setZoomRatio(currentZoomRatio * delta)
                        }
                    }

                    // 点击操作
                    override fun click(x: Float, y: Float) {
                        page.focusView.let { focus ->
                            val factory = page.cameraPreview.meteringPointFactory
                            // 设置对焦位置
                            val point = factory.createPoint(x, y)
                            val action =
                                FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                                    // 3秒内自动调用取消对焦
                                    .setAutoCancelDuration(3, TimeUnit.SECONDS).build()
                            // 执行对焦
                            focus.startFocus(Point(x.toInt(), y.toInt()))
                            val future: ListenableFuture<*> =
                                cameraHolder.camera?.cameraControl!!.startFocusAndMetering(
                                    action
                                )
                            future.addListener(
                                {
                                    try {
                                        // 获取对焦结果
                                        val result = future.get() as FocusMeteringResult
                                        if (result.isFocusSuccessful) {
                                            focus.onFocusSuccess()
                                        } else {
                                            focus.onFocusFailed()
                                        }
                                    } catch (e: java.lang.Exception) {
                                        Log.e(TAG, e.toString())
                                    }
                                },
                                ContextCompat.getMainExecutor(this@CameraXFragment.requireContext())
                            )
                        }
                    }

                    // 双击操作
                    override fun doubleClick(x: Float, y: Float) {
                        cameraHolder.changeZoom()
                    }

                    override fun longPress(x: Float, y: Float) {
                        Log.d(TAG, "长按")
                    }
                })
            }
        // 添加监听事件
        page.cameraPreview.setOnTouchListener(cameraXPreviewViewTouchListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Unregister the broadcast receivers and listeners
        broadcastManager.unregisterReceiver(volumeDownReceiver)
    }

    companion object {
        const val TAG = "NewCameraxFragment"

        /**
         * @param cameraConfig [ManagerConfig]
         * @return A new instance of fragment NewCameraXFragment.
         */
        @JvmStatic
        fun newInstance(
            cameraConfig: ManagerConfig,
            eventListener: CameraXFragmentEventListener,
            captureResultListener: CaptureResultListener
        ) = CameraXFragment().apply {
            this.arguments = Bundle().apply {
                putParcelable(CAMERA_CONFIG, cameraConfig)
            }
            this.eventListener = eventListener
            this.captureResultListener = captureResultListener
        }
    }

    override fun setCaptureResultListener(captureListener: CaptureResultListener) {
        this.captureResultListener = captureListener
        cameraHolder.captureResultListener = captureListener
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
     * @param open: true:打开手电筒。 false:关闭手电筒
     */
    override fun openFlash(open: Boolean) {
        cameraHolder.setFlashAlwaysOn(open)
    }

    override fun getCurrentStatus(): Int {
        return cameraHolder.currentStatus
    }

    override fun startRecord(recordConfig: VideoRecordConfig?) {
        cameraHolder.startRecord(recordConfig)
    }

    override fun reBindUseCase() {
        cameraHolder.reBindUseCase()
    }

    override fun pauseRecord() {
        cameraHolder.pauseRecord()
    }

    override fun resumeRecord() {
        cameraHolder.resumeRecord()
    }

    override fun recordMute(mute: Boolean) {
        cameraHolder.recordMute(mute)
    }

    override fun stopRecord(time: Long) {
        cameraHolder.stopRecord()
    }

    override fun setCameraUseCase(mode: Int) {
        cameraHolder.setCamera(mode)
    }

    override fun takePhoto(imageCaptureConfig: ImageCaptureConfig?) {
        cameraHolder.takePhoto(imageCaptureConfig)
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

    override fun cameraHolder(): CameraHolder = cameraHolder

    /**
     * 可以通过获取[cameraHolder.cameraConfig]，然后重新赋值来调整现有的配置，
     * 也可以调用此方法调整现有配置
     */
    override fun reSetConfig(func: ManagerConfig.() -> Unit) {
        cameraHolder.cameraConfig.func()
        reBindUseCase()
    }

    /**
     * 设置曝光补偿
     */
    fun setExposure(value: Int) = cameraHolder.setExposure(value)

    /**
     * 查询曝光补偿范围
     */
    fun queryExposureRange(): Range<Int> = cameraHolder.queryExposureRange()

    /**
     * 查询当前相机的曝光参数
     */
    fun queryExposureState(): ExposureState? = cameraHolder.queryExposureState()
}

interface CameraXFragmentEventListener {
    /**
     * 开始初始化CameraHolder，此时处于绑定生命周期之前. 触发时机早于[CameraManagerEventListener.initCameraStart]
     */
    fun cameraHolderInitStart(cameraHolder: CameraHolder)

    /**
     * cameraXHolder初始化完成 触发时机晚于[CameraManagerEventListener.initCameraFinished]
     */
    fun cameraHolderInitFinish(cameraHolder: CameraHolder)

    /**
     * 相机预览数据开始
     */
    fun cameraPreviewStreamStart() {}

    /**
     * 切换前置或后置摄像头
     */
    fun switchCamera(lensFacing: Int) {}

    /**
     * 设备旋转，对坐标做转换
     */
    fun cameraRotationChanged(rotation: Int, angle: Int) {}
}
