package com.kiylx.camerax_lib.utils;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.kiylx.camerax_lib.R;


/**
 * 权限提示，筛选出要提示的权限，动态拼接展示
 * 真的需要这么高吗？
 *
 */
public class PermissionTipsDialog extends AlertDialog {
    private Context mContext;
    private TextView mUnConfirm, mContent, mBtnConfirm,mTitle;
    private String[] permissions;
    private PermissionCallBack callBack;
    private static boolean isShowingPermissionTipsDialog=false;

    public interface PermissionCallBack{
        void onContinue();
        void onCancel();
    }

    public PermissionTipsDialog(Context context, String[] permissions, PermissionCallBack callBack) {
        super(context);
        this.mContext = context;
        this.permissions=permissions;
        this.callBack=callBack;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setBackgroundDrawable(new BitmapDrawable());
        setContentView(R.layout.permission_tips_alert);
        this.setCancelable(false);

        mUnConfirm = findViewById(R.id.btn_close);
        mContent = findViewById(R.id.content);
        mTitle= findViewById(R.id.title);
        mBtnConfirm = findViewById(R.id.btn_confirm);

        // 防止是默认值是0 的
        try {
            mContent.setText(getPermissionTipsStr());
        }catch (Exception e){

        }finally {

        }

        mBtnConfirm.setOnClickListener(v -> {
            if(callBack!=null){
                callBack.onContinue();
                this.dismiss();
            }
        });

        mUnConfirm.setOnClickListener(v -> {
            if(callBack!=null){
                callBack.onCancel();
                this.dismiss();
            }
        });

    }


    private String getPermissionTipsStr(){
        StringBuilder stringBuilder=new StringBuilder();
        for(int i=0;i< permissions.length;i++){
            stringBuilder.append(getStrByKey(permissions[i]));
        }

        String tips=stringBuilder.toString();

        return tips.substring(0,tips.length()-4);
    }

    /**
     * 默认值0 是无效的值
     * 需要过滤已经授权过的吗？
     *
     * @param key
     * @return stringId
     */
    private String getStrByKey(String key){
        int stringId = mContext.getResources().getIdentifier(key,"string", mContext.getPackageName());
        if(stringId==0){
            return "";
        }else {
            return mContext.getString(stringId);
        }
    }


}