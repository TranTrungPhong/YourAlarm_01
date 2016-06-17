package com.framgia.youralarm1.utils;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.widget.Toast;
import com.framgia.youralarm1.R;
import com.framgia.youralarm1.broadcast.AlarmReceiver;
import com.framgia.youralarm1.contstant.Const;
import com.framgia.youralarm1.data.MySqliteHelper;
import com.framgia.youralarm1.models.ItemAlarm;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * Created by vuduychuong1994 on 6/10/16.
 */
public class AlarmUtils {
    private static final String TAG = NotificationUtils.class.getName();

    public static void setAlarm(Context context, ItemAlarm itemAlarm, int numDayAfter) {
        if (itemAlarm.isStatus()) {
            AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, AlarmReceiver.class);
            Bundle bundle = new Bundle();
            bundle.putInt(MySqliteHelper.COLUMN_ID, itemAlarm.getId());
            intent.putExtras(bundle);

            PendingIntent alarmIntent =
                    PendingIntent.getBroadcast(context, itemAlarm.getId(), intent,
                                               PendingIntent.FLAG_UPDATE_CURRENT);

            Calendar calendar = Calendar.getInstance(Locale.getDefault());
            if (numDayAfter > 0) {
                calendar.add(Calendar.DATE, numDayAfter);
            } else {
                if (calendar.get(Calendar.HOUR_OF_DAY) > itemAlarm.getTime() / 60) {
                    calendar.add(Calendar.DATE, 1);
                } else if (calendar.get(Calendar.MINUTE) >= itemAlarm.getTime() % 60) {
                    calendar.add(Calendar.DATE, 1);
                }
            }

            calendar.set(Calendar.HOUR_OF_DAY, itemAlarm.getTime() / 60);
            calendar.set(Calendar.MINUTE, itemAlarm.getTime() % 60);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            if (itemAlarm.isRepeat())
                alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                                             AlarmManager.INTERVAL_DAY, alarmIntent);
            else
                alarmMgr.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmIntent);
        } else {
            cancelAlarm(context, itemAlarm);
        }
    }

    public static void setNextAlarm(Context context, ItemAlarm itemAlarm, boolean isRinging) {
        int numdayNext = 0;
        int today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        if(itemAlarm.getWeekDayHashMap().containsValue(true)) {
            numdayNext = 7;
            Iterator iterator = itemAlarm.getWeekDayHashMap().entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                if ((boolean) entry.getValue()) {
                    int dayAlarm = ((ItemAlarm.WeekDay) entry.getKey()).dayNumber();
                    if (dayAlarm > today) {
                        if (dayAlarm - today < numdayNext)
                            numdayNext = dayAlarm - today;
                    } else {
                        if (dayAlarm + 7 - today < numdayNext)
                            numdayNext = dayAlarm + 7 - today;
                    }
                }
            }
        } else {
            if (isRinging) {
                Calendar calendar = Calendar.getInstance(Locale.getDefault());
                int nowTime = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
                if (nowTime > itemAlarm.getTime())
                    itemAlarm.setStatus(false);
            }
        }
        setAlarm(context, itemAlarm, numdayNext);
    }

    public static void cancelAlarm(Context context, ItemAlarm itemAlarm) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        Bundle bundle = new Bundle();
        bundle.putInt(MySqliteHelper.COLUMN_ID, itemAlarm.getId());
        intent.putExtras(bundle);
        PendingIntent alarm = PendingIntent.getBroadcast(context, itemAlarm.getId(), intent,
                                                         PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.cancel(alarm);
        NotificationUtils.cancelNotification(context, itemAlarm);
        Toast.makeText(context, R.string.alarm_cancel, Toast.LENGTH_SHORT).show();
        MySqliteHelper mySqliteHelper = new MySqliteHelper(context);
        try {
            if (mySqliteHelper.haveAlarm(itemAlarm))
                mySqliteHelper.updateAlarm(itemAlarm);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    public static void setSnoozeAlarm(Context context, ItemAlarm itemAlarm, long snoozeTime) {
        NotificationUtils.cancelNotification(context, itemAlarm);
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        Bundle bundle = new Bundle();
        bundle.putInt(MySqliteHelper.COLUMN_ID, itemAlarm.getId());
        intent.putExtras(bundle);

        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, itemAlarm.getId(), intent,
                                                               PendingIntent.FLAG_UPDATE_CURRENT);
        Calendar calendar = Calendar.getInstance(Locale.getDefault());

        int currenTime = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        int count =
                (int) ((currenTime - itemAlarm.getTime()) / (snoozeTime / Const.MINUTE_IN_MILIS));

        calendar.set(Calendar.HOUR_OF_DAY, itemAlarm.getTime() / 60);
        calendar.set(Calendar.MINUTE, itemAlarm.getTime() % 60);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        alarmMgr.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis() + (count + 1) * snoozeTime,
                     alarmIntent);
    }
}
