package com.updatefota;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.StrictMode;
import android.support.v4.app.JobIntentService;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.updatefota.BackgroundService.mServiceHandler;
import static com.updatefota.BootReciver.Push;

public class SchedulingService extends JobIntentService {
    static String TAG = "UpdateFOTA";
    boolean DEBUG = false;
    public static final int JOB_ID = 1;
    static boolean data;
    static String mDataHash;
    static String mDataVersion;
    static String mDataLink;
    static String mDataName;
    static int responseCode;
    static int responseCode1;
    static String UPDATEREADY = "com.catalia.mabu.application.MabuApplication.updateReady";
    static String NOUPDATE = "com.catalia.mabu.application.MabuApplication.noUpdate";
    static final int UPDATE_SERVICE_MSG = 1;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, SchedulingService.class, JOB_ID, work);
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "Start Polling FOTA");
        super.onCreate();
    }

    @Override
    protected void onHandleWork(Intent intent) {

        String cookie = null;
        String serial = null;

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        cookie = setLogin();
        getDataServer(cookie);
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public boolean onStopCurrentWork() {
        return super.onStopCurrentWork();
    }

    public int getVersionCode(String PackageName) {
        int verCode = 0;
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(PackageName, 0);
            verCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return verCode;
    }

    public String getSerialNumber() {
        String SerialNumber = "";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                SerialNumber = Build.getSerial();
            } else {
                SerialNumber = Build.SERIAL;
            }
        } catch (SecurityException e) {
        }
        return SerialNumber;
    }

    public String buildVersion() {
        String Build_version = null;
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            Build_version = (String) get.invoke(c, "ro.build.version.incremental");
        } catch (Exception ignored) {
        }
        return Build_version;
    }

    public String setLogin() {
        String cookie = null;
        try {
            URL url = new URL("https://api.staging.cataliahealth.com/fota/login");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /*milliseconds*/);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");

            JSONObject cred = new JSONObject();
            cred.put("username", "wr_agent");
            cred.put("password", "faVddNNbimSXq0JKd7C8w9LRw6AG0zN9nyrYLF0yvuaxfDoCDW3ZVWm3uebXJm7R");

            OutputStream os = conn.getOutputStream();
            os.write(cred.toString().getBytes("UTF-8"));
            os.close();
            responseCode = conn.getResponseCode();
            if (DEBUG) {
                Log.d(TAG, "Sending 'GET' request to URL : " + url);
                Log.d(TAG, "Response Code : " + responseCode);
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));
            String output;
            StringBuilder sb = new StringBuilder();
            while ((output = br.readLine()) != null) {
                sb.append(output + "\n");
            }
            String headerName = null;
            for (int i = 1; (headerName = conn.getHeaderFieldKey(i)) != null; i++) {
                if (headerName.equals("Set-Cookie")) {
                    cookie = conn.getHeaderField(i);
                }
            }

            conn.disconnect();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return cookie;
    }

    public void getDataServer(String cookie) {
        try {
            int verApk1 = getVersionCode("com.catalia.mabu");
            int verApk2 = getVersionCode("org.opencv.engine");
            int verApk3 = getVersionCode("com.catalia.mabu.voice");
            int verApk4 = getVersionCode("com.catalia.mabu.softkeyboard");
            String verBuild = buildVersion();
            String serial = getSerialNumber();
            String url = "https://api.staging.cataliahealth.com/fota/apk_updates?serial="+serial+"&have_versions=com.catalia.mabu:" + verApk1 + ",org.opencv.engine:" + verApk2 + ",com.catalia.mabu.voice:" + verApk3 + ",com.catalia.mabu.softkeyboard:" + verApk4 + ",ro.build.version.incremental:" + verBuild;
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Cookie", cookie);
            responseCode1 = con.getResponseCode();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            if (DEBUG) {
                Log.d(TAG, "Sending 'GET' request to URL : " + url);
                Log.d(TAG, "Response Code : " + responseCode1);
            }
            try {
                JSONObject response1 = new JSONObject(response.toString());
                JSONArray array = response1.getJSONArray("data");

                if (array != null & array.length() > 0) { //check data server empty or not
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject data = array.getJSONObject(i);
                        mDataHash = data.getString("apk_hash");
                        mDataLink = data.getString("link");
                        mDataVersion = data.getString("version");
                        mDataName = data.getString("name");
                        if (DEBUG) {
                            Log.d(TAG, "Response from server = " + array);
                        }
                    }
                    if (!Push) {
                        Intent updateready = new Intent(UPDATEREADY);
                        sendBroadcast(updateready);
                        mServiceHandler.sendEmptyMessageDelayed(UPDATE_SERVICE_MSG, 60000);
                    } else {
                        Intent myIntent = new Intent(getApplicationContext(), ShowNote.class);
                        myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getApplicationContext().startActivity(myIntent);
                    }

                } else if (array.length() < 1) {
                    mServiceHandler.removeMessages(UPDATE_SERVICE_MSG);
                    Log.d(TAG, "Data from server empty. " + array);
                }
                con.disconnect();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}