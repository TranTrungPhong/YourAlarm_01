package com.framgia.youralarm1.broadcast;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import com.framgia.youralarm1.data.MySqliteHelper;
import com.framgia.youralarm1.service.SchedulingService;

public class AlarmReceiver extends WakefulBroadcastReceiver {
    private static final String TAG = AlarmReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {

        Intent service = new Intent(context, SchedulingService.class);
        Bundle bundle = new Bundle();
        bundle.putInt(MySqliteHelper.COLUMN_ID,
                      intent.getExtras().getInt(MySqliteHelper.COLUMN_ID, - 1));
        service.putExtras(bundle);
        Log.d(TAG, String.valueOf(intent.getExtras().getInt(MySqliteHelper.COLUMN_ID)));
        startWakefulService(context, service);
    }
}
