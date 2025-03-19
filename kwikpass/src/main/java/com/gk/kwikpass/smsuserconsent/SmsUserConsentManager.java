package com.gk.kwikpass.smsuserconsent;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import androidx.activity.result.ActivityResultLauncher;

public class SmsUserConsentManager {
    private static final String TAG = "SmsUserConsentManager";
    public static final int SMS_CONSENT_REQUEST = 2;

    private final Activity activity;
    private SmsBroadcastReceiver broadcastReceiver;
    private SmsConsentCallback callback;
    private ActivityResultLauncher<Intent> launcher;

    public interface SmsConsentCallback {
        void onSmsReceived(String sms);
        void onError(String errorCode, String errorMessage);
    }

    public SmsUserConsentManager(Activity activity, SmsConsentCallback callback, ActivityResultLauncher<Intent> launcher) {
        this.activity = activity;
        this.callback = callback;
        this.launcher = launcher;
        Log.d(TAG, "SmsUserConsentManager initialized");
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public void startSmsListener() throws SmsUserConsentException {
        if (activity == null) {
            Log.e(TAG, "Activity is null");
            throw new SmsUserConsentException(
                    Errors.NULL_ACTIVITY,
                    "Could not start SMS listener, activity is null"
            );
        }

        try {
            Log.d(TAG, "Starting SMS User Consent API");
            SmsRetriever.getClient(activity).startSmsUserConsent(null)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "SMS User Consent API started successfully");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to start SMS User Consent API", e);
                        if (callback != null) {
                            callback.onError(String.valueOf(Errors.SMS_RETRIEVER_ERROR), e.getMessage());
                        }
                    });

            broadcastReceiver = new SmsBroadcastReceiver(activity, this);
            Log.d(TAG, "Registering broadcast receiver");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                activity.registerReceiver(
                        broadcastReceiver,
                        new IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION),
                        SmsRetriever.SEND_PERMISSION,
                        null,
                        Context.RECEIVER_EXPORTED
                );
            } else {
                activity.registerReceiver(
                        broadcastReceiver,
                        new IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION),
                        SmsRetriever.SEND_PERMISSION,
                        null
                );
            }
            Log.d(TAG, "Broadcast receiver registered successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error starting SMS listener", e);
            throw new SmsUserConsentException(
                    Errors.SMS_RETRIEVER_ERROR,
                    "Failed to start SMS listener: " + e.getMessage()
            );
        }
    }

    public void stopSmsListener() throws SmsUserConsentException {
        if (activity == null) {
            Log.e(TAG, "Activity is null");
            throw new SmsUserConsentException(
                    Errors.NULL_ACTIVITY,
                    "Could not stop SMS listener, activity is null"
            );
        }

        if (broadcastReceiver == null) {
            Log.e(TAG, "Broadcast receiver is null");
            throw new SmsUserConsentException(
                    Errors.NULL_BROADCAST_RECEIVER,
                    "Could not stop SMS listener, broadcastReceiver is null"
            );
        }

        try {
            Log.d(TAG, "Unregistering broadcast receiver");
            activity.unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
            Log.d(TAG, "Broadcast receiver unregistered successfully");
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error unregistering receiver", e);
            throw new SmsUserConsentException(
                    Errors.NULL_BROADCAST_RECEIVER,
                    "Could not stop SMS listener, broadcastReceiver is null"
            );
        }
    }

    public void handleSms(String sms) {
        Log.d(TAG, "Handling SMS: " + sms);
        if (callback != null) {
            callback.onSmsReceived(sms);
            Log.d(TAG, "SMS callback triggered");
        } else {
            Log.e(TAG, "Callback is null");
        }
        restartSmsListener();
    }

    public void handleError(SmsUserConsentException e) {
        Log.e(TAG, "Handling error: " + e.getMessage());
        if (callback != null) {
            callback.onError(e.getCode().toString(), e.getMessage());
            Log.d(TAG, "Error callback triggered");
        } else {
            Log.e(TAG, "Callback is null");
        }
        restartSmsListener();
    }

    public void startConsentIntent(Intent consentIntent) {
        if (launcher != null) {
            Log.d(TAG, "Starting consent intent with launcher");
            launcher.launch(consentIntent);
        } else {
            Log.e(TAG, "Launcher is null");
            handleError(new SmsUserConsentException(
                    Errors.COULD_NOT_HANDLE_BROADCAST,
                    "Launcher is null"
            ));
        }
    }

    private void restartSmsListener() {
        try {
            Log.d(TAG, "Restarting SMS listener");
            stopSmsListener();
        } catch (SmsUserConsentException e) {
            handleError(e);
            return;
        }

        try {
            startSmsListener();
            Log.d(TAG, "SMS listener restarted successfully");
        } catch (SmsUserConsentException e) {
            handleError(e);
        }
    }
} 