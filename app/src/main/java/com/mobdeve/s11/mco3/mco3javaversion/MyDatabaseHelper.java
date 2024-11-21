package com.mobdeve.s11.mco3.mco3javaversion;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class MyDatabaseHelper extends SQLiteOpenHelper {

    private Context context;
    private static final String DATABASE_NAME = "Recordings.db";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_NAME = "my_recording";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_DATE = "recording_date";
    private static final String COLUMN_TIMESTAMP = "recording_timestamp";

    private static final String TABLE2_NAME = "coordinates_table";
    private static final String COLUMN2_COORDINATE_ID = "coordinate_id";
    private static final String COLUMN2_RECORDING_ID = "recording_id";
    private static final String COLUMN2_LATITUDE = "latitude";
    private static final String COLUMN2_LONGITUDE = "longitude";
    private static final String COLUMN2_ANOMALY = "anomaly";

    private static final String TABLE3_NAME = "anomaly_table";
    private static final String COLUMN3_ID = "anomaly_id";
    private static final String COLUMN3_ANOMALY_NAME = "anomaly_name";


    public MyDatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        Toast.makeText(this.context, "DB INITIALIZED", Toast.LENGTH_SHORT).show();

        String createAnomalyTableQuery = "CREATE TABLE " + TABLE3_NAME + "(" +
                COLUMN3_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN3_ANOMALY_NAME + " TEXT UNIQUE NOT NULL);";
        db.execSQL(createAnomalyTableQuery);

        // Step 2: Populate anomaly_table with initial allowed values
        String insertInitialAnomaliesQuery = "INSERT INTO " + TABLE3_NAME + " (" + COLUMN3_ANOMALY_NAME + ") VALUES " +
                "('Pothole'), ('Road Crack'), ('Speed Bump');"; // Add more as needed
        db.execSQL(insertInitialAnomaliesQuery);

        String createCoordinatesTable = "CREATE TABLE " + TABLE2_NAME +
                " (" + COLUMN2_COORDINATE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN2_RECORDING_ID + " INTEGER, " +
                COLUMN2_LATITUDE + " REAL," +
                COLUMN2_LONGITUDE + " REAL," +
                COLUMN2_ANOMALY + " TEXT NOT NULL," +
                "FOREIGN KEY(" + COLUMN2_ANOMALY + ") REFERENCES " + TABLE3_NAME + "(" + COLUMN3_ANOMALY_NAME + ") ON DELETE RESTRICT, " +
                "FOREIGN KEY(" + COLUMN2_RECORDING_ID + ") REFERENCES " + TABLE_NAME + "(" + COLUMN_ID + "));";
        db.execSQL(createCoordinatesTable);

        String createRecordingsTable = "CREATE TABLE " + TABLE_NAME +
                " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_DATE + " TEXT, " +
                COLUMN_TIMESTAMP + " TEXT);";
        db.execSQL(createRecordingsTable);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE3_NAME); // Drop anomaly_table
        db.execSQL("DROP TABLE IF EXISTS " + TABLE2_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);

        onCreate(db);
    }

    // Use only when needed
    void dropTable() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE3_NAME); // Drop anomaly_table
        db.execSQL("DROP TABLE IF EXISTS " + TABLE2_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);

        onCreate(db);

        Toast.makeText(this.context, "DB DROPPED", Toast.LENGTH_SHORT).show();
    }

    long addRecording(String date, String timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_DATE, date);
        cv.put(COLUMN_TIMESTAMP, timestamp);
        long row_result = db.insert(TABLE_NAME, null, cv);
        if (row_result == -1) {
            Toast.makeText(context, "Recording failed", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Recording successful", Toast.LENGTH_SHORT).show();
        }
        return row_result;
    }

    Cursor readAllData() {
        String query = "SELECT * FROM " + TABLE_NAME;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = null;
        if (db != null) {
            cursor = db.rawQuery(query, null);
        }

        return cursor;
    }

    void addCoordinate(int recordingId, double latitude, double longitude, String anomaly) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(COLUMN2_RECORDING_ID, recordingId);
        cv.put(COLUMN2_LATITUDE, latitude);
        cv.put(COLUMN2_LONGITUDE, longitude);
        cv.put(COLUMN2_ANOMALY, anomaly);

        long result = db.insert("coordinates_table", null, cv);
        if (result == -1) {
            Toast.makeText(context, "Failed to add anomaly", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Anomaly added", Toast.LENGTH_SHORT).show();
        }
    }

    Cursor getCoordinates(int recordingId) {
        String query = "SELECT " + COLUMN2_LATITUDE + ", " + COLUMN2_LONGITUDE + ", " + COLUMN2_ANOMALY+
                " FROM " + TABLE2_NAME + " WHERE " + COLUMN2_RECORDING_ID + " = ?";
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = null;
        if (db != null) {
            cursor = db.rawQuery(query, new String[]{String.valueOf(recordingId)});
        }
        return cursor;

    }

    public void addAllowedAnomaly(String anomalyName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("anomaly_name", anomalyName);

        long result = db.insert("anomaly_table", null, cv);
        if (result == -1) {
            Toast.makeText(context, "Failed to add anomaly", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Anomaly added successfully", Toast.LENGTH_SHORT).show();
        }
    }


    public Cursor getAllAnomalyNames() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN3_ANOMALY_NAME + " FROM " + TABLE3_NAME;

        // Execute the query and return the cursor with results
        return db.rawQuery(query, null);
    }

}
