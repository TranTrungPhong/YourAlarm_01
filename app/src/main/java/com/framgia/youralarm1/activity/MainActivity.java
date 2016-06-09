package com.framgia.youralarm1.activity;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TimePicker;
import android.widget.Toast;

import com.framgia.youralarm1.R;
import com.framgia.youralarm1.adapter.AlarmAdapter;
import com.framgia.youralarm1.contstant.Const;
import com.framgia.youralarm1.data.MySqliteHelper;
import com.framgia.youralarm1.models.ItemAlarm;
import com.framgia.youralarm1.util.EventUtil;
import com.framgia.youralarm1.util.PreferenceUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity
        implements AlarmAdapter.OnAdapterInterActionListener,
        EasyPermissions.PermissionCallbacks {

    private static final String TAG = MainActivity.class.getName();

    private FloatingActionButton mFab;
    private Toolbar mToolbar;
    private RecyclerView mRecyclerView;
    private AlarmAdapter mAlarmAdapter;
    private List<ItemAlarm> mAlarmList;
    private MySqliteHelper mMySqliteHelper;
    private GoogleAccountCredential mCredential;
    private EventUtil mEventUtil;
    private PreferenceUtil mPreferenceUtil;
    private ProgressDialog mProgress;
    private List<String> mCalendarItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCredential = GoogleAccountCredential
                .usingOAuth2(getApplicationContext(), Arrays.asList(Const.SCOPES))
                .setBackOff(new ExponentialBackOff());
        setContentView(R.layout.activity_main);
        mEventUtil = new EventUtil(MainActivity.this, mCredential);
        mPreferenceUtil = new PreferenceUtil(MainActivity.this);
        setView();
        setEvent();
    }

    private void setView() {
        mProgress = new ProgressDialog(this);
        mProgress.setMessage(getString(R.string.calling_api));
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        mAlarmList = new ArrayList<>();
        mMySqliteHelper = new MySqliteHelper(this);
        mAlarmList.addAll(mMySqliteHelper.getListAlarm());
        mAlarmAdapter = new AlarmAdapter(MainActivity.this, mAlarmList);
        mRecyclerView.setAdapter(mAlarmAdapter);
        checkAccount();
    }

    private void setEvent() {
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addAlarm();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case Const.CHOOSE_RINGTONE_REQUEST:
                    if (data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) != null) {
                        Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                        Log.d(TAG, uri.toString());
                        int listSize = mAlarmList.size();
                        for (int i = 0; i < listSize; i++) {
                            ItemAlarm itemAlarm = mAlarmList.get(i);
                            if (itemAlarm.isExpand()) {
                                itemAlarm.setRingTonePath(uri.toString());
                                onChangedAlarm(i);
                            }
                        }
                        mAlarmAdapter.notifyDataSetChanged();
                    }
                    break;
                // calendar
                case Const.REQUEST_ACCOUNT_PICKER:
                    if (data != null && data.getExtras() != null) {
                        String accountName =
                                data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                        if (accountName != null) {
                            mPreferenceUtil.putStringData(Const.PREF_ACCOUNT_NAME, accountName);
                            mCredential.setSelectedAccountName(accountName);
                            checkAccount();
                        }
                    }
                    break;
                case Const.REQUEST_AUTHORIZATION:
                    checkAccount();
                    break;
            }
        } else {
            if (requestCode == Const.REQUEST_GOOGLE_PLAY_SERVICES) {
                Toast.makeText(this, R.string.install_play_service, Toast.LENGTH_SHORT)
                        .show();
            } else {
                checkAccount();
            }
        }
    }

    @Override
    public void onChooseWeekdayListener(int idView, int position,
                                        boolean isSelected) {
        ItemAlarm itemAlarm = mAlarmList.get(position);
        switch (idView) {
            case R.id.circle_button_sunday:
                itemAlarm.getWeekDayHashMap().put(ItemAlarm.WeekDay.SUNDAY, isSelected);
                break;
            case R.id.circle_button_monday:
                itemAlarm.getWeekDayHashMap().put(ItemAlarm.WeekDay.MONDAY, isSelected);
                break;
            case R.id.circle_button_tuesday:
                itemAlarm.getWeekDayHashMap().put(ItemAlarm.WeekDay.TUESDAY, isSelected);
                break;
            case R.id.circle_button_wednesday:
                itemAlarm.getWeekDayHashMap().put(ItemAlarm.WeekDay.WEDNESDAY, isSelected);
                break;
            case R.id.circle_button_thursday:
                itemAlarm.getWeekDayHashMap().put(ItemAlarm.WeekDay.THURSDAY, isSelected);
                break;
            case R.id.circle_button_friday:
                itemAlarm.getWeekDayHashMap().put(ItemAlarm.WeekDay.FRIDAY, isSelected);
                break;
            case R.id.circle_button_saturday:
                itemAlarm.getWeekDayHashMap().put(ItemAlarm.WeekDay.SATURDAY, isSelected);
                break;
            default:
                break;
        }
        mAlarmAdapter.notifyDataSetChanged();
        try {
            updateAlarmDay(itemAlarm);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDeletedAlarm(int position) {
        try {
            mMySqliteHelper.deleteAlarm(mAlarmList.get(position));
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        mAlarmList.remove(position);
        mAlarmAdapter.notifyDataSetChanged();
    }

    @Override
    public void onChangedAlarm(int position) {
        try {
            updateAlarmDay(mAlarmList.get(position));
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    private void addAlarm() {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        TimePickerDialog timePickerDialog =
                new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        if (view.isShown()) {
                            for (ItemAlarm alarm : mAlarmList) {
                                alarm.setExpand(false);
                            }
                            ItemAlarm itemAlarm = new ItemAlarm();
                            itemAlarm.setTime(hourOfDay * 60 + minute);
                            try {
                                int idAlarm = addAlarmCache(itemAlarm);
                                itemAlarm.setId(idAlarm);
                                mAlarmList.add(itemAlarm);
                                mAlarmAdapter.notifyDataSetChanged();
                                if (mAlarmList.size() > 0)
                                    mRecyclerView.smoothScrollToPosition(mAlarmList.size() - 1);
                            } catch (SQLiteException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);
        timePickerDialog.show();
    }

    private int addAlarmCache(ItemAlarm itemAlarm) throws SQLiteException {
        //TODO: Create schedule
        return (int) mMySqliteHelper.addAlarm(itemAlarm);
    }

    private void updateAlarmDay(ItemAlarm itemAlarm) throws SQLiteException {
        //TODO: Update schedule
        mMySqliteHelper.updateAlarm(itemAlarm);
    }


    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }

    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = mPreferenceUtil.getStringData(Const.PREF_ACCOUNT_NAME);
            if (accountName != null) {
                mPreferenceUtil.putStringData(Const.PREF_ACCOUNT_NAME, accountName);
                mCredential.setSelectedAccountName(accountName);
                checkAccount();
            } else {
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        Const.REQUEST_ACCOUNT_PICKER);
            }
        } else {
            EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.access_acount),
                    Const.REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;

        public MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar
                    .Calendar.Builder(transport, jsonFactory, credential)
                    .setApplicationName(getString(R.string.application_name))
                    .build();
        }

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
            DateTime now = new DateTime(System.currentTimeMillis() - Const.TIME);
            List<String> eventStrings = new ArrayList<String>();
            Events events = mService.events().list(getString(R.string.primary))
                    .setMaxResults(Const.COUNT_RESULT)
                    .setTimeMin(now)
                    .setOrderBy(getString(R.string.startTime))
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
            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            mProgress.hide();
            if (output == null || output.size() == 0) {
                Toast.makeText(MainActivity.this, R.string.no_result, Toast.LENGTH_SHORT)
                        .show();
            } else {
                mCalendarItems.clear();
                mCalendarItems.addAll(output);
                Toast.makeText(MainActivity.this,
                        getString(R.string.size_event) + mCalendarItems.size(),
                        Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    mEventUtil.showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            Const.REQUEST_AUTHORIZATION);
                } else {
                    Toast.makeText(MainActivity.this, R.string.error_occurred, Toast.LENGTH_SHORT)
                            .show();
                }
            } else {
                Toast.makeText(MainActivity.this, R.string.request_canclled, Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    private void checkAccount() {
        String result = mEventUtil.getResultsFromApi();
        if (result.equals(Const.ACCOUNT_SUCCESS)) {
            new MakeRequestTask(mCredential).execute();
        } else if (result.equals(Const.NO_INTERNET)) {
            Toast.makeText(this, R.string.no_network, Toast.LENGTH_SHORT).show();
        } else if (result.equals(Const.NO_ACCOUNT)) {
            chooseAccount();
        }
    }
}