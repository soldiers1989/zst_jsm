package com.zst.ynh.widget.person.certification.Identity;

import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.alibaba.fastjson.JSON;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.megvii.idcardquality.IDCardQualityLicenseManager;
import com.megvii.licensemanager.Manager;
import com.megvii.livenessdetection.LivenessLicenseManager;
import com.zst.ynh.R;
import com.zst.ynh.bean.FaceResultBean;
import com.zst.ynh.bean.IdCardResultBean;
import com.zst.ynh.bean.PersonInfoBean;
import com.zst.ynh.config.ArouterUtil;
import com.zst.ynh.config.BundleKey;
import com.zst.ynh.config.SPkey;
import com.zst.ynh.event.CertificationEvent;
import com.zst.ynh.event.StringEvent;
import com.zst.ynh.megvii.livenesslib.util.ConUtil;
import com.zst.ynh.utils.DialogUtil;
import com.zst.ynh.utils.WeakHandler;
import com.zst.ynh.view.IdentifyCertificationTipDialog;
import com.zst.ynh_base.mvp.view.BaseActivity;
import com.zst.ynh_base.util.Layout;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * 身份认证页面
 */
@Route(path = ArouterUtil.IDENTITY_CERTIFICATION)
@Layout(R.layout.activity_identity_certification_layout)
public class IdentityCertificationActivity extends BaseActivity implements IIdentityCertificationView {
    @BindView(R.id.iv_face)
    ImageView ivFace;
    @BindView(R.id.iv_id_front)
    ImageView ivIdFront;
    @BindView(R.id.iv_id_back)
    ImageView ivIdBack;
    @BindView(R.id.et_card_name)
    EditText etCardName;
    @BindView(R.id.et_card_number)
    EditText etCardNumber;
    @BindView(R.id.ll_detail)
    LinearLayout llDetail;
    @BindView(R.id.btn_save)
    Button btnSave;

    //这是判断是否从认证页面来的 true:下方按钮显示下一步； false:下方按钮显示保存
    @Autowired(name = BundleKey.IS_FROM_TOCERTIFICATION)
    boolean isFromToCertification;
    private IdentityCertificationPresent identityCertificationPresent;
    private int mask_bck = R.mipmap.bg_success;
    private boolean isChanged;//是否进行了改变 如果改变了 返回的时候要弹窗
    private IdentifyCertificationTipDialog identifyCertificationTipDialog;
    //人脸
    private final int FACE_TYPE = 1;
    //身份证前面
    private final int ID_CARD_TYPE_FRONT = 2;
    //身份证后面
    private final int ID_CARD_TYPE_BACK = 3;
    private WeakHandler weakHandler;
    private String faceImgUrl;
    private String IdCardFrontImgUrl;
    private String IdCardBackImgUrl;
    private int realVerifyStatus;
    private boolean isShowTipDialog;//这个是弹窗是否显示过

    @Override
    public void getPersonInfoData(PersonInfoBean personInfoBean) {
        loadContentView();
        realVerifyStatus = personInfoBean.item.real_verify_status;
        if (personInfoBean.item.real_verify_status == 1) {
            //如果已经认证 edittext不能点击
            etCardName.setEnabled(false);
            etCardName.setFocusable(false);
            etCardNumber.setEnabled(false);
            etCardNumber.setFocusable(false);
        }
        //设置下方的名字与身份证号
        if (!TextUtils.isEmpty(personInfoBean.item.name) && !TextUtils.isEmpty(personInfoBean.item.id_number)) {
            llDetail.setVisibility(View.VISIBLE);
            etCardName.setText(personInfoBean.item.name);
            etCardNumber.setText(personInfoBean.item.id_number);
        }
        //人脸
        changeBitmap(TextUtils.isEmpty(personInfoBean.item.face_recognition_picture), ivFace, personInfoBean);
        //身份证正面
        changeBitmap(TextUtils.isEmpty(personInfoBean.item.id_number_z_picture), ivIdFront, personInfoBean);
        //身份证背面
        changeBitmap(TextUtils.isEmpty(personInfoBean.item.id_number_f_picture), ivIdBack, personInfoBean);
    }

    @Override
    public void showLoadingProgressView() {
        showLoadingView();
    }

    @Override
    public void hideLoadingProgressView() {
        hideLoadingView();
    }


    /**
     * 改变imageview的图片 根据状态
     *
     * @param isUrlEmpty
     * @param imageView
     */
    private void changeBitmap(boolean isUrlEmpty, ImageView imageView, PersonInfoBean personInfoBean) {
        if (isUrlEmpty) {
            imageView.setEnabled(true);
            if (imageView == ivIdFront) {
                etCardNumber.setText("");
                etCardName.setText("");
            }
        } else {
            if (personInfoBean.item.real_verify_status == 1) {//等于1的代表 人脸以及身份证都上传成功
                imageView.setEnabled(false);
            } else {
                imageView.setEnabled(true);
            }
            imageView.setImageResource(mask_bck);
        }
    }

    @Override
    protected boolean isUseEventBus() {
        return true;
    }

    /**
     * 扫描成功的回调
     *
     * @param certificationEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(CertificationEvent certificationEvent) {
        switch (certificationEvent.getType()) {
            case FACE_TYPE:
                FaceResultBean faceResultBean = JSON.parseObject(certificationEvent.getData(), FaceResultBean.class);
                if (faceResultBean != null && faceResultBean.imgs.size() > 0) {
                    ToastUtils.showShort(faceResultBean.result);
                    int resID = faceResultBean.resultcode;
                    if (resID == R.string.verify_success) {
                        faceImgUrl = "file://" + faceResultBean.imgs.get(0);
                        ivFace.setEnabled(false);
                        ivFace.setImageResource(mask_bck);
                        identityCertificationPresent.uploadPicture(Uri.parse(faceImgUrl));
                    }
                } else {
                    ToastUtils.showShort("图片获取失败");
                }
                break;
            case ID_CARD_TYPE_FRONT:
                IdCardResultBean idCardFrontResultBean = JSON.parseObject(certificationEvent.getData(), IdCardResultBean.class);
                if (idCardFrontResultBean != null) {
                    ToastUtils.showShort(idCardFrontResultBean.result);
                    IdCardFrontImgUrl = "file://" + idCardFrontResultBean.idcardImg;
                    ivIdFront.setEnabled(false);
                    ivIdFront.setImageResource(mask_bck);
                    identityCertificationPresent.uploadPicture(Uri.parse(IdCardFrontImgUrl));
                } else {
                    ToastUtils.showShort("图片获取失败");
                }
                break;
            case ID_CARD_TYPE_BACK:
                IdCardResultBean idCardBackResultBean = JSON.parseObject(certificationEvent.getData(), IdCardResultBean.class);
                if (idCardBackResultBean != null) {
                    ToastUtils.showShort(idCardBackResultBean.result);
                    IdCardFrontImgUrl = "file://" + idCardBackResultBean.idcardImg;
                    ivIdBack.setEnabled(false);
                    ivIdBack.setImageResource(mask_bck);
                    identityCertificationPresent.uploadPicture(Uri.parse(IdCardFrontImgUrl));
                } else {
                    ToastUtils.showShort("图片获取失败");
                }
                break;
        }
    }


    @Override
    public void onRetry() {
        identityCertificationPresent.getPersonInfo();
    }

    @Override
    public void initView() {
        ARouter.getInstance().inject(this);
        mTitleBar.setTitle("身份认证");
        identityCertificationPresent = new IdentityCertificationPresent();
        identityCertificationPresent.attach(this);
        identityCertificationPresent.getPersonInfo();
        if (isFromToCertification) {
            btnSave.setText("下一步");
            btnSave.setEnabled(true);
        } else {
            btnSave.setText("保存");
            btnSave.setEnabled(false);
        }
        handlerMessage();
    }

    private void handlerMessage() {
        weakHandler = new WeakHandler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case FACE_TYPE:
                        ARouter.getInstance().build(ArouterUtil.LIVENESS_FACE).withInt(BundleKey.TYPE, FACE_TYPE).navigation();
                        break;
                    case ID_CARD_TYPE_FRONT:
                        ARouter.getInstance().build(ArouterUtil.ID_CARD_SACN).withInt(BundleKey.SIDE, 0)
                                .withBoolean(BundleKey.ISVERTICAL, false).withInt(BundleKey.TYPE, ID_CARD_TYPE_FRONT).navigation();
                        break;
                    case ID_CARD_TYPE_BACK:
                        ARouter.getInstance().build(ArouterUtil.ID_CARD_SACN).withInt(BundleKey.SIDE, 0)
                                .withBoolean(BundleKey.ISVERTICAL, false).withInt(BundleKey.TYPE, ID_CARD_TYPE_FRONT).navigation();
                        break;
                }
                return true;
            }
        });
    }

    @Override
    public void showLoading() {
        loadLoadingView();
    }

    @Override
    public void hideLoading() {

    }

    @Override
    public void ToastErrorMessage(String msg) {
        loadErrorView();
        ToastUtils.showShort(msg);
    }

    @OnClick({R.id.iv_face, R.id.iv_id_front, R.id.iv_id_back, R.id.btn_save})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.iv_face:
                //如果已经确定不显示提示弹窗
                if (SPUtils.getInstance().getBoolean(SPkey.TIP_SELECTED)||isShowTipDialog) {
                    toCertificationFromType(FACE_TYPE);
                } else {
                    identifyCertificationTipDialog = new IdentifyCertificationTipDialog(this);
                    identifyCertificationTipDialog.callBack(new IdentifyCertificationTipDialog.ICbSelect() {
                        @Override
                        public void setIsSelected(boolean isSelected) {
                            if (isSelected == true)
                                SPUtils.getInstance().put(SPkey.TIP_SELECTED, isSelected);
                        }
                    }, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            isShowTipDialog=true;
                            DialogUtil.hideDialog(identifyCertificationTipDialog);
                        }
                    });
                    identifyCertificationTipDialog.show();
                }
                break;
            case R.id.iv_id_front:
                toCertificationFromType(ID_CARD_TYPE_FRONT);
                break;
            case R.id.iv_id_back:
                toCertificationFromType(ID_CARD_TYPE_BACK);
                break;
            case R.id.btn_save:
//                identityCertificationPresent.saveMessage();
                break;
        }
    }

    /**
     * 根据type进行不同操作验证
     *
     * @param type
     */
    private void toCertificationFromType(int type) {
        final String uuid = ConUtil.getUUIDString(this);
        Manager manager = new Manager(this);
        switch (type) {
            case FACE_TYPE:
                LivenessLicenseManager licenseManager = new LivenessLicenseManager(this);
                manager.registerLicenseManager(licenseManager);
                manager.takeLicenseFromNetwork(uuid);
                if (licenseManager.checkCachedLicense() > 0)
                    weakHandler.sendEmptyMessage(FACE_TYPE);
                else
                    weakHandler.sendEmptyMessage(4);
                break;
            case ID_CARD_TYPE_FRONT:
            case ID_CARD_TYPE_BACK:
                IDCardQualityLicenseManager idCardLicenseManager = new IDCardQualityLicenseManager(this);
                manager.registerLicenseManager(idCardLicenseManager);
                manager.takeLicenseFromNetwork(uuid);
                if (idCardLicenseManager.checkCachedLicense() > 0)
                    weakHandler.sendEmptyMessage(type);
                else
                    weakHandler.sendEmptyMessage(4);
                break;

        }
    }
}