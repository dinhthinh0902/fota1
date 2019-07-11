package com.updatefota;

import android.content.Context;
import android.os.PowerManager;

import java.io.File;
import java.io.FileOutputStream;

public class OtaRecovery {
    private static final String recovery_command_file = "/cache/recovery/command";
    private static final String update_package_command = "--update_package=/cache/H7R-ota-095258.zip";
    static void RebootRecovery(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        pm.reboot("recovery");

    }
    static void WriteRecoveryCommand(Context context) {
        try {
            File file = new File(recovery_command_file);
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(update_package_command.getBytes());
            stream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
    static void RemoveSystemUpdateFile(Context context) {
        try {
            new File(context.getFilesDir(),"H7R-ota-095258.zip").delete();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
