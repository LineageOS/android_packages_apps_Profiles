/*
 * Copyright (C) 2017 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cyanogenmod.profiles;

import java.util.UUID;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import cyanogenmod.app.ProfileManager;

public class ProfileLocaleFireReceiver extends BroadcastReceiver {

    private static final String TAG = "ProfileLocaleFireReceiver";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Bundle dataBundle = intent.getBundleExtra(ProfilePickerActivity.EXTRA_LOCALE_BUNDLE);
        if (dataBundle == null)
            return;

        String uuidStr = dataBundle.getString(ProfileManager.EXTRA_PROFILE_PICKED_UUID);

        UUID selectedProfile;
        try {
            selectedProfile = UUID.fromString(uuidStr);
        } catch (Exception e) {
            Log.e(TAG, "action for unparsable UUID received: " + uuidStr);
            return;
        }

        ProfileManager pm = ProfileManager.getInstance(context);
        pm.setActiveProfile(selectedProfile);
    }
}

