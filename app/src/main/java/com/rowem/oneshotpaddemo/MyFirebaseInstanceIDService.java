package com.rowem.oneshotpaddemo;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * Created by cjw on 2017-07-06.
 */
@Deprecated
public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    private static FCMRegistListener mFCMRegistListener;

    private static final String TAG = MyFirebaseInstanceIDService.class.getSimpleName();

    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);
        if (mFCMRegistListener != null) {
            mFCMRegistListener.onRegist(refreshedToken);
        }
    }

    public static void getToken(FCMRegistListener listener) {
        String token = FirebaseInstanceId.getInstance().getToken();
        if (token == null) {
            mFCMRegistListener = listener;
        } else {
            listener.onRegist(token);
        }
    }

    public interface FCMRegistListener {
        void onRegist(String token);
    }
}
