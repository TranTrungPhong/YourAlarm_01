package com.framgia.youralarm1.utils;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
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
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.alarm)
                .setContentTitle(resources.getString(R.string.app_name))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(itemAlarm.getTitle()))
                .setPriority(Notification.PRIORITY_MAX)
                .setAutoCancel(true)
                .setWhen(0)
                .setCategory(Notification.CATEGORY_ALARM)
                .setContentText(itemAlarm.getTitle());

        Bundle bundle = new Bundle();
        bundle.putSerializable(Const.ITEM_ALARM, itemAlarm);
        Intent dismissIntent = new Intent(context, AlertAlarmActivity.class);
        dismissIntent.setAction(Const.ACTION_DISMISS_ALARM);
        dismissIntent.putExtras(bundle);
        Intent snoozeIntent = new Intent(context, AlertAlarmActivity.class);
        snoozeIntent.setAction(Const.ACTION_SNOOZE_ALARM);
        snoozeIntent.putExtras(bundle);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Uri ringtoneUri;
            if (itemAlarm.getRingTonePath() != null && !itemAlarm.getRingTonePath().equals("")) {
                ringtoneUri = Uri.parse(itemAlarm.getRingTonePath());
            } else
                ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            mBuilder.setSound(ringtoneUri, AudioManager.STREAM_ALARM)
                    .setVibrate(new long[]{500, 500});
            NotificationCompat.Action actionSnooze
                    = new NotificationCompat.Action.Builder(
                        R.drawable.av_timer,
                        resources.getString(R.string.snooze_title),
                        PendingIntent.getActivity(context,
                                                  Const.REQUEST_CODE_SNOOZE,
                                                  snoozeIntent,
                                                  PendingIntent.FLAG_UPDATE_CURRENT))
                        .build();

            NotificationCompat.Action actionDismiss
                    = new NotificationCompat.Action.Builder(
                        R.drawable.alarm_off,
                        resources.getString(R.string.dismiss_title),
                        PendingIntent.getActivity(context,
                                                  Const.REQUEST_CODE_DISMISS,
                                                  dismissIntent,
                                                  PendingIntent.FLAG_UPDATE_CURRENT))
                        .build();
            mBuilder.addAction(actionSnooze);
            mBuilder.addAction(actionDismiss);
            mBuilder.setDeleteIntent(PendingIntent.getActivity(context,
                                                               Const.REQUEST_CODE_DISMISS,
                                                               dismissIntent,
                                                               PendingIntent.FLAG_UPDATE_CURRENT));
        }

        Intent fullScreenIntent = new Intent(context, AlertAlarmActivity.class);
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