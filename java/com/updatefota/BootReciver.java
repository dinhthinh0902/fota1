package com.updatefota;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import static com.updatefota.BackgroundService.mServiceHandler;
import static com.updatefota.SchedulingService.UPDATE_SERVICE_MSG;


/**
 * This receiver is set to be disabled (android:enabled="false") in the
 * application's manifest file. When the user sets the alarm, the receiver is enabled.
 * When the user cancels the alarm, the receiver is disabled, so that rebooting the
 * device will not trigger this receiver.
 */
// BEGIN_INCLUDE(autostart)
public class BootReciver extends BroadcastReceiver {
    private final AlarmReceiver alarm = new AlarmReceiver();
    String TAG = "UpdateFOTA";
    boolean DEBUG = false;
    static boolean Push =false;
    static String PUSHUPDATE = "com.catalia.mabu.application.WatchdogThread.pushUpdate";
    static String NOUPDATE = "com.catalia.mabu.application.MabuApplication.noUpdate";
    String BOOTCOMPLETE ="android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(BOOTCOMPLETE)) {
            alarm.setAlarm(context);
            Intent startServiceIntent = new Intent(context, BackgroundService.class);
            context.startForegroundService(startServiceIntent);
        } else if (intent.getAction().equals(PUSHUPDATE)) {
            mServiceHandler.removeMessages(UPDATE_SERVICE_MSG);
            SchedulingService.enqueueWork(context, new Intent());
            Push =true;
        }else if(intent.getAction().equals(NOUPDATE)) {
            mServiceHandler.removeMessages(UPDATE_SERVICE_MSG);
        }
    }
}
//END_INCLUDE(autostart)