/*
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package osm.custommaps.create;


import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.SeekBar;


import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.overlays.InfoWindow;
import org.osmdroid.bonuspack.overlays.MapEventsOverlay;
import org.osmdroid.bonuspack.overlays.MapEventsReceiver;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.NetworkLocationIgnorer;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.DirectedLocationOverlay;
import org.osmdroid.views.overlay.MinimapOverlay;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;

import java.util.ArrayList;

import osm.custommaps.HelpDialogManager;
import osm.custommaps.MapImageOverlay;
import osm.custommaps.R;
import osm.custommaps.WMS.WMSTileSourceFactory;


/**
 * TiePointActivity allows users to tie points on bitmap images to geo
 * coordinates on Google Maps.
 *
 * @author Marko Teittinen
 */
public class TiePointActivity extends Activity implements MapEventsReceiver, LocationListener, SensorEventListener {

    protected IGeoPoint pointLocation;
    protected IGeoPoint currentLocation;
    protected int pointLocationLat;
    protected int pointLocationLon;
    protected MapView map;
    private static final String EXTRA_PREFIX = "osm.custommaps";
    public static final String BITMAP_DATA = EXTRA_PREFIX + ".BitmapData";
    public static final String IMAGE_POINT = EXTRA_PREFIX + ".ImagePoint";
    public static final String RESTORE_SETTINGS = EXTRA_PREFIX + ".RestoreSettings";
    public static final String GEO_POINT_E6 = EXTRA_PREFIX + ".GeoPointE6";
    public static final String LASTPOINTLON = EXTRA_PREFIX + "";
    public static final String LASTPOINTLAT = EXTRA_PREFIX + "";
    private static final String LOG_TAG = "Custom Maps";
    protected IGeoPoint startPoint, destinationPoint;
    protected ArrayList<GeoPoint> viaPoints;
    protected DirectedLocationOverlay myLocationOverlay;
    protected LocationManager mLocationManager;
    private ImageButton mapModeButton;
    private ImageButton centerButton;
    private Button doneButton;
    private SeekBar scaleBar;
    private SeekBar transparencyBar;
    private SeekBar rotateBar;
    protected boolean mTrackingMode;
    Button mTrackingModeButton;
    float mAzimuthAngleSpeed = 0.0f;
    SharedPreferences prefs;

    private MapImageOverlay imageOverlay;
    private HelpDialogManager helpDialogManager;
    private SharedPreferences mPrefs;
    private CompassOverlay mCompassOverlay;
    private MinimapOverlay mMinimapOverlay;
    private ScaleBarOverlay mScaleBarOverlay;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.tiepoints);

        DisplayMetrics dms = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dms);



        DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();

        prefs = getSharedPreferences("osm.custommaps", MODE_PRIVATE);

        MapView mapTryView = (MapView) findViewById(R.id.custom_mapview);
        mapTryView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        map = mapTryView;
        map.setMaxZoomLevel(20);
        map.setMinZoomLevel(3);
        map.setTileSource(WMSTileSourceFactory.BING_HYBRID);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        IMapController mapController = map.getController();
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        mapController.setZoom(prefs.getInt("MAP_ZOOM_LEVEL", 1));
        myLocationOverlay = new DirectedLocationOverlay(this);
        if (savedInstanceState == null) {
            Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null)
                location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                //location known:
                onLocationChanged(location);
            } else {
                //no location known: hide myLocationOverlay
                myLocationOverlay.setEnabled(false);
            }
            startPoint = null;
            destinationPoint = null;
            viaPoints = new ArrayList<GeoPoint>();
        } else
            myLocationOverlay.setLocation((GeoPoint) savedInstanceState.getParcelable("location"));
        currentLocation = myLocationOverlay.getLocation();

        if (pointLocation == null) {
            float lat = getSharedPreferences("osm.custommaps", MODE_PRIVATE).getFloat("MAP_CENTER_LAT", 48.5f);
            float lon = getSharedPreferences("osm.custommaps", MODE_PRIVATE).getFloat("MAP_CENTER_LON", 2.5f);
            if (lat == 48.5f && lon == 2.5f) {
                mapController.setZoom(prefs.getInt("MAP_ZOOM_LEVEL", 5));
                mapController.setCenter(currentLocation);
            } else {
                pointLocation = new GeoPoint(lat, lon);
                if (pointLocation != null) {
                    mapController.setZoom(prefs.getInt("MAP_ZOOM_LEVEL", 5));
                    mapController.setCenter(pointLocation);
                }
            }
        }
        ViewParent p = mapTryView.getParent();
        if (p instanceof ViewGroup) {
            ViewGroup layout = (ViewGroup) p;
            LayoutParams layoutParams = mapTryView.getLayoutParams();

            //To use MapEventsReceiver methods, we add a MapEventsOverlay:
            MapEventsOverlay overlay = new MapEventsOverlay(this, this);
            map.getOverlays().add(overlay);

            //map prefs:
            map.getOverlays().add(myLocationOverlay);
            ScaleBarOverlay scaleBarOverlay = new ScaleBarOverlay(this);
            map.getOverlays().add(scaleBarOverlay);

            mTrackingModeButton = (Button) findViewById(R.id.buttonTrackingMode);
            mTrackingModeButton.setOnClickListener(new View.OnClickListener() {
                                                       public void onClick(View view) {
                                                           mTrackingMode = !mTrackingMode;
                                                           updateUIWithTrackingMode();
                                                       }
                                                   }
            );

            if (savedInstanceState != null) {
                mTrackingMode = savedInstanceState.getBoolean("tracking_mode");
                updateUIWithTrackingMode();
            } else
                mTrackingMode = false;
            map.setMaxZoomLevel(20);
            map.setMinZoomLevel(3);


            layout.removeView(mapTryView);
            layout.addView(mapTryView, layoutParams);
            mScaleBarOverlay = new ScaleBarOverlay(getApplicationContext());
            mScaleBarOverlay.setCentred(true);
            mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 10);
        }
        prepareUI();
        map.setBuiltInZoomControls(true);

        Bundle extras = getIntent().getExtras();
        int[] center = extras.getIntArray(IMAGE_POINT);
        byte[] pngImage = extras.getByteArray(BITMAP_DATA);
        Bitmap image = BitmapFactory.decodeByteArray(pngImage, 0, pngImage.length);
        imageOverlay = new

                MapImageOverlay(getApplicationContext());
        imageOverlay.setOverlayImage(image, center[0], center[1]);
        map.getOverlays().add(imageOverlay);

        if (extras.getBoolean(RESTORE_SETTINGS, false)) {
            PreferenceHelper helpedPrefs = new PreferenceHelper();
            writeTransparencyUi(helpedPrefs.getTransparency());
            writeScaleUi(helpedPrefs.getScale());
            writeRotationUi(helpedPrefs.getRotation());
        } else {
            writeTransparencyUi(50);
            imageOverlay.setTransparency(50);
            writeScaleUi(1.0f);
            writeRotationUi(0);
        }
        helpDialogManager = new HelpDialogManager(this, HelpDialogManager.HELP_TIE_POINT, getString(R.string.geo_point_help));
    }
    void updateUIWithTrackingMode() {
        if (mTrackingMode) {
            mTrackingModeButton.setBackgroundResource(R.drawable.btn_tracking_on);
            if (myLocationOverlay.isEnabled() && myLocationOverlay.getLocation() != null) {
                map.getController().animateTo(myLocationOverlay.getLocation());
            }
            map.setMapOrientation(-mAzimuthAngleSpeed);
            mTrackingModeButton.setKeepScreenOn(true);
        } else {
            mTrackingModeButton.setBackgroundResource(R.drawable.btn_tracking_off);
            map.setMapOrientation(0.0f);
            mTrackingModeButton.setKeepScreenOn(false);
        }
    }
    void savePrefs() {
        SharedPreferences prefs = getSharedPreferences("osm.custommaps", MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putInt("MAP_ZOOM_LEVEL", map.getZoomLevel());
        IGeoPoint c = map.getMapCenter();
        ed.putInt("MAP_ZOOM_LEVEL", map.getZoomLevel());

        ed.putFloat("MAP_CENTER_LAT", (float) c.getLatitude());
        ed.putFloat("MAP_CENTER_LON", (float) c.getLongitude());
        ed.putInt("LASTPOINTLAT", pointLocationLat);
        ed.putInt("LASTPOINTLON", pointLocationLon);
        //View searchPanel = (View) findViewById(osm.custommaps.R.id.search_panel);
        //  ed.putInt("PANEL_VISIBILITY", searchPanel.getVisibility());
        MapTileProviderBase tileProvider = map.getTileProvider();
        String tileProviderName = tileProvider.getTileSource().name();
        ed.putString("TILE_PROVIDER", tileProviderName);
        ed.commit();
    }

    boolean startLocationUpdates() {
        boolean result = false;
        for (final String provider : mLocationManager.getProviders(true)) {
            mLocationManager.requestLocationUpdates(provider, 2 * 1000, 0.0f, this);
            result = true;
        }
        return result;
    }

     @Override
    protected void onResume() {
        super.onResume();
        boolean isOneProviderEnabled = startLocationUpdates();
        myLocationOverlay.setEnabled(isOneProviderEnabled);
        Bundle extras = getIntent().getExtras();

        if (extras.containsKey(GEO_POINT_E6)) {
            // Editing a tiepoint that was previously placed, center it on view
            int[] geoLocationE6 = extras.getIntArray(GEO_POINT_E6);
            map.getController().setCenter(new GeoPoint(geoLocationE6[0], geoLocationE6[1]));
            // Prevent resetting of map center point on device orientation change
            extras.remove(GEO_POINT_E6);
        }
        imageOverlay.setTransparency(readTransparencyUi());
        imageOverlay.setScale(readScaleUi());
        imageOverlay.setRotate(readRotationUi());
        map.postInvalidate();
        helpDialogManager.onResume();
    }

    @Override
    protected void onPause() {
        helpDialogManager.onPause();
        mLocationManager.removeUpdates(this);
        if (isFinishing() && imageOverlay != null) {
            Bitmap image = imageOverlay.getOverlayImage();
            if (image != null && !image.isRecycled()) {
                image.recycle();
                imageOverlay.setOverlayImage(null, 0, 0);
            }
        }
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        helpDialogManager.onSaveInstanceState(outState);
        outState.putParcelable("location", myLocationOverlay.getLocation());
        outState.putBoolean("tracking_mode", mTrackingMode);
        outState.putParcelable("start", ((GeoPoint)startPoint));
        outState.putParcelable("destination", ((GeoPoint)destinationPoint));
        outState.putParcelableArrayList("viapoints", viaPoints);
        // GUI widget states are automatically stored, no need to add anything
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        helpDialogManager.onRestoreInstanceState(savedInstanceState);
    }

    private void returnGeoPoint(IGeoPoint location) {
        // Release memory used by the overlay image
        Bitmap image = imageOverlay.getOverlayImage();
        imageOverlay.setOverlayImage(null, 0, 0);
        if (image != null && !image.isRecycled()) {
            image.recycle();
        }
        System.gc();

        // Save UI values for next invocation
        new PreferenceHelper().saveValues();

        // Return the selected GeoPoint to calling activity in the original Intent
        Intent result = getIntent();
        int[] geoPoint = new int[]{location.getLatitudeE6(), location.getLongitudeE6()};
        result.putExtra(GEO_POINT_E6, geoPoint);
        pointLocationLat = location.getLatitudeE6();
        pointLocationLon = location.getLongitudeE6();
        setResult(RESULT_OK, result);
        savePrefs();
        finish();
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    //------------ SensorEventListener implementation
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        myLocationOverlay.setAccuracy(accuracy);
        map.invalidate();
    }

    private final NetworkLocationIgnorer mIgnorer = new NetworkLocationIgnorer();
    long mLastTime = 0; // milliseconds
    double mSpeed = 0.0; // km/h

    @Override
    public void onLocationChanged(final Location pLoc) {
        long currentTime = System.currentTimeMillis();
        if (mIgnorer.shouldIgnore(pLoc.getProvider(), currentTime))
            return;
        double dT = currentTime - mLastTime;
        if (dT < 100.0) {
            return;
        }
        mLastTime = currentTime;

        GeoPoint newLocation = new GeoPoint(pLoc);
        if (!myLocationOverlay.isEnabled()) {
            //we get the location for the first time:
            myLocationOverlay.setEnabled(true);
            map.getController().animateTo(newLocation);
        }

        GeoPoint prevLocation = myLocationOverlay.getLocation();
        myLocationOverlay.setLocation(newLocation);
        myLocationOverlay.setAccuracy((int) pLoc.getAccuracy());

        if (prevLocation != null && pLoc.getProvider().equals(LocationManager.GPS_PROVIDER)) {

            mSpeed = pLoc.getSpeed() * 3.6;
          //TODO: check if speed is not too small
            if (mSpeed >= 0.1) {
                mAzimuthAngleSpeed = (float) pLoc.getBearing();
                myLocationOverlay.setBearing(mAzimuthAngleSpeed);
            }
        }

        if (mTrackingMode) {
            //keep the map view centered on current location:
            map.getController().animateTo(newLocation);
            map.setMapOrientation(-mAzimuthAngleSpeed);
        } else {
            //just redraw the location overlay:
            map.invalidate();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ORIENTATION:
                if (mSpeed < 0.1) {
                }
                //at higher speed, we use speed vector, not phone orientation.
                break;
            default:
                break;
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        return helpDialogManager.onCreateDialog(id);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        helpDialogManager.onPrepareDialog(id, dialog);
    }

// --------------------------------------------------------------------------
// Activity UI

    /**
     * @return rotation value displayed in widget
     */
    private int readRotationUi() {
        return rotateBar.getProgress() - 180;
    }

    private void writeRotationUi(int degrees) {
        rotateBar.setProgress(degrees + 180);
    }

    /**
     * @return scale value displayed in widget
     */
    private float readScaleUi() {
        return 1.0f + (scaleBar.getProgress() / 10f);
    }

    private void writeScaleUi(float scale) {
        scaleBar.setProgress(Math.round(10 * (scale - 1.0f)));
    }

    /**
     * @return transparency value displayed in widget
     */
    private int readTransparencyUi() {
        return transparencyBar.getProgress();
    }

    private void writeTransparencyUi(int transparencyPercent) {
        transparencyBar.setProgress(transparencyPercent);
    }

    private void prepareUI() {

        doneButton = (Button) findViewById(R.id.selectPoint);
        mapModeButton = (ImageButton) findViewById(R.id.mapmode);
        centerButton = (ImageButton) findViewById(R.id.centerButton);
        rotateBar = (SeekBar) findViewById(R.id.rotateBar);
        scaleBar = (SeekBar) findViewById(R.id.scaleBar);
        transparencyBar = (SeekBar) findViewById(R.id.transparencyBar);

        // Toggle between map and satellite view
        centerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myLocationOverlay.isEnabled() && myLocationOverlay.getLocation() != null) {
                    map.getController().animateTo(myLocationOverlay.getLocation());
                }
            }
        });
        mapModeButton.setOnClickListener(new View.OnClickListener() {
                                             public void onClick(View view) {
                                                 PopupMenu popupMenu = new PopupMenu(getApplicationContext(), view);
                                                 popupMenu.inflate(R.menu.menu_options);
                                                 popupMenu.show();

                                                 popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                                     @Override
                                                     public boolean onMenuItemClick(MenuItem item) {
                                                         switch (item.toString()) {
                                                             case "Google Satellite":
                                                                 map.invalidate();
                                                                 map.setMaxZoomLevel(20);
                                                                 map.setMinZoomLevel(3);
                                                                 map.setTileSource(WMSTileSourceFactory.GOOGLE_SATELLITE);
                                                                 break;
                                                             case "Google Maps":
                                                                 map.invalidate();
                                                                 map.setMaxZoomLevel(20);
                                                                 map.setMinZoomLevel(3);
                                                                 map.setTileSource(WMSTileSourceFactory.GOOGLE_MAPS);
                                                                 break;
                                                             case "Google Terrain":
                                                                 map.invalidate();
                                                                 map.setMaxZoomLevel(20);
                                                                 map.setMinZoomLevel(3);
                                                                 map.setTileSource(WMSTileSourceFactory.GOOGLE_TERRAIN);
                                                                 break;
                                                             case "Bing Maps":
                                                                 map.invalidate();
                                                                 map.setMaxZoomLevel(20);
                                                                 map.setMinZoomLevel(3);
                                                                 map.setTileSource(WMSTileSourceFactory.BING_MAPS);
                                                                 break;
                                                             case "Bing Earth":
                                                                 map.invalidate();
                                                                 map.setMaxZoomLevel(20);
                                                                 map.setMinZoomLevel(3);
                                                                 map.setTileSource(WMSTileSourceFactory.BING_EARTH);
                                                                 break;
                                                             case "Bing Hybrid":
                                                                 map.invalidate();
                                                                 map.setMaxZoomLevel(20);
                                                                 map.setMinZoomLevel(3);
                                                                 map.setTileSource(WMSTileSourceFactory.BING_HYBRID);
                                                                 break;
                                                             case "Mapquest Aerial":
                                                                 map.invalidate();
                                                                 map.setMaxZoomLevel(20);
                                                                 map.setMinZoomLevel(3);
                                                                 map.setTileSource(WMSTileSourceFactory.MAPQUESTAERIAL_US);
                                                                 break;
                                                             case "Mapquest OSM":
                                                                 map.invalidate();
                                                                 map.setMaxZoomLevel(20);
                                                                 map.setMinZoomLevel(3);
                                                                 map.setTileSource(WMSTileSourceFactory.MAPQUESTOSM);
                                                                 break;
                                                             case "Mapnik":
                                                                 map.invalidate();
                                                                 map.setMaxZoomLevel(20);
                                                                 map.setMinZoomLevel(3);
                                                                 map.setTileSource(WMSTileSourceFactory.MAPNIK);
                                                                 break;
                                                         }
                                                         return false;
                                                     }
                                                 });
                                             }
                                         }
        );

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {   //TODO had to edit my cast to geopoint again
                returnGeoPoint(map.getMapCenter());
            }
        });

        rotateBar.setOnSeekBarChangeListener(new ImageOverlayAdjuster() {
            @Override
            protected void updateImageOverlay(int value) {
                imageOverlay.setRotate(value - 180);
                map.invalidate();
            }
        });

        scaleBar.setOnSeekBarChangeListener(new ImageOverlayAdjuster() {
            @Override
            protected void updateImageOverlay(int value) {
                imageOverlay.setScale(1.0f + (value / 10f));
                map.invalidate();
            }
        });

        transparencyBar.setOnSeekBarChangeListener(new ImageOverlayAdjuster() {
            @Override
            protected void updateImageOverlay(int value) {
                imageOverlay.setTransparency(value);
                map.invalidate();
            }
        });
    }

    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        InfoWindow.closeAllInfoWindowsOn(map);
        return true;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        return false;
    }

// --------------------------------------------------------------------------
// Base inner class to modify image overlay based on seekbar changes

    abstract class ImageOverlayAdjuster implements SeekBar.OnSeekBarChangeListener {
        protected abstract void updateImageOverlay(int value);

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                updateImageOverlay(progress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            updateImageOverlay(seekBar.getProgress());
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            updateImageOverlay(seekBar.getProgress());
        }
    }

// --------------------------------------------------------------------------
// Activity preferences

    private class PreferenceHelper {
        private static final String SCALE = "Scale";
        private static final String ROTATION = "Rotation";
        private static final String TRANSPARENCY = "Transparency";

        private SharedPreferences preferences;

        PreferenceHelper() {
            preferences = getPreferences(MODE_PRIVATE);
        }

        private int getRotation() {
            return preferences.getInt(ROTATION, 0);
        }

        private float getScale() {
            return preferences.getFloat(SCALE, 1.0f);
        }

        private int getTransparency() {
            return preferences.getInt(TRANSPARENCY, 50);
        }

        private void saveValues() {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(ROTATION, readRotationUi());
            editor.putFloat(SCALE, readScaleUi());
            editor.putInt(TRANSPARENCY, readTransparencyUi());
            editor.commit();
        }
    }
}






