package com.kiylx.store_lib

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.kiylx.store_lib.mediastore.MediaStoreFragment
import com.kiylx.store_lib.mediastore.MediaStoreMethod
import com.kiylx.store_lib.saf.FileMethod
import com.kiylx.store_lib.saf.SafImplFragment

class StorageXHelper : LifecycleEventObserver {
    private var activity: FragmentActivity
        set(value) {
            field = value
            value.lifecycle.addObserver(this)
        }
    private var fragment: Fragment? = null

    constructor(fragment: Fragment) {
        this.fragment = fragment
        this.activity = fragment.requireActivity()
    }

    constructor(fragmentActivity: FragmentActivity) {
        this.activity = fragmentActivity
    }

    /**
     * 在Activity中获取 FragmentManager，如果在Fragment中，则获取 ChildFragmentManager。
     */
    private val fragmentManager: FragmentManager
        get() {
            return fragment?.childFragmentManager ?: activity.supportFragmentManager
        }

    /**
     * 使用此实例操作文件
     */
    val safHelper: SafHelper.Helper by lazy {
        SafHelper(fragmentManager).helper
    }

    val mediaStoreHelper: MediaStoreHelper.Helper by lazy {
        MediaStoreHelper(fragmentManager).helper
    }

    companion object {
        /**
         * TAG of InvisibleFragment to find and create.
         */
        internal const val SAF_FRAGMENT_TAG = "SafInvisibleFragment"

        /**
         * MediaStore Fragment tag
         */
        internal const val MS_FRAGMENT_TAG = "MediaStoreInvisibleFragment"

    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            val t = fragmentManager.beginTransaction()
            val existed1 = fragmentManager.findFragmentByTag(SAF_FRAGMENT_TAG)
            val existed2 = fragmentManager.findFragmentByTag(MS_FRAGMENT_TAG)
            if (existed2 != null) {
                t.remove(existed2)
            }
            if (existed1 != null) {
                t.remove(existed1)
            }
            t.commitAllowingStateLoss()
        }
    }
}

class SafHelper(private val fragmentManager: FragmentManager) {
    private val invisibleFragment: SafImplFragment
        get() {
            val existed = fragmentManager.findFragmentByTag(StorageXHelper.SAF_FRAGMENT_TAG)
            if (existed != null) {
                return existed as SafImplFragment
            } else {
                val invisibleFragment = SafImplFragment()
                fragmentManager.beginTransaction()
                    .add(invisibleFragment, StorageXHelper.SAF_FRAGMENT_TAG)
                    .commitNowAllowingStateLoss()
                return invisibleFragment
            }
        }

    /**
     * 使用此实例操作文件
     */
    val helper: Helper by lazy {
        Helper(invisibleFragment)
    }

    /**
     * 所有的文件操作，全部委托给[invisibleFragment]
     * 使用委托的方式，隐藏掉fragment，只向外界提供接口的实现
     */
    inner class Helper(private val invisibleFragment: SafImplFragment) :
        FileMethod by invisibleFragment
}

class MediaStoreHelper(private val fragmentManager: FragmentManager) {
    private val invisibleFragment: MediaStoreFragment
        get() {
            val existed = fragmentManager.findFragmentByTag(StorageXHelper.MS_FRAGMENT_TAG)
            if (existed != null) {
                return existed as MediaStoreFragment
            } else {
                val invisibleFragment = MediaStoreFragment()
                fragmentManager.beginTransaction()
                    .add(invisibleFragment, StorageXHelper.MS_FRAGMENT_TAG)
                    .commitNowAllowingStateLoss()
                return invisibleFragment
            }
        }

    /**
     * 使用此实例操作文件
     */
    val helper: Helper by lazy {
        Helper(invisibleFragment)
    }

    /**
     * 所有的文件操作，全部委托给[invisibleFragment]
     * 使用委托的方式，隐藏掉fragment，只向外界提供接口的实现
     */
    inner class Helper(private val invisibleFragment: MediaStoreFragment) :
        MediaStoreMethod by invisibleFragment

}
