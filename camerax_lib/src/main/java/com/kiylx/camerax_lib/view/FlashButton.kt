package com.kiylx.camerax_lib.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import com.kiylx.camerax_lib.R
import com.kiylx.camerax_lib.databinding.FlashButtonBinding
import com.kiylx.camerax_lib.main.manager.model.FlashModel

/**
 * 闪光灯按钮
 */
class FlashButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes) {
    private var page: FlashButtonBinding
    var stateInference: IFlashButtonState? = null
    
    init {
        val viewTmp = LayoutInflater.from(context).inflate(R.layout.flash_button, this, true)
        page = FlashButtonBinding.bind(viewTmp)
        initFlashButton()
    }


    //<editor-fold desc="闪光设置">
    private fun initFlashButton() {
        page.flushBtn.setOnClickListener {
            if (page.flashLayout.visibility == View.VISIBLE) {
                page.flashLayout.visibility = View.INVISIBLE
            } else {
                page.flashLayout.visibility = View.VISIBLE
            }
        }
        page.flashOn.setOnClickListener {
            initFlashSelectColor()
            page.flashOn.setTextColor(resources.getColor(R.color.flash_selected))
            page.flushBtn.setImageResource(R.drawable.flash_on)
            stateInference?.stateChanged(FlashModel.CAMERA_FLASH_ON)
        }
        page.flashOff.setOnClickListener {
            initFlashSelectColor()
            page.flashOff.setTextColor(resources.getColor(R.color.flash_selected))
            page.flushBtn.setImageResource(R.drawable.flash_off)
            stateInference?.stateChanged(FlashModel.CAMERA_FLASH_OFF)
        }
        page.flashAuto.setOnClickListener {
            initFlashSelectColor()
            page.flashAuto.setTextColor(resources.getColor(R.color.flash_selected))
            page.flushBtn.setImageResource(R.drawable.flash_auto)
            stateInference?.stateChanged(FlashModel.CAMERA_FLASH_AUTO)
        }
        page.flashAllOn.setOnClickListener {
            initFlashSelectColor()
            page.flashAllOn.setTextColor(resources.getColor(R.color.flash_selected))
            page.flushBtn.setImageResource(R.drawable.flash_all_on)
            stateInference?.stateChanged(FlashModel.CAMERA_FLASH_ALL_ON)
        }

    }

    private fun initFlashSelectColor() {
        page.flashOn.setTextColor(resources.getColor(R.color.white))
        page.flashOff.setTextColor(resources.getColor(R.color.white))
        page.flashAuto.setTextColor(resources.getColor(R.color.white))
        page.flashAllOn.setTextColor(resources.getColor(R.color.white))
        page.flashLayout.visibility = View.INVISIBLE
    }
    //</editor-fold>

}

fun interface IFlashButtonState {
    fun stateChanged(mode: Int)
}