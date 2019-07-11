package com.updatefota;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ShowNote extends Activity {
    private static final int PKGBUFSIZE = 1024 * 1024;
    private static final int MD5MASK = 0xff;
    private static final String MD5TAG = "MD5";
    static final int URL_CONNECT_TIMEOUT = 120000; // 120 second
    static final int URL_READ_TIMEOUT = 120000; // 120 second
    static final int HTTP_TRANSFER_BUF = 16 * 1024;
    static final String DOWNLOAD_DONE = "ACTION_DOWNLOAD_COMPLETE";
    private BroadcastReceiver receiver;
    String TAG = "UpdateFOTA";
    Boolean DEBUG = false;
    static String namedataOTA = "ro.build.version.incremental";
    String Direcfilezip = "/cache/";
    String Namefilezip = "H7R-ota-095258.zip";
    String Direcfileapk = "/storage/emulated/0/catalia/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Start Download and Install FOTA");
        if (SchedulingService.mDataName.equals(namedataOTA)) {
            String Linkfilezip = SchedulingService.mDataLink;
            useDownloadFiles OtaDownload = new useDownloadFiles(Linkfilezip, Direcfilezip, Namefilezip);
            OtaDownload.start();
            finish();
        } else {
            clearDownload(SchedulingService.mDataName);
            String LinkfileOTA = SchedulingService.mDataLink;
            useDownloadFiles ApkDownload = new useDownloadFiles(LinkfileOTA, Direcfileapk, SchedulingService.mDataName);
            ApkDownload.start();
            finish();
        }

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                String name;
                if (action.equals(DOWNLOAD_DONE)) {
                    name = intent.getStringExtra("nameapk");
                    if (name.equals("H7R-ota-095258.zip")) {
                        if (useVerifyMD5((SchedulingService.mDataHash), "/cache/H7R-ota-095258.zip")) {
                            OtaRecovery.RemoveSystemUpdateFile(getApplicationContext());
                            OtaRecovery.WriteRecoveryCommand(getApplicationContext());
                            OtaRecovery.RebootRecovery(getApplicationContext());
                        }
                    } else {
                        if (useVerifyMD5(SchedulingService.mDataHash, "/storage/emulated/0/catalia/" + name)) {
                            Log.d(TAG, "Apk Install Successfully" + name);
                            installApk(name);
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException ex) {
                            }
                            SchedulingService.enqueueWork(context, new Intent()); //start polling again
                        } else {
                            finish();
                        }
                    }
                }
            }
        };
    }

    private void installApk(String apkname) {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        Intent intent_install = new Intent(Intent.ACTION_VIEW);
        intent_install.setDataAndType(Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/catalia/" + apkname)), "application/vnd.android.package-archive");
        intent_install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (DEBUG) {
            Log.d("phone path", Environment.getExternalStorageDirectory() + "/catalia/" + apkname);
        }
        startActivity(intent_install);
    }

    public boolean useVerifyMD5(String hash, String fileDir) {
        String fileMD5 = getFileMD5(fileDir);
        String comp = null;
        if (fileMD5 != null) {
            comp = fileMD5.substring(0, 32);
        }
        if (comp != null && comp.equals(hash)) {
            Log.d(TAG, "Check md5 ok");
            return true;
        } else {
            Log.d(TAG, "Check md5 failed");
            return false;
        }
    }

    private String getFileMD5(String fileDir) {
        FileInputStream fis;
        try {
            MessageDigest md = MessageDigest.getInstance(MD5TAG);

            fis = new FileInputStream(fileDir);
            int length = -1;
            byte[] buffer = new byte[PKGBUFSIZE];

            if (fis == null || md == null) {
                return null;
            }

            while ((length = fis.read(buffer)) != -1) {
                md.update(buffer, 0, length);
            }

            if (fis != null) {
                fis.close();
            }

            byte[] bytes = md.digest();
            if (bytes == null) {
                return null;
            }

            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < bytes.length; i++) {
                String md5s = Integer.toHexString(bytes[i] & MD5MASK);
                if (md5s == null || buf == null) {
                    return null;
                }
                if (md5s.length() == 1) {
                    buf.append("0");
                }
                buf.append(md5s);
            }
            return buf.toString();
        } catch (NoSuchAlgorithmException ex) {

            ex.printStackTrace();
            return null;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private void clearDownload(String nameapk) {
        File catalia;
        catalia = new File(Environment.getExternalStorageDirectory() + "/catalia/", nameapk);
        if (DEBUG) {
            Log.d(TAG, "Erase file to download " + nameapk);
        }
        if (catalia != null && catalia.exists()) {
            catalia.delete();
        }
    }

    class useDownloadFiles extends Thread {
        String mDownloadFile;
        long mFileSize;
        String Direcfile;
        String namefile;

        public useDownloadFiles(String file, String Direc, String name) {
            mDownloadFile = file;
            Direcfile = Direc;
            namefile = name;

        }

        public void run() {
            URL url;
            HttpURLConnection urlConnection = null;
            FileOutputStream outputStream;
            int count = 0;
            Integer result;
            long total = 0;
            long LocalFileSize = 0;
            int preProgress = 0, Progress = 0;
            try {
                url = new URL(mDownloadFile);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(URL_CONNECT_TIMEOUT);
                urlConnection.setReadTimeout(URL_READ_TIMEOUT);
                mFileSize = (long) urlConnection.getContentLength();

                InputStream in = new BufferedInputStream(urlConnection.getInputStream(), HTTP_TRANSFER_BUF);
                File LocalFile = new File(Direcfile, namefile);
                if (LocalFile != null)
                    LocalFileSize = LocalFile.length();
                else
                    LocalFileSize = 0;
                outputStream = new FileOutputStream(LocalFile);
                byte data[] = new byte[HTTP_TRANSFER_BUF];
                while ((count = in.read(data)) != -1) {
                    if (isInterrupted()) break;
                    total += count;
                    outputStream.write(data, 0, count);
                    Progress = (int) ((total * 100) / (float) mFileSize);
                    if (DEBUG) {
                        Log.d(TAG, "write data : " + total + " " + Progress);
                    }
                }

                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
                if ((total != mFileSize) || (isInterrupted())) {
                    Log.d(TAG, "Download is interrupted");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
            }
            // successfully download
            Log.d(TAG, "end thread");
            if (Progress == 100) {
                registerReceiver(receiver, new IntentFilter(DOWNLOAD_DONE));
                Intent i = new Intent(DOWNLOAD_DONE);
                i.putExtra("nameapk", namefile);
                sendBroadcast(i);
            }
        }

    }
}