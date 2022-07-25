package com.yeyupiaoling.cameraxapp.view

import android.content.Context
import android.graphics.Point
import android.os.Handler
import android.util.AttributeSet
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import com.kiylx.camerax_lib.R

/**
 * 对焦动图显示
 */
class FocusImageView : AppCompatImageView {
    private var mFocusImg = NO_ID
    private var mFocusSucceedImg = NO_ID
    private var mFocusFailedImg = NO_ID
    private val mAnimation: Animation
    private val mHandler: Handler

    constructor(context: Context?) : super(context!!) {
        mAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.focusview_show)
        visibility = GONE
        mHandler = Handler()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        mAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.focusview_show)
        mHandler = Handler()
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.FocusImageView)
        mFocusImg = typedArray.getResourceId(R.styleable.FocusImageView_focus_focusing_id, NO_ID)
        mFocusSucceedImg =
            typedArray.getResourceId(R.styleable.FocusImageView_focus_success_id, NO_ID)
        mFocusFailedImg = typedArray.getResourceId(R.styleable.FocusImageView_focus_fail_id, NO_ID)
        typedArray.recycle()

        //聚焦图片不能为空
        if (mFocusImg == NO_ID || mFocusSucceedImg == NO_ID || mFocusFailedImg == NO_ID) {
            throw RuntimeException("mFocusImg,mFocusSucceedImg,mFocusFailedImg is null")
        }
    }

    /**
     * 显示对焦图案
     */
    fun startFocus(point: Point) {
        if (mFocusImg == NO_ID || mFocusSucceedImg == NO_ID || mFocusFailedImg == NO_ID) {
            throw RuntimeException("focus image is null")
        }
        //根据触摸的坐标设置聚焦图案的位置
        val params = layoutParams as FrameLayout.LayoutParams
        params.topMargin = point.y - measuredHeight / 2
        params.leftMargin = point.x - measuredWidth / 2
        layoutParams = params
        //设置控件可见，并开始动画
        visibility = VISIBLE
        setImageResource(mFocusImg)
        startAnimation(mAnimation)
    }

    /**
     * 聚焦成功回调
     */
    fun onFocusSuccess() {
        setImageResource(mFocusSucceedImg)
        //移除在startFocus中设置的callback，1秒后隐藏该控件
        mHandler.removeCallbacks({ }, null)
        mHandler.postDelayed({ visibility = GONE }, 1000)
    }

    /**
     * 聚焦失败回调
     */
    fun onFocusFailed() {
        setImageResource(mFocusFailedImg)
        //移除在startFocus中设置的callback，1秒后隐藏该控件
        mHandler.removeCallbacks({ }, null)
        mHandler.postDelayed({ visibility = GONE }, 1000)
    }

    /**
     * 设置开始聚焦时的图片
     *
     * @param focus
     */
    fun setFocusImg(focus: Int) {
        mFocusImg = focus
    }

    /**
     * 设置聚焦成功显示的图片
     *
     * @param focusSucceed
     */
    fun setFocusSucceedImg(focusSucceed: Int) {
        mFocusSucceedImg = focusSucceed
    }

    companion object {
        private const val NO_ID = -1
    }
}