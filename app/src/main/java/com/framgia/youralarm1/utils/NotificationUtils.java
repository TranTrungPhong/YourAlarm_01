package com.framgia.youralarm1.utils;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import com.framgia.youralarm1.R;
import com.framgia.youralarm1.activity.AlertAlarmActivity;
import com.framgia.youralarm1.contstant.Const;
import com.framgia.youralarm1.models.ItemAlarm;

/**
 * Created by vuduychuong1994 on 6/9/16.
 */
public class NotificationUtils {
    private static final String TAG = NotificationUtils.class.getName();

    public static void showNotification(Context context, ItemAlarm itemAlarm) {
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Resources resources = context.getResources();
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context).setSmallIcon(
                R.mipmap.ic_launcher).setContentTitle(
                resources.getString(R.string.app_name)).setStyle(
                new NotificationCompat.BigTextStyle().bigText(itemAlarm.getTitle())).setPriority(
                Notification.PRIORITY_MAX).setAutoCancel(true).setWhen(
                0).setCategory(Notification.CATEGORY_ALARM).setContentText(itemAlarm.getTitle());

        Intent fullScreenIntent = new Intent(context, AlertAlarmActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable(Const.ITEM_ALARM, itemAlarm);
        fullScreenIntent.putExtras(bundle);
        fullScreenIntent.setAction(Const.ACTION_FULLSCREEN_ACTIVITY);
        if (isScreenOn(context))
            fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        else
            fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

        PendingIntent alarmIntent =
                PendingIntent.getActivity(context, itemAlarm.getId(), fullScreenIntent,
                                          PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(alarmIntent);
        mBuilder.setFullScreenIntent(alarmIntent, true);
        nm.cancel(itemAlarm.getId());
        nm.notify(itemAlarm.getId(), mBuilder.build());
    }

    public static void cancelNotification(Context context, ItemAlarm itemAlarm){
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(itemAlarm.getId());
    }

    public static boolean isScreenOn(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            if (pm.isInteractive())
                return true;
        } else if (pm.isScreenOn())
            return true;
        return false;
    }
}