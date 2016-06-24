package com.framgia.youralarm1.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.framgia.youralarm1.contstant.Const;
import com.framgia.youralarm1.data.MySqliteHelper;
import com.framgia.youralarm1.models.ItemAlarm;
import com.framgia.youralarm1.utils.AlarmUtils;
import java.util.List;

public class AlarmBootReceiver extends BroadcastReceiver {
    private static final String TAG = AlarmBootReceiver.class.getName();

    public AlarmBootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Const.ACTION_BOOT_COMPLETED)) {
            //TODO: Setup Alarm
            setupAlarm(context);
        }
    }

    private void setupAlarm(Context context) {
        MySqliteHelper mySqliteHelper = new MySqliteHelper(context);
        List<ItemAlarm> alarmList
                = mySqliteHelper.getListAlarmByStatus(String.valueOf(Const.ALARM_STATUS_ON));
        for (ItemAlarm itemAlarm : alarmList) {
            AlarmUtils.setNextAlarm(context, itemAlarm, false, false);
        }
    }
}