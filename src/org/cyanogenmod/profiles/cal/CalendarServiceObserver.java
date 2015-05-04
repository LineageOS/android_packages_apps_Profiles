package org.cyanogenmod.profiles.cal;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.CalendarContract;
import android.util.ArraySet;
import android.util.Log;
import cyanogenmod.app.ProfilePluginManager;
import cyanogenmod.app.Trigger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class CalendarServiceObserver extends Service {

    private static final String TAG = CalendarServiceObserver.class.getSimpleName();
    public static final String TRIGGER_ID = "calendar_availability";
    public static final String PREFS = "calendars";
    public static final String PREFS_CALENDARS = "interested_when_busy";

    private ContentObserver mObserver;

    private List<BusyEvent> mEvents = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        registerTriggers(this);
        mObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                scheduleEvents();
            }
        };
        getContentResolver().registerContentObserver(CalendarContract.Instances.CONTENT_URI, true,
                mObserver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContentResolver().unregisterContentObserver(mObserver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand intent=" + intent);
        if (intent != null) {
            if (BusyEvent.ACTION_BUSY_START.equals(intent.getAction())) {
                int eventId = intent.getIntExtra("id", -1);
                Log.w(TAG, "BUSY_START for event: " + eventId);

                ProfilePluginManager.getInstance(this).sendTrigger(TRIGGER_ID, "busy");
            } else if (BusyEvent.ACTION_BUSY_END.equals(intent.getAction())) {
                int eventId = intent.getIntExtra("id", -1);
                Log.w(TAG, "BUSY_END for event: " + eventId);

                ProfilePluginManager.getInstance(this).sendTrigger(TRIGGER_ID, "available");
            } else {
                scheduleEvents();
            }
        } else {
            scheduleEvents();
        }
        return START_STICKY;
    }

    public static final void registerTriggers(Context context) {
        Trigger trigger = new Trigger(TRIGGER_ID, "Calendar Busy Trigger");
        trigger.addState(new Trigger.State("busy", "Busy"));
        trigger.addState(new Trigger.State("available", "Available"));
        ProfilePluginManager.getInstance(context).registerTrigger(trigger);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void scheduleEvents() {
        Log.d(TAG, "scheduleEvents");

        AlarmManager alarms = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // first cancel previous events we may have queued
        // we need to do this in case any calendar events were cancelled
        for (BusyEvent event : mEvents) {
            alarms.cancel(event.getPendingStartIntent(this));
            alarms.cancel(event.getPendingEndIntent(this));
        }
        mEvents.clear();

        String[] EVENT_PROJECTION = new String[]{
                CalendarContract.Events.TITLE,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Events.EVENT_LOCATION,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Events.ALL_DAY,
                CalendarContract.Calendars.OWNER_ACCOUNT,
        };

        ContentResolver resolver = getContentResolver();

        Uri.Builder eventsUriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon();

        // grab all scheduleEvents events in the next 24h
        ContentUris.appendId(eventsUriBuilder, System.currentTimeMillis());
        ContentUris.appendId(eventsUriBuilder, System.currentTimeMillis() + (1000 * 60 * 60 * 24));

        Uri eventUri = eventsUriBuilder.build();

        Cursor eventCursor = resolver.query(eventUri, EVENT_PROJECTION,
                null, null, CalendarContract.Instances.BEGIN + " ASC");
        SharedPreferences prefs = getSharedPreferences(
                CalendarServiceObserver.PREFS, 0);
        Set<String> stringSet = prefs.getStringSet(CalendarServiceObserver.PREFS_CALENDARS,
                new ArraySet<String>());

        while (eventCursor.moveToNext()) {
            String eventTitle = eventCursor.getString(0);
            String accountName = eventCursor.getString(1);
            String eventLocation = eventCursor.getString(2);
            long eventStart = eventCursor.getLong(3);
            long eventEnd = eventCursor.getLong(4);
            boolean allDay = eventCursor.getInt(5) != 0;
            String ownerAccount = eventCursor.getString(6);

            // TODO check this in a better way
            if (!stringSet.contains(ownerAccount + "/" + accountName)) continue;
            if (allDay) continue;

            BusyEvent event = new BusyEvent(eventTitle, eventStart, eventEnd, eventLocation);
            Log.d(TAG, "found and scheduling for event=" + event.toString());
            alarms.set(AlarmManager.RTC_WAKEUP, eventStart, event.getPendingStartIntent(this));
            alarms.set(AlarmManager.RTC_WAKEUP, eventEnd, event.getPendingEndIntent(this));

            mEvents.add(event);
        }
    }
}
