package com.framgia.youralarm1.util;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.framgia.youralarm1.R;
import com.framgia.youralarm1.contstant.Const;
import com.framgia.youralarm1.models.ItemAlarm;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.io.IOException;

/**
 * Created by phongtran on 16/06/2016.
 */
public class DeleteEvent extends Fragment {
    private Activity mContext;
    private com.google.api.services.calendar.Calendar mService = null;
    private ItemAlarm mItemAlarm;
    private ProgressDialog mProgress;
    private GoogleAccountCredential mCredential;
    private MakeRequestTask mMake;

    public DeleteEvent() {
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
    public void excuteDelete(){
        new DeleteEventCalendar().execute();
        EventUtil.setTimeOutDialog(mProgress);
    }

    public class DeleteEventCalendar extends AsyncTask<Void, Void, String> {
        private DeleteEventCalendar mDeleteEventCalendar;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDeleteEventCalendar = this;
            if (mItemAlarm.getIdEvent().equals(Const.DEFAULT_EVENT_ID)) {
                cancel(true);
            }
            mProgress = new ProgressDialog(mContext);
            mProgress.setMessage(mContext.getString(R.string.delete_api));
            mProgress.show();
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                mService.events().delete(mContext.getResources()
                        .getString(R.string.primary), mItemAlarm.getIdEvent()).execute();
                return Const.DELETE_SUCCESS;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return Const.DELETE_FAIL;
        }

        @Override
        protected void onPostExecute(String aVoid) {
            if (mProgress != null && mProgress.isShowing()) {
                mProgress.dismiss();
            }
            if (aVoid.equals(Const.DELETE_SUCCESS)) {
                mMake = new MakeRequestTask();
                mMake.getData(mContext, mCredential);
                mMake.excuteMake();
//            new MakeRequestTask(mContext, mCredential).execute()
            } else {
                Toast.makeText(mContext, R.string.delete_fail, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if (mProgress != null && mProgress.isShowing()) {
                mProgress.dismiss();
            }
            Toast.makeText(mContext, R.string.delete_fail, Toast.LENGTH_SHORT).show();
        }
    }
}
