package com.framgia.youralarm1.util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.framgia.youralarm1.R;
import com.framgia.youralarm1.activity.MainActivity;
import com.framgia.youralarm1.contstant.Const;
import com.framgia.youralarm1.data.MySqliteHelper;
import com.framgia.youralarm1.models.ItemAlarm;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by phongtran on 16/06/2016.
 */
public class SyncEventToServer extends AsyncTask<Void, Void, String> {
    private com.google.api.services.calendar.Calendar mService = null;
    private List<ItemAlarm> mItemAlarms;
    private List<String> mDaylist = new ArrayList<>();
    private StringBuffer mByDay = new StringBuffer();
    private ProgressDialog mProgress;
    private Activity mContext;
    private MySqliteHelper mySqliteHelper;
    private String mTimeEvent;

    public SyncEventToServer(Activity context,
                             GoogleAccountCredential credential,
                             List<ItemAlarm> itemAlarms) {
        mContext = context;
        mItemAlarms = itemAlarms;
        mySqliteHelper = new MySqliteHelper(mContext);
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.calendar.Calendar.Builder(
                transport, jsonFactory, credential)
                .setApplicationName(mContext.getString(R.string.application_name))
                .build();
    }

    @Override
    protected String doInBackground(Void... params) {
        String response = null;
        for (ItemAlarm itemAlarm : mItemAlarms) {
//            if (itemAlarm.getTime() != 0) {
            mTimeEvent = EventTimeUtil.eventTime(itemAlarm.getTime());
//            }
            if (mTimeEvent != null && itemAlarm.getIdEvent().equals(Const.DEFAULT_EVENT_ID)) {
                Event event = new Event()
                        .setSummary(itemAlarm.getTitle().toString());
                DateTime startDateTime = null;
                try {
                    startDateTime = new DateTime(mTimeEvent);
                } catch (NumberFormatException e) {
                    String sc = mTimeEvent.substring(mTimeEvent.length() - 2);
                    String sd = mTimeEvent.substring(0, mTimeEvent.length() - 2);
                    mTimeEvent = sd + ":" + sc;
                    startDateTime = new DateTime(mTimeEvent);
                }
                EventDateTime start = new EventDateTime()
                        .setDateTime(startDateTime)
                        .setTimeZone(mContext.getString(R.string.time_zone));
                event.setStart(start);
                DateTime endDateTime = new DateTime(mTimeEvent);
                EventDateTime end = new EventDateTime()
                        .setDateTime(endDateTime)
                        .setTimeZone(mContext.getString(R.string.time_zone));
                event.setEnd(end);
                try {
                    event = mService.events()
                            .insert(mContext.getString(R.string.primary), event)
                            .execute();
                    itemAlarm.setIdEvent(event.getId());
                    mySqliteHelper.updateAlarm(itemAlarm);
                    response = event.toString();
                    continue;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (mTimeEvent != null && !itemAlarm.getIdEvent().equals(Const.DEFAULT_EVENT_ID)) {
                mDaylist = getRecurrence(itemAlarm);
                for (int i = 0; i < mDaylist.size(); i++) {
                    if (i < mDaylist.size() - 1) {
                        mByDay.append(mDaylist.get(i).toString()).append(",");
                    } else {
                        mByDay.append(mDaylist.get(i).toString());
                    }
                }
                try {
                    Event event = mService.events()
                            .get(mContext.getString(R.string.primary), itemAlarm.getIdEvent())
                            .execute();
                    event.setSummary(itemAlarm.getTitle().toString());
                    if (mDaylist.size() != 0) {
                        String[] recurrence = new String[]{
                                mContext.getString(R.string.recurrence_update) + mByDay
                        };
                        event.setRecurrence(Arrays.asList(recurrence));
                    }
                    event = mService.events()
                            .update(mContext.getString(R.string.primary), event.getId(), event)
                            .execute();
                    mySqliteHelper.updateAlarm(itemAlarm);
                    response = event.toString();
                    continue;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return response;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgress = new ProgressDialog(mContext);
        mProgress.setMessage(mContext.getString(R.string.sync_server));
        mProgress.show();
    }

    @Override
    protected void onPostExecute(String s) {
        mItemAlarms.clear();
        mItemAlarms.addAll(mySqliteHelper.getListAlarm());
        super.onPostExecute(s);
        if (mProgress != null && mProgress.isShowing()) {
            mProgress.dismiss();
        }
        if (s == null) {
            Toast.makeText(mContext, R.string.syn_fails, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(mContext, R.string.syn_success, Toast.LENGTH_SHORT).show();
        }
    }

    private List<String> getRecurrence(ItemAlarm itemAlarm) {
        List<String> listDay = new ArrayList<>();
        if (itemAlarm.getWeekDayHashMap().containsValue(true)) {
            Iterator iterator = itemAlarm.getWeekDayHashMap().entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                if ((boolean) entry.getValue()) {
                    int dayAlarm = ((ItemAlarm.WeekDay) entry.getKey()).dayNumber();
                    switch (dayAlarm) {
                        case 1:
                            listDay.add(Const.SUNDAY);
                            break;
                        case 2:
                            listDay.add(Const.MONDAY);
                            break;
                        case 3:
                            listDay.add(Const.TUESDAY);
                            break;
                        case 4:
                            listDay.add(Const.WENESDAY);
                            break;
                        case 5:
                            listDay.add(Const.THIRDAY);
                            break;
                        case 6:
                            listDay.add(Const.FRIDAY);
                            break;
                        case 7:
                            listDay.add(Const.SATUDAY);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        return listDay;
    }
}
