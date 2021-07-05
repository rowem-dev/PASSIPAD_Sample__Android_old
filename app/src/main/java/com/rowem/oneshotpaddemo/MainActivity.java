package com.rowem.oneshotpaddemo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.iid.FirebaseInstanceId;
import com.rowem.oneshotpaddemo.sample.SpEncKey;
import com.rowem.oneshotpaddemo.sample.SpEncKeyListener;
import com.rowem.oneshotpaddemo.sample.SpEncKeyResponse;
import com.rowem.oneshotpadlib.crypto.RSACryptor;
import com.rowem.oneshotpadlib.manager.OneShotPadManager;
import com.rowem.oneshotpadlib.net.OneShotPadListener;
import com.rowem.oneshotpadlib.net.res.BaseResponse;
import com.rowem.oneshotpadlib.net.res.SimpleResponse;
import com.rowem.oneshotpadlib.util.MLog;

/**
 * Tarket SDK 26 update - added Runtime permission - android.permission.READ_EXTERNAL_STORAGE
 */
public class MainActivity extends AppCompatActivity {

    private EditText mEtCusNo;
    private EditText mEtCusId;
    private EditText mEtCertNo;
    private EditText mEtCertPwd;
    private EditText mEtAddCusId;

    private int mAuthType = 0; // Default 가입
    private String mPushToken;
    private String mAuthToken;
    private String mSpEncKey;

    private final String CUS_NO = "1234567809"; // 고객번호
    private final String CUS_ID = "testid_a2"; // 고객 ID
    private final String ADD_CUS_ID = "test_id_a2"; // 추가등록 고객 ID
    private final String CERT_NO = "136496678"; // 테스트용 인증서 시리얼번호
    private final String CERT_PWD = "1q2w3e4r5t@"; // 테스트용 인증서 암호
    private final String APP_ID = "android"; // 앱 아이디


      private String mBaseUrl = "http://192.168.10.24:8080";
//    private String mBaseUrl = "http://passipad.com";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MLog.PRINT_LOG = true;
        OneShotPadManager.getInstance().setBaseUrl(mBaseUrl);
        OneShotPadManager.getInstance().setAppType("1");

        RSACryptor.getInstance().init(this);

        init();
    }

    /**
     * 초기화
     */
    private void init() {
        mEtCusNo = (EditText) findViewById(R.id.et_cus_no);
        mEtCusNo.setText(CUS_NO);
        mEtCusId = (EditText) findViewById(R.id.et_cus_id);
        mEtCusId.setText(CUS_ID);
        mEtCertNo = (EditText) findViewById(R.id.et_cert_no);
        mEtCertNo.setText(CERT_NO);
        mEtCertPwd = (EditText) findViewById(R.id.et_cert_pwd);
        mEtCertPwd.setText(CERT_PWD);
        mEtAddCusId = (EditText) findViewById(R.id.et_add_cus_id);
        mEtAddCusId.setText(ADD_CUS_ID);

        findViewById(R.id.btn_auth_type_sel).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_req_sp_enc_key).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_check_join).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_proc_join).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_req_login_push).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_req_cert_login_push).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_req_change_pwd_push).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_req_close_push).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_check_token).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_add_cus_id).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_req_join_bio_auth).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_req_bio_auth).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_bio_close).setOnClickListener(mOnClickListener);

        checkPermission();
        //RSACryptor.getInstance().init(this);

    }

    private void checkPermission(){
        int check = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if(check != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1111);
            return;
        }

        initFcm();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean result = true;
        for(int i=0; i<permissions.length; i++){
            if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                if(!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permissions[i])){
                    finish();
                    return;
                }
                result = false;
                break;
            }
        }

        if(result) initFcm();
    }

    private void initFcm() {
        // FCM token 요청
        MyFirebaseInstanceIDService.getToken(new MyFirebaseInstanceIDService.FCMRegistListener() {
            @Override
            public void onRegist(String token) {
                MLog.d("MainActivity", "FCM Token : " + token);
                mPushToken = token;
            }
        });

        String token = FirebaseInstanceId.getInstance().getToken();
        if (!TextUtils.isEmpty(token)) {
            reqCheckToken();
        }
    }

    /**
     * 버튼 클릭 리스너
     */
    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_auth_type_sel:
                    showSelDialog();
                    break;
                case R.id.btn_req_sp_enc_key:
                    reqSpEncKey();
                    break;
                case R.id.btn_check_join:
                    reqCheckJoin();
                    break;
                case R.id.btn_proc_join:
                    reqProcJoin();
                    break;
                case R.id.btn_req_login_push:
                    reqPush(OneShotPadManager.USED_TYPE_LOGIN, "passipad USED_TYPE_LOGIN");
                    break;
                case R.id.btn_req_cert_login_push:
                    reqPush(OneShotPadManager.USED_TYPE_CERT_LOGIN, "passipad USED_TYPE_CERT_LOGIN");
                    break;
                case R.id.btn_req_join_bio_auth:
                    reqPush(OneShotPadManager.USED_TYPE_JOIN_BIO_AUTH, "passipad USED_TYPE_JOIN_BIO_AUTH");
                    break;
                case R.id.btn_req_bio_auth:
                        reqBioAuth();
                    break;
                case R.id.btn_req_change_pwd_push:
                    reqPush(OneShotPadManager.USED_TYPE_CHANGE_PWD, "passipad USED_TYPE_CHANGE_PWD");
                    break;
                case R.id.btn_req_close_push:
                    reqPush(OneShotPadManager.USED_TYPE_CLOSE, "passipad USED_TYPE_CLOSE");
                    break;
                case R.id.btn_check_token:
                    initFcm();
                    break;
                case R.id.btn_add_cus_id:
                    reqPush(OneShotPadManager.USED_TYPE_ADD_CUS_ID, "passipad USED_TYPE_ADD_CUS_ID");
                    break;
                case R.id.btn_bio_close:
                    //생체인증 해지
                    //bioAuthClose();
                    reqPush(OneShotPadManager.USED_TYPE_BIO_CLOSE, "passipad USED_TYPE_BIO_CLOSE");

                    break;


            }
        }
    };

    /**
     * 인증타입 선택 Dialog
     */
    private void showSelDialog() {
        final CharSequence[] choiceList = { "가입", "로그인", "이체", "비밀번호변경", "해지" };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("인증타입선택").setCancelable(false).setSingleChoiceItems(choiceList, mAuthType,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mAuthType = which;
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * SP 암호키 및 인증토큰 요청 (향후 사이트 서버에서 받아와야 하는 부분..)
     */
    private void reqSpEncKey() {
        String cus_no = mEtCusNo.getText().toString();
        if (TextUtils.isEmpty(cus_no)) {
            Toast.makeText(this, "고객번호를 입력해주세요..", Toast.LENGTH_SHORT).show();
            return;
        }

        SpEncKeyListener<SpEncKeyResponse> l = new SpEncKeyListener<SpEncKeyResponse>() {
            @Override
            public void onResult(SpEncKeyResponse res) {
                setSpEncKeyData(res);
            }
        };
        SpEncKey.getInstance(this).reqSpEncKey(null, cus_no, String.valueOf(mAuthType + 1), l);
    }

    /**
     * SP 암호키 및 인증토큰 요청해서 받아온 데이터 설정 및 출력
     *
     * @param res
     */
    private void setSpEncKeyData(SpEncKeyResponse res) {
        mAuthToken = res.auth_token;
        mSpEncKey = res.sp_enc_key;

        StringBuilder sb = new StringBuilder();
        String text = sb.append("code : ").append(res.code)
                .append("\nmessage : ").append(res.message)
                .append("\ncus_no : ").append(res.cus_no)
                .append("\nauth_token : ").append(res.auth_token)
                .append("\nsp_enc_key : ").append(res.sp_enc_key)
                .toString();

        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    /**
     * OneShotPad 사용 설정상태 체크
     */
    private void reqCheckJoin() {
        String cus_id = mEtCusId.getText().toString();

        SPinManager.getInstance().reqCheckJoin(this, cus_id);
    }

    /**
     * OneShotPad 가입요청
     */
    private void reqProcJoin() {
        String cus_no = mEtCusNo.getText().toString();
        String cus_id = mEtCusId.getText().toString();

        SPinManager.getInstance().reqJoin(this, mAuthToken,  mPushToken, cus_no, cus_id,  mSpEncKey);
    }

    /**
     * OneShotPad Push 요청
     *
     * @param used_type
     */
    private void reqPush(final String used_type, String signData) {
        String cus_id = mEtCusId.getText().toString();
        //String cert_no = mEtCertNo.getText().toString();
        String add_cus_id = mEtAddCusId.getText().toString();

        SPinManager.getInstance().reqPush(this, used_type, cus_id,  add_cus_id, signData);
    }

    /**
     * OneShotPad 푸시 토큰 변경 체크 및 업데이트
     */
    private void reqCheckToken() {
        SPinManager.getInstance().reqUpdatePushToken(this, mPushToken, false);
    }


    /**
     * OneShotPad 생체인증 해지
     */
    private void bioAuthClose() {
        String cus_no = mEtCusNo.getText().toString();
        String cus_id = mEtCusId.getText().toString();

        SPinManager.getInstance().bioAuthClose(this,  cus_no, cus_id );
    }

    private void reqBioAuth() {
        OneShotPadListener<SimpleResponse> l = new OneShotPadListener<SimpleResponse>() {
            @Override
            public void onResult(SimpleResponse res) {
                BaseResponse.printLog(res);
                // 결과 값
                // 생체인증 가입 자이면 푸시 요청
                if(res.code.equals(BaseResponse.REGISTERED_SUCCESS_AND_USED_BIO)){
                    reqPush(OneShotPadManager.USED_TYPE_BIO_AUTH, "passipad USED_TYPE_BIO_AUTH");
                }else if(res.code.equals(BaseResponse.CD_NORMAL_USER)){
                    Toast.makeText(MainActivity.this, "생체인증에 가입해 주세요", Toast.LENGTH_SHORT).show();
                }
            }
        };
        String cus_id = mEtCusId.getText().toString();
        SPinManager.getInstance().reqCheckBioJoin(this, cus_id, l);
    }

}
