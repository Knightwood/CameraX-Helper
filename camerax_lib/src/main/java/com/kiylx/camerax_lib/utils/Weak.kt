package com.kiylx.camerax_lib.utils

import android.util.Log
import java.lang.ref.WeakReference
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * 使用kotlin委托机制创建弱引用的工具类
 */
class Weak<T : Any>(initializer: () -> T?) : ReadWriteProperty<Any?, T?> {
    var weakReference = WeakReference<T?>(initializer())

    //次级构造函数，最终是调用主构造函数
    constructor() : this(initializer = fun(): T? {
        return null
    })

    override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        return weakReference.get()
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        weakReference = WeakReference(value)
    }


}