package com.kiylx.camerax_lib.main.manager.util

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentActivity
import com.kiylx.store_lib.kit.MimeTypeConsts.it

/*
FitsSystemWindows 的默认行为是：通过 padding 为 System bar 预留出空间


Android 30 引入了 WindowInsetsController 来控制 WindowInsets，主要功能包括：

1. 显示/隐藏 System bar
2. 设置 System bar 前景（如状态栏的文字图标）是亮色还是暗色
3. 逐帧控制 insets 动画，例如可以让软键盘弹出得更丝滑
*/

/**
 * 使内容布局拓展到状态栏下面
 * @param ids 当内容布局拓展到状态栏下面时，需要偏移的视图的布局id集合
 * @param alsoApplyNavigationBar true:让视图延展到底部导航栏
 */
fun FragmentActivity.setWindowEdgeToEdge(
    alsoApplyNavigationBar: Boolean = true,
    func:(insets:Insets)->Unit,
) {
    val rootLayout: View = findViewById(android.R.id.content)

    // 1. 使内容区域全屏
    WindowCompat.setDecorFitsSystemWindows(window, false)

    // 2. 设置 System bar 透明
    window.statusBarColor = Color.TRANSPARENT
    if (alsoApplyNavigationBar)
        window.navigationBarColor = Color.TRANSPARENT
    rootLayout.doOnAttach {
        val insets = ViewCompat.getRootWindowInsets(window.decorView)
            ?.getInsets(WindowInsetsCompat.Type.systemBars()) ?: Insets.NONE
        func(insets)
    }
}

/**
 * @param type: 显示或隐藏 状态栏、导航栏软键盘。 WindowInsetsCompat.Type.statusBars() 、navigationBars()、systemBar()
 * @param hide: true:隐藏状态栏，false:显示状态栏
 */

@RequiresApi(Build.VERSION_CODES.R)
fun FragmentActivity.hideShow(
    type: Int = WindowInsetsCompat.Type.statusBars(),
    hide: Boolean = true,
) {
    if (hide)
        window.insetsController?.hide(type)
    else
        window.insetsController?.show(type)
}

/**
 * @param type: 状态栏、导航栏、软键盘等，是否可见。WindowInsetsCompat.Type.statusBars() 、navigationBars()、systemBar()
 */
fun View.isVisibility(
    type: Int = WindowInsetsCompat.Type.statusBars(),
): Boolean {
    return ViewCompat.getRootWindowInsets(this)
        ?.isVisible(type) ?: true
}

fun Activity.statusBarTheme(light:Boolean){
    val rootLayout: View = findViewById(android.R.id.content)
    val controller = WindowCompat.getInsetsController(window,rootLayout)
    controller.isAppearanceLightStatusBars=light
}

fun Activity.navBarTheme(light:Boolean){
    val rootLayout: View = findViewById(android.R.id.content)
    val controller = WindowCompat.getInsetsController(window,rootLayout)
    controller.isAppearanceLightNavigationBars=light
}