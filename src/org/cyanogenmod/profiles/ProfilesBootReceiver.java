package org.cyanogenmod.profiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.cyanogenmod.profiles.cal.CalendarServiceObserver;

public class ProfilesBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent().setClass(context, CalendarServiceObserver.class));
    }
}
