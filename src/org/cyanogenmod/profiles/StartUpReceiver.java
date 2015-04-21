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

    @Override
    public void onReceive(Context context, Intent intent) {
        // add ProfilesTrustAgent to list of trusted agents
        LockPatternUtils lockUtils = new LockPatternUtils(context);
        ComponentName profileTrustAgent = new ComponentName(context, ProfilesTrustAgent.class);

        List<ComponentName> enabledTrustAgents = lockUtils.getEnabledTrustAgents();
        if (!enabledTrustAgents.contains(profileTrustAgent)) {
            enabledTrustAgents.add(profileTrustAgent);
            lockUtils.setEnabledTrustAgents(enabledTrustAgents);
        }

        // disable the receiver once it has enabled ProfilesTrustAgent
        ComponentName name = new ComponentName(context, StartUpReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(name,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

}
