package com.kiylx.store_lib.mediastore

import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.fragment.app.Fragment
import com.kiylx.store_lib.kit.noNullUriResult
import java.io.File

class MediaStoreFragment : Fragment(), MediaStoreMethod {
    /**
     * @param relativePath 相对图片文件夹的相对路径
     * 例如 传入test，会存入 storage/emulated/0/Pictures/test/ 文件夹
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun newPhoto(
        name: String,
        mime: String,
        relativePath: String,
        block: noNullUriResult,
    ) {
        if ((relativePath.isNotEmpty())) {
            genPic(name, "${Environment.DIRECTORY_DCIM}${File.separator}$relativePath", mime, block)
        } else {
            genPic(name, Environment.DIRECTORY_DCIM, mime, block)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun newPicture(
        name: String,
        relativePath: String,
        mime: String,
        block: noNullUriResult,
    ) {
        if ((relativePath.isNotEmpty())) {
            genPic(name, "${Environment.DIRECTORY_PICTURES}${File.separator}$relativePath", mime, block)
        } else {
            genPic(name, Environment.DIRECTORY_PICTURES, mime, block)
        }
    }

    /**
     * @param relativePath 相对图片文件夹的相对路径
     * 例如 传入test，会产生 storage/emulated/0/Pictures/test/
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun genPic(
        name: String,
        path: String,
        mime: String,
        block: noNullUriResult,
    ) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.RELATIVE_PATH, path)
            put(MediaStore.Images.ImageColumns.DISPLAY_NAME, name)
            put(MediaStore.Images.ImageColumns.MIME_TYPE, mime)
        }
        val contentResolver = requireActivity().contentResolver
        // 通过 ContentResolver 在指定的公共目录下按照指定的 ContentValues 创建文件，会返回文件的 content uri（类似这样的地址 content://media/external/images/media/102）
        val uri: Uri? =
            contentResolver.insert(FileLocate.IMAGE.uri, contentValues)
        if (uri != null) {
            block(uri)
        } else {
            throw Exception("底层内容提供程序返回null或崩溃")
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun newDownloadFile(
        name: String,
        relativePath: String,
        mime: String,
        block: noNullUriResult,
    ) {
        if (relativePath.isNotEmpty()) {
            genDownloadFile(name, "${Environment.DIRECTORY_DOWNLOADS}${File.separator}$relativePath", mime, block)
        } else {
            genDownloadFile(name, Environment.DIRECTORY_DOWNLOADS, mime, block)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun genDownloadFile(
        name: String,
        path: String,
        mime: String,
        block: noNullUriResult,
    ) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.RELATIVE_PATH, path)
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mime)
        }
        val contentResolver = requireActivity().contentResolver
        // 通过 ContentResolver 在指定的公共目录下按照指定的 ContentValues 创建文件，会返回文件的 content uri（类似这样的地址 content://media/external/images/media/102）
        val uri: Uri? =
            contentResolver.insert(FileLocate.DOWNLOAD.uri, contentValues)
        if (uri != null) {
            block(uri)
        } else {
            throw Exception("底层内容提供程序返回null或崩溃")
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun newMovieFile(
        name: String,
        relativePath: String,
        mime: String,
        block: noNullUriResult,
    ) {
        if (relativePath.isNotEmpty()) {
            genMovieFile(name, "${Environment.DIRECTORY_MOVIES}${File.separator}$relativePath", mime, block)
        } else {
            genMovieFile(name, Environment.DIRECTORY_MOVIES, mime, block)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun genMovieFile(
        name: String,
        path: String,
        mime: String,
        block: noNullUriResult,
    ) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.RELATIVE_PATH, path)
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, mime)
        }
        val contentResolver = requireActivity().contentResolver
        // 通过 ContentResolver 在指定的公共目录下按照指定的 ContentValues 创建文件，会返回文件的 content uri（类似这样的地址 content://media/external/images/media/102）
        val uri: Uri? =
            contentResolver.insert(FileLocate.VIDEO.uri, contentValues)
        if (uri != null) {
            block(uri)
        } else {
            throw Exception("底层内容提供程序返回null或崩溃")
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun newMusicFile(
        name: String,
        relativePath: String,
        mime: String,
        block: noNullUriResult,
    ) {
        if (relativePath.isNotEmpty()) {
            genMusicFile(name, "${Environment.DIRECTORY_MUSIC}${File.separator}$relativePath", mime, block)
        } else {
            genMusicFile(name, Environment.DIRECTORY_MUSIC, mime, block)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun genMusicFile(
        name: String,
        path: String,
        mime: String,
        block: noNullUriResult,
    ) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.RELATIVE_PATH, path)
            put(MediaStore.Audio.Media.DISPLAY_NAME, name)
            put(MediaStore.Audio.Media.MIME_TYPE, mime)
        }
        val contentResolver = requireActivity().contentResolver
        // 通过 ContentResolver 在指定的公共目录下按照指定的 ContentValues 创建文件，会返回文件的 content uri（类似这样的地址 content://media/external/images/media/102）
        val uri: Uri? =
            contentResolver.insert(FileLocate.AUDIO.uri, contentValues)
        if (uri != null) {
            block(uri)
        } else {
            throw Exception("底层内容提供程序返回null或崩溃")
        }
    }

    /**
     * 根据条件查询媒体文件，执行完block块中的行为，会自动对cursor调用close()
     *
     * @param fileLocate [FileLocate] 查询什么媒体文件
     * @param projection：指定需要返回的字段，null 会返回所有字段
     * @param selection：查询的 where 语句，类似 xxx = ?
     * @param selectionArgs：查询的 where 语句的值，类似 new String[] { xxx }
     * @param sortOrder：排序语句，类似 xxx DESC
     *
     *
     * 例如：查询一些视频
     * val fileLocate = FileLocate.VIDEO
     *
     *要查询这些内容
     * val projection = arrayOf(
     *  MediaStore.Video.Media._ID,
     *  MediaStore.Video.Media.DISPLAY_NAME,
     *  MediaStore.Video.Media.DURATION,
     *  MediaStore.Video.Media.SIZE
     * )
     *
     *仅显示时长至少 5 分钟的视频。
     *  val selection = "${MediaStore.Video.Media.DURATION} >= ?"
     *  val selectionArgs = arrayOf(
     *  TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES).toString()
     * )
     *
     *根据显示名称按字母顺序显示视频。
     *  val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"
     */
    @WorkerThread
    override fun queryFile(
        fileLocate: FileLocate, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?,
        block: (cursor: Cursor) -> Unit,
    ) {
        requireActivity().contentResolver
            .query(fileLocate.uri, projection, selection, selectionArgs, sortOrder)
            ?.let { cursor ->
                block(cursor)
                if (!cursor.isClosed)
                    cursor.close()
            }
    }

