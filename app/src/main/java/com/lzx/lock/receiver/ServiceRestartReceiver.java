package com.lzx.lock.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.lzx.lock.base.AppConstants;
import com.lzx.lock.service.LockService;
import com.lzx.lock.utils.SpUtil;

/**
 * Broadcast-Empfänger zum Neustart des LockService nach unerwarteter Beendigung.
 * Wird über AlarmManager ausgelöst, wenn der Dienst beendet wurde.
 */
public class ServiceRestartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (SpUtil.getInstance().getBoolean(AppConstants.LOCK_STATE, false)) {
            Intent serviceIntent = new Intent(context, LockService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }
}
