package com.framgia.youralarm1.activity;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteException;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import com.framgia.youralarm1.util.EventTimeUtil;
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
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;

import java.io.IOException;

import com.framgia.youralarm1.utils.AlarmUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private String[] mArrPermission = new String[]{Manifest.permission.GET_ACCOUNTS};
    private int[] mArrRequestCode = new int[]{Const.MY_PERMISSION_REQUEST_GET_ACCOUNTS};

    private BroadcastReceiver mInvalidDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateListAlarm();
        }
    };

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setPermission(mArrPermission, mArrRequestCode);
        }
    }

    private void setPermission(String[] permissions, int[] requestCode) {
        //TODO: check Vibrate permission
        for (int i = 0; i < permissions.length; i++) {

            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    permissions[i])
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permissions[i]},
                        requestCode[i]);
            } else {
                Log.d(TAG, permissions[i]);
            }
        }
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
        try {
            mAlarmList.addAll(mMySqliteHelper.getListAlarm());
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        mAlarmAdapter = new AlarmAdapter(MainActivity.this, mAlarmList);
        mRecyclerView.setAdapter(mAlarmAdapter);
        checkAccount();
    }

    private void setEvent() {
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setPermission(mArrPermission, mArrRequestCode);
                }
                addAlarm();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mInvalidDataReceiver, new IntentFilter(Const.ACTION_UPDATE_DATA));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mInvalidDataReceiver);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Const.MY_PERMISSION_REQUEST_GET_ACCOUNTS:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, getString(R.string.permission_get_account_ok));
                } else {
                    Toast.makeText(MainActivity.this,
                            getString(R.string.permission_get_account_failed),
                            Toast.LENGTH_SHORT).show();
                    Log.d(TAG, getString(R.string.permission_get_account_failed));
                }
                return;
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
            if (itemAlarm.isStatus())
                AlarmUtils.setNextAlarm(MainActivity.this, itemAlarm);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDeletedAlarm(int position) {
        try {
            new DeleteEvent(mCredential, mAlarmList.get(position)).execute();
            mMySqliteHelper.deleteAlarm(mAlarmList.get(position));
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        AlarmUtils.cancelAlarm(MainActivity.this, mAlarmList.get(position));
        mAlarmList.remove(position);
        mAlarmAdapter.notifyDataSetChanged();
    }

    @Override
    public void onChangedAlarm(int position) {
        new UpdateEvent(mCredential, mAlarmList.get(position)).execute();
        try {
            updateAlarmDay(mAlarmList.get(position));
            if (mAlarmList.get(position).isStatus())
                AlarmUtils.setNextAlarm(MainActivity.this, mAlarmList.get(position));
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
                                AlarmUtils.setNextAlarm(MainActivity.this, itemAlarm);
                                new CreatEvent(mCredential, itemAlarm).execute();
                            } catch (SQLiteException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE) + 1, false);
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

    private void updateListAlarm() {
        try {
            mAlarmList.clear();
            mAlarmList.addAll(mMySqliteHelper.getListAlarm());
            mAlarmAdapter.notifyDataSetChanged();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }

    public void chooseAccount() {
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

    public class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
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
            DateTime now = new DateTime(System.currentTimeMillis() - Const.BEFORE_CURENT_TIME);
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
            if (mProgress != null && !mProgress.isShowing()) {
                mProgress.setMessage(getString(R.string.calling_api));
                mProgress.show();
            }
        }

        @Override
        protected void onPostExecute(List<String> output) {
            if (mProgress != null && mProgress.isShowing()) {
                mProgress.hide();
            }
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
            if (mProgress != null && mProgress.isShowing()) {
                mProgress.hide();
            }
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

    public class CreatEvent extends AsyncTask<String, String, String> {
        private com.google.api.services.calendar.Calendar mService = null;
        private ItemAlarm mItemAlarm;
        private String mTimeEvent;

        public CreatEvent(GoogleAccountCredential credential, ItemAlarm itemAlarm) {
            mItemAlarm = itemAlarm;
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName(getResources().getString(R.string.application_name))
                    .build();
        }

        @Override
        protected void onPreExecute() {
            if (mProgress != null && !mProgress.isShowing()) {
                mProgress.setMessage(getString(R.string.calling_api));
                mProgress.show();
            }
            checkAccount();
            EventTimeUtil eventTimeUtil = new EventTimeUtil();
            mTimeEvent = eventTimeUtil.eventTime(mItemAlarm.getTime());
        }

        @Override
        protected String doInBackground(String... params) {

            Event event = new Event()
                    .setSummary(mItemAlarm.getTitle().toString());

            DateTime startDateTime = new DateTime(mTimeEvent);
            EventDateTime start = new EventDateTime()
                    .setDateTime(startDateTime)
                    .setTimeZone(getString(R.string.time_zone));
            event.setStart(start);

            DateTime endDateTime = new DateTime(mTimeEvent);
            EventDateTime end = new EventDateTime()
                    .setDateTime(endDateTime)
                    .setTimeZone(getString(R.string.time_zone));
            event.setEnd(end);
            try {
                event = mService.events().insert(getResources().getString(R.string.primary), event)
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return event.getId().toString();
        }

        @Override
        protected void onPostExecute(String aVoid) {
            if (mProgress != null && mProgress.isShowing()) {
                mProgress.hide();
            }
            if (aVoid == null) {
                Toast.makeText(MainActivity.this, R.string.event_id_null, Toast.LENGTH_SHORT).show();
            } else {
                mItemAlarm.setIdEvent(aVoid);
                mMySqliteHelper.updateAlarm(mItemAlarm);
                new MakeRequestTask(mCredential).execute();
            }
        }
    }

    private class DeleteEvent extends AsyncTask<Void, Void, String> {
        private com.google.api.services.calendar.Calendar mService = null;
        private ItemAlarm mItemAlarm;

        public DeleteEvent(GoogleAccountCredential credential, ItemAlarm itemAlarm) {
            mItemAlarm = itemAlarm;
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName(getResources().getString(R.string.application_name))
                    .build();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mProgress != null && !mProgress.isShowing()) {
                mProgress.setMessage(getString(R.string.delete_api));
                mProgress.show();
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                mService.events().delete(getResources()
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
                mProgress.hide();
            }
            if (aVoid.equals(Const.DELETE_SUCCESS)) {
                new MakeRequestTask(mCredential).execute();
            } else {
                Toast.makeText(MainActivity.this, R.string.delete_fail, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class UpdateEvent extends AsyncTask<Void, Void, String> {
        private com.google.api.services.calendar.Calendar mService = null;
        private ItemAlarm mItemAlarm;
        private List<String> mDaylist = new ArrayList<>();
        private StringBuffer mByDay = new StringBuffer();

        public UpdateEvent(GoogleAccountCredential credential, ItemAlarm itemAlarm) {
            mItemAlarm = itemAlarm;
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName(getResources().getString(R.string.application_name))
                    .build();
        }

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
            if (mProgress != null && !mProgress.isShowing()) {
                mProgress.setMessage(getString(R.string.update_api));
                mProgress.show();
            }
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
                        .get(getResources().getString(R.string.primary), mItemAlarm.getIdEvent())
                        .execute();
                event.setSummary(mItemAlarm.getTitle().toString());
                String s = mItemAlarm.getTitle().toString();
                //             Lap event
                if (mDaylist.size() != 0) {
                    String[] recurrence = new String[]{
                            getResources().getString(R.string.recurrence_update) + mByDay
                    };
                    event.setRecurrence(Arrays.asList(recurrence));
                }
                event = mService.events()
                        .update(getResources().getString(R.string.primary), event.getId(), event)
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
                mProgress.hide();
            }
            if (aVoid != null) {
                new MakeRequestTask(mCredential).execute();
            } else {
                Toast.makeText(MainActivity.this, R.string.update_fail, Toast.LENGTH_SHORT).show();
            }
        }
    }

}