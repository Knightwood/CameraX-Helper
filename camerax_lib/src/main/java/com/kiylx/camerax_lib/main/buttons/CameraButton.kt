package com.kiylx.camerax_lib.main.buttons

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.kiylx.camerax_lib.R

class CameraButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var longPassRecord = false//是否使用长按录制
    private var state = STATE_IDLE //当前按钮触摸事件
    private var buttonEvent = EVENT_IDLE//录制或拍照等事件
    var buttonMode = BUTTON_STATE_ONLY_CAPTURE // 当前按钮支持的模式

    private val progress_color = -0x11e951ea //进度条颜色
    private val outside_color = -0x11232324 //外圆背景色
    private val inside_color = -0x1 //内圆背景色
    private val inside_record_color = -0xe8bc //内圆录制时背景色


    private var event_Y = 0f//Touch_Event_Down时候记录的Y值
    private val mPaint: Paint by lazy {
        Paint()
    }
    private var strokeWidth = 0f//进度条宽度
    private var outside_add_size = 0//长按外圆半径变大的Size
    private var inside_reduce_size = 0//长安内圆缩小的Size

    //中心坐标
    private var center_X = 0f
    private var center_Y = 0f

    private var button_radius = 0f//按钮半径
    private var button_outside_radius = 0f//外圆半径
    private var button_inside_radius = 0f//内圆半径
    private var button_size = 80 //按钮大小
    private lateinit var rectF: RectF

    private var progress = 0f//录制视频的进度
    private var duration: Long = 60 * 1000 //录制视频最大时间长度,毫秒
    private var recorded_time = 0//记录当前录制的时间

    private var longPressRunnable
            : LongPressRunnable = LongPressRunnable()//长按后处理的逻辑Runnable
    private var captureListener
            : CaptureListener? = null//按钮回调接口
    private lateinit var timer
            : RecordCountDownTimer //计时器

    init {
        initView(context, attrs)
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    private fun dip2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    /**
     * 总要支持一下XML 中布局吧
     *
     * @param context CONTEXT
     * @param attrs   ATTRS
     */
    fun initView(context: Context, attrs: AttributeSet?) {
        val arr = getContext().obtainStyledAttributes(attrs, R.styleable.CameraButton)
        button_size =
            dip2px(context, arr.getInteger(R.styleable.CameraButton_size, button_size).toFloat())
        val tmp = arr.getInteger(R.styleable.CameraButton_maxDuration, 60)
        if (tmp > 1) {
            duration = tmp * 1000L
        }

        buttonMode = arr.getInteger(R.styleable.CameraButton_buttonMode, BUTTON_STATE_ONLY_CAPTURE)
        longPassRecord=arr.getBoolean(R.styleable.CameraButton_longPassRecord,false)

        button_radius = button_size / 2.0f
        button_outside_radius = button_radius
        button_inside_radius = button_radius * 0.85f
        strokeWidth = (button_size / 15).toFloat()
        outside_add_size = button_size / 8
        inside_reduce_size = button_size / 8
        mPaint.isAntiAlias = true

        center_X = ((button_size + outside_add_size * 2) / 2).toFloat()
        center_Y = ((button_size + outside_add_size * 2) / 2).toFloat()
        rectF = RectF(
            center_X - (button_radius + outside_add_size - strokeWidth / 2),
            center_Y - (button_radius + outside_add_size - strokeWidth / 2),
            center_X + (button_radius + outside_add_size - strokeWidth / 2),
            center_Y + (button_radius + outside_add_size - strokeWidth / 2)
        )
        timer = RecordCountDownTimer(duration, duration / 360) //录制定时器
        arr.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(button_size + outside_add_size * 2, button_size + outside_add_size * 2)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mPaint.style = Paint.Style.FILL
        mPaint.color = outside_color //外圆（半透明灰色）
        canvas.drawCircle(center_X, center_Y, button_outside_radius, mPaint)
        mPaint.color = inside_color //内圆（白色）
        canvas.drawCircle(center_X, center_Y, button_inside_radius, mPaint)

        //如果状态为录制状态，则绘制录制进度条
        if (buttonEvent == EVENT_RECORDERING) {
            if (longPassRecord) {
                mPaint.color = progress_color
                mPaint.style = Paint.Style.STROKE
                mPaint.strokeWidth = strokeWidth
                canvas.drawArc(rectF, -90f, progress, false, mPaint)
            } else {
                mPaint.setColor(inside_record_color);  //内圆（红色）
                canvas.drawCircle(center_X, center_Y, button_inside_radius, mPaint);
            }
        }

    }

    /**
     * 点按录制和长按录制，点按拍照。
     * 当手指按下的时候，就启动一个延迟500毫秒的逻辑，
     *  1.当500毫秒后没收松开手指，就会开始录制，此时即为长按事件，松开手指时停止录制
     *  2.当500毫秒内松开手指，就会把这个延迟逻辑取消掉，并认为是个点击事件而不是长按事件
     * 还有根据模式进行判断：
     *  1.如果是点击拍照，full模式，则点击为拍照，长按为录制
     *  2.如果是点击录制，则点击为录制
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (event.pointerCount > 1 || buttonEvent == EVENT_CAPTURE) {
                    return false
                }
                event_Y = event.y //记录Y值
                state = STATE_PRESS //修改当前状态为点击按下

                //判断按钮状态是否为可录制状态
                if ((buttonMode == BUTTON_STATE_ONLY_RECORDER || buttonMode == BUTTON_STATE_BOTH) && longPassRecord) {
                    postDelayed(
                        longPressRunnable,
                        500
                    ) //同时延长500启动长按后处理的逻辑Runnable
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (captureListener != null && buttonEvent == EVENT_RECORDERING
                    && (buttonMode == BUTTON_STATE_ONLY_RECORDER || buttonMode == BUTTON_STATE_BOTH)
                ) {
                    //记录当前Y值与按下时候Y值的差值，调用缩放回调接口
                    captureListener!!.recordZoom(event_Y - event.y)
                }
            }
            MotionEvent.ACTION_UP -> {
                //根据当前按钮的状态进行相应的处理
                handlerPressByState()
                state = STATE_IDLE
            }
        }
        return true
    }

    //当手指松开按钮时候处理的逻辑
    private fun handlerPressByState() {
        removeCallbacks(longPressRunnable) //移除长按逻辑的Runnable
        if (state == STATE_PRESS) {
            if (captureListener != null) {
                when (buttonMode) {
                    //点击录视频和停止录制逻辑
                    BUTTON_STATE_ONLY_RECORDER -> {
                        if (buttonEvent == EVENT_RECORDERING) {
                            timer.cancel() //停止计时器
                            recordEnd()    //录制结束
                        } else {
                            postDelayed(longPressRunnable, 500);    //同时延长500启动长按后处理的逻辑Runnable
                        }
                    }
                    //拍照
                    BUTTON_STATE_ONLY_CAPTURE ,BUTTON_STATE_BOTH -> {
                        startCaptureAnimation(button_inside_radius)
                    }
                }
            }
        } else if (state== STATE_LONG_PRESS){
            if (buttonEvent == EVENT_RECORDERING) {
                timer.cancel() //停止计时器
                recordEnd()    //录制结束
            }
        }
    }

    //录制结束
    fun recordEnd() {
        if (captureListener != null) {
            captureListener!!.recordShouldEnd(recorded_time.toLong()) //回调录制结束
        }
        resetRecordAnim() //重制按钮状态
    }

    //重制状态
    private fun resetRecordAnim() {
        state = STATE_IDLE
        buttonEvent = EVENT_IDLE
        progress = 0f //重制进度
        invalidate()
        //还原按钮初始状态动画
        startRecordAnimation(
            button_outside_radius,
            button_radius,
            button_inside_radius,
            button_radius * 0.75f
        )
    }

    //内圆动画
    private fun startCaptureAnimation(inside_start: Float) {
        val inside_anim = ValueAnimator.ofFloat(inside_start, inside_start * 0.75f, inside_start)
        inside_anim.addUpdateListener { animation: ValueAnimator ->
            button_inside_radius = animation.animatedValue as Float
            invalidate()
        }
        inside_anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                buttonEvent = EVENT_IDLE
            }

            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
                if (captureListener != null) {
                    captureListener!!.takePictures()
                }
                state = STATE_IDLE
                buttonEvent = EVENT_CAPTURE
            }
        })
        inside_anim.duration = 50
        inside_anim.start()
    }

    //内外圆动画
    private fun startRecordAnimation(
        outside_start: Float,
        outside_end: Float,
        inside_start: Float,
        inside_end: Float
    ) {
        val outside_anim = ValueAnimator.ofFloat(outside_start, outside_end)
        val inside_anim = ValueAnimator.ofFloat(inside_start, inside_end)
        //外圆动画监听
        outside_anim.addUpdateListener { animation: ValueAnimator ->
            button_outside_radius = animation.animatedValue as Float
            invalidate()
        }
        //内圆动画监听
        inside_anim.addUpdateListener { animation: ValueAnimator ->
            button_inside_radius = animation.animatedValue as Float
            invalidate()
        }
        val set = AnimatorSet()
        //当动画结束后启动录像Runnable并且回调录像开始接口
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                //设置为录制状态
                if (state == STATE_LONG_PRESS) {
                    captureListener?.recordStart()
                    buttonEvent = EVENT_RECORDERING
                    timer.start()
                } else {
                    // 此处动画包括长按起始动画和还原动画 若不是长按状态应该还原状态为空闲？？？？---CodeReview
                    state = STATE_IDLE
                }
            }
        })
        set.playTogether(outside_anim, inside_anim)
        set.duration = 100
        set.start()
    }

    //更新进度条
    private fun updateProgress(millisUntilFinished: Long) {
        recorded_time = (duration - millisUntilFinished).toInt()
        progress = 360f - millisUntilFinished / duration.toFloat() * 360f
        invalidate()
    }

    //录制视频计时器
    private inner class RecordCountDownTimer internal constructor(
        millisInFuture: Long,
        countDownInterval: Long
    ) :
        CountDownTimer(millisInFuture, countDownInterval) {
        override fun onTick(millisUntilFinished: Long) {
            updateProgress(millisUntilFinished)
        }

        override fun onFinish() {
            recordEnd()
        }
    }

    //长按线程
    private inner class LongPressRunnable : Runnable {
        override fun run() {
            state = STATE_LONG_PRESS //如果按下后经过500毫秒则会修改当前状态为长按状态
            //启动按钮动画，外圆变大，内圆缩小
            startRecordAnimation(
                button_outside_radius,
                button_outside_radius + outside_add_size,
                button_inside_radius,
                button_inside_radius - inside_reduce_size
            )
        }
    }

    /*************************************************** 对外提供的API**************************************/
    /**
     * 设置最长录制时间,，单位：秒
     * 当到达设定的录制时长时，触发回调通知
     */
    fun setDuration(duration: Int) {
        if (duration<1) {
            return
        }
        this.duration = duration*1000L
        timer = RecordCountDownTimer(this.duration, this.duration / 360) //录制定时器
    }

    //设置回调接口
    fun setCaptureListener(captureListener: CaptureListener?) {
        this.captureListener = captureListener
    }

    //设置按钮功能（拍照和录像）
    fun setButtonFeatures(state: Int) {
        buttonMode = state
    }

    //是否空闲状态
    val isIdle: Boolean
        get() = if (state == STATE_IDLE) true else false

    //设置状态
    fun resetState() {
        state = STATE_IDLE
    }

    companion object {
        // 选择拍照 拍视频 或者都有
        const val BUTTON_STATE_ONLY_CAPTURE = 1 //只能拍照
        const val BUTTON_STATE_ONLY_RECORDER = 2 //只能录像
        const val BUTTON_STATE_BOTH = 3
        const val BUTTON_STATE_BOTH_NOT = 4

        //点击状态
        const val STATE_IDLE = 0x001 //空闲状态
        const val STATE_PRESS = 0x002 //按下状态
        const val STATE_LONG_PRESS = 0x003 //长按状态

        //事件
        const val EVENT_IDLE = 0x001 //空闲状态
        const val EVENT_RECORDERING = 0x002 //录制状态
        const val EVENT_CAPTURE = 0x003 //拍照状态
    }
}
