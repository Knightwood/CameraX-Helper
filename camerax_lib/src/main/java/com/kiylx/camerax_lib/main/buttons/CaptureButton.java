package com.kiylx.camerax_lib.main.buttons;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.kiylx.camerax_lib.R;

/**
 * 拍照按钮，拍视频动画
 * 点击拍照，长按录像
 *
 */
public class CaptureButton extends View {
    // 选择拍照 拍视频 或者都有
    public static final int BUTTON_STATE_ONLY_CAPTURE = 0x101;      //只能拍照
    public static final int BUTTON_STATE_ONLY_RECORDER = 0x102;     //只能录像
    public static final int BUTTON_STATE_BOTH = 0x103;

    private int state;                  //当前按钮状态
    private int button_state = 240;       //默认的大小不可用

    public static final int STATE_IDLE = 0x001;        //空闲状态
    public static final int STATE_PRESS = 0x002;       //按下状态
    public static final int STATE_LONG_PRESS = 0x003;  //长按状态
    public static final int STATE_RECORDERING = 0x004; //录制状态
    public static final int STATE_BAN = 0x005;         //禁止状态

    private int progress_color = 0xEE16AE16;            //进度条颜色
    private int outside_color = 0xEEDCDCDC;             //外圆背景色
    private int inside_color = 0xFFFFFFFF;              //内圆背景色

    private float event_Y;  //Touch_Event_Down时候记录的Y值

    private Paint mPaint;

    private float strokeWidth;          //进度条宽度
    private int outside_add_size;       //长按外圆半径变大的Size
    private int inside_reduce_size;     //长安内圆缩小的Size

    //中心坐标
    private float center_X;
    private float center_Y;

    private float button_radius;            //按钮半径
    private float button_outside_radius;    //外圆半径
    private float button_inside_radius;     //内圆半径
    private int button_size = 80;             //按钮大小

    private float progress;         //录制视频的进度
    private int duration = 15;      //录制视频最大时间长度,秒
    private int recorded_time;      //记录当前录制的时间

    private RectF rectF;

    private LongPressRunnable longPressRunnable;    //长按后处理的逻辑Runnable
    private CaptureListener captureListener;        //按钮回调接口
    private RecordCountDownTimer timer;             //计时器

    public CaptureButton(Context context) {
        super(context);
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    private int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 总要支持一下XML 中布局吧
     *
     * @param context CONTEXT
     * @param attrs   ATTRS
     */
    public CaptureButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray arr = getContext().obtainStyledAttributes(attrs, R.styleable.CaptureButton);

        this.button_size = dip2px(context, arr.getInteger(R.styleable.CaptureButton_size, button_size));
        this.duration = arr.getInteger(R.styleable.CaptureButton_maxDuration, duration)*1000;

        button_radius = button_size / 2.0f;

        button_outside_radius = button_radius;
        button_inside_radius = button_radius * 0.85f;

        strokeWidth = button_size / 15;
        outside_add_size = button_size / 8;
        inside_reduce_size = button_size / 8;

        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        progress = 0;
        longPressRunnable = new LongPressRunnable();

        state = STATE_IDLE;                //初始化为空闲状态
        button_state = BUTTON_STATE_BOTH;  //初始化按钮为可录制可拍照

        center_X = (button_size + outside_add_size * 2) / 2;
        center_Y = (button_size + outside_add_size * 2) / 2;

        rectF = new RectF(
                center_X - (button_radius + outside_add_size - strokeWidth / 2),
                center_Y - (button_radius + outside_add_size - strokeWidth / 2),
                center_X + (button_radius + outside_add_size - strokeWidth / 2),
                center_Y + (button_radius + outside_add_size - strokeWidth / 2));

        timer = new RecordCountDownTimer(duration, duration / 360);    //录制定时器

        arr.recycle();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(button_size + outside_add_size * 2, button_size + outside_add_size * 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mPaint.setStyle(Paint.Style.FILL);

        mPaint.setColor(outside_color); //外圆（半透明灰色）
        canvas.drawCircle(center_X, center_Y, button_outside_radius, mPaint);

        mPaint.setColor(inside_color);  //内圆（白色）
        canvas.drawCircle(center_X, center_Y, button_inside_radius, mPaint);

        //如果状态为录制状态，则绘制录制进度条
        if (state == STATE_RECORDERING) {
            mPaint.setColor(progress_color);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(strokeWidth);
            canvas.drawArc(rectF, -90, progress, false, mPaint);
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (event.getPointerCount() > 1 || state != STATE_IDLE)
                    break;
                event_Y = event.getY();     //记录Y值
                state = STATE_PRESS;        //修改当前状态为点击按下

                //判断按钮状态是否为可录制状态
                if ((button_state == BUTTON_STATE_ONLY_RECORDER || button_state == BUTTON_STATE_BOTH))
                    postDelayed(longPressRunnable, 500);    //同时延长500启动长按后处理的逻辑Runnable
                break;
            case MotionEvent.ACTION_MOVE:
                if (captureListener != null
                        && state == STATE_RECORDERING
                        && (button_state == BUTTON_STATE_ONLY_RECORDER || button_state == BUTTON_STATE_BOTH)) {
                    //记录当前Y值与按下时候Y值的差值，调用缩放回调接口
                    captureListener.recordZoom(event_Y - event.getY());
                }
                break;
            case MotionEvent.ACTION_UP:
                //根据当前按钮的状态进行相应的处理  ----CodeReview---抬起瞬间应该重置状态 当前状态可能为按下和正在录制
                //state = STATE_BAN;
                handlerPressByState();
                break;
        }
        return true;
    }

    //当手指松开按钮时候处理的逻辑
    private void handlerPressByState() {
        removeCallbacks(longPressRunnable); //移除长按逻辑的Runnable
        //根据当前状态处理
        switch (state) {
            //当前是点击按下
            case STATE_PRESS:
                if (captureListener != null && (button_state == BUTTON_STATE_ONLY_CAPTURE || button_state ==
                        BUTTON_STATE_BOTH)) {
                    startCaptureAnimation(button_inside_radius);
                } else {
                    state = STATE_IDLE;
                }
                break;
            // ---CodeReview---当内外圆动画未结束时已经是长按状态 但还没有置为STATE_RECORDERING时 应该也要结束录制  此处是一个bug
            case STATE_LONG_PRESS:
                //当前是长按状态
            case STATE_RECORDERING:
                timer.cancel(); //停止计时器
                recordEnd();    //录制结束
                break;
        }
        state = STATE_IDLE;
    }

    //录制结束
    public void recordEnd() {
        if (captureListener != null) {
            captureListener.recordEnd(recorded_time);  //回调录制结束
        }
        resetRecordAnim();  //重制按钮状态
    }

    //重制状态
    private void resetRecordAnim() {
        state = STATE_BAN;
        progress = 0;       //重制进度
        invalidate();
        //还原按钮初始状态动画
        startRecordAnimation(
                button_outside_radius,
                button_radius,
                button_inside_radius,
                button_radius * 0.75f
        );
    }

    //内圆动画
    private void startCaptureAnimation(float inside_start) {
        ValueAnimator inside_anim = ValueAnimator.ofFloat(inside_start, inside_start * 0.75f, inside_start);
        inside_anim.addUpdateListener(animation -> {
            button_inside_radius = (float) animation.getAnimatedValue();
            invalidate();
        });
        inside_anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                //回调拍照接口
//                if (captureLisenter != null) {
//                    captureLisenter.takePictures();
//                }
                // 为何拍照完成要将状态掷为禁止？？？？此处貌似bug！！！！！！---CodeReview
                //state = STATE_BAN;
                //state = STATE_IDLE;
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if (captureListener != null) {
                    captureListener.takePictures();
                }
                // 防止重复点击 状态重置
                state = STATE_BAN;
            }
        });
        inside_anim.setDuration(50);
        inside_anim.start();
    }

    //内外圆动画
    private void startRecordAnimation(float outside_start, float outside_end, float inside_start, float inside_end) {
        ValueAnimator outside_anim = ValueAnimator.ofFloat(outside_start, outside_end);
        ValueAnimator inside_anim = ValueAnimator.ofFloat(inside_start, inside_end);
        //外圆动画监听
        outside_anim.addUpdateListener(animation -> {
            button_outside_radius = (float) animation.getAnimatedValue();
            invalidate();
        });
        //内圆动画监听
        inside_anim.addUpdateListener(animation -> {
            button_inside_radius = (float) animation.getAnimatedValue();
            invalidate();
        });
        AnimatorSet set = new AnimatorSet();
        //当动画结束后启动录像Runnable并且回调录像开始接口
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                //设置为录制状态
                if (state == STATE_LONG_PRESS) {
                    if (captureListener != null)
                        captureListener.recordStart();
                    state = STATE_RECORDERING;
                    timer.start();
                } else {
                    // 此处动画包括长按起始动画和还原动画 若不是长按状态应该还原状态为空闲？？？？---CodeReview
                    state = STATE_IDLE;
                }
            }
        });
        set.playTogether(outside_anim, inside_anim);
        set.setDuration(100);
        set.start();
    }


    //更新进度条
    private void updateProgress(long millisUntilFinished) {
        recorded_time = (int) (duration - millisUntilFinished);
        progress = 360f - millisUntilFinished / (float) duration * 360f;
        invalidate();
    }

    //录制视频计时器
    private class RecordCountDownTimer extends CountDownTimer {
        RecordCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            updateProgress(millisUntilFinished);
        }

        @Override
        public void onFinish() {
            //updateProgress(duration);
            recordEnd();
        }
    }

    //长按线程
    private class LongPressRunnable implements Runnable {
        @Override
        public void run() {
            state = STATE_LONG_PRESS;   //如果按下后经过500毫秒则会修改当前状态为长按状态
            //启动按钮动画，外圆变大，内圆缩小
            startRecordAnimation(
                    button_outside_radius,
                    button_outside_radius + outside_add_size,
                    button_inside_radius,
                    button_inside_radius - inside_reduce_size
            );
        }
    }

    /**************************************************
     * 对外提供的API                     *
     **************************************************/

    //设置最长录制时间
    public void setDuration(int duration) {
        this.duration = duration;
        timer = new RecordCountDownTimer(duration, duration / 360);    //录制定时器
    }

    //设置回调接口
    public void setCaptureListener(CaptureListener captureListener) {
        this.captureListener = captureListener;
    }

    //设置按钮功能（拍照和录像）
    public void setButtonFeatures(int state) {
        this.button_state = state;
    }

    // 获取当前按钮支持状态
    public int getButtonState() {
        return button_state;
    }

    //是否空闲状态
    public boolean isIdle() {
        return state == STATE_IDLE ? true : false;
    }

    //设置状态
    public void resetState() {
        state = STATE_IDLE;
    }

}
