package com.kiylx.camerax_lib.main.manager.util

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TimeInterval(
    private val interval: Long,//时间间隔
) {
    private var timeLast = System.currentTimeMillis()
    private var lock: ReentrantLock = ReentrantLock()

    /**
     * 当大于阈值时，执行block块
     */
    fun then(block: () -> Unit) {
        lock.withLock {
            if (System.currentTimeMillis() - timeLast > interval) {
                timeLast = System.currentTimeMillis()
                block()
            }
        }
    }
}