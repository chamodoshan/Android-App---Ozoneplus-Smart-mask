package com.sk.ozoneplus.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by ShimaK on 15-Apr-17.
 */

public class MaskDB_Manger extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ozone_plus.db";
    private static final int DATABASE_VERSION = 1;

    private static final String COLUMN_USERID = "userId";
    private static final String COLUMN_GASID = "gasId";

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
                    COLUMN_GAS_YEARLY_MONTH + " INTEGER PRIMARY KEY," +
                    COLUMN_GAS_YEARLY_LEVEL + ")";
    private static final String CREATE_ENTRIES_TABLE_MONTHLY =
            "CREATE TABLE " + TABLE_GAS_MONTHLY + "(" +
                    COLUMN_GAS_MONTHLY_DAY + " INTEGER PRIMARY KEY," +
                    COLUMN_GAS_MONTHLY_LEVEL + ")";
    private static final String CREATE_ENTRIES_TABLE_DAILY =
            "CREATE TABLE " + TABLE_GAS_DAILY + "(" +
                    COLUMN_GAS_DAILY_HOUR + " INTEGER PRIMARY KEY," +
                    COLUMN_GAS_DAILY_LEVEL + ")";

    public MaskDB_Manger(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_ENTRIES_TABLE_DAILY);
        db.execSQL(CREATE_ENTRIES_TABLE_MONTHLY);
        db.execSQL(CREATE_ENTRIES_TABLE_YEARLY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
