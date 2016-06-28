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

    public static void setAlarm(Context context, ItemAlarm itemAlarm, int numDayAfter,
                                boolean isNotify) {
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
            if (isNotify)
                Toast.makeText(context, setAlarmToast(context, calendar), Toast.LENGTH_SHORT).show();
        } else {
            cancelAlarm(context, itemAlarm);
        }
    }

    private static String setAlarmToast(Context context, Calendar calendar) {
        StringBuilder stringBuilder = new StringBuilder();
        Calendar nowCal = Calendar.getInstance();
        int day = Math.round((calendar.getTimeInMillis() - nowCal.getTimeInMillis()) /
                            (Const.DAY_IN_MINUTES * Const.MINUTE_IN_MILIS));
        int hour = calendar.get(Calendar.HOUR_OF_DAY) - nowCal.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE) - 1 - nowCal.get(Calendar.MINUTE);
        if (minute < 0) {
            minute = 60 + minute;
            hour -= 1;
        } else if (minute == 0)
            minute = 1;
        if (hour < 0) {
            hour = 24 + hour;
            day -= 1;
        }

        stringBuilder.append(context.getString(R.string.next_alarm));
        if (day > 0) stringBuilder.append(" " + day + " days");
        if (hour > 0) stringBuilder.append( " " + hour + " hours");
        if (minute > 0) stringBuilder.append(" " + minute + " minutes");

        return stringBuilder.toString();
    }

    public static void setNextAlarm(Context context, ItemAlarm itemAlarm, boolean isRinging,
                                    boolean isNotify) {
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
                    } else if (dayAlarm == today) {
                        int time = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60 +
                                Calendar.getInstance().get(Calendar.MINUTE);
                        if (time >= itemAlarm.getTime())
                            numdayNext = 7;
                        else numdayNext = 0;
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
                if (nowTime >= itemAlarm.getTime())
                    itemAlarm.setStatus(false);
            }
        }
        setAlarm(context, itemAlarm, numdayNext, isNotify);
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
