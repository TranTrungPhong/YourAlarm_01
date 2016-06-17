package com.framgia.youralarm1.util;

import com.google.api.services.calendar.model.EventDateTime;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by phongtran on 14/06/2016.
 */
public class EventTimeUtil {

    public static String eventTime(int minute) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, minute / 60);
        calendar.set(java.util.Calendar.MINUTE, minute % 60);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.setTimeZone(TimeZone.getDefault());
        return getDate(calendar.getTimeInMillis());
    }

    public static String getDate(long timeStamp) {

        try {
            DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ");
            Date netDate = new Date(timeStamp);
            return sdf.format(netDate);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static int timeAlarm(EventDateTime eventDateTime) {
        try {
            Date date = new Date(eventDateTime.getDateTime().getValue());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            int minute = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
            return minute;
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
