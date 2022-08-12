package com.kiylx.store_lib.kit

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

/**
 * 简化StartActivityForResult操作
 */
object RequestHelper {

    /**
     * 生成一个隐藏的fragment，并获得结果，最终销毁这个隐藏的fragment
     */
    inline fun finalStartActivityForResult(
        starter: FragmentActivity?,
        intent: Intent,
        crossinline block: ((result: Intent?) -> Unit),
    ) {
        starter?.let { activity_ ->
            val fragment = CallbackFragment()
            fragment.init(intent) { result ->
                block(result)//将得到的intent传递给函数的调用者，并结束此隐藏的fragment
                activity_.supportFragmentManager
                    .beginTransaction()
                    .remove(fragment)
                    .commitAllowingStateLoss()
            }
            activity_.supportFragmentManager.beginTransaction()
                .add(fragment, CallbackFragment::class.qualifiedName)
                .commitAllowingStateLoss()
        }
    }

}

class CallbackFragment : Fragment() {
    private var intent: Intent? = null
    private var callback: ((result: Intent?) -> Unit)? = null
    private var register =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            val result: Intent? =
                if (activityResult.resultCode == Activity.RESULT_OK) activityResult.data else null
            callback?.let { it(result) }
        }


    fun init(intent: Intent, callback: ((result: Intent?) -> Unit)) {
        this.intent = intent
        this.callback = callback
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        register.launch(intent)
    }

    override fun onDetach() {
        super.onDetach()
        intent = null
        callback = null
        register.unregister()
    }
}

