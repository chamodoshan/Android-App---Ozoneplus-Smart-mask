package com.sk.ozoneplus;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by ShimaK on 15-Apr-17.
 */

public class MaskDB_Manger extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "ozone_plus.db";
    private static final int DATABASE_VERSION = 1;
    private static String USERNAME;

    private static final String COLUMN_USERNAME = "userId";
    private static final String COLUMN_GAS_ID = "gasId";

    private static final String TABLE_GAS_YEARLY = "Gas_Yearly";
    private static final String COLUMN_GAS_YEARLY_MONTH = "month";
    private static final String COLUMN_GAS_YEARLY_LEVEL = "level";

    private static final String TABLE_GAS_MONTHLY = "Gas_Monthly";
    private static final String COLUMN_GAS_MONTHLY_DAY = "day";
    private static final String COLUMN_GAS_MONTHLY_LEVEL = "level";

    private static final String TABLE_GAS_DAILY = "Gas_Daily";
    private static final String COLUMN_GAS_DAILY_HOUR = "hour";
    private static final String COLUMN_GAS_DAILY_LEVEL = "level";

    private static final String CREATE_ENTRIES_TABLE_YEARLY =
            "CREATE TABLE " + TABLE_GAS_YEARLY + "(" +
                    COLUMN_GAS_YEARLY_MONTH + " INTEGER NOT NULL," +
                    COLUMN_GAS_YEARLY_LEVEL + " INTEGER NOT NULL," +
                    COLUMN_GAS_ID + " INTEGER NOT NULL," +
                    "PRIMARY KEY (" + COLUMN_GAS_YEARLY_MONTH + "," + COLUMN_GAS_YEARLY_LEVEL
                    + "," + COLUMN_GAS_ID + "))";
    private static final String CREATE_ENTRIES_TABLE_MONTHLY =
            "CREATE TABLE " + TABLE_GAS_MONTHLY + "(" +
                    COLUMN_GAS_MONTHLY_DAY + " INTEGER NOT NULL," +
                    COLUMN_GAS_MONTHLY_LEVEL + " INTEGER NOT NULL," +
                    COLUMN_GAS_ID + " INTEGER NOT NULL," +
                    "PRIMARY KEY (" + COLUMN_GAS_MONTHLY_DAY + "," + COLUMN_GAS_MONTHLY_LEVEL
                    + "," + COLUMN_GAS_ID + "))";
    private static final String CREATE_ENTRIES_TABLE_DAILY =
            "CREATE TABLE " + TABLE_GAS_DAILY + "(" +
                    COLUMN_GAS_DAILY_HOUR + " INTEGER NOT NULL," +
                    COLUMN_GAS_DAILY_LEVEL + " INTEGER NOT NULL," +
                    COLUMN_GAS_ID + " INTEGER NOT NULL," +
                    "PRIMARY KEY (" + COLUMN_GAS_DAILY_HOUR + "," + COLUMN_GAS_DAILY_LEVEL
                    + "," + COLUMN_GAS_ID + "))";

    public MaskDB_Manger(Context context, String username) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        USERNAME = username;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_ENTRIES_TABLE_DAILY);
        db.execSQL(CREATE_ENTRIES_TABLE_MONTHLY);
        db.execSQL(CREATE_ENTRIES_TABLE_YEARLY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //TODO search about it
        //db.execSQL("DROP TABLE IF EXISTS " + TABLE_GAS_DAILY);
    }

    public void dropDatabase() {
        SQLiteDatabase database = this.getWritableDatabase();
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_GAS_DAILY);
    }

    public void insertDaily(int hour, int level, int gasId) {
        ContentValues contents = new ContentValues();
        //contents.put(COLUMN_USERNAME, USERNAME);
        contents.put(COLUMN_GAS_ID, gasId);
        contents.put(COLUMN_GAS_DAILY_HOUR, hour);
        contents.put(COLUMN_GAS_DAILY_LEVEL, level);
        this.getWritableDatabase().insert(TABLE_GAS_DAILY, null, contents);
    }

    public void insertMonthly(int day, int level, int gasId) {
        ContentValues contents = new ContentValues();
        //contents.put(COLUMN_USERNAME, USERNAME);
        contents.put(COLUMN_GAS_ID, gasId);
        contents.put(COLUMN_GAS_MONTHLY_DAY, day);
        contents.put(COLUMN_GAS_MONTHLY_LEVEL, level);
        this.getWritableDatabase().insert(TABLE_GAS_MONTHLY, null, contents);
    }

    public void insertYearly(int month, int level, int gasId) {
        ContentValues contents = new ContentValues();
        //contents.put(COLUMN_USERNAME, USERNAME);
        contents.put(COLUMN_GAS_ID, gasId);
        contents.put(COLUMN_GAS_YEARLY_MONTH, month);
        contents.put(COLUMN_GAS_YEARLY_LEVEL, level);
        this.getWritableDatabase().insert(TABLE_GAS_MONTHLY, null, contents);
    }

    public Cursor getDaily() {
        return this.getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_GAS_DAILY, null);
    }

    public Cursor getMonthly() {
        return this.getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_GAS_MONTHLY, null);
    }

    public Cursor getYearly() {
        return this.getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_GAS_YEARLY, null);
    }
}
