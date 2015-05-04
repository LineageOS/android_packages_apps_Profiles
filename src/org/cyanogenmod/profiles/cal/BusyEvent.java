package org.cyanogenmod.profiles.cal;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.text.SimpleDateFormat;
import java.util.Date;

public class BusyEvent {

    public static final String ACTION_BUSY_START = "org.cyanogenmod.profiles.cal.action.BUSY_EVENT_START";
    public static final String ACTION_BUSY_END = "org.cyanogenmod.profiles.cal.action.BUSY_EVENT_END";

    private static int sNumEvents = 0;

    private String mEventTitle;
    private long mStartTime;
    private long mEndTime;
    private String mLocation;
    private int mEventNum;

    public BusyEvent(String title, long start, long end, String location) {
        this.mEventTitle = title;
        this.mStartTime = start;
        this.mEndTime = end;
        this.mLocation = location;

        mEventNum = sNumEvents++;
    }

    public int getEventNumber() {
        return mEventNum;
    }

    public PendingIntent getPendingStartIntent(Context context) {
        return PendingIntent.getService(context, mEventNum,
                new Intent(ACTION_BUSY_START)
                        .setClass(context, CalendarServiceObserver.class)
                        .putExtra("id", mEventNum),
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public PendingIntent getPendingEndIntent(Context context) {
        return PendingIntent.getService(context, mEventNum,
                new Intent(ACTION_BUSY_END)
                        .setClass(context, CalendarServiceObserver.class)
                        .putExtra("id", mEventNum),
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    @Override
    public String toString() {
        return mEventTitle + " starts at "
                + SimpleDateFormat.getDateTimeInstance().format(new Date(mStartTime)) + " until "
                + SimpleDateFormat.getDateTimeInstance().format(new Date(mEndTime));
    }
}
