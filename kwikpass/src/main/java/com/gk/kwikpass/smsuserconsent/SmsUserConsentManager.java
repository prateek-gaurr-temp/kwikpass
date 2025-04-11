package com.gk.kwikpass.smsuserconsent;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import androidx.activity.result.ActivityResultLauncher;

import java.lang.ref.WeakReference;

public class SmsUserConsentManager {
    private static final String TAG = "SmsUserConsentManager";
    public static final int SMS_CONSENT_REQUEST = 2;
    
    public enum ErrorCode {
        NULL_ACTIVITY("1001", "Activity is null"),
        SMS_RETRIEVER_ERROR("1002", "Failed to start SMS listener"),
        NULL_BROADCAST_RECEIVER("1003", "Broadcast receiver is null"),
        COULD_NOT_HANDLE_BROADCAST("1004", "Could not handle broadcast"),
        REGISTRATION_ERROR("1005", "Failed to register broadcast receiver"),
        MAX_RETRIES_REACHED("1006", "Maximum retry attempts reached"),
        UNKNOWN_ERROR("1007", "An unknown error occurred");

        private final String code;
        private final String message;

        ErrorCode(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }

    // Retry configuration
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 1000; // 1 second
    private static final long MAX_RETRY_DELAY_MS = 10000; // 10 seconds
    private int currentRetryCount = 0;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final WeakReference<Activity> activityRef;
    private SmsBroadcastReceiver broadcastReceiver;
    private SmsConsentCallback callback;
    private ActivityResultLauncher<Intent> launcher;

    public interface SmsConsentCallback {
        void onSmsReceived(String sms);
        void onError(ErrorCode errorCode, String errorMessage);
    }

    public SmsUserConsentManager(Activity activity, SmsConsentCallback callback, ActivityResultLauncher<Intent> launcher) {
        this.activityRef = new WeakReference<>(activity);
        this.callback = callback;
        this.launcher = launcher;
        Log.d(TAG, "SmsUserConsentManager initialized");
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public void startSmsListener() throws SmsUserConsentException {
        Activity activity = activityRef.get();
        if (activity == null) {
            Log.e(TAG, ErrorCode.NULL_ACTIVITY.getMessage());
            throw new SmsUserConsentException(
                    ErrorCode.NULL_ACTIVITY,
                    ErrorCode.NULL_ACTIVITY.getMessage()
            );
        }

        try {
            Log.d(TAG, "Starting SMS User Consent API");
            startSmsUserConsentWithRetry(activity);
        } catch (Exception e) {
            Log.e(TAG, "Error starting SMS listener", e);
            throw new SmsUserConsentException(
                    ErrorCode.SMS_RETRIEVER_ERROR,
                    ErrorCode.SMS_RETRIEVER_ERROR.getMessage() + ": " + e.getMessage()
            );
        }
    }

    private void startSmsUserConsentWithRetry(Activity activity) {
        SmsRetriever.getClient(activity).startSmsUserConsent(null)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "SMS User Consent API started successfully");
                    currentRetryCount = 0; // Reset retry count on success
                    registerBroadcastReceiver(activity);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to start SMS User Consent API", e);
                    if (currentRetryCount < MAX_RETRIES) {
                        long delay = calculateRetryDelay();
                        Log.d(TAG, "Retrying in " + delay + "ms (Attempt " + (currentRetryCount + 1) + "/" + MAX_RETRIES + ")");
                        mainHandler.postDelayed(() -> {
                            currentRetryCount++;
                            startSmsUserConsentWithRetry(activity);
                        }, delay);
                    } else {
                        Log.e(TAG, ErrorCode.MAX_RETRIES_REACHED.getMessage());
                        if (callback != null) {
                            callback.onError(ErrorCode.MAX_RETRIES_REACHED, 
                                ErrorCode.MAX_RETRIES_REACHED.getMessage());
                        }
                    }
                });
    }

    private long calculateRetryDelay() {
        // Exponential backoff with jitter
        long delay = Math.min(INITIAL_RETRY_DELAY_MS * (long) Math.pow(2, currentRetryCount), MAX_RETRY_DELAY_MS);
        // Add jitter (random variation) to prevent thundering herd problem
        return delay + (long) (Math.random() * 1000);
    }

    private void registerBroadcastReceiver(Activity activity) {
        try {
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
            Log.e(TAG, "Error registering broadcast receiver", e);
            if (callback != null) {
                callback.onError(ErrorCode.REGISTRATION_ERROR, 
                    ErrorCode.REGISTRATION_ERROR.getMessage() + ": " + e.getMessage());
            }
        }
    }

    public void stopSmsListener() throws SmsUserConsentException {
        Activity activity = activityRef.get();
        if (activity == null) {
            Log.e(TAG, ErrorCode.NULL_ACTIVITY.getMessage());
            throw new SmsUserConsentException(
                    ErrorCode.NULL_ACTIVITY,
                    ErrorCode.NULL_ACTIVITY.getMessage()
            );
        }

        if (broadcastReceiver == null) {
            Log.d(TAG, "Broadcast receiver is already null, nothing to unregister");
            return;
        }

        try {
            Log.d(TAG, "Unregistering broadcast receiver");
            activity.unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
            Log.d(TAG, "Broadcast receiver unregistered successfully");
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error unregistering receiver", e);
            throw new SmsUserConsentException(
                    ErrorCode.NULL_BROADCAST_RECEIVER,
                    ErrorCode.NULL_BROADCAST_RECEIVER.getMessage()
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
            // Only restart on error (null callback)
            restartSmsListener();
        }
    }

    public void handleError(SmsUserConsentException e) {
        Log.e(TAG, "Handling error: " + e.getMessage());
        if (callback != null) {
            callback.onError(e.getErrorCode(), e.getMessage());
            Log.d(TAG, "Error callback triggered");
        } else {
            Log.e(TAG, "Callback is null");
        }
        // Always restart on error
        restartSmsListener();
    }

    public void startConsentIntent(Intent consentIntent) {
        if (launcher != null) {
            Log.d(TAG, "Starting consent intent with launcher");
            launcher.launch(consentIntent);
        } else {
            Log.e(TAG, "Launcher is null");
            handleError(new SmsUserConsentException(
                    ErrorCode.COULD_NOT_HANDLE_BROADCAST,
                    ErrorCode.COULD_NOT_HANDLE_BROADCAST.getMessage()
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