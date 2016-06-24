package com.framgia.youralarm1.util;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.framgia.youralarm1.R;
import com.framgia.youralarm1.contstant.Const;
import com.framgia.youralarm1.models.ItemAlarm;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

/**
 * Created by phongtran on 16/06/2016.
 */
public class UpdateEvent extends Fragment {
    private com.google.api.services.calendar.Calendar mService = null;
    private ItemAlarm mItemAlarm;
    private List<String> mDaylist = new ArrayList<>();
    private StringBuffer mByDay = new StringBuffer();
    private Activity mContext;
    private ProgressDialog mProgress;
    private GoogleAccountCredential mCredential;
    private DateTime mDateTime;
    private MakeRequestTask mMake;

    public UpdateEvent() {
    }

    public void getData(Activity context, GoogleAccountCredential credential, ItemAlarm itemAlarm) {
        mContext = context;
        mItemAlarm = itemAlarm;
        mCredential = credential;
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.calendar.Calendar.Builder(
                transport, jsonFactory, credential)
                .setApplicationName(mContext.getResources().getString(R.string.application_name))
                .build();
    }

    public void executeUpdate() {
        new UpdateEventCalendar().execute();
    }

    public class UpdateEventCalendar extends AsyncTask<Void, Void, String> {

        private List<String> getRecurrence() {
            List<String> listDay = new ArrayList<>();
            if (mItemAlarm.getWeekDayHashMap().containsValue(true)) {
                Iterator iterator = mItemAlarm.getWeekDayHashMap().entrySet().iterator();
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

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mItemAlarm.getIdEvent().equals(Const.DEFAULT_EVENT_ID)) {
                cancel(true);
            }
            mProgress = new ProgressDialog(mContext);
            mProgress.setMessage(mContext.getString(R.string.update_api));
            mProgress.show();
            mDaylist = getRecurrence();
            for (int i = 0; i < mDaylist.size(); i++) {
                if (i < mDaylist.size() - 1) {
                    mByDay.append(mDaylist.get(i).toString()).append(",");
                } else {
                    mByDay.append(mDaylist.get(i).toString());
                }
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                Event event = mService.events()
                        .get(mContext.getResources().getString(R.string.primary), mItemAlarm.getIdEvent())
                        .execute();
                event.setSummary(mItemAlarm.getTitle().toString());
                //             Lap event
                if (mDaylist.size() != 0) {
                    String[] recurrence = new String[]{
                            mContext.getResources().getString(R.string.recurrence_update) + mByDay
                    };
                    event.setRecurrence(Arrays.asList(recurrence));
                }
                if (event.getId().equals(Const.DEFAULT_EVENT_ID)) {
                    return null;
                }
                event = mService.events()
                        .update(mContext.getResources().getString(R.string.primary), event.getId(), event)
                        .execute();
                return event.toString();
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
            if (aVoid != null) {
//            new MakeRequestTask(mContext, mCredential).execute()
                mMake = new MakeRequestTask();
                mMake.getData(mContext, mCredential);
                mMake.excuteMake();
            } else {
                Toast.makeText(mContext, R.string.update_fail, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if (mProgress != null && mProgress.isShowing()) {
                mProgress.hide();
            }
            Toast.makeText(mContext, R.string.update_fail, Toast.LENGTH_SHORT).show();
        }
    }
}
