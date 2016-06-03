package com.framgia.youralarm1.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import com.framgia.youralarm1.models.ItemAlarm;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by vuduychuong1994 on 4/18/16.
 */
public class MySqliteHelper extends SQLiteOpenHelper {
    //Database Config
    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "MyAlarm.db";
    //Table name
    public static final String TABLE_ALARM = "Alarm";

    //Column name
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_IS_VIBRATE = "is_vibrate";
    public static final String COLUMN_IS_RINGTONE = "is_ringtone";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_RINGTONE_PATH = "ringtone_path";
    public static final String COLUMN_WEEKDAY = "weekday";

    public MySqliteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        StringBuffer createAlarmTable =
                new StringBuffer().append("CREATE TABLE ").append(TABLE_ALARM + " (")
                        .append(COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, ")
                        .append(COLUMN_TIME).append(" INTEGER, ")
                        .append(COLUMN_STATUS).append(" TEXT, ")
                        .append(COLUMN_IS_VIBRATE).append(" TEXT, ")
                        .append(COLUMN_IS_RINGTONE).append(" INTEGER, ")
                        .append(COLUMN_TITLE).append(" TEXT, ")
                        .append(COLUMN_RINGTONE_PATH).append(" TEXT, ")
                        .append(COLUMN_WEEKDAY).append(" INTEGER)");
        db.execSQL(createAlarmTable.toString());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ALARM);
        onCreate(db);
    }

    //region ALARM
    // Add Alarm
    public long addAlarm(ItemAlarm itemAlarm) throws SQLiteException {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        if ( itemAlarm != null ) {
            cv.put(COLUMN_TIME, itemAlarm.getTime());
            cv.put(COLUMN_STATUS, itemAlarm.isStatus());
            cv.put(COLUMN_IS_VIBRATE, itemAlarm.isVibrate());
            cv.put(COLUMN_IS_RINGTONE, itemAlarm.isRingtone());
            cv.put(COLUMN_TITLE, itemAlarm.getTitle());
            cv.put(COLUMN_RINGTONE_PATH, itemAlarm.getRingTonePath());
            cv.put(COLUMN_WEEKDAY, new Gson().toJson(itemAlarm.getWeekDayHashMap()));
        }
        long id = db.insert(TABLE_ALARM, null, cv);
        db.close();
        return id;
    }

    public int addListAlarm(List<ItemAlarm> alarmList){
        int count = 0;
        for (ItemAlarm itemAlarm : alarmList) {
            addAlarm(itemAlarm);
            count++;
        }
        return count;
    }

    // Read Alarm

    public ItemAlarm getAlarm(int id) throws SQLiteException {
        ItemAlarm itemAlarm = new ItemAlarm();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor =
                db.query(TABLE_ALARM, null, COLUMN_ID + " = ?", new String[]{String.valueOf(id)},
                         null, null, null);
        if ( cursor != null && cursor.moveToFirst() ) {
            itemAlarm.setId(cursor.getInt(cursor.getColumnIndex(COLUMN_ID)));
            itemAlarm.setTime(cursor.getInt(cursor.getColumnIndex(COLUMN_TIME)));
            itemAlarm.setStatus(cursor.getInt(cursor.getColumnIndex(COLUMN_STATUS)) == 1);
            itemAlarm.setVibrate(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_VIBRATE)) == 1);
            itemAlarm.setRingtone(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_RINGTONE)) == 1);
            itemAlarm.setTitle(cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)));
            itemAlarm.setRingTonePath(
                    cursor.getString(cursor.getColumnIndex(COLUMN_RINGTONE_PATH)));
            Type type = new TypeToken<HashMap<ItemAlarm.WeekDay, Boolean>>() {}.getType();
            itemAlarm.setWeekDayHashMap((HashMap<ItemAlarm.WeekDay, Boolean>) new Gson().fromJson(
                    cursor.getString(cursor.getColumnIndex(COLUMN_WEEKDAY)), type));
        }
        if ( cursor != null )
            cursor.close();
        db.close();
        return itemAlarm;
    }

    public List<ItemAlarm> getListAlarm() throws SQLiteException {
        List<ItemAlarm> alarmList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ALARM, null, null, null, null, null, null);
        if ( cursor.getCount() > 0 && cursor.moveToFirst() ) {
            do {
                ItemAlarm itemAlarm = new ItemAlarm();
                itemAlarm.setId(cursor.getInt(cursor.getColumnIndex(COLUMN_ID)));
                itemAlarm.setTime(cursor.getInt(cursor.getColumnIndex(COLUMN_TIME)));
                itemAlarm.setStatus(cursor.getInt(cursor.getColumnIndex(COLUMN_STATUS)) == 1);
                itemAlarm.setVibrate(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_VIBRATE)) == 1);
                itemAlarm.setRingtone(
                        cursor.getInt(cursor.getColumnIndex(COLUMN_IS_RINGTONE)) == 1);
                itemAlarm.setTitle(cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)));
                itemAlarm.setRingTonePath(
                        cursor.getString(cursor.getColumnIndex(COLUMN_RINGTONE_PATH)));
                Type type = new TypeToken<HashMap<ItemAlarm.WeekDay, Boolean>>() {}.getType();
                itemAlarm.setWeekDayHashMap(
                        (HashMap<ItemAlarm.WeekDay, Boolean>) new Gson().fromJson(
                                cursor.getString(cursor.getColumnIndex(COLUMN_WEEKDAY)), type));
                alarmList.add(itemAlarm);
            }
            while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return alarmList;
    }

    // Update Alarm
    public long updateAlarm(ItemAlarm itemAlarm) throws SQLiteException {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        if ( itemAlarm != null ) {
            cv.put(COLUMN_ID, itemAlarm.getId());
            cv.put(COLUMN_TIME, itemAlarm.getTime());
            cv.put(COLUMN_STATUS, itemAlarm.isStatus());
            cv.put(COLUMN_IS_VIBRATE, itemAlarm.isVibrate());
            cv.put(COLUMN_IS_RINGTONE, itemAlarm.isRingtone());
            cv.put(COLUMN_TITLE, itemAlarm.getTitle());
            cv.put(COLUMN_RINGTONE_PATH, itemAlarm.getRingTonePath());
            cv.put(COLUMN_WEEKDAY, new Gson().toJson(itemAlarm.getWeekDayHashMap()));
        }
        long numRow = db.update(TABLE_ALARM, cv, COLUMN_ID + " = ?",
                                new String[]{String.valueOf(itemAlarm.getId())});
        db.close();
        return numRow;
    }
    //endregion

    // Delete Alarm
    public boolean deleteAlarm(ItemAlarm itemAlarm) throws SQLiteException {
        SQLiteDatabase db = this.getWritableDatabase();
        if (itemAlarm != null) {
            long numRow = db.delete(TABLE_ALARM, COLUMN_ID + " = ?",
                                    new String[]{String.valueOf(itemAlarm.getId())});
            db.close();
            return numRow != 0;
        } else
            return false;
    }

    public int countTable(String tableName) throws SQLiteException {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = (int) DatabaseUtils.queryNumEntries(db, tableName);
        db.close();
        return count;
    }

}
