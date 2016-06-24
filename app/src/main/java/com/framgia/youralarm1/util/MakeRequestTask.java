package com.framgia.youralarm1.util;

import android.app.Activity;
import android.app.Fragment;
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
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by phongtran on 16/06/2016.
 */
public class MakeRequestTask extends Fragment {
    private Activity mContext;
    private ProgressDialog mProgress;
    private com.google.api.services.calendar.Calendar mService = null;
    private Exception mLastError = null;
    private List<String> mCalendarItems = new ArrayList<>();
    private MySqliteHelper mySqliteHelper;

    public MakeRequestTask() {
    }

    public void getData(Activity context, GoogleAccountCredential credential) {
        mContext = context;
        mySqliteHelper = new MySqliteHelper(mContext);
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.calendar
                .Calendar.Builder(transport, jsonFactory, credential)
                .setApplicationName(mContext.getString(R.string.application_name))
                .build();
    }

    public void excuteMake() {
        new MakeRequestEvent().execute();
    }
    public class MakeRequestEvent extends AsyncTask<Void, Void, List<String>> {
//        private Activity mContext;
//        private ProgressDialog mProgress;
//        private com.google.api.services.calendar.Calendar mService = null;
//        private Exception mLastError = null;
//        private List<String> mCalendarItems = new ArrayList<>();
//        private MySqliteHelper mySqliteHelper;
//
//        public MakeRequestTask(Activity context, GoogleAccountCredential credential) {
//            mContext = context;
//            mySqliteHelper = new MySqliteHelper(mContext);
//            HttpTransport transport = AndroidHttp.newCompatibleTransport();
//            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
//            mService = new com.google.api.services.calendar
//                    .Calendar.Builder(transport, jsonFactory, credential)
//                    .setApplicationName(mContext.getString(R.string.application_name))
//                    .build();
//        }

        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        private List<String> getDataFromApi() throws IOException {
            DateTime now = new DateTime(System.currentTimeMillis() - Const.BEFORE_CURENT_TIME * 60);
            List<String> eventStrings = new ArrayList<String>();
            Events events = mService.events().list(mContext.getString(R.string.primary))
                    .setMaxResults(Const.COUNT_RESULT)
                    .setTimeMin(now)
                    .setOrderBy(mContext.getString(R.string.startTime))
                    .setSingleEvents(true)
                    .execute();
            List<Event> items = events.getItems();
            for (Event event : items) {
                DateTime start = event.getStart().getDateTime();
                if (start == null) {
                    start = event.getStart().getDate();
                }
                eventStrings.add(String.format("%s (%s)", event.getSummary(), start));
            }
            return eventStrings;
        }


        @Override
        protected void onPreExecute() {
            mProgress = new ProgressDialog(mContext);
            mProgress.setMessage(mContext.getString(R.string.calling_api));
            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            if (mProgress != null && mProgress.isShowing()) {
                mProgress.dismiss();
            }
            if (output == null || output.size() == 0) {
                Toast.makeText(mContext, R.string.no_result, Toast.LENGTH_SHORT)
                        .show();
            } else {
                mCalendarItems.clear();
                mCalendarItems.addAll(output);
                Toast.makeText(mContext,
                        mContext.getString(R.string.done),
                        Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            if (mProgress != null && mProgress.isShowing()) {
                mProgress.dismiss();
            }
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    EventUtil.showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    mContext.startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            Const.REQUEST_AUTHORIZATION);
                } else {
                    Toast.makeText(mContext, R.string.error_occurred, Toast.LENGTH_SHORT)
                            .show();
                }
            } else {
                Toast.makeText(mContext, R.string.request_canclled, Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }
}
