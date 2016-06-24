package com.framgia.youralarm1.service;

import android.app.IntentService;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.framgia.youralarm1.R;
import com.framgia.youralarm1.activity.AlertAlarmActivity;
import com.framgia.youralarm1.broadcast.AlarmReceiver;
import com.framgia.youralarm1.contstant.Const;
import com.framgia.youralarm1.data.MySqliteHelper;
import com.framgia.youralarm1.models.ItemAlarm;
import com.framgia.youralarm1.utils.NotificationUtils;
import java.util.Calendar;

public class SchedulingService extends IntentService {
    private static final String TAG = SchedulingService.class.getName();
    private KeyguardManager myKM;

    public SchedulingService() {
        super(TAG);
    }


    @Override
    protected void onHandleIntent(Intent intent) {

        Log.d(TAG, String.valueOf(intent.getExtras().getInt(MySqliteHelper.COLUMN_ID, - 1)));
        myKM = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        MySqliteHelper mySqliteHelper = new MySqliteHelper(this);
        ItemAlarm itemAlarm =
                mySqliteHelper.getAlarm(intent.getExtras().getInt(MySqliteHelper.COLUMN_ID, - 1));
        int time = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60 +
                Calendar.getInstance().get(Calendar.MINUTE);
//        NotificationUtils.showNotification(this, itemAlarm);
        Intent fullScreenIntent = new Intent(this, AlertAlarmActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable(Const.ITEM_ALARM, itemAlarm);
        fullScreenIntent.putExtras(bundle);
        fullScreenIntent.setAction(Const.ACTION_FULLSCREEN_ACTIVITY);
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(fullScreenIntent);


        if (myKM.inKeyguardRestrictedInputMode()) {
            //TODO: do when screen locked
            Log.i(TAG, getString(R.string.screen_locked));
        } else {
            //TODO: do when screen unlocked
            Log.i(TAG, getString(R.string.screen_unlocked));
        }
        AlarmReceiver.completeWakefulIntent(intent);
    }
}
