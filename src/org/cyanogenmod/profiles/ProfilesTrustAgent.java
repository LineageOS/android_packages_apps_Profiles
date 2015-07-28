/*
* Copyright (C) 2015 The CyanogenMod Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.cyanogenmod.profiles;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.service.trust.TrustAgentService;
import android.util.Log;

import cyanogenmod.app.Profile;
import cyanogenmod.app.ProfileManager;

import java.lang.ref.WeakReference;

/**
 * Profiles Trust Agent
 *
 * Watches for changes in the current {@link Profile} and grants or revokes trust (whether
 * lock screen security is enforced).
 */
public class ProfilesTrustAgent extends TrustAgentService {

    private static final String TAG = ProfilesTrustAgent.class.getSimpleName();
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final int GRANT_DURATION_MS = 1000 * 60 * 5; // 5 minutes

    private static final int MSG_UPDATE_STATE = 100;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mHandler.sendEmptyMessage(MSG_UPDATE_STATE);
        }
    };

    private ProfileManager mProfileManager;
    private ProfileHandler mHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mProfileManager = ProfileManager.getInstance(this);
        mHandler = new ProfileHandler(ProfilesTrustAgent.this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ProfileManager.INTENT_ACTION_PROFILE_SELECTED);
        filter.addAction(ProfileManager.INTENT_ACTION_PROFILE_UPDATED);

        registerReceiver(mReceiver, filter);

        setManagingTrust(true);
    }

    @Override
    public void onDestroy() {
        mHandler = null;
        mProfileManager = null;
        setManagingTrust(false);
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public void onTrustTimeout() {
        mHandler.sendEmptyMessage(MSG_UPDATE_STATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void handleApplyCurrentProfileState() {
        final DevicePolicyManager devicePolicyManager =
                (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (devicePolicyManager != null && devicePolicyManager.requireSecureKeyguard()) {
            revokeTrust();
            return;
        }

        Profile p = mProfileManager.getActiveProfile();
        int lockscreenState = p != null ? p.getScreenLockMode().getValue()
                : Profile.LockMode.DEFAULT;
        switch (lockscreenState) {
            case Profile.LockMode.DISABLE:
            case Profile.LockMode.DEFAULT:
                if (DEBUG) Log.w(TAG, "revoking trust.");
                revokeTrust();
                break;
            case Profile.LockMode.INSECURE:
                if (DEBUG) Log.w(TAG, "granting trust for profile " + p.getName());
                grantTrust(getString(R.string.trust_by_profile), GRANT_DURATION_MS, false);
                break;
        }
    }

    private static class ProfileHandler extends Handler {
        private final WeakReference<ProfilesTrustAgent> mService;

        private ProfileHandler(ProfilesTrustAgent service) {
            this.mService = new WeakReference<ProfilesTrustAgent>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_STATE:
                    ProfilesTrustAgent service = mService.get();
                    if (service != null) {
                        service.handleApplyCurrentProfileState();
                    }
                    break;
            }
        }
    }
}
