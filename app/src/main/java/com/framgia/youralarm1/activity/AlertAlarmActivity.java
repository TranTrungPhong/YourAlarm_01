package com.framgia.youralarm1.activity;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import com.framgia.youralarm1.R;
import com.framgia.youralarm1.contstant.Const;
import com.framgia.youralarm1.data.MySqliteHelper;
import com.framgia.youralarm1.models.ItemAlarm;
import com.framgia.youralarm1.utils.AlarmUtils;
import com.framgia.youralarm1.utils.NotificationUtils;
import com.framgia.youralarm1.widget.CircularButton;
import java.io.IOException;

public class AlertAlarmActivity extends AppCompatActivity
        implements CircularButton.OnClickInteractionListener {
    private static final String TAG = AlertAlarmActivity.class.getName();
    private static final long SNOOZE_TIME = 60000;
    private CircularButton mCircularButtonDismiss;
    private CircularButton mCircularButtonSnooze;
    private ItemAlarm itemAlarm;
    private MediaPlayer mMediaPlayer;
    private Vibrator vibrator;
    private AudioManager audioManager;
    private CountDownTimer mCountDownTimer;

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
        setAction();

    }

    private void setAction() {
        Intent intent = getIntent();
        String action = intent.getAction();
        switch (action){
            case Const.ACTION_SNOOZE_ALARM:
                onSnoozeAlarm();
                break;
            case Const.ACTION_DISMISS_ALARM:
                onDismissAlarm();
                break;
            case Const.ACTION_FULLSCREEN_ACTIVITY:
                playRingAndVibrate();
                break;
            default:
                playRingAndVibrate();
                break;
        }
    }

    private void setData() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null)
            itemAlarm = (ItemAlarm) bundle.getSerializable(Const.ITEM_ALARM);
        if (itemAlarm != null) {
            if (itemAlarm.getWeekDayHashMap().containsValue(true)) {
                //TODO: set the next time alarm;
                AlarmUtils.setNextAlarm(AlertAlarmActivity.this, itemAlarm);
            }
        }
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mCountDownTimer = new CountDownTimer(Const.RING_TIME, Const.RING_TIME) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                AlarmUtils.setSnoozeAlarm(AlertAlarmActivity.this, itemAlarm, SNOOZE_TIME);
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
        mCircularButtonDismiss = (CircularButton) findViewById(R.id.circle_button_dismiss);
        mCircularButtonDismiss.setOnClickInteractionListener(this);
        mCircularButtonSnooze = (CircularButton) findViewById(R.id.circle_button_snooze);
        mCircularButtonSnooze.setOnClickInteractionListener(this);
    }

    @Override
    public void onClickInteractionListener(View view, int id, boolean isSelected) {
        switch (view.getId()) {
            case R.id.circle_button_dismiss:
                onDismissAlarm();
                break;
            case R.id.circle_button_snooze:
                onSnoozeAlarm();
                break;
        }
    }

    private void onDismissAlarm() {
        MySqliteHelper mySqliteHelper = new MySqliteHelper(AlertAlarmActivity.this);
        itemAlarm.setStatus(false);
        mySqliteHelper.updateAlarm(itemAlarm);
        mCountDownTimer.cancel();
        AlarmUtils.cancelAlarm(AlertAlarmActivity.this, itemAlarm);
        updateWhenAppOpenning();
        finish();
    }

    private void onSnoozeAlarm() {
        mCountDownTimer.cancel();
        AlarmUtils.setSnoozeAlarm(AlertAlarmActivity.this, itemAlarm, SNOOZE_TIME);
        updateWhenAppOpenning();
        finish();
    }

    private void updateWhenAppOpenning(){
        Intent mainIntent = new Intent(Const.ACTION_UPDATE_DATA);
        sendBroadcast(mainIntent);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlarm();
    }
}