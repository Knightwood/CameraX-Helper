package com.kiylx.store_lib.saf

import android.content.ContentResolver
import android.net.Uri
import com.kiylx.store_lib.kit.input
import com.kiylx.store_lib.kit.noNullUriResult
import com.kiylx.store_lib.kit.output

interface FileMethod {
    /**
     * 申请一个文件夹的使用权限
     */
    fun requestOneFolder(pickerInitialUri: String? = null, block: noNullUriResult)

    /**
     * 保留对应目录的读写权限
     */
    fun savePerms(uri: Uri)
    /**
     * 取消某个目录的权限
     * @param uri 需要取消权限的目录uri
     */
    fun releaseFolderPerms(
        uri: Uri,
    )
    /**
     * 使用saf打开一个用户选择的文件
     */
    fun selectFile(
        pickerInitialUri: String? = null,
        fileType: String = "*/*",
        block: noNullUriResult,
    )

    /**
     * @param fileName 文件名
     * @param pickerInitialUri （可选）在您的应用创建文档之前，为应该在系统文件选择器中打开的目录指定一个 URI。
     * @param fileType （可选）文件的类型
     * @param block 文件创建后，将会回调此匿名函数
     *
     * @see com.kiylx.store_lib.kit.MimeTypeConsts
     */
    fun createFile(
        fileName: String,
        fileType: String = "*/*",
        pickerInitialUri: String? = null,
        block: noNullUriResult,
    )

    fun readFile(
        uri: Uri,
        input: input,
    )

    fun writeFile(
        uri: Uri,
        output: output,
    )

    fun getContentResolver(): ContentResolver

}