package com.lzx.lock.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lzx.lock.base.AppConstants;
import com.lzx.lock.service.LoadAppListService;
import com.lzx.lock.service.LockService;
import com.lzx.lock.utils.LogUtil;
import com.lzx.lock.utils.SpUtil;

/**
 * Broadcast für Systemstart
 * Created by xian on 2017/3/4.
 */

public class BootBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        LogUtil.i("Dienst beim Systemstart gestartet....");
        context.startService(new Intent(context, LoadAppListService.class));
        if (SpUtil.getInstance().getBoolean(AppConstants.LOCK_STATE, false)) {
            context.startService(new Intent(context, LockService.class));
        }
    }
}
