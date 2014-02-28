package se.powerapp.mymapapp;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class MyGeoContentProvider extends ContentProvider {
    public static final int DB_VERSION = 1;
    public static final String DB_NAME = "MyGeofenceDatabase";
    private static final String CREATE_SQL = "CREATE TABLE geofence " +
            "(_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "longitude REAL, latitude REAL, radius REAL, created INTEGER)";
    private MyDatabaseHelper mDatabaseHelper;

    public static final class Contract {
        public static final String ID = "_id";
        public static final String LONGITUDE = "longitude";
        public static final String LATITUDE = "latitude";
        public static final String RADIUS = "radius";
        public static final String CREATED = "created";
        public static final String TABLE_NAME = "geofence";
        public static final String AUTHORITY = "se.powerapp.mymapapp.provider";
        public static final Uri GEOFENCES
                = Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);
    }

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final int GEOFENCES_CODE = 1;
    private static final int GEOFENCE_CODE = 2;

    public MyGeoContentProvider() {
    }


    @Override
    public boolean onCreate() {
        mDatabaseHelper = new MyDatabaseHelper(getContext(), DB_NAME, null, DB_VERSION);
        sUriMatcher.addURI(Contract.AUTHORITY, Contract.TABLE_NAME, GEOFENCES_CODE);
        sUriMatcher.addURI(Contract.AUTHORITY, Contract.TABLE_NAME + "/#", GEOFENCE_CODE);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        int match = sUriMatcher.match(uri);
        Cursor cursor = null;
        switch (match) {
            case GEOFENCES_CODE:
                cursor = db.query(Contract.TABLE_NAME, projection, selection, selectionArgs, "", "", sortOrder);
                break;
            case GEOFENCE_CODE:
                String id = uri.getLastPathSegment();
                cursor = db.query(Contract.TABLE_NAME, projection, "_id = ?", new String[]{id}, "", "", sortOrder);
                break;
            default:
                break;
        }
        if (cursor != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        switch (match) {
            case GEOFENCES_CODE:
                long newId = db.insert(Contract.TABLE_NAME, "", values);
                if (newId != -1) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }
                return Uri.parse("content://" + Contract.AUTHORITY + "/task/" + newId);
            default:
                return null;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        int numberOfRowsChanged;
        switch (match) {
            case GEOFENCES_CODE:
                numberOfRowsChanged = db.update(Contract.TABLE_NAME, values, selection, selectionArgs);
                break;
            case GEOFENCE_CODE:
                String id = uri.getLastPathSegment();
                numberOfRowsChanged = db.update(Contract.TABLE_NAME, values, "_id = ?", new String[]{id});
                break;
            default:
                numberOfRowsChanged = 0;
                break;
        }

        if (numberOfRowsChanged > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return numberOfRowsChanged;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        int numberOfRowsChanged;
        switch (match) {
            case GEOFENCES_CODE:
                numberOfRowsChanged = db.delete(Contract.TABLE_NAME, selection, selectionArgs);
                break;
            case GEOFENCE_CODE:
                String id = uri.getLastPathSegment();
                numberOfRowsChanged = db.delete(Contract.TABLE_NAME, "_id = ?", new String[]{id});
                break;
            default:
                numberOfRowsChanged = 0;
                break;
        }
        if (numberOfRowsChanged > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return numberOfRowsChanged;
    }

    @Override
    public String getType(Uri uri) {
        int match = sUriMatcher.match(uri);
        switch (match) {
            case GEOFENCES_CODE:
                return "vnd.android.cursor.dir/" + Contract.TABLE_NAME;
            case GEOFENCE_CODE:
                return "vnd.android.cursor.item/" + Contract.TABLE_NAME;
            default:
                return null;
        }
    }


    class MyDatabaseHelper extends SQLiteOpenHelper {

        public MyDatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_SQL);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }

}
