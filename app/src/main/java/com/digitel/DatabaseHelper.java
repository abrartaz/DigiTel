package com.digitel;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "fuel_db";
    private static final int DATABASE_VERSION = 2; // Increment version when changing schema
    public static final String TABLE_NAME = "fuel_entries";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_PRICE_PER_LITRE = "pricePerLitre";
    public static final String COLUMN_TOTAL_SALE = "totalSale";
    public static final String COLUMN_TIME = "time";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PRICE_PER_LITRE + " REAL, " +
                COLUMN_TOTAL_SALE + " REAL, " +
                COLUMN_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP)";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop the old table and recreate it
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void insertFuelEntry(Context context, double pricePerLiter, double totalSale) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PRICE_PER_LITRE, pricePerLiter);
        values.put(COLUMN_TOTAL_SALE, totalSale);
        long result = db.insert(TABLE_NAME, null, values);
        db.close();

        // Check if the insertion was successful and show a toast message accordingly
//        if (result != -1) {
//            // Entry inserted successfully
//            Toast.makeText(context, "Entry inserted successfully", Toast.LENGTH_SHORT).show();
//        } else {
//            // Failed to insert entry
//            Toast.makeText(context, "Failed to insert entry", Toast.LENGTH_SHORT).show();
//        }
    }

    public Cursor getAllEntries() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_NAME, null, null, null, null, null, null);
    }
}
