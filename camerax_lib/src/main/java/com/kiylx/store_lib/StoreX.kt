@file:Suppress("unused")

package com.kiylx.store_lib

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

class StoreX {
    companion object {
        @JvmStatic
        fun with(fragmentActivity: FragmentActivity): StorageXHelper {
            return StorageXHelper(fragmentActivity)
        }

        @JvmStatic
        fun with(fragment: Fragment): StorageXHelper {
            return StorageXHelper(fragment)
        }

        @JvmStatic
        fun StorageXHelper.safHelper(): SafHelper.Helper {
            return this.safHelper
        }

        @JvmStatic
        fun StorageXHelper.mediaStoreHelper(): MediaStoreHelper.Helper {
            return this.mediaStoreHelper
        }

    }
}