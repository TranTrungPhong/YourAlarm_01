package com.framgia.youralarm1.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by phongtran on 14/06/2016.
 */
public class EventTimeUtil {

    public String eventTime(int minute) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR, minute / 60);
        calendar.set(java.util.Calendar.MINUTE, minute % 60);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        return getDate(calendar.getTimeInMillis());
    }

    public String getDate(long timeStamp) {

        try {
            DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSZZZZZ");
            Date netDate = new Date(timeStamp);
            return sdf.format(netDate);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
