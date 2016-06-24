package com.framgia.youralarm1.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import com.framgia.youralarm1.R;
import com.framgia.youralarm1.contstant.Const;
import com.framgia.youralarm1.models.ItemAlarm;
import com.framgia.youralarm1.util.PreferenceUtil;
import com.framgia.youralarm1.utils.AlarmUtils;
import com.framgia.youralarm1.utils.ParseTimeUtils;
import com.framgia.youralarm1.widget.SlideButton;
import java.io.IOException;

public class AlertAlarmActivity extends AppCompatActivity
        implements SlideButton.OnSlideListener {
    private static final String TAG = AlertAlarmActivity.class.getName();
    private static final long SNOOZE_TIME = 60000;
    private TextView mTextTitle;
    private TextView mTextTime;
    private SlideButton mSlideButtonAlarm;
    private ItemAlarm itemAlarm;
    private MediaPlayer mMediaPlayer;
    private Vibrator vibrator;
    private AudioManager audioManager;
    private CountDownTimer mCountDownTimer;
    private boolean mIsDismissed = false;
    private boolean mIsSnooze = false;
    private BroadcastReceiver mPowerPressReceiver;
    private BroadcastReceiver mStatusReceiver;
    private long mSnoozeTime;
    private PreferenceUtil mPreferenceUtil;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                                          WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                                          WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                                  WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                                          WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                                          WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_alarm);
        setView();
        setData();
        setEvent();
        setAction();

    }

    @Override
    public void onSlideListener(View view, int swipedSide) {
        switch (swipedSide) {
            case SlideButton.SLIDE_LEFT:
                onSnoozeAlarm();
                break;
            case SlideButton.SLIDE_RIGHT:
                onDismissAlarm();
                break;
            default:
                break;
        }
    }



    private void setAction() {
        Intent intent = getIntent();
        String action = intent.getAction();
        switch (action){
            case Const.ACTION_SNOOZE_ALARM:
                Intent snoozeIntent = new Intent(Const.ACTION_SNOOZE_ALARM);
                sendBroadcast(snoozeIntent);
                break;
            case Const.ACTION_DISMISS_ALARM:
                Intent dismissIntent = new Intent(Const.ACTION_DISMISS_ALARM);
                sendBroadcast(dismissIntent);
                break;
            case Const.ACTION_FULLSCREEN_ACTIVITY:
                playRingAndVibrate();
                break;
            default:
                playRingAndVibrate();
                break;
        }
    }

    private void setEvent() {
        mSlideButtonAlarm.setOnSlideListener(this);
    }

    private void setData() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null)
            itemAlarm = (ItemAlarm) bundle.getSerializable(Const.ITEM_ALARM);
        if (itemAlarm != null) {
            if (itemAlarm.getWeekDayHashMap().containsValue(true)) {
                //TODO: set the next time alarm;
                AlarmUtils.setNextAlarm(AlertAlarmActivity.this, itemAlarm, true, false);
            }
            mTextTitle.setText(itemAlarm.getTitle());
            mTextTime.setText(ParseTimeUtils.formatTextTime(itemAlarm.getTime()));
        }
        mPreferenceUtil = new PreferenceUtil(AlertAlarmActivity.this);
        try {
            mSnoozeTime = Long.parseLong(mPreferenceUtil.getStringData(Const.MY_PREFERENCES,
                                                                       Const.PRE_SNOOZE_TIME));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            mSnoozeTime = -1;
        }


        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mCountDownTimer = new CountDownTimer(Const.RING_TIME, Const.RING_TIME) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                if (mSnoozeTime != -1)
                    AlarmUtils.setSnoozeAlarm(AlertAlarmActivity.this, itemAlarm, mSnoozeTime);
                updateWhenAppOpenning();
                finish();
            }
        };
    }

    private void playRingAndVibrate() {
        mCountDownTimer.start();
        if (vibrator.hasVibrator()) {
            try {
                vibrator.vibrate(new long[]{500, 500}, 0);
            } catch (ArrayIndexOutOfBoundsException e){
                e.printStackTrace();
            }
        }
        Uri ringtoneUri;
        if (itemAlarm.getRingTonePath() != null) {
            ringtoneUri = Uri.parse(itemAlarm.getRingTonePath());
        } else
            ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(this, ringtoneUri);
            if (audioManager.getStreamVolume(AudioManager.STREAM_RING) != 0) {
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
                mMediaPlayer.setLooping(true);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopAlarm() {
        if (vibrator.hasVibrator()) {
            vibrator.cancel();
        }
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
            mMediaPlayer = null;
        }
    }

    private void setView() {
        mSlideButtonAlarm = (SlideButton) findViewById(R.id.slidebutton_alarm);
        mTextTitle = (TextView) findViewById(R.id.text_time_alarm);
        mTextTime = (TextView) findViewById(R.id.text_time_alarm);
    }

    private void onDismissAlarm() {
        mIsDismissed = true;
        AlarmUtils.setNextAlarm(AlertAlarmActivity.this, itemAlarm, true, false);
        mCountDownTimer.cancel();
        updateWhenAppOpenning();
        finish();
    }

    private void onSnoozeAlarm() {
        mIsSnooze = true;
        mCountDownTimer.cancel();
        AlarmUtils.setSnoozeAlarm(AlertAlarmActivity.this, itemAlarm, mSnoozeTime);
        updateWhenAppOpenning();
        finish();
    }

    private void updateWhenAppOpenning(){
        Intent mainIntent = new Intent(Const.ACTION_UPDATE_DATA);
        sendBroadcast(mainIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsDismissed = false;
        mIsSnooze = false;

        if (mStatusReceiver == null) {
            mStatusReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (intent.getAction()) {
                        case Const.ACTION_DISMISS_ALARM:
                            onDismissAlarm();
                            break;
                        case Const.ACTION_SNOOZE_ALARM:
                            onSnoozeAlarm();
                            break;
                    }
                }
            };
        }
        IntentFilter filterAlarm = new IntentFilter(Const.ACTION_ALARM);
        registerReceiver(mStatusReceiver, filterAlarm);

        if (mPowerPressReceiver == null) {
            mPowerPressReceiver  = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (intent.getAction()) {
                        case Intent.ACTION_SCREEN_OFF:
                            if (!mIsDismissed)
                                onDismissAlarm();
                            break;
                        default:
                            break;
                    }
                }
            };
        }
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mPowerPressReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPowerPressReceiver != null) {
            unregisterReceiver(mPowerPressReceiver);
        }
        if (mStatusReceiver != null) {
            unregisterReceiver(mStatusReceiver);
        }
        if (!mIsDismissed && !mIsSnooze)
            onDismissAlarm();
        stopAlarm();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (!mIsDismissed && !mIsSnooze)
            onDismissAlarm();
    }
}
