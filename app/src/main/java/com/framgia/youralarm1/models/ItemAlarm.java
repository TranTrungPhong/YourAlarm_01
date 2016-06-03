package com.framgia.youralarm1.models;
import com.framgia.youralarm1.contstant.Const;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by vuduychuong1994 on 6/3/16.
 */
public class ItemAlarm {
    public enum WeekDay {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;
    }
    private int mId;
    private int mTime;
    private boolean mStatus;
    private boolean mIsVibrate;
    private boolean mIsRingtone;
    private boolean mIsExpand;
    private String mTitle;
    private String mRingTonePath;
    private HashMap<WeekDay, Boolean> mWeekDayHashMap;

    public ItemAlarm() {
        mTime = 0;
        mStatus = true;
        mIsVibrate = true;
        mIsRingtone = true;
        mIsExpand = false;
        mTitle = Const.DEFAUL_TITLE;
        mWeekDayHashMap = new HashMap<>();

    }

    public ItemAlarm(int time, boolean status, boolean isVibrate, boolean isRingtone,
                     boolean isExpand, String title, String ringTonePath,
                     HashMap<WeekDay, Boolean> weekDayHashMap) {
        mTime = time;
        mStatus = status;
        mIsVibrate = isVibrate;
        mIsRingtone = isRingtone;
        mIsExpand = isExpand;
        mTitle = title;
        mRingTonePath = ringTonePath;
        mWeekDayHashMap = weekDayHashMap;
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }

    public int getTime() {
        return mTime;
    }

    public void setTime(int time) {
        mTime = time;
    }

    public boolean isStatus() {
        return mStatus;
    }

    public void setStatus(boolean status) {
        mStatus = status;
    }

    public boolean isVibrate() {
        return mIsVibrate;
    }

    public void setVibrate(boolean vibrate) {
        mIsVibrate = vibrate;
    }

    public boolean isRingtone() {
        return mIsRingtone;
    }

    public void setRingtone(boolean ringtone) {
        mIsRingtone = ringtone;
    }

    public boolean isExpand() {
        return mIsExpand;
    }

    public void setExpand(boolean expand) {
        mIsExpand = expand;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getRingTonePath() {
        return mRingTonePath;
    }

    public void setRingTonePath(String ringTonePath) {
        mRingTonePath = ringTonePath;
    }

    public HashMap<WeekDay, Boolean> getWeekDayHashMap() {
        return mWeekDayHashMap;
    }

    public void setWeekDayHashMap(HashMap<WeekDay, Boolean> weekDayHashMap) {
        mWeekDayHashMap = weekDayHashMap;
    }
}
