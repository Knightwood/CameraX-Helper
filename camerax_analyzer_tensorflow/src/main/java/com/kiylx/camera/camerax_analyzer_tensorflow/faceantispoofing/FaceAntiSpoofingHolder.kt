package com.kiylx.camera.camerax_analyzer_tensorflow.faceantispoofing

import android.app.Application
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 单例持有tensorflow lite 处理器
 * 在[Dispatchers.Default]调度的协程中检测是否是真人
 */
class FaceAntiSpoofingHolder private constructor(ctx: Application) {
    private val faceAntiSpoofing: FaceAntiSpoofing = FaceAntiSpoofing(ctx.assets)

    /**
     * 活体检测
     * @param bitmap
     * @return 评分 评分越高，为假的概率越高
     */
    suspend fun anti(bitmap: Bitmap) = withContext(Dispatchers.Default) {
        return@withContext faceAntiSpoofing.antiSpoofing(bitmap)
    }

    /**
     * 拉普拉斯算法计算清晰度
     * @param bitmap
     * @return 分数
     */
    suspend fun laplacian(bitmap: Bitmap) = withContext(Dispatchers.Default) {
        return@withContext faceAntiSpoofing.laplacian(bitmap)
    }

    companion object {
        @Volatile
        private var holder: FaceAntiSpoofingHolder? = null

        fun getInstance(ctx: Application) =
            holder ?: synchronized(this) {
                holder ?: FaceAntiSpoofingHolder(ctx).also { holder = it }
            }
    }
}