package com.rowem.oneshotpaddemo;

import android.util.Log;
import android.view.WindowManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.rowem.oneshotpadlib.data.PushInfo;

/**
 * Created by cjw on 2017-07-06.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private final String TAG = MyFirebaseMessagingService.class.getSimpleName();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.i(TAG, "onMessageReceived(" + remoteMessage + ")");

        SPinManager.getInstance().setPushInfo(getApplicationContext(), PushInfo.parsePushInfo(remoteMessage));
    }
}
