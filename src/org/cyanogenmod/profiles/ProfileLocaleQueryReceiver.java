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
import cyanogenmod.app.Profile;

public class ProfileLocaleQueryReceiver extends BroadcastReceiver {

    private static final int RESULT_CONDITION_SATISFIED = 16;
    private static final int RESULT_CONDITION_UNKNOWN = 18;
    private static final int RESULT_CONDITION_UNSATISFIED = 17;

    private static final String TAG = "ProfileLocaleQueryReceiver";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Bundle dataBundle = intent.getBundleExtra(ProfilePickerActivity.EXTRA_LOCALE_BUNDLE);
        if (dataBundle == null) {
            setResultCode(RESULT_CONDITION_UNKNOWN);
            return;
        }

        String uuidStr = dataBundle.getString(ProfileManager.EXTRA_PROFILE_PICKED_UUID);

        UUID queryProfileUUID;
        try {
            queryProfileUUID = UUID.fromString(uuidStr);
        } catch (Exception e) {
            Log.e(TAG, "query for unparsable UUID received: " + uuidStr);
            setResultCode(RESULT_CONDITION_UNKNOWN);
            return;
        }

        ProfileManager pm = ProfileManager.getInstance(context);
        Profile activeProfile = pm.getActiveProfile();
        if (activeProfile == null) {
            Log.e(TAG, "no profile active!");
            setResultCode(RESULT_CONDITION_UNSATISFIED);
            return;
        }

        UUID activeUUID = activeProfile.getUuid();
        if (!queryProfileUUID.equals(activeUUID)) {
            setResultCode(RESULT_CONDITION_UNSATISFIED);
            return;
        }

        setResultCode(RESULT_CONDITION_SATISFIED);
        return;
    }
}

