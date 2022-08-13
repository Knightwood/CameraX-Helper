package com.kiylx.camerax_lib.main.manager.ui

import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentActivity

/*
FitsSystemWindows 的默认行为是：通过 padding 为 System bar 预留出空间


Android 30 引入了 WindowInsetsController 来控制 WindowInsets，主要功能包括：

1. 显示/隐藏 System bar
2. 设置 System bar 前景（如状态栏的文字图标）是亮色还是暗色
3. 逐帧控制 insets 动画，例如可以让软键盘弹出得更丝滑
*/

/**
 * 使内容布局拓展到状态栏下面
 * @param rootLayout 根布局
 * @param ids 当内容布局拓展到状态栏下面时，需要偏移的视图的布局id集合
 * @param alsoApplyNavigationBar true:让视图延展到底部导航栏
 */
fun FragmentActivity.setWindowEdgeToEdge(
    rootLayout: View,
    vararg ids: Int,
    alsoApplyNavigationBar: Boolean = true,
) {
    // 1. 使内容区域全屏
    WindowCompat.setDecorFitsSystemWindows(window, false)

    // 2. 设置 System bar 透明
    window.statusBarColor = Color.TRANSPARENT
    if (alsoApplyNavigationBar)
        window.navigationBarColor = Color.TRANSPARENT

    // 3. 可能出现视觉冲突的 view 处理 insets
    ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view_, windowInsetsCompat ->
        val insets = windowInsetsCompat.getInsets(WindowInsetsCompat.Type.systemBars())
        // 此处更改的 margin，也可设置 padding，视情况而定
        ids.forEach {
            view_.findViewById<View>(it).updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
            }
        }
        WindowInsetsCompat.CONSUMED
    }
}

/**
 * 使内容布局拓展到状态栏下面
 * 旧实现，Android11以上 systemUiVisibility()被废弃
 */
fun FragmentActivity.setWindowEdgeToEdgeOld(
    rootLayout: View,
    vararg ids: Int,
) {
    window.statusBarColor = Color.TRANSPARENT
    rootLayout.systemUiVisibility = (
            SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

    ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view_, windowInsetsCompat ->//监听顶部偏移，对特定view做偏移
        val insets = windowInsetsCompat.getInsets(WindowInsetsCompat.Type.systemBars())
        // 此处更改的 margin，也可设置 padding，视情况而定
        ids.forEach {
            view_.findViewById<View>(it).updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
            }
        }
        windowInsetsCompat
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

/**
 * 给状态栏、导航栏 设置颜色
 * @param isLight 设置亮色
 * @param alsoApplyNavigationBar true:底部导航栏也变色
 */
fun View.setColor(
    isLight: Boolean = true,
    alsoApplyNavigationBar: Boolean = true,
) {
    ViewCompat.getWindowInsetsController(this)?.isAppearanceLightStatusBars = isLight
    if (alsoApplyNavigationBar)
        ViewCompat.getWindowInsetsController(this)?.isAppearanceLightNavigationBars = isLight
}