package com.framgia.youralarm1.util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.framgia.youralarm1.R;
import com.framgia.youralarm1.activity.MainActivity;
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

/**
 * Created by phongtran on 16/06/2016.
 */
public class CreatEvent extends AsyncTask<String, String, String> {
    private Activity mContext;
    private ProgressDialog mProgress;
    private com.google.api.services.calendar.Calendar mService = null;
    private ItemAlarm mItemAlarm;
    private String mTimeEvent;
    private MySqliteHelper mySqliteHelper;
    private GoogleAccountCredential mCredential;

    public CreatEvent(Activity context, GoogleAccountCredential credential, ItemAlarm itemAlarm) {
        mContext = context;
        mItemAlarm = itemAlarm;
        mCredential = credential;
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.calendar.Calendar.Builder(
                transport, jsonFactory, credential)
                .setApplicationName(mContext.getResources().getString(R.string.application_name))
                .build();
        mySqliteHelper = new MySqliteHelper(mContext);
    }

    @Override
    protected void onPreExecute() {
        mProgress = new ProgressDialog(mContext);
        mProgress.setMessage(mContext.getString(R.string.calling_api));
        mProgress.show();
        mTimeEvent = EventTimeUtil.eventTime(mItemAlarm.getTime());
    }

    @Override
    protected String doInBackground(String... params) {

        Event event = new Event()
                .setSummary(mItemAlarm.getTitle().toString());
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
            return event.getId().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String aVoid) {
        if (mProgress != null && mProgress.isShowing()) {
            mProgress.dismiss();
        }
        if (aVoid == null) {
            Toast.makeText(mContext, R.string.create_fail, Toast.LENGTH_SHORT).show();
        } else {
            mItemAlarm.setIdEvent(aVoid);
            mySqliteHelper.updateAlarm(mItemAlarm);
            new MakeRequestTask(mContext, mCredential).execute();
        }
    }
}
