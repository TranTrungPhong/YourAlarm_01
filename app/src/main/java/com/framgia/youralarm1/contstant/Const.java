package com.framgia.youralarm1.contstant;

import android.content.Intent;
import com.google.api.services.calendar.CalendarScopes;

/**
 * Created by vuduychuong1994 on 6/3/16.
 */
public class Const {
    public static final int MY_PERMISSION_REQUEST_GET_ACCOUNTS = 241;
    public static final String DEFAULT = " (Default)";
    public static final int CHOOSE_RINGTONE_REQUEST = 333;
    public static final String DEFAUL_TITLE = "Title";
    public static final String LIST_ALARM_PREFERENCE = "list_alarm";
    public static final String LIST_ALARM = "list_alarm";
    public static final int HEIGHT_MEASURE_SPEC_DEFAULT = 300;
    public static final int REQUEST_ACCOUNT_PICKER = 1000;
    public static final int REQUEST_AUTHORIZATION = 1001;
    public static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    public static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    public static final String BUTTON_TEXT = "Call Google Calendar API";
    public static final String PREF_ACCOUNT_NAME = "accountName";
    public static final String PREF_NAME = "share preference";
    public static final String[] SCOPES = {CalendarScopes.CALENDAR};
    public static final long BEFORE_CURENT_TIME = 2592000;
    public static final int COUNT_RESULT = 100;
    public static final String NO_INTERNET = "no internet";
    public static final String NO_ACCOUNT = "no acount";
    public static final String ACCOUNT_SUCCESS = "account success";
    public static final String ITEM_ALARM = "item_alarm";
    public static final long RING_TIME = 60000;
    public static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    public static final int ALARM_STATUS_ON = 1;
    public static final long MINUTE_IN_MILIS = 60000;
    public static final String ACTION_FULLSCREEN_ACTIVITY = "fullscreen_activity";
    public static final int REQUEST_CODE_SNOOZE = 1;
    public static final int REQUEST_CODE_DISMISS = 2;
    public static final String ACTION_SNOOZE_ALARM = "ACTION_SNOOZE_ALARM";
    public static final String ACTION_DISMISS_ALARM = "ACTION_DISMISS_ALARM";
    public static final String ACTION_UPDATE_DATA = "ACTION_UPDATE_DATA";
    public static final String SUNDAY = "SU";
    public static final String MONDAY = "MO";
    public static final String TUESDAY = "TU";
    public static final String WENESDAY = "WE";
    public static final String THIRDAY = "TH";
    public static final String FRIDAY = "FR";
    public static final String SATUDAY = "SA";
    public static final String DELETE_FAIL = "fail";
    public static final String DELETE_SUCCESS = "success";
    public static final String DEFAULT_EVENT_ID = "deefault_event_id";
    public static final int DAY_IN_MINUTES = 1440;
}
