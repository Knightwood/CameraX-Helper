package com.kiylx.camerax_lib.main.manager.imagedetection.face

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Point
import android.util.AttributeSet
import android.view.View
import androidx.camera.core.CameraSelector

interface Overlay {

    //更新当前使用的相机镜头是前置还是后置
    fun toggleSelector(lensFacing: Int)

    /**
     * 设备旋转，对坐标做转换
     */
    fun rotationChanged(rotation: Int, angle: Int)

}

/**
 * 目前有个问题：
 * 如果不限制activity的方向，任其自由旋转，没有问题
 * 如果限制为横屏或竖屏，没有问题。但此时旋转手机，面部识别后，绘制坐标会缺失旋转变换。
 */
open class GraphicOverlay(context: Context?, attrs: AttributeSet?) :
    View(context, attrs), Overlay {
    lateinit var scaleMatrix: Matrix//you can set a matrix to provide map point

    private val lock = Any()
    private val graphics: MutableList<Graphic> = ArrayList()

    var cameraSelector: Int = CameraSelector.LENS_FACING_BACK
    lateinit var processBitmap: Bitmap
    lateinit var processCanvas: Canvas

    fun isFrontMode() = cameraSelector == CameraSelector.LENS_FACING_FRONT

    override fun toggleSelector(lensFacing: Int) {
        cameraSelector =lensFacing
//            if (cameraSelector == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT
//            else CameraSelector.LENS_FACING_BACK
    }

    fun clear() {
        synchronized(lock) { graphics.clear() }
        postInvalidate()
    }

    fun add(graphic: Graphic) {
        synchronized(lock) { graphics.add(graphic) }
    }

    fun remove(graphic: Graphic) {
        synchronized(lock) { graphics.remove(graphic) }
        postInvalidate()
    }

    private fun initProcessCanvas() {
        processBitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
        processCanvas = Canvas(processBitmap)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        synchronized(lock) {
            //initProcessCanvas()
            graphics.forEach {
                it.draw(canvas)
                //it.draw(processCanvas)
            }
        }
    }

    /**
     * for land scape
     * 判断是不是横屏（不包括通过传感器一类获取方向后手动改变方向这类）
     */
    fun isLandScapeMode(): Boolean {
        val b = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        return b
    }

    var angle: Int = 0

    /**
     * 设备旋转，对坐标做转换
     */
    override fun rotationChanged(rotation: Int, angle: Int) {
        this.angle = angle
    }

}

/**
 * 绕某点旋转
 */
fun calcNewPoint(p: Point, pCenter: Point, angle: Float): Point {
    // calc arc
    val l = (angle * Math.PI / 180).toFloat()

    //sin/cos value
    val cosv = Math.cos(l.toDouble()).toFloat()
    val sinv = Math.sin(l.toDouble()).toFloat()

    // calc new point
    val newX = ((p.x - pCenter.x) * cosv - (p.y - pCenter.y) * sinv + pCenter.x) as Float
    val newY = ((p.x - pCenter.x) * sinv + (p.y - pCenter.y) * cosv + pCenter.y) as Float
    return Point(newX.toInt(), newY.toInt())
}