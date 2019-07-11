package com.updatefota;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;

import static com.updatefota.BootReciver.PUSHUPDATE;
import static com.updatefota.SchedulingService.NOUPDATE;


public class BackgroundService extends Service {
    static final int UPDATE_SERVICE_MSG = 1;
    private Looper mServiceLooper;
    public static ServiceHandler mServiceHandler;
    BootReciver mReceiver = new BootReciver();

    public BackgroundService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
        String CHANNEL_ID = "my_channel_01";
        HandlerThread thread = new HandlerThread("IntentService");
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT);
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(com.updatefota.R.drawable.ic_launcher_background)
                .setContentTitle("")
                .setContentText("").build();
        startForeground(1, notification);
    }

    public class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_SERVICE_MSG:
                    Intent myIntent = new Intent(getApplicationContext(), ShowNote.class);
                    myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getApplicationContext().startActivity(myIntent);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IntentFilter intentFilter = new IntentFilter(PUSHUPDATE);
        registerReceiver(mReceiver, intentFilter);

        IntentFilter intent1 = new IntentFilter(NOUPDATE);
        registerReceiver(mReceiver, intent1);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}