/*
 * SPDX-FileCopyrightText: 2015 The CyanogenMod Project
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.profiles;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import com.android.internal.widget.LockPatternUtils;

import java.util.ArrayList;
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

        List<ComponentName> enabledTrustAgents =
                lockUtils.getEnabledTrustAgents(UserHandle.USER_SYSTEM);
        if (enabledTrustAgents == null) {
            enabledTrustAgents = new ArrayList<>();
        }
        if (!enabledTrustAgents.contains(profileTrustAgent)) {
            enabledTrustAgents.add(profileTrustAgent);
            lockUtils.setEnabledTrustAgents(enabledTrustAgents, UserHandle.USER_SYSTEM);
        }

        // disable the receiver once it has enabled ProfilesTrustAgent
        ComponentName name = new ComponentName(context, StartUpReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(name,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

}
