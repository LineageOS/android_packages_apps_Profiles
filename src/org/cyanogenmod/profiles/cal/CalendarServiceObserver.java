package org.cyanogenmod.profiles.cal;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.provider.CalendarContract;
import android.util.Log;
import cyanogenmod.app.profiles.ProfilePluginManager;
import cyanogenmod.app.profiles.Trigger;

public class CalendarServiceObserver extends Service {

    private static final String TAG = CalendarServiceObserver.class.getSimpleName();
    public static final String TRIGGER_ID = "calendar_availability";

    @Override
    public void onCreate() {
        super.onCreate();
        registerTriggers(this);
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
        return START_NOT_STICKY;
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
        String[] EVENT_PROJECTION = new String[]{
                CalendarContract.Events.TITLE,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Events.EVENT_LOCATION,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Events.ALL_DAY
        };

        ContentResolver resolver = getContentResolver();

        Uri.Builder eventsUriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon();

        // grab all scheduleEvents events in the next 24h
        ContentUris.appendId(eventsUriBuilder, System.currentTimeMillis());
        ContentUris.appendId(eventsUriBuilder, System.currentTimeMillis() + (1000 * 60 * 60 * 24));

        Uri eventUri = eventsUriBuilder.build();

        String selection = null;
        String[] selectionArgs = null;
        // TODO add proper account picker
        // String selection = "(" + CalendarContract.Calendars.ACCOUNT_NAME + " = ?)";
        // String[] selectionArgs = new String[]{
        // "<you@gmail.com>"
        // };

        Cursor eventCursor = resolver.query(eventUri, EVENT_PROJECTION,
                selection, selectionArgs, CalendarContract.Instances.BEGIN + " ASC");
        AlarmManager alarms = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        while (eventCursor.moveToNext()) {
            String eventTitle = eventCursor.getString(0);
            String eventLocation = eventCursor.getString(2);
            long eventStart = eventCursor.getLong(3);
            long eventEnd = eventCursor.getLong(4);
            boolean allDay = eventCursor.getInt(5) != 0;

            if (allDay) continue;

            BusyEvent event = new BusyEvent(eventTitle, eventStart, eventEnd, eventLocation);
            Log.d(TAG, "found and scheduling for event=" + event.toString());

            //alarms.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5000,
            //        event.getPendingStartIntent(this));
            //alarms.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 10000,
            //        event.getPendingEndIntent(this));
            alarms.set(AlarmManager.RTC_WAKEUP, eventStart, event.getPendingStartIntent(this));
            alarms.set(AlarmManager.RTC_WAKEUP, eventEnd, event.getPendingEndIntent(this));
        }

        // reschedule this service again in 15m
        alarms.set(AlarmManager.RTC, System.currentTimeMillis() + (1000 * 60 * 15),
                PendingIntent.getService(this, 0,
                        new Intent().setClass(this, CalendarServiceObserver.class),
                        PendingIntent.FLAG_CANCEL_CURRENT));
    }
}