    /**
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
    override fun deleteFile(
        fileLocate: FileLocate,
        selection: String?,
        selectionArgs: Array<String>?,
    ): Int {
        val n = try {
            requireActivity().contentResolver.delete(fileLocate.uri, selection, selectionArgs)
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
        return n
    }

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
    override fun deleteFile(
        contentUri: Uri,
        extras: Bundle?,
    ): Int {
        val n = try {
            requireActivity().contentResolver.delete(contentUri, extras)
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
        return n
    }


}

/**
 * Environment下的字段，表示几个公共文件夹的位置
 *
 * IMAGE：Environment.DIRECTORY_PICTURES
 * DCIM：Environment.DIRECTORY_DCIM
 * VIDEO：Environment.DIRECTORY_MOVIES
 * AUDIO：Environment.DIRECTORY_MUSIC
 * DOWNLOAD：Environment.DIRECTORY_DOWNLOADS
 *
 * MediaStore下的EXTERNAL_CONTENT_URI，表示数据库的字段
 */
enum class FileLocate(val uri: Uri) {
    IMAGE(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL
        )
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }),
    VIDEO(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL
            )
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
    ),
    AUDIO(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL
            )
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
    ),

    @RequiresApi(Build.VERSION_CODES.Q)
    DOWNLOAD(
        MediaStore.Downloads.getContentUri(
            MediaStore.VOLUME_EXTERNAL
        )
    )
}