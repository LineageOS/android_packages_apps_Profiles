package org.cyanogenmod.profiles;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import com.android.internal.widget.LockPatternUtils;

import java.util.List;

/**
 * Boot receiver which checks enables the ProfilesTrustAgent once, then disables itself.
 * We only need to do this once to make sure we don't override if it was disabled at a later point.
 */
public class StartUpReceiver extends BroadcastReceiver {

    private static String PREFS_FILE_NAME = "state";
    private static String KEY_ENABLED_ONCE = "enabled_trust_agent_once";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!enabledProfileAgentOnce(context)) {
            // add ProfilesTrustAgent to list of trusted agents
            LockPatternUtils lockUtils = new LockPatternUtils(context);
            ComponentName profileTrustAgent = new ComponentName(context, ProfilesTrustAgent.class);

            List<ComponentName> enabledTrustAgents = lockUtils.getEnabledTrustAgents();
            if (!enabledTrustAgents.contains(profileTrustAgent)) {
                enabledTrustAgents.add(profileTrustAgent);
                lockUtils.setEnabledTrustAgents(enabledTrustAgents);
            }

            context.getSharedPreferences(PREFS_FILE_NAME, 0)
                    .edit()
                    .putBoolean(KEY_ENABLED_ONCE, true)
                    .apply();
        }

        // disable the receiver once it has run once.
        ComponentName name = new ComponentName(context, StartUpReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(name,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    private boolean enabledProfileAgentOnce(Context context) {
        return context.getSharedPreferences(PREFS_FILE_NAME, 0)
                .getBoolean(KEY_ENABLED_ONCE, false);
    }

}
