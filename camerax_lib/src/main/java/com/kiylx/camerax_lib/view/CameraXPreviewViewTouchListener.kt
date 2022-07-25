package com.kiylx.camerax_lib.view

import android.content.Context
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import android.view.View.OnTouchListener

/**
 * 自定义CameraX点击事件
 */
class CameraXPreviewViewTouchListener(context: Context?) : OnTouchListener {
    private val mGestureDetector: GestureDetector
    private var mCustomTouchListener: CustomTouchListener? = null
    private val mScaleGestureDetector: ScaleGestureDetector

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        mScaleGestureDetector.onTouchEvent(event)
        if (!mScaleGestureDetector.isInProgress) {
            mGestureDetector.onTouchEvent(event)
        }
        return true
    }

    // 设置监听
    fun setCustomTouchListener(customTouchListener: CustomTouchListener?) {
        mCustomTouchListener = customTouchListener
    }

    // 缩放监听
    private var onScaleGestureListener: OnScaleGestureListener = object : SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val delta = detector.scaleFactor
            if (mCustomTouchListener != null) {
                mCustomTouchListener!!.zoom(delta)
            }
            return true
        }
    }

    // 点击监听
    var onGestureListener: SimpleOnGestureListener = object : SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            if (mCustomTouchListener != null) {
                // 长按
                mCustomTouchListener!!.longPress(e.x, e.y)
            }
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (mCustomTouchListener != null) {
                // 单击
                mCustomTouchListener!!.click(e.x, e.y)
            }
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (mCustomTouchListener != null) {
                // 双击
                mCustomTouchListener!!.doubleClick(e.x, e.y)
            }
            return true
        }
    }

    // 操作接口
    interface CustomTouchListener {
        // 放大缩小
        fun zoom(delta: Float)

        // 点击
        fun click(x: Float, y: Float)

        // 双击
        fun doubleClick(x: Float, y: Float)

        // 长按
        fun longPress(x: Float, y: Float)
    }

    init {
        mGestureDetector = GestureDetector(context, onGestureListener)
        mScaleGestureDetector = ScaleGestureDetector(context, onScaleGestureListener)
    }
}