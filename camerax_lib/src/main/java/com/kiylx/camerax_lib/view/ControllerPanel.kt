package com.kiylx.camerax_lib.view

import android.content.Context
import android.graphics.Color
import android.view.View
import com.google.android.material.textview.MaterialTextView
import com.kiylx.camerax_lib.R
import com.kiylx.camerax_lib.databinding.BottomControllerPanelBinding
import com.kiylx.camerax_lib.main.buttons.CameraButton
import com.kiylx.camerax_lib.main.buttons.CaptureListener
import com.kiylx.camerax_lib.main.manager.model.UseCaseMode.Companion.takePhoto
import com.kiylx.camerax_lib.view.ControllerPanelUseCaseMode.Companion.recordVideo

interface IControllerPanel {
    var eventListener: IControllerPanelEventListener?
//    var captureListener: CaptureListener?

    fun setBottomButtonTextColor(id: Int)

    /**
     * 显示或隐藏切换功能ui
     */
    fun showHideCameraSwitch(hide: Boolean = true)
    fun showHideUseCaseSwitch(hide: Boolean = true)

    /**
     * 显示隐藏拍照按钮，相册等
     *
     * @param hide
     */
    fun showHideControllerButton(hide: Boolean = true)
    fun initAll()

    /**
     * 显示或隐藏所有ui
     *
     * @param hide
     */
    fun showHideAll(hide: Boolean)
    fun switchBetweenCaptureAndRecord(mode: Int = ControllerPanelUseCaseMode.recordVideo)
}

/**
 * camera bottom controller panel
 *
 * @constructor Create empty Controller panel
 */
class ControllerPanel(
    val context: Context,
    val binding: BottomControllerPanelBinding
) : IControllerPanel {
    //底部切换拍照和录像的文本
    val bottomButtonList: MutableList<MaterialTextView> = mutableListOf()
    override var eventListener: IControllerPanelEventListener? = null
//    override var captureListener: CaptureListener? = null

    override fun initAll() {
        //底部切换拍照和录像的文本
        bottomButtonList.add(binding.btnTakePhoto)
        bottomButtonList.add(binding.btnRecordVideo)
        setBottomButtonTextColor(R.id.btn_take_photo)//default setting

        //点击切换到录像
        binding.btnRecordVideo.setOnClickListener {
            switch2Record()
        }
        //点击切换到拍照
        binding.btnTakePhoto.setOnClickListener {
            switch2TakePhoto()
        }
        //切换摄像头
        binding.switchBtn.setOnClickListener {
            eventListener?.switchCamera()
        }
    }

    private fun switch2Record() {
        binding.fullCaptureBtn.buttonMode = CameraButton.BUTTON_STATE_ONLY_RECORDER
        binding.fullCaptureBtn.visibility = View.VISIBLE
        setBottomButtonTextColor(R.id.btn_record_video)
        eventListener?.switchCaptureBtnType(recordVideo)
    }

    private fun switch2TakePhoto() {
        binding.fullCaptureBtn.buttonMode = CameraButton.BUTTON_STATE_ONLY_CAPTURE
        binding.fullCaptureBtn.visibility = View.VISIBLE
        setBottomButtonTextColor(R.id.btn_take_photo)
        eventListener?.switchCaptureBtnType(takePhoto)
    }

    override fun switchBetweenCaptureAndRecord(mode: Int) {
        if (mode == recordVideo) {
            switch2Record()
        } else {
            switch2TakePhoto()
        }
    }

    /**
     * 设置拍照录像的监听器
     *
     * @param listener
     */
    fun setCaptureListener(listener: CaptureListener?) {
        binding.fullCaptureBtn.setCaptureListener(listener)
    }

    override fun setBottomButtonTextColor(id: Int) {
        bottomButtonList.forEach {
            if (id == it.id) {
                it.setTextColor(context.resources.getColor(R.color.orange1))
            } else {
                it.setTextColor(Color.WHITE)
            }
        }
    }


    override fun showHideCameraSwitch(hide: Boolean) {
        binding.switchBtn.visibility =
            if (hide) View.INVISIBLE else View.VISIBLE
    }

    override fun showHideControllerButton(hide: Boolean) {
        binding.fullCaptureBtn.visibility =
            if (hide) View.INVISIBLE else View.VISIBLE
        binding.photoViewBtn.visibility =
            if (hide) View.INVISIBLE else View.VISIBLE
    }

    override fun showHideUseCaseSwitch(hide: Boolean) {
        binding.switchUsercaseController.visibility =
            if (hide) View.GONE else View.VISIBLE
    }

    override fun showHideAll(hide: Boolean) {
        binding.root.visibility = if (hide) View.GONE else View.VISIBLE
    }

    companion object {

    }
}

class ControllerPanelUseCaseMode{
    companion object{
        const val takePhoto = 0
        const val recordVideo = 1
    }
}

interface IControllerPanelEventListener {
    /**
     * notify camera switch button clicked
     */
    fun switchCamera()

    /**
     * notify capture btn type had changed
     *
     * @param type [ControllerPanel.takePhoto] or [ControllerPanel.recordVideo]
     */
    fun switchCaptureBtnType(type: Int)
}