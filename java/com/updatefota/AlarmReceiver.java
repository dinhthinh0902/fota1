package com.updatefota;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.Calendar;
import java.util.Random;

import static com.updatefota.BootReciver.PUSHUPDATE;
import static com.updatefota.BootReciver.Push;

public class AlarmReceiver extends BroadcastReceiver {
    String TAG = "UpdateFOTA";
    boolean DEBUG = false;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {
        SchedulingService.enqueueWork(context, new Intent());
        setAlarm(context);
        Push = false;
    }

    // BEGIN_INCLUDE(set_alarm)
    public void setAlarm(Context context) {
        Log.d(TAG, "Start Set Time for next update FOTA");
        Random rand = new Random();
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(PUSHUPDATE);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        Calendar calendar = Calendar.getInstance();
        Calendar copy = (Calendar) calendar.clone();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 2);
        calendar.set(Calendar.MINUTE, 50 + rand.nextInt(20));
        //calendar.set(Calendar.MINUTE, 17);
        calendar.set(Calendar.SECOND, 0);
        int result = calendar.compareTo(copy);
        if (result > 0) {
            if (DEBUG) {
                Log.d(TAG, "Set alarm pending." + calendar);
            }
            alarmMgr.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmIntent);
        } else if (result < 0) {
            calendar.add(Calendar.HOUR_OF_DAY, 24);
            if (DEBUG) {
                Log.d(TAG, "Add 24h and pending" + calendar);
            }
            alarmMgr.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmIntent);
        }
    }
}

