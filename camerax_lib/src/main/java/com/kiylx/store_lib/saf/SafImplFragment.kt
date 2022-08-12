package com.kiylx.store_lib.saf

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.fragment.app.Fragment
import com.kiylx.store_lib.kit.ext.readFileFromUri
import com.kiylx.store_lib.kit.ext.runSafelyNoNull
import com.kiylx.store_lib.kit.ext.startActivityForResult
import com.kiylx.store_lib.kit.ext.writeFileFromUri
import com.kiylx.store_lib.kit.input
import com.kiylx.store_lib.kit.noNullUriResult
import com.kiylx.store_lib.kit.output

/** 从 MediaStore接口或者SAF获取到文件Uri后，请利用Uri打开FD 或者输入输出流，而不要转换成文件路径去访问。 */
class SafImplFragment : Fragment(), FileMethod {

    /** 申请一个文件夹的使用权限 如果直接返回，则intent为null，不触发后续处理。如果intent.data为null,则报错 */
    override fun requestOneFolder(
        pickerInitialUri: String?,
        block: noNullUriResult, /* = (uri: android.net.Uri) -> kotlin.Unit */
    ) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            //可选，添加一个初始路径
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && pickerInitialUri != null) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
            }
        }
        startActivityForResult(intent) {
            it?.data.runSafelyNoNull(block)
        }
    }

    /** “获取”系统提供的永久性 URI 访问权限 */
    override fun savePerms(uri: Uri) {
        val contentResolver = getContentResolver()
        val takeFlags: Int = (Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        //检查
        contentResolver.takePersistableUriPermission(uri, takeFlags)
    }

    /**
     * 取消某个目录的权限
     *
     * @param uri 需要取消权限的目录uri
     */
    override fun releaseFolderPerms(
        uri: Uri,
    ) {
        val takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION
                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        getContentResolver().releasePersistableUriPermission(uri, takeFlags)
    }

    /** 使用saf打开一个用户选择的文件 */
    override fun selectFile(
        pickerInitialUri: String?,
        fileType: String,
        block: noNullUriResult,/* = (uri: android.net.Uri) -> kotlin.Unit */
    ) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = fileType
            //可选，添加一个初始路径
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && pickerInitialUri != null) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
            }
        }
        startActivityForResult(intent) {
            it?.data.runSafelyNoNull(block)
        }
    }

    /**
     * @param fileName 文件名
     * @param fileType （可选）文件的类型[com.kiylx.store_lib.kit.MimeTypeConsts]
     * @param pickerInitialUri （可选）在您的应用创建文档之前，为应该在系统文件选择器中打开的目录指定一个 URI。
     * @param block 文件创建后，将会回调此匿名函数.如果intent或intent.getData()为null,则不会调用block块
     */
    override fun createFile(
        fileName: String,
        fileType: String,
        pickerInitialUri: String?,
        block: noNullUriResult, /* (uri: Uri) -> Unit */
    ) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = fileType
            putExtra(Intent.EXTRA_TITLE, fileName)
            //可选，添加一个初始路径
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && pickerInitialUri != null) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
            }
        }
        startActivityForResult(intent) {
            it?.data.runSafelyNoNull(block)
        }
    }

    override fun readFile(uri: Uri, input: input) {
        requireActivity().readFileFromUri(uri, input)
    }

    override fun writeFile(uri: Uri, output: output) {
        requireActivity().writeFileFromUri(uri, output)
    }

    override fun getContentResolver(): ContentResolver = requireActivity().contentResolver
}
