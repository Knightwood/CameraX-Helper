package com.kiylx.store_lib.mediastore

import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import com.kiylx.store_lib.kit.noNullUriResult

/**
 *如需访问由其他应用创建的文件，必须满足以下所有条件：
 *您的应用已获得 READ_EXTERNAL_STORAGE 权限。
 *这些文件位于以下任一明确定义的媒体集合中：
 *MediaStore.Images
 *MediaStore.Video
 *MediaStore.Audio
 *注意：只要文件可通过 MediaStore.Images、MediaStore.Video 或 MediaStore.Audio 查询进行查看，
 *   则表示该文件也可以通过 MediaStore.Files 查询进行查看。
 *如果应用想要访问 MediaStore.Downloads 集合中某个并非由其创建的文件，则必须使用存储访问框架。
 *
 *
 * 如果您的应用在搭载 Android 9 或更低版本的设备上使用，或者您的应用暂时停用分区存储，
 * 您必须请求 READ_EXTERNAL_STORAGE 权限才能访问媒体文件。
 * 如果要修改媒体文件，您还必须请求 WRITE_EXTERNAL_STORAGE 权限。
 */
interface MediaStoreMethod {
    /**
     * @param relativePath 相对图片文件夹的相对路径
     * 例如 传入test，会存入 storage/emulated/0/Pictures/test/ 文件夹
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun newPhoto(
        name: String,
        mime: String,
        relativePath: String = "",
        block: noNullUriResult,
    )

    @RequiresApi(Build.VERSION_CODES.Q)
    fun newPicture(
        name: String,
        relativePath: String = "",
        mime: String,
        block: noNullUriResult,
    )

    @RequiresApi(Build.VERSION_CODES.Q)
    fun newDownloadFile(
        name: String,
        relativePath: String = "",
        mime: String,
        block: noNullUriResult,
    )

    @RequiresApi(Build.VERSION_CODES.Q)
    fun newMovieFile(
        name: String,
        relativePath: String = "",
        mime: String,
        block: noNullUriResult,
    )

    @RequiresApi(Build.VERSION_CODES.Q)
    fun newMusicFile(
        name: String,
        relativePath: String = "",
        mime: String,
        block: noNullUriResult,
    )

    /**
     * 根据条件查询，执行完block块中的行为，会自动对cursor调用close()
     * @param fileLocate [FileLocate] 查询什么媒体文件
     * @param projection：指定需要返回的字段，null 会返回所有字段
     * @param selection：查询的 where 语句，类似 xxx = ?
     * @param selectionArgs：查询的 where 语句的值，类似 new String[] { xxx }
     * @param sortOrder：排序语句，类似 xxx DESC
     */
    fun queryFile(
        fileLocate: FileLocate, projection: Array<String>?=null, selection: String?=null,
        selectionArgs: Array<String>?=null, sortOrder: String?=null,
        block: (cursor: Cursor) -> Unit,
    )

    /**
     * 通过 MediaStore 删除符合查询条件的媒体文件（只能删除本 app 创建的文件）
     * 注：如果你的 app 卸载后再重装的话系统不会认为是同一个 app（也就是你卸载之前创建的文件，再次安装 app 后是无法通过这种方式删除它的）
     *
     * @param fileLocate [FileLocate] 删除什么类型的媒体文件
     * @param selection：查询的 where 语句，类似 xxx = ?
     * @param selectionArgs：查询的 where 语句的值，类似 new String[] { xxx }
     * @return 返回删除了几行 如果发生异常，返回-1
     *
     *仅删除时长至少 5 分钟的视频。
     *  val selection = "${MediaStore.Video.Media.DURATION} >= ?"
     *  val selectionArgs = arrayOf(
     *  TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES).toString()
     * )
     */
    @WorkerThread
    fun deleteFile(
        fileLocate: FileLocate, selection: String?,
        selectionArgs: Array<String>?,
    ): Int

    /**
     * @param contentUri 文件的contentUri
     * @param extras 包含操作所需的附加信息的 Bundle。参数可能包括 SQL 样式参数，
     *               例如 QUERY_ARG_SQL_LIMIT，但请注意，每个单独的提供程序的文档将指示它们支持哪些参数。
     * @return 返回删除了几行 如果发生异常，返回-1
     *
     * 例如删除一个图片，要先拿到需要删除的图片的 content uri（类似这样的地址 content://media/external/images/media/102）
     *
     * val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
     * val contentId = cursor.getLong(idColumn)
     * val contentUri = Uri.parse("${MediaStore.Images.Media.EXTERNAL_CONTENT_URI}/${contentId})
     */
    @RequiresApi(Build.VERSION_CODES.R)
    @WorkerThread
    fun deleteFile(
        contentUri: Uri,
        extras: Bundle? = null,
    ): Int
}