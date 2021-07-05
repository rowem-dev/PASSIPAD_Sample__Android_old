package com.rowem.oneshotpaddemo;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.rowem.oneshotpaddemo.dialog.BioDialog;
import com.rowem.oneshotpaddemo.dialog.InterfaceCodes;
import com.rowem.oneshotpaddemo.dialog.PinpadDialog;
import com.rowem.oneshotpaddemo.dialog.PinpadDialog.OnInputListener;
import com.rowem.oneshotpaddemo.sample.SpVerify;
import com.rowem.oneshotpaddemo.sample.SpVerifyListener;
import com.rowem.oneshotpaddemo.sample.SpVerifyResponse;
import com.rowem.oneshotpadlib.data.PushInfo;
import com.rowem.oneshotpadlib.manager.OneShotPadManager;
import com.rowem.oneshotpadlib.net.OneShotPadListener;
import com.rowem.oneshotpadlib.net.res.BaseResponse;
import com.rowem.oneshotpadlib.net.res.DataResponse;
import com.rowem.oneshotpadlib.net.res.SimpleResponse;
import com.rowem.oneshotpadlib.util.MLog;

public class SPinManager extends Handler {

    /*
     * 인증 시도 시 푸시가 수신되지 않았을 때,
     * 인자 값을 임시로 담아두기 위한 변수
     */
    private String mTempPwd;
    private String mTempAddCusId;
    private String mSignData;
    private String mCus_id;
    /*
     * 푸시 수신 시 담아두기 위한 객체
     */
    private PushInfo mPushInfo;

    private ProgressDialog mProgressDialog;

    private static SPinManager mInstance;

    private final int MSG_PUSH_TIMEOUT_START = 0;
    private final int MSG_PUSH_TIMEOUT_STOP = 1;
    private final int MSG_PUSH_TOKEN_UPD_INTERVAL_START = 2;
    private final int MSG_PUSH_TOKEN_UPD_INTERVAL_STOP = 3;

    private final long PUSH_TIMEOUT = 10 * 1000; // 푸시 타임아웃 시간
    private final long PUSH_TOKEN_UPD_INTERVAL = 10 * 1000; // 푸시 토큰 업데이트 요청 간격

    private final int RETRY_PUSH_TOKEN_UPD = 3; // 푸시 토큰 업데이트 요청 반복 횟수
    private int mPushTokenUpdCnt = 0; // 푸시 토큰 업데이트 재시도 횟수 체크하기 위한 변수 (증가 값)

    private final int RETRY_GET_PUSH_DATA = 3; // 푸시 데이터 요청 반복 횟수
    private int mGetPushDataCnt = 0; // 푸시 데이터 요청 횟수를 체크하기 위한 변수

    private final String KEY_CUS_ID = "cus_id";
    private boolean mIsPushTimeout = false;

    private static final String TAG = SPinManager.class.getSimpleName();

    /**
     * 싱글톤 객체 반환
     *
     * @return
     */
    public static SPinManager getInstance() {
        if (mInstance == null) {
            mInstance = new SPinManager();
        }
        return mInstance;
    }

    /**
     * 가입 상태 체크
     *
     * @param cus_id
     */
    public void reqCheckJoin(final Context ctx, String cus_id) {
        OneShotPadListener<SimpleResponse> l = new OneShotPadListener<SimpleResponse>() {
            @Override
            public void onResult(SimpleResponse res) {
                BaseResponse.printLog(res);
                showToast(ctx, res);
                // 결과 값 반환
            }
        };
        OneShotPadManager.getInstance().reqCheckJoin(ctx, cus_id, l);
    }

    /**
     * 생체 인증 가입 상태 체크
     *
     * @param cus_id
     * @param l
     */
    public void reqCheckBioJoin(final Context ctx, String cus_id, OneShotPadListener<SimpleResponse> l) {

        OneShotPadManager.getInstance().reqCheckJoin(ctx, cus_id, l);
    }

    /**
     * 가입 요청
     *
     * @param auth_token
     * @param push_token
     * @param cus_no
     * @param cus_id
     * @param sp_enc_key
     */
    public void reqJoin(final Context ctx, final String auth_token, final String push_token, final String cus_no, final String cus_id, final String sp_enc_key) {
        // 라이브러리 결과 값 리스너
        final OneShotPadListener<SimpleResponse> l = new OneShotPadListener<SimpleResponse>() {
            @Override
            public void onResult(SimpleResponse res) {
                BaseResponse.printLog(res);
                showToast(ctx, res);
                // 결과 값 반환
            }
        };
        // 핀패드 리스너
        OnInputListener pinpadListener = new OnInputListener() {
            @Override
            public void onStep1Finished(PinpadDialog dlg) {
                dlg.setLabel1("비밀번호 입력 확인");
                dlg.setLabel2("비밀번호를 한번 더 입력하세요.");
            }

            @Override
            public void onInvalidInput(PinpadDialog dlg, int code) {
                String msg = "";
                switch (code) {
                    case InterfaceCodes.ERR_TOO_SHORT:
                        msg = "비밀번호는 4자리여야 합니다.";
                        break;
                    case InterfaceCodes.ERR_NOT_MATCH:
                        msg = "비밀번호가 일치하지 않습니다.";
                        break;
                }
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onInputPw(PinpadDialog dlg, String pw) {
                dlg.dismiss();
                // 라이브러리 호출
                // RSA키 생성및 전자서명 전달
                OneShotPadManager.getInstance().reqProcJoinEx(ctx, auth_token, push_token, cus_no, cus_id, pw, sp_enc_key, l);
            }

            @Override
            public void onCanceled(PinpadDialog dlg) {
                dlg.dismiss();
            }
        };
        PinpadDialog dlg = PinpadDialog.getSetDialog(ctx, pinpadListener);
        dlg.setSubTitle("OneShotPad 가입");
        dlg.setLabel1("비밀번호 입력");
        dlg.setLabel2("비밀번호를 입력하세요.");
        dlg.show();
    }

    /**
     * 푸시 요청
     *
     * @param ctx
     * @param used_type
     * @param cus_id
     * @param add_cus_id
     */
    public void reqPush(final Context ctx, final String used_type, final String cus_id, final String add_cus_id, final String signData) {
        OneShotPadListener<DataResponse> l = new OneShotPadListener<DataResponse>() {
            @Override
            public void onResult(DataResponse res) {
                BaseResponse.printLog(res);
                showToast(ctx, res);
                // 결과 값 반환
                if (res.code.equals(BaseResponse.CD_OK)) {
                    startPushTimeout(ctx, cus_id); // 푸시 타임아웃 시작
                    if (used_type == OneShotPadManager.USED_TYPE_BIO_AUTH) {
                        showBioAuthPinpadDialog(ctx, add_cus_id, cus_id, signData);
                    } else {
                        showAuthPinpadDialog(ctx, add_cus_id, cus_id, signData);
                    }


                } else if (res.code.equals(BaseResponse.CD_PUSH_TOKEN)) {
                    initGcm(ctx, true);
                }
            }
        };
        OneShotPadManager.getInstance().reqPushEx(ctx, used_type, cus_id, add_cus_id, l);
    }

    /**
     * 비밀번호 인증 요청
     *
     * @param ctx
     * @param pwd
     * @param add_cus_id
     */
    private void reqAuthPinPad(final Context ctx, String pwd, final String add_cus_id, final String cus_id, final String signData) {
        OneShotPadListener<DataResponse> l = new OneShotPadListener<DataResponse>() {
            @Override
            public void onResult(final DataResponse res) {
                BaseResponse.printLog(res);
                showToast(ctx, res);
                if (res.code.equals(BaseResponse.CD_OK)) {
                    if (!OneShotPadManager.USED_TYPE_CLOSE.equals(mPushInfo.used_type)) { // 해지 요청이 아닌 경우
                        if (true/*certSign(res.cert, res.cert_pwd)*/) { // Sign..
                            Log.d(TAG, "Sign 성공!!");
                            //전자 서명 검증
                            //전자 서명 값이 있다면 검증 요청

                            // 서명값 리턴이 없어도 비밀번호 변경 로직은 유지
                            if (OneShotPadManager.USED_TYPE_CHANGE_PWD.equals(mPushInfo.used_type)) { // 비밀번호 변경
                                try {
                                    showChangePwdPinpadDialog(ctx,  cus_id, mPushInfo, res.sign, res.sign_text);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else if (OneShotPadManager.USED_TYPE_JOIN_BIO_AUTH.equals(mPushInfo.used_type)) {


                                OneShotPadListener<DataResponse> drl = new OneShotPadListener<DataResponse>() {
                                    @Override
                                    public void onResult(DataResponse dataResponse) {
                                        if (res.code.equals(BaseResponse.CD_OK)) {
                                            SpVerifyListener<SpVerifyResponse> l = new SpVerifyListener<SpVerifyResponse>() {
                                                @Override
                                                public void onResult(SpVerifyResponse sres) {
                                                    if (sres.resultData.getCode().equals(BaseResponse.CD_OK)) {
                                                        //검증 성공
                                                        Toast.makeText(ctx, "검증 성공", Toast.LENGTH_LONG).show();
                                                        //TODO 생체인증 가입 3-4 종료

                                                    }

                                                    mPushInfo = null;
                                                    mTempPwd = null;
                                                    mTempAddCusId = null;
                                                    mSignData = null;
                                                    mCus_id = null;
                                                }
                                            };
                                            SpVerify.getInstance(ctx).reqVerify("", res.cus_id, res.sign, res.sign_text, mPushInfo.auth_token, l);
                                        }
                                    }
                                };

                                showBioJoinPinpadDialog(ctx, mPushInfo, cus_id, signData, drl);


                            } else {
                                SpVerifyListener<SpVerifyResponse> l = new SpVerifyListener<SpVerifyResponse>() {
                                    @Override
                                    public void onResult(SpVerifyResponse sres) {
                                        if (sres.resultData.getCode().equals(BaseResponse.CD_OK)) {
                                            //검증 성공
                                            //Toast.makeText(ctx,"검증 성공",Toast.LENGTH_LONG).show();
                                            // 비밀번호 변경
                                            if (OneShotPadManager.USED_TYPE_CHANGE_PWD.equals(mPushInfo.used_type)) { // 비밀번호 변경
                                                try {
                                                    showChangePwdPinpadDialog(ctx,  cus_id, mPushInfo, res.sign, res.sign_text);
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            } else {

                                            }

                                        }

                                        mPushInfo = null;
                                        mTempPwd = null;
                                        mTempAddCusId = null;
                                        mSignData = null;
                                        mCus_id = null;
                                    }
                                };
                                SpVerify.getInstance(ctx).reqVerify("", res.cus_id, res.sign, res.sign_text, mPushInfo.auth_token, l);
                            }


                        }

                    } else {
                        Log.e(TAG, "Sign 실패..");
                    }
                }
            }

        };
        if (mPushInfo == null) {
            // 푸시가 아직 도착하지 않음
            /* 2016.12.06 cjw mIsPushTimeout 추가
             * 기존에는 인증 핀패드가 떠서 바로 인증 시도를 하지않고,
             * 푸시 타임아웃 시간 [20초] 이 지난 후에
             * 인증을 시도하게 되면, 무한 프로그레스 다이얼로그가 발생한다..
             * 이를 방지하기 위해 타임아웃 Flag 를 추가함.
             */
            if (mIsPushTimeout) { // 푸시 타임아웃이 종료되기 전..
                showPushProgressDialog(ctx, pwd, add_cus_id, cus_id, signData);
            } else {
                Toast.makeText(ctx, "간편인증 응답 실패.", Toast.LENGTH_SHORT).show();
            }
        } else {
            OneShotPadManager.getInstance().reqAuthPinPadEx(ctx, pwd, mPushInfo, add_cus_id, cus_id, signData, l);
        }

    }


    /**
     * 생체 인증 요청
     *
     * @param ctx
     * @param pwd
     * @param add_cus_id
     */
    private void reqAuthBio(final Context ctx, String pwd, String add_cus_id, final String cus_id, final String signData) {
        OneShotPadListener<DataResponse> l = new OneShotPadListener<DataResponse>() {
            @Override
            public void onResult(final DataResponse res) {
                BaseResponse.printLog(res);
                showToast(ctx, res);
                if (res.code.equals(BaseResponse.CD_OK)) {
                    if (!OneShotPadManager.USED_TYPE_CLOSE.equals(mPushInfo.used_type)) { // 해지 요청이 아닌 경우
                        if (true/*certSign(res.cert, res.cert_pwd)*/) { // Sign..
                            Log.d(TAG, "Sign 성공!!");

                            //전자 서명 검증
                            //전자 서명 값이 있다면 검증 요청


                            if (res.sign != null) {
                                //SpEncKey.getInstance(this).reqSpEncKey
                                SpVerifyListener<SpVerifyResponse> l = new SpVerifyListener<SpVerifyResponse>() {
                                    @Override
                                    public void onResult(SpVerifyResponse sres) {
                                        if (sres.resultData.getCode().equals(BaseResponse.CD_OK)) {
                                            //검증 성공
                                            Toast.makeText(ctx, "검증 성공", Toast.LENGTH_LONG).show();
                                            // 비밀번호 변경
                                            if (OneShotPadManager.USED_TYPE_CHANGE_PWD.equals(mPushInfo.used_type)) { // 비밀번호 변경
                                                try {
                                                    showChangePwdPinpadDialog(ctx, cus_id, mPushInfo, res.sign, res.sign_text);
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            } else {

                                            }
                                        }

                                        mPushInfo = null;
                                        mTempPwd = null;
                                        mTempAddCusId = null;
                                        mSignData = null;
                                        mCus_id = null;
                                    }
                                };
                                SpVerify.getInstance(ctx).reqVerify("", res.cus_id, res.sign, res.sign_text, mPushInfo.auth_token, l);
                            } else {
                                // 서명값 리턴이 없어도 비밀번호 변경 로직은 유지
                                if (OneShotPadManager.USED_TYPE_CHANGE_PWD.equals(mPushInfo.used_type)) { // 비밀번호 변경
                                    try {
                                        showChangePwdPinpadDialog(ctx, cus_id, mPushInfo, res.sign, res.sign_text);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                } else {

                                }


                                mPushInfo = null;
                                mTempPwd = null;
                                mTempAddCusId = null;
                                mSignData = null;
                                mCus_id = null;
                            }
                        } else {
                            Log.e(TAG, "Sign 실패..");
                        }
                    }
                }

            }
        };
        if (mPushInfo == null) { // 푸시가 아직 도착하지 않음
            /* 2016.12.06 cjw mIsPushTimeout 추가
             * 기존에는 인증 핀패드가 떠서 바로 인증 시도를 하지않고,
             * 푸시 타임아웃 시간 [20초] 이 지난 후에
             * 인증을 시도하게 되면, 무한 프로그레스 다이얼로그가 발생한다..
             * 이를 방지하기 위해 타임아웃 Flag 를 추가함.
             */
            if (mIsPushTimeout) { // 푸시 타임아웃이 종료되기 전..
                showPushProgressDialog(ctx, pwd, add_cus_id, cus_id, signData);
            } else {
                Toast.makeText(ctx, "간편인증 응답 실패.", Toast.LENGTH_SHORT).show();
            }
        } else {
            OneShotPadManager.getInstance().reqAuthBioPadEx(ctx, pwd, mPushInfo, add_cus_id, cus_id, signData, l);
        }
    }


    /**
     * 비밀번호 변경 요청
     *
     * @param ctx
     * @param pwd
     * @param push_info
     */
    private void reqChangePwd(final Context ctx, String pwd, String cus_id, final PushInfo push_info, String sign, String sign_text) {
        OneShotPadListener<DataResponse> l = new OneShotPadListener<DataResponse>() {
            @Override
            public void onResult(DataResponse res) {

                // 결과 값 반환
                BaseResponse.printLog(res);
                showToast(ctx, res);
                if (res.sign != null) {
                    SpVerifyListener<SpVerifyResponse> l = new SpVerifyListener<SpVerifyResponse>() {
                        @Override
                        public void onResult(SpVerifyResponse sres) {
                            if (sres.resultData.getCode().equals(BaseResponse.CD_OK)) {
                                //검증 성공
                                Toast.makeText(ctx, "검증 성공", Toast.LENGTH_LONG).show();
                            }
                        }
                    };
                    SpVerify.getInstance(ctx).reqVerify("", res.cus_id, res.sign, res.sign_text, push_info.auth_token, l);
                }

            }
        };
        OneShotPadManager.getInstance().reqChangePwdEx(ctx, pwd, cus_id, push_info, sign, sign_text, l);
    }

    /**
     * 푸시 토큰 갱신 요청
     *
     * @param ctx
     * @param push_token
     * @param isServer
     */
    public void reqUpdatePushToken(final Context ctx, final String push_token, final boolean isServer) {
        OneShotPadListener<SimpleResponse> l = new OneShotPadListener<SimpleResponse>() {
            @Override
            public void onResult(SimpleResponse res) {
                // 결과 값 반환
                BaseResponse.printLog(res);
                showToast(ctx, res);
                if (res.code.equals(BaseResponse.CD_OK)) {
                    // Count 증가 및 타임아웃 설정
                    mPushTokenUpdCnt++;
                    if (mPushTokenUpdCnt < RETRY_PUSH_TOKEN_UPD) { // 3회까지만.. 0,1,2..
                        startPushTokenUpdateTimeout(ctx, isServer);
                    } else {
                        stopPushTokenUpdateTimeout();
                    }
                }
            }
        };
        OneShotPadManager.getInstance().reqUpdatePushToken(ctx, push_token, isServer, l);
    }

    /**
     * 푸시 토큰 요청 및 갱신
     *
     * @param ctx
     */
    private void initGcm(final Context ctx, final boolean isServer) {
        // FCM token 요청
        MyFirebaseInstanceIDService.getToken(new MyFirebaseInstanceIDService.FCMRegistListener() {
            @Override
            public void onRegist(String token) {
                MLog.d("SPinManager", "FCM Token : " + token);
                reqUpdatePushToken(ctx, token, isServer);
            }
        });
    }

    /**
     * 인증 핀패드
     */
    private void showAuthPinpadDialog(final Context ctx, final String add_cus_id, final String cus_id, final String signData) {
        OnInputListener l = new OnInputListener() {
            @Override
            public void onStep1Finished(PinpadDialog dlg) {
            }

            @Override
            public void onInvalidInput(PinpadDialog dlg, int code) {
                String msg = "";
                switch (code) {
                    case InterfaceCodes.ERR_TOO_SHORT:
                        msg = "비밀번호는 4자리여야 합니다.";
                        break;
                    case InterfaceCodes.ERR_NOT_MATCH:
                        msg = "비밀번호가 일치하지 않습니다.";
                        break;
                }
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onInputPw(PinpadDialog dlg, String pw) {
                dlg.dismiss();
                reqAuthPinPad(ctx, pw, add_cus_id, cus_id, signData);
            }

            @Override
            public void onCanceled(PinpadDialog dlg) {
                dlg.dismiss();
            }
        };
        PinpadDialog dlg = PinpadDialog.getCertDialog(ctx, l);
        dlg.setSubTitle("OneShotPad 인증");
        dlg.setLabel1("비밀번호 입력");
        dlg.setLabel2("비밀번호를 입력하세요.");
        dlg.show();
    }


    /**
     * 생체인증 인증 핀패드
     */
    private void showBioAuthPinpadDialog(final Context ctx, final String add_cus_id, final String cus_id, final String signData) {
        BioDialog.OnInputListener l = new BioDialog.OnInputListener() {
            @Override
            public void onStep1Finished(BioDialog dlg) {

            }

            @Override
            public void onInvalidInput(BioDialog dlg, int code) {
                /*String msg = "";
                switch (code) {
                    case InterfaceCodes.ERR_TOO_SHORT:
                        msg = "비밀번호는 4자리여야 합니다.";
                        break;
                    case InterfaceCodes.ERR_NOT_MATCH:
                        msg = "비밀번호가 일치하지 않습니다.";
                        break;
                }
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();*/
            }

            @Override
            public void onInputPw(BioDialog dlg, String pw) {
                dlg.dismiss();
                reqAuthBio(ctx, pw, add_cus_id, cus_id, signData);
            }

            @Override
            public void onCanceled(BioDialog dlg) {
                dlg.dismiss();
            }
        };
        BioDialog dlg = BioDialog.getDialog(ctx, l);
        dlg.show();
    }


    /**
     * 생체인증 인증 가입 핀패드
     */
    private void showBioJoinPinpadDialog(final Context ctx, final PushInfo pushInfo, final String cus_id, final String signData, final OneShotPadListener<DataResponse> drl) {
        BioDialog.OnInputListener l = new BioDialog.OnInputListener() {
            @Override
            public void onStep1Finished(BioDialog dlg) {

            }

            @Override
            public void onInvalidInput(BioDialog dlg, int code) {
                /*String msg = "";
                switch (code) {
                    case InterfaceCodes.ERR_TOO_SHORT:
                        msg = "비밀번호는 4자리여야 합니다.";
                        break;
                    case InterfaceCodes.ERR_NOT_MATCH:
                        msg = "비밀번호가 일치하지 않습니다.";
                        break;
                }
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();*/
            }

            @Override
            public void onInputPw(BioDialog dlg, String pw) {
                dlg.dismiss();
                OneShotPadManager.getInstance().reqSign(ctx, pushInfo, cus_id, signData, drl);
            }

            @Override
            public void onCanceled(BioDialog dlg) {
                dlg.dismiss();
            }
        };
        BioDialog dlg = BioDialog.getDialog(ctx, l);
        dlg.show();
    }

    /**
     * 비밀번호 변경 핀패드
     *
     * @param push_info
     */
    private void showChangePwdPinpadDialog(final Context ctx,  final String cus_id, final PushInfo push_info, final String sign, final String sign_text) {
        OnInputListener l = new OnInputListener() {
            @Override
            public void onStep1Finished(PinpadDialog dlg) {
                dlg.setLabel1("비밀번호 입력 확인");
                dlg.setLabel2("비밀번호를 한번 더 입력하세요.");
            }

            @Override
            public void onInvalidInput(PinpadDialog dlg, int code) {
                String msg = "";
                switch (code) {
                    case InterfaceCodes.ERR_TOO_SHORT:
                        msg = "비밀번호는 4자리여야 합니다.";
                        break;
                    case InterfaceCodes.ERR_NOT_MATCH:
                        msg = "비밀번호가 일치하지 않습니다.";
                        break;
                }
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onInputPw(PinpadDialog dlg, String pw) {
                dlg.dismiss();
                reqChangePwd(ctx, pw, cus_id, push_info, sign, sign_text);
            }

            @Override
            public void onCanceled(PinpadDialog dlg) {
                dlg.dismiss();
            }
        };
        PinpadDialog dlg = PinpadDialog.getSetDialog(ctx, l);
        dlg.setSubTitle("OneShotPad 비밀번호 변경");
        dlg.setLabel1("비밀번호 입력");
        dlg.setLabel2("비밀번호를 입력하세요.");
        dlg.show();

    }

    /**
     * 인증 시도 시 Push 가 도착하지 않았을 경우 띄우는 Progress Dialog
     * 사용자로부터 받은 pwd 와, 서버에서 받은 cert_no 값을 임의로 저장.
     *
     * @param pwd
     */
    private void showPushProgressDialog(Context ctx, String pwd, String add_cus_id, String cus_id, String signData) {
        mTempPwd = pwd;
        mTempAddCusId = add_cus_id;
        mSignData = signData;
        mCus_id = cus_id;
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(ctx);
        }
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage("Push Wait..");
        mProgressDialog.show();
    }

    /**
     * Push 수신 시 Progress Dialog 가 떠있는지 체크하여,
     * 떠있을 경우 (Push 가 수신되기 전에 인증을 시도 한 상태) 인증 시도
     */
    private void dismissPushProgressDialog(Context ctx) {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            reqAuthPinPad(ctx, mTempPwd, mTempAddCusId, mCus_id, mSignData);
        }
    }
	

    private void retryPushData(final Context ctx, final String cus_id) {
        // Count 증가 및 타임아웃 설정
        mGetPushDataCnt++;
        if (mGetPushDataCnt < RETRY_GET_PUSH_DATA) { // 3회까지만.. 0,1,2..
            startPushTimeout(ctx, cus_id);
        } else {
            stopPushTimeout();
            dismissPushProgressDialog(ctx);
        }

    }

    /**
     * 푸시 데이터 전달
     *
     * @param ctx
     * @param push_info
     */
    public void setPushInfo(Context ctx, PushInfo push_info) {
        mPushInfo = push_info;
        if (mPushInfo != null) {
            // 푸시 토큰 업데이트
            if (OneShotPadManager.USED_TYPE_UPDATE_PUSH_TOKEN.equals(mPushInfo.used_type)) {
                stopPushTokenUpdateTimeout(); // 푸시 토큰 업데이트 핸들러 종료
                OneShotPadManager.getInstance().setUpdatePushToken(ctx, mPushInfo.token);
                // 푸시 토큰 업데이트가 완료 된 것을 사용자에게 알려주는 것이..
//				showSuccessPushTokenUpdateDialog(ctx);
            } else {
                stopPushTimeout(); // 푸시 타임아웃 종료
                dismissPushProgressDialog(ctx);
            }
        }
    }

    /**
     * 푸시 타임아웃 핸들러 시작
     *
     * @param ctx
     */
    private void startPushTimeout(Context ctx, String cus_id) {
        Log.i(TAG, "startPushTimeout(" + ctx + ")");
        mIsPushTimeout = true; // Push Timeout Flag 변경
        Bundle bundle = new Bundle();
        bundle.putString(KEY_CUS_ID, cus_id);
        Message msg = obtainMessage(MSG_PUSH_TIMEOUT_START, ctx);
        msg.setData(bundle);
        sendMessageDelayed(msg, PUSH_TIMEOUT);

    }

    /**
     * 푸시 타임아웃 핸들러 종료
     */
    private void stopPushTimeout() {
        Log.i(TAG, "stopPushTimeout()");
        mIsPushTimeout = false; // Push Timeout Flag 변경
        sendMessage(obtainMessage(MSG_PUSH_TIMEOUT_STOP));
    }

    /**
     * 푸시 토큰 업데이트 타임아웃 핸들러 시작
     *
     * @param ctx
     * @param isServer
     */
    private void startPushTokenUpdateTimeout(Context ctx, boolean isServer) {
        Log.i(TAG, "startPushTokenUpdateTimeout(" + ctx + ", " + isServer + ")");
        Message msg = obtainMessage(MSG_PUSH_TOKEN_UPD_INTERVAL_START, ctx);
        msg.arg1 = isServer ? 1 : 0; // isServer 값이 true 이면 1 아니면 0
        sendMessageDelayed(msg, PUSH_TOKEN_UPD_INTERVAL);
    }

    /**
     * 푸시 토큰 업데이트 타임아웃 핸들러 종료
     */
    private void stopPushTokenUpdateTimeout() {
        Log.i(TAG, "stopPushTokenUpdateTimeout()");
        sendMessage(obtainMessage(MSG_PUSH_TOKEN_UPD_INTERVAL_STOP));
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_PUSH_TIMEOUT_START:
                Log.d(TAG, "MSG_PUSH_TIMEOUT_START");

                Bundle bundle = msg.getData();
                String cus_id = bundle.getString(KEY_CUS_ID);
                Context context = (Context) msg.obj;
                retryPushData(context, cus_id);
                break;

            case MSG_PUSH_TIMEOUT_STOP:
                Log.d(TAG, "MSG_PUSH_TIMEOUT_STOP");
                mGetPushDataCnt = 0;
                // MSG_PUSH_TIMEOUT_START Message 제거
                removeMessages(MSG_PUSH_TIMEOUT_START);
                break;

            case MSG_PUSH_TOKEN_UPD_INTERVAL_START:
                Log.d(TAG, "MSG_PUSH_TOKEN_UPD_INTERVAL_START");
                Context ctx = (Context) msg.obj;
                initGcm(ctx, msg.arg1 == 1); // 1 이면 true 아니면 false
                break;

            case MSG_PUSH_TOKEN_UPD_INTERVAL_STOP:
                Log.d(TAG, "MSG_PUSH_TOKEN_UPD_INTERVAL_STOP");
                mPushTokenUpdCnt = 0;
                removeMessages(MSG_PUSH_TOKEN_UPD_INTERVAL_START);
                break;
        }
    }

    /**
     * 테스트용 토스트 메시지 출력
     *
     * @param ctx
     * @param res
     */
    private void showToast(Context ctx, BaseResponse res) {
        StringBuilder sb = new StringBuilder();
        String text = sb.append("code : ").append(res.code)
                .append("\nmessage : ").append(res.message)
                .toString();

        Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show();
    }



    public void bioAuthClose(final Context ctx, final String cus_no, final String cus_id) {
        OneShotPadListener<SimpleResponse> l = new OneShotPadListener<SimpleResponse>() {
            @Override
            public void onResult(SimpleResponse res) {
                // 결과 값 반환
                BaseResponse.printLog(res);
                showToast(ctx, res);

            }
        };

        OneShotPadManager.getInstance().bioAuthTermination(ctx, cus_no, cus_id, l);
    }
}
