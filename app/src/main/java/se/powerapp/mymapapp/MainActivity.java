package se.powerapp.mymapapp;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity implements ActionBar.TabListener,
        GoogleMap.OnMapLongClickListener, GoogleMap.OnMapClickListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    SectionsPagerAdapter mSectionsPagerAdapter;

    ViewPager mViewPager;
    public static final int MAP_POSITION = 0;
    public static final int LIST_POSITION = 1;
    private MyMapFragment mMapFragment;
    private MyMapFragment mListFragment;
    private int sectionNumber = -1;

    private static final float GEOFENCE_RADIUS = 25.0f;
    private GoogleMap mMap;
    private PolygonOptions mPolygonOptions = null;
    private LocationClient mLocationClient;
    private List<Geofence> mGeofences;
    private Map<String, GeofenceData> mGeofenceDatas;
    private SimpleCursorAdapter mListAdapter;
    private MyListFragment myListFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGeofences = new LinkedList<Geofence>();
        mGeofenceDatas = new HashMap<String, GeofenceData>();

        mLocationClient = new LocationClient(this, this, this);


        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        mMapFragment = MyMapFragment.newInstance(0);
        myListFragment = new MyListFragment();
        mListAdapter = new SimpleCursorAdapter(this,
                R.layout.fragment_list,
                null,
                new String[]{MyGeoContentProvider.Contract.LATITUDE,
                        MyGeoContentProvider.Contract.LONGITUDE},
                new int[]{R.id.latitude, R.id.longitude}, 0);


        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());


        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {

            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }

        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this.getApplicationContext());

            if (resultCode == ConnectionResult.SUCCESS) {
                Toast.makeText(this, "Looking for geofence!", Toast.LENGTH_SHORT).show();

            } else if (resultCode == ConnectionResult.SERVICE_MISSING ||
                    resultCode == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED ||
                    resultCode == ConnectionResult.SERVICE_DISABLED) {
                Toast.makeText(this, "Google Maps not available!", Toast.LENGTH_SHORT).show();
            }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLocationClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        mGeofences.clear();
        mGeofenceDatas.clear();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mLocationClient.disconnect();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        boolean didRemove = false;

        for (Map.Entry<String, GeofenceData> geofenceDataEntry : mGeofenceDatas.entrySet()) {
            String requestId = geofenceDataEntry.getKey();
            GeofenceData geofenceData = geofenceDataEntry.getValue();
            float[] distance = new float[1];
            Location.distanceBetween(geofenceData.latitude, geofenceData.longitude,
                    latLng.latitude, latLng.longitude, distance);

            if (distance[0] <= GEOFENCE_RADIUS) {
                didRemove = true;
                Uri geofenceUri = Uri.withAppendedPath(MyGeoContentProvider.Contract.GEOFENCES,
                        requestId);
                getContentResolver().delete(geofenceUri, null, null);
                getLoaderManager().restartLoader(0, null, this);
            }
        }

        if (!didRemove) {
            String requestId = insertGeofenceInDatabase(latLng, GEOFENCE_RADIUS);
            addAndPaintGeofence(requestId, latLng, GEOFENCE_RADIUS);
        }
    }

    private void addAndPaintGeofence(String requestId, LatLng latLng, float radius) {
        addGeofencesToList(requestId, latLng, radius);
        Intent showGeofenceToast =
                new Intent(MyReceiver.ACTION_GEOFENCE_TOAST);
        PendingIntent pendingIntent
                = PendingIntent
                .getBroadcast(this, 0, showGeofenceToast, 0);
        mLocationClient.addGeofences(mGeofences, pendingIntent,
                new LocationClient.OnAddGeofencesResultListener() {
                    @Override
                    public void onAddGeofencesResult(int i, String[] strings) {
                        Log.e("GeofenceDemo", "Geofences added!");
                    }
                });

        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(latLng).radius(radius).fillColor(Color.argb(60,255,0,0)).
                strokeColor((Color.TRANSPARENT));
        mMap.addCircle(circleOptions);
    }

    private void addGeofencesToList(String requestId, LatLng latLng, float radius) {
        Geofence.Builder builder = new Geofence.Builder();
        Geofence geofence = builder
                .setRequestId(requestId)
                .setCircularRegion(latLng.latitude,
                        latLng.longitude, radius)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER
                        | Geofence.GEOFENCE_TRANSITION_EXIT)
                .setExpirationDuration(1000 * 60 * 5)
                .build();

        mGeofences.add(geofence);
        mGeofenceDatas.put(requestId, new GeofenceData(radius,
                (float) latLng.latitude, (float) latLng.longitude));
    }

    private String insertGeofenceInDatabase(LatLng latLng, float radius) {
        ContentValues values = new ContentValues();
        values.put(MyGeoContentProvider.Contract.LATITUDE, latLng.latitude);
        values.put(MyGeoContentProvider.Contract.LONGITUDE, latLng.longitude);
        values.put(MyGeoContentProvider.Contract.RADIUS, radius);
        values.put(MyGeoContentProvider.Contract.CREATED, System.currentTimeMillis());
        return getContentResolver()
                .insert(MyGeoContentProvider.Contract.GEOFENCES, values)
                .getLastPathSegment();
    }

    @Override
    public void onMapClick(LatLng latLng) {
    }

    @Override
    public void onConnected(Bundle bundle) {
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, MyGeoContentProvider.Contract.GEOFENCES,
                new String[]{MyGeoContentProvider.Contract.ID,
                        MyGeoContentProvider.Contract.LATITUDE,
                        MyGeoContentProvider.Contract.LONGITUDE,
                        MyGeoContentProvider.Contract.RADIUS,
                        MyGeoContentProvider.Contract.CREATED},
                null, null, "");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mListAdapter.swapCursor(cursor);

        mGeofences.clear();
        mGeofenceDatas.clear();
        mMap.clear();

        while (cursor != null && cursor.moveToNext()) {
            String requestId = cursor
                    .getString(cursor
                            .getColumnIndex(MyGeoContentProvider.Contract.ID));
            float latitude = cursor
                    .getFloat(cursor
                            .getColumnIndex(MyGeoContentProvider.Contract.LATITUDE));
            float longitude = cursor
                    .getFloat(cursor
                            .getColumnIndex(MyGeoContentProvider.Contract.LONGITUDE));
            float radius = cursor
                    .getFloat(cursor
                            .getColumnIndex(MyGeoContentProvider.Contract.RADIUS));

            mLocationClient.removeGeofences(Arrays.asList(requestId),
                    new LocationClient.OnRemoveGeofencesResultListener() {
                @Override
                public void onRemoveGeofencesByRequestIdsResult(int i, String[] strings) {
                    // Ignore
                }

                @Override
                public void onRemoveGeofencesByPendingIntentResult(int i, PendingIntent pendingIntent) {

                }
            });

            addAndPaintGeofence(requestId, new LatLng(latitude, longitude), radius);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    class GeofenceData {

        GeofenceData(float radius, float latitude, float longitude) {
            this.radius = radius;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public float radius;
        public float latitude;
        public float longitude;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);
        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setMyLocationEnabled(true);


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }


    public class SectionsPagerAdapter extends FragmentPagerAdapter {


        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }


        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case MAP_POSITION:
                    return mMapFragment;
                case LIST_POSITION:
                    return myListFragment;
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);

            }
            return null;
        }
    }


    public static class MyMapFragment extends Fragment implements
            android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor>{

        private static final String ARG_SECTION_NUMBER = "section_number";


        public static MyMapFragment newInstance(int sectionNumber) {
            MyMapFragment fragment = new MyMapFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public MyMapFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            Bundle args = getArguments();
            int sectionNumber = args.getInt(ARG_SECTION_NUMBER);


            switch (sectionNumber) {
                case LIST_POSITION:

                    View list = inflater.inflate(R.layout.fragment_list, container, false);

                return list;
                default:
                    View rootView = inflater.inflate(R.layout.fragment_map, container, false);
                    return rootView;
            }
        }

        @Override
        public android.support.v4.content.Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            return null;
        }

        @Override
        public void onLoadFinished(android.support.v4.content.Loader<Cursor> cursorLoader, Cursor cursor) {

        }

        @Override
        public void onLoaderReset(android.support.v4.content.Loader<Cursor> cursorLoader) {

        }
     }

    public class MyListFragment extends ListFragment  {
        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            Log.d("ID! ", "ID: " + MyGeoContentProvider.Contract.ID);

           setListAdapter(mListAdapter);

        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            super.onListItemClick(l, v, position, id);

            Uri geofenceuri = Uri.withAppendedPath(MyGeoContentProvider.Contract.GEOFENCES, String.valueOf(id));
            getContentResolver().delete(geofenceuri, null, null);
        }
    }



}

