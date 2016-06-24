package com.framgia.youralarm1.activity;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TimePicker;
import android.widget.Toast;

import com.framgia.youralarm1.R;
import com.framgia.youralarm1.adapter.AlarmAdapter;
import com.framgia.youralarm1.contstant.Const;
import com.framgia.youralarm1.data.MySqliteHelper;
import com.framgia.youralarm1.models.ItemAlarm;
import com.framgia.youralarm1.util.CreatEvent;
import com.framgia.youralarm1.util.DeleteEvent;
import com.framgia.youralarm1.util.EventTimeUtil;
import com.framgia.youralarm1.util.EventUtil;
import com.framgia.youralarm1.util.MakeRequestTask;
import com.framgia.youralarm1.util.PreferenceUtil;
import com.framgia.youralarm1.util.SyncEventToServer;
import com.framgia.youralarm1.util.UpdateEvent;
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

import com.framgia.youralarm1.utils.AlarmUtils;
import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.iconics.IconicsDrawable;

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
    private static final String TAG_TASK_FRAGMENT = "task_fragment";
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
    private MakeRequestTask mMake;
    private CreatEvent mTestNew;
    private DeleteEvent mDeleteEvent;
    private UpdateEvent mUpdateEvent;
    private SyncEventToServer mToServer;
    private SynEventFromServer mFromServer;
    private boolean mCheckSyn = false;
    private MenuItem mMenuItem;
    private Toast mToast;

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
        FragmentManager fm = getSupportFragmentManager();
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
        ItemTouchHelper.SimpleCallback simpleCallback =
                new ItemTouchHelper.SimpleCallback(ItemTouchHelper.LEFT, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                onDeletedAlarm(viewHolder.getAdapterPosition());
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView,
                                    RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {

                Bitmap icon;
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;
                    float height = (float) itemView.getBottom() - (float) itemView.getTop();
                    float width = height / 3;
                    Paint paint = new Paint();
                    paint.setColor(getResources().getColor(R.color.bg_layut_delete));
                    RectF background = new RectF((float) itemView.getRight() + dX,
                                                 (float) itemView.getTop(),
                                                 (float) itemView.getRight(),
                                                 (float) itemView.getBottom());
                    c.drawRect(background, paint);
                    icon = new IconicsDrawable(MainActivity.this, FontAwesome.Icon.faw_trash)
                            .color(Color.WHITE)
                            .toBitmap();
                    RectF icon_dest = new RectF((float) itemView.getRight() - 2 * width,
                                                 (float) itemView.getTop() + width,
                                                 (float) itemView.getRight() - width,
                                                 (float)itemView.getBottom() - width);
                    c.drawBitmap(icon, null, icon_dest, paint);
                    }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(mRecyclerView);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (mFab != null) {
                    if (dy > Const.SCROLL_FAB_SHOW_THRESHOLD)
                        mFab.hide();
                    else if (dy <= -1 * Const.SCROLL_FAB_SHOW_THRESHOLD)
                        mFab.show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mInvalidDataReceiver, new IntentFilter(Const.ACTION_UPDATE_DATA));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mProgress != null) {
            mProgress.dismiss();
            mProgress = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mInvalidDataReceiver != null) {
            unregisterReceiver(mInvalidDataReceiver);
        }
    }

    @Override
    public void onBackPressed() {
        if (mToast == null) {
            mToast = Toast.makeText(MainActivity.this, R.string.confirm_exit, Toast.LENGTH_SHORT);
            mToast.show();
        } else {
            if (mToast.getView().isShown())
                super.onBackPressed();
            else mToast.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case Const.CHOOSE_RINGTONE_REQUEST:
                    if (data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) != null) {
                        Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                        if(uri.equals(Settings.System.DEFAULT_RINGTONE_URI))
                            uri = Uri.parse("");
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
                if (mRecyclerView != null)
                    Snackbar.make(mRecyclerView,
                                  R.string.install_play_service,
                                  Snackbar.LENGTH_SHORT)
                            .show();
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
                    if (mRecyclerView != null)
                        Snackbar.make(mRecyclerView,
                                      R.string.permission_get_account_failed,
                                      Snackbar.LENGTH_SHORT)
                                .show();
                    Log.d(TAG, getString(R.string.permission_get_account_failed));
                }
                return;
            default:
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
                AlarmUtils.setNextAlarm(MainActivity.this, itemAlarm, false, false);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDeletedAlarm(int position) {
        if(mPreferenceUtil.getStringData(Const.PREF_ACCOUNT_NAME) != null
                && EventUtil.isDeviceOnline()) {
            mDeleteEvent = new DeleteEvent();
            mDeleteEvent.getData(MainActivity.this,mCredential,mAlarmList.get(position));
            mDeleteEvent.excuteDelete();
        }
        try {
            mMySqliteHelper.deleteAlarm(mAlarmList.get(position));
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        AlarmUtils.cancelAlarm(MainActivity.this, mAlarmList.get(position));
        if (mFab != null) {
            if (!mFab.isShown())
                mFab.show();
            Snackbar.make(mFab, R.string.alarm_deleted, Snackbar.LENGTH_SHORT).show();
        }
        mAlarmList.remove(position);
        mAlarmAdapter.notifyItemRemoved(position);
        mAlarmAdapter.notifyDataSetChanged();
    }

    @Override
    public void onChangedAlarm(int position) {
        if(mPreferenceUtil.getStringData(Const.PREF_ACCOUNT_NAME) != null
                && EventUtil.isDeviceOnline()) {
            mUpdateEvent  = new UpdateEvent();
            mUpdateEvent.getData(MainActivity.this, mCredential, mAlarmList.get(position));
//            mUpdateEvent.executeUpdate();
        }
        try {
            updateAlarmDay(mAlarmList.get(position));
            if (mAlarmList.get(position).isStatus())
                AlarmUtils.setNextAlarm(MainActivity.this, mAlarmList.get(position), false, false);
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
                                if (mAlarmList.size() > 0) {
                                    mAlarmAdapter.setSelectedView(mAlarmList.size() - 1);
                                    mRecyclerView.smoothScrollToPosition(mAlarmList.size() - 1);
                                    if (mFab != null && !mFab.isShown())
                                        mFab.show();
                                }
                                AlarmUtils.setNextAlarm(MainActivity.this, itemAlarm, false, true);
                            } catch (SQLiteException e) {
                                e.printStackTrace();
                            }
                            if(mPreferenceUtil.getStringData(Const.PREF_ACCOUNT_NAME) != null
                                    && EventUtil.isDeviceOnline()){
                                mTestNew = new CreatEvent();
                                mTestNew.getData(MainActivity.this,mCredential,itemAlarm);
//                                mTestNew.excuteAsyntask();
                            }
                        }
                    }
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE) + 1, true);
        timePickerDialog.show();
    }

    private int addAlarmCache(ItemAlarm itemAlarm) throws SQLiteException {
        //TODO: Create schedule
        String s = itemAlarm.getIdEvent();
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

    private void checkAccount() {
        String result = mEventUtil.getResultsFromApi();
        if (result.equals(Const.ACCOUNT_SUCCESS)) {
            mMake = new MakeRequestTask();
            mMake.getData(MainActivity.this,mCredential);
//            mMake.excuteMake();
        } else if (result.equals(Const.NO_INTERNET)) {
            if (mRecyclerView != null)
                Snackbar.make(mRecyclerView, R.string.no_network, Snackbar.LENGTH_SHORT).show();
        } else if (result.equals(Const.NO_ACCOUNT)) {
            chooseAccount();
        }
        mAlarmList.clear();
        mAlarmList.addAll(mMySqliteHelper.getListAlarm());
        mAlarmAdapter.notifyDataSetChanged();
        if(mCheckSyn){
            String acc = mPreferenceUtil.getStringData(Const.PREF_ACCOUNT_NAME);
            if(acc != null
                    && EventUtil.isDeviceOnline()) {
                mFromServer = new SynEventFromServer();
                mFromServer.getData(MainActivity.this,mCredential,mAlarmList,mMenuItem);
                mFromServer.executeSyncFromServer();
            }else{
                mCheckSyn = false;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        checkAccount();
        mMenuItem = item;
        mCheckSyn = true;
        mAlarmList.clear();
        mAlarmList.addAll(mMySqliteHelper.getListAlarm());
        mAlarmAdapter.notifyDataSetChanged();
        if (item.getItemId() == R.id.action_sync) {
            item.setEnabled(false);
            if (mRecyclerView != null)
                Snackbar.make(mRecyclerView, R.string.sync_server, Snackbar.LENGTH_SHORT).show();
            if (mProgress != null && mProgress.isShowing()) {
                mProgress.dismiss();
            }
            String acc = mPreferenceUtil.getStringData(Const.PREF_ACCOUNT_NAME);
            if(acc != null
                    && EventUtil.isDeviceOnline()) {
                mFromServer = new SynEventFromServer();
                mFromServer.getData(MainActivity.this,mCredential,mAlarmList,item);
                mFromServer.executeSyncFromServer();
            }else{
                item.setEnabled(true);
            }
        } else if (item.getItemId() == R.id.action_settings) {
            Intent settingIntent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(settingIntent);
        }
        return super.onOptionsItemSelected(item);
    }
    public class SynEventFromServer extends Fragment{
        private Activity mActivity;
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;
        private List<ItemAlarm> mItemAlarms;
        private MenuItem mMenuItem;

        public SynEventFromServer() {
        }

        public void getData(Activity activity,
                            GoogleAccountCredential credential,
                            List<ItemAlarm> list,
                            MenuItem item) {
            mActivity = activity;
            mItemAlarms = list;
            mMenuItem = item;
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar
                    .Calendar.Builder(transport, jsonFactory, credential)
                    .setApplicationName(mActivity.getString(R.string.application_name))
                    .build();
        }
        public void executeSyncFromServer(){
            new SyncFromServer().execute();
        }
        public class SyncFromServer extends AsyncTask<Void, Void, List<Event>> {


            @Override
            protected List<Event> doInBackground(Void... params) {
                try {
                    return getDataFromApi();
                } catch (Exception e) {
                    mLastError = e;
                    cancel(true);
                    return null;
                }
            }

            private List<Event> getDataFromApi() throws IOException {
                DateTime now = new DateTime(System.currentTimeMillis() - Const.BEFORE_CURENT_TIME * 60);
                Events events = mService.events().list(mActivity.getString(R.string.primary))
                        .setMaxResults(Const.COUNT_RESULT)
                        .setTimeMin(now)
                        .setOrderBy(mActivity.getString(R.string.startTime))
                        .setSingleEvents(true)
                        .execute();
                List<Event> items = events.getItems();
                return items;
            }

            @Override
            protected void onPreExecute() {
                if (mProgress != null && !mProgress.isShowing()) {
                    mProgress.show();
                    mProgress.setCancelable(false);
                    mProgress.setCanceledOnTouchOutside(false);
                }
            }

            @Override
            protected void onPostExecute(List<Event> events) {
                if (events == null || events.size() == 0) {
                    for (ItemAlarm alarm : mItemAlarms) {
                        if (!alarm.getIdEvent().equals(Const.DEFAULT_EVENT_ID)) {
                            mItemAlarms.remove(alarm);
                            mMySqliteHelper.deleteAlarm(alarm);
                            mAlarmAdapter.notifyDataSetChanged();
                        }
                    }
                } else if (events.size() != 0) {
                    for (int i = 0; i < mItemAlarms.size(); i++) {
                        boolean flag = false;
                        for (Event event : events) {
                            if (mItemAlarms.get(i).getIdEvent().equals(getIdeventRepeat(event.getId()))) {
                                flag = true;
                            }
                        }
                        if (!flag && !mItemAlarms.get(i).getIdEvent().equals(Const.DEFAULT_EVENT_ID)) {
                            mMySqliteHelper.deleteAlarm(mItemAlarms.get(i));
                            mItemAlarms.clear();
                            mItemAlarms.addAll(mMySqliteHelper.getListAlarm());
                            mAlarmAdapter.notifyDataSetChanged();
                        }
                    }
                    for (Event event : events) {
                        int sum = event.getId().toString().length();
                        int eventTimeStart = EventTimeUtil.timeAlarm(event.getStart());// phut
                        if (sum < 30) {
                            boolean flag = false;
                            for (ItemAlarm alarm : mItemAlarms) {
                                if (event.getId().equals(alarm.getIdEvent())) {
                                    flag = true;
                                }
                            }
                            if (!flag) {
                                ItemAlarm itemAlarm = new ItemAlarm();
                                itemAlarm.setIdEvent(event.getId());
                                itemAlarm.setTime(eventTimeStart);
                                itemAlarm.setTitle(event.getSummary().toString());
                                int idAlarm = addAlarmCache(itemAlarm);
                                itemAlarm.setId(idAlarm);
                                itemAlarm.setIdEvent(event.getId());
                                mItemAlarms.clear();
                                mItemAlarms.addAll(mMySqliteHelper.getListAlarm());
                                mAlarmAdapter.notifyDataSetChanged();
                            }
                        } else if (sum > 30) {
                            boolean flag = false;
                            for (ItemAlarm alarm : mItemAlarms) {
                                if (getIdeventRepeat(event.getId())
                                        .equals(getIdeventRepeat(alarm.getIdEvent()))) {
                                    flag = true;
                                }
                            }
                            if (!flag) {
                                ItemAlarm itemAlarm = new ItemAlarm();
                                itemAlarm.setIdEvent(getIdeventRepeat(event.getId()));
                                itemAlarm.setTime(eventTimeStart);
                                itemAlarm.setTitle(event.getSummary().toString());
                                int idAlarm = addAlarmCache(itemAlarm);
                                itemAlarm.setId(idAlarm);
                                mItemAlarms.clear();
                                mItemAlarms.addAll(mMySqliteHelper.getListAlarm());
                                mAlarmAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                }
                if (mMySqliteHelper.getListAlarm().size() == 0) {
                    mItemAlarms.clear();
                    mAlarmAdapter.notifyDataSetChanged();
                }
                mToServer = new SyncEventToServer();
                mToServer.getData(mActivity, mCredential, mItemAlarms,mMenuItem, mProgress);
                mToServer.executeSync();
            }

            private String getIdeventRepeat(String s) {
                if (s.length() > 30) {
                    String result[] = s.split("[_]");
                    return result[0];
                }
                return s;
            }

            @Override
            protected void onCancelled() {
                if (mLastError != null) {
                    if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                        EventUtil.showGooglePlayServicesAvailabilityErrorDialog(
                                ((GooglePlayServicesAvailabilityIOException) mLastError)
                                        .getConnectionStatusCode());
                    } else if (mLastError instanceof UserRecoverableAuthIOException) {
                        startActivityForResult(
                                ((UserRecoverableAuthIOException) mLastError).getIntent(),
                                Const.REQUEST_AUTHORIZATION);
                    } else {
                        if (mRecyclerView != null)
                            Snackbar.make(mRecyclerView, R.string.error_occurred, Snackbar.LENGTH_SHORT)
                                    .show();
                    }
                } else {
                    if (mRecyclerView != null)
                        Snackbar.make(mRecyclerView,
                                      R.string.request_canclled,
                                      Snackbar.LENGTH_SHORT)
                                .show();
                }
                mToServer = new SyncEventToServer();
                mToServer.getData(mActivity, mCredential, mItemAlarms, mMenuItem, mProgress);
                mToServer.executeSync();
            }
        }
    }
}