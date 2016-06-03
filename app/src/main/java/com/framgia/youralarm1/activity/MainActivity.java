package com.framgia.youralarm1.activity;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TimePicker;
import com.framgia.youralarm1.R;
import com.framgia.youralarm1.adapter.AlarmAdapter;
import com.framgia.youralarm1.contstant.Const;
import com.framgia.youralarm1.data.MySqliteHelper;
import com.framgia.youralarm1.models.ItemAlarm;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements AlarmAdapter.OnAdapterInterActionListener {

    private static final String TAG = MainActivity.class.getName();

    private FloatingActionButton mFab;
    private Toolbar mToolbar;
    private RecyclerView mRecyclerView;
    private AlarmAdapter mAlarmAdapter;
    private List<ItemAlarm> mAlarmList;
    private MySqliteHelper mMySqliteHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setView();
        setEvent();

    }

    private void setView() {
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
        if (resultCode == RESULT_OK)
            switch (requestCode) {
                case Const.CHOOSE_RINGTONE_REQUEST:
                    if (data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) !=
                            null) {
                        Uri uri =
                                data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
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
                                if(mAlarmList.size() > 0)
                                    mRecyclerView.smoothScrollToPosition(mAlarmList.size() - 1);
                            } catch (SQLiteException e){
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
}