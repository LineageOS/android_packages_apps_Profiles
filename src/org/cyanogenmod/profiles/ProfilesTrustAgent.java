package org.cyanogenmod.profiles;

import android.app.Profile;
import android.app.ProfileManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.service.trust.TrustAgentService;
import android.util.Log;

import java.lang.ref.WeakReference;

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
        mProfileManager = (ProfileManager) getSystemService(Context.PROFILE_SERVICE);
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void handleApplyCurrentProfileState() {
        Profile p = mProfileManager.getActiveProfile();
        int lockscreenState = p != null ? p.getScreenLockMode() : Profile.LockMode.DEFAULT;
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
