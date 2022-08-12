package com.kiylx.store_lib.kit.ext

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.kiylx.store_lib.kit.RequestHelper
import java.io.Serializable

/**
 * 简化startActivityForResult()操作
 * @param intent intent
 * @param callback 处理执行startActivityForResult后在onActivityResult得到的结果
 */
inline fun FragmentActivity?.startActivityForResult(
    intent: Intent, crossinline callback: ((result: Intent?) -> Unit),
) = RequestHelper.finalStartActivityForResult(this, intent, callback)

inline fun Fragment.startActivityForResult(
    intent: Intent, crossinline callback: ((result: Intent?) -> Unit),
) = RequestHelper.finalStartActivityForResult(this.requireActivity(), intent, callback)


/**
 * 换了下postDelayed的参数顺序
 */
fun Handler.postDelay(delayMillis: Long, runnable: Runnable): Boolean =
    postDelayed(runnable, delayMillis)


fun FragmentActivity.makeToast(msg: String, duration: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(applicationContext, msg, duration).show()

fun Fragment.makeToast(msg: String, duration: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(requireActivity().applicationContext, msg, duration).show()


/**
 *  [Intent]的扩展方法，用来批量put键值对
 *  示例：
 *  <pre>
 *      intent.putExtras(
 *          "Key1" to "Value",
 *          "Key2" to 123,
 *          "Key3" to false,
 *          "Key4" to arrayOf("4", "5", "6")
 *      )
 * </pre>
 *
 * @param params 键值对
 */
fun <T> Intent.putExtras(vararg params: Pair<String, T>): Intent {
    if (params.isEmpty()) return this
    params.forEach { (key, value) ->
        when (value) {
            is Int -> putExtra(key, value)
            is Byte -> putExtra(key, value)
            is Char -> putExtra(key, value)
            is Long -> putExtra(key, value)
            is Float -> putExtra(key, value)
            is Short -> putExtra(key, value)
            is Double -> putExtra(key, value)
            is Boolean -> putExtra(key, value)
            is Bundle -> putExtra(key, value)
            is String -> putExtra(key, value)
            is IntArray -> putExtra(key, value)
            is ByteArray -> putExtra(key, value)
            is CharArray -> putExtra(key, value)
            is LongArray -> putExtra(key, value)
            is FloatArray -> putExtra(key, value)
            is Parcelable -> putExtra(key, value)
            is ShortArray -> putExtra(key, value)
            is DoubleArray -> putExtra(key, value)
            is BooleanArray -> putExtra(key, value)
            is CharSequence -> putExtra(key, value)
            is Array<*> -> {
                when {
                    value.isArrayOf<String>() ->
                        putExtra(key, value as Array<String?>)
                    value.isArrayOf<Parcelable>() ->
                        putExtra(key, value as Array<Parcelable?>)
                    value.isArrayOf<CharSequence>() ->
                        putExtra(key, value as Array<CharSequence?>)
                    else -> putExtra(key, value)
                }
            }
            is Serializable -> putExtra(key, value)
        }
    }
    return this
}