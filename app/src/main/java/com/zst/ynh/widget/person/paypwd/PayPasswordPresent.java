package com.zst.ynh.widget.person.paypwd;

import com.zst.ynh.config.ApiUrl;
import com.zst.ynh_base.mvp.present.BasePresent;
import com.zst.ynh_base.net.BaseParams;
import com.zst.ynh_base.net.HttpManager;

import java.util.Map;

public class PayPasswordPresent extends BasePresent<IPayPasswordView> {
    /**
     * 设置交易密码
     */
    public void setPwd(String pwd){
        mView.showLoading();
        Map<String,String> map=BaseParams.getBaseParams();
        httpManager.executePostString(ApiUrl.SET_PAY_PWD, map, new HttpManager.ResponseCallBack<String>() {

            @Override
            public void onCompleted() {
                mView.hideLoading();
            }

            @Override
            public void onError(int code,String errorMSG) {
                mView.ToastErrorMessage(errorMSG);
            }

            @Override
            public void onSuccess(String response) {
                mView.setPayPwdSuccess();
            }
        });
    }
}
