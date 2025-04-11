package com.gk.kwikpass.smsuserconsent;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

import java.lang.ref.WeakReference;

public class SmsBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsBroadcastReceiver";
    private final WeakReference<Activity> activityRef;
    private final SmsUserConsentManager manager;

    public SmsBroadcastReceiver(Activity activity, SmsUserConsentManager manager) {
        super();
        this.activityRef = new WeakReference<>(activity);
        this.manager = manager;
        Log.d(TAG, "SmsBroadcastReceiver initialized");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive called with action: " + intent.getAction());
        
        if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction())) {
            Bundle extras = intent.getExtras();
            if (extras == null) {
                Log.e(TAG, "Intent extras are null");
                manager.handleError(new SmsUserConsentException(
                        SmsUserConsentManager.ErrorCode.COULD_NOT_HANDLE_BROADCAST,
                        "Intent extras are null"
                ));
                return;
            }

            Status smsRetrieverStatus = (Status) extras.get(SmsRetriever.EXTRA_STATUS);
            if (smsRetrieverStatus == null) {
                Log.e(TAG, "SMS retriever status is null");
                manager.handleError(new SmsUserConsentException(
                        SmsUserConsentManager.ErrorCode.COULD_NOT_HANDLE_BROADCAST,
                        "SMS retriever status is null"
                ));
                return;
            }

            Intent consentIntent = extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT);
            if (consentIntent == null) {
                Log.e(TAG, "Consent intent is null");
                manager.handleError(new SmsUserConsentException(
                        SmsUserConsentManager.ErrorCode.COULD_NOT_HANDLE_BROADCAST,
                        "Consent intent is null"
                ));
                return;
            }

            Log.d(TAG, "Consent intent received: " + consentIntent.toString());

            switch (smsRetrieverStatus.getStatusCode()) {
                case CommonStatusCodes.SUCCESS:
                    try {
                        Activity activity = activityRef.get();
                        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                            Log.e(TAG, "Activity is null or destroyed");
                            manager.handleError(new SmsUserConsentException(
                                    SmsUserConsentManager.ErrorCode.COULD_NOT_HANDLE_BROADCAST,
                                    "Activity is not available"
                            ));
                            return;
                        }
                        Log.d(TAG, "Starting consent intent with launcher");
                        manager.startConsentIntent(consentIntent);
                        Log.d(TAG, "Consent intent started successfully");
                    } catch (android.content.ActivityNotFoundException e) {
                        Log.e(TAG, "Activity not found for consent intent", e);
                        manager.handleError(new SmsUserConsentException(
                                SmsUserConsentManager.ErrorCode.COULD_NOT_HANDLE_BROADCAST,
                                "No activity found to handle consent intent: " + e.getMessage()
                        ));
                    } catch (SecurityException e) {
                        Log.e(TAG, "Security exception while starting consent intent", e);
                        manager.handleError(new SmsUserConsentException(
                                SmsUserConsentManager.ErrorCode.COULD_NOT_HANDLE_BROADCAST,
                                "Security exception: " + e.getMessage()
                        ));
                    } catch (IllegalStateException e) {
                        Log.e(TAG, "Illegal state while starting consent intent", e);
                        manager.handleError(new SmsUserConsentException(
                                SmsUserConsentManager.ErrorCode.COULD_NOT_HANDLE_BROADCAST,
                                "Illegal state: " + e.getMessage()
                        ));
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to start consent intent", e);
                        manager.handleError(new SmsUserConsentException(
                                SmsUserConsentManager.ErrorCode.COULD_NOT_HANDLE_BROADCAST,
                                "Failed to start consent intent: " + e.getMessage()
                        ));
                    }
                    break;
                case CommonStatusCodes.TIMEOUT:
                    Log.e(TAG, "SMS retrieval timeout");
                    manager.handleError(new SmsUserConsentException(
                            SmsUserConsentManager.ErrorCode.COULD_NOT_HANDLE_BROADCAST,
                            "SMS was not retrieved in 5 minutes"
                    ));
                    break;
                default:
                    Log.e(TAG, "Unknown status code: " + smsRetrieverStatus.getStatusCode());
                    manager.handleError(new SmsUserConsentException(
                            SmsUserConsentManager.ErrorCode.COULD_NOT_HANDLE_BROADCAST,
                            "Unknown status code: " + smsRetrieverStatus.getStatusCode()
                    ));
                    break;
            }
        } else {
            Log.d(TAG, "Received intent with different action: " + intent.getAction());
        }
    }
}
