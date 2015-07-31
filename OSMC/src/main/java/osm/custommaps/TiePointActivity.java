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
package osm.custommaps;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Address;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import osm.custommaps.KmlStylesActivity;
import osm.custommaps.KmlTreeActivity;
import osm.custommaps.POIActivity;
import osm.custommaps.RouteActivity;
import osm.custommaps.ViaPointInfoWindow;

import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.cachemanager.CacheManager;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.bonuspack.kml.KmlFeature;
import org.osmdroid.bonuspack.kml.KmlFolder;
import org.osmdroid.bonuspack.location.GeocoderNominatim;
import org.osmdroid.bonuspack.location.POI;
import org.osmdroid.bonuspack.mapsforge.GenericMapView;
import org.osmdroid.bonuspack.overlays.FolderOverlay;
import org.osmdroid.bonuspack.overlays.InfoWindow;
import org.osmdroid.bonuspack.overlays.MapEventsOverlay;
import org.osmdroid.bonuspack.overlays.MapEventsReceiver;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.Polygon;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.NetworkLocationIgnorer;
import org.osmdroid.util.ResourceProxyImpl;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.DirectedLocationOverlay;
import org.osmdroid.views.overlay.MinimapOverlay;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * TiePointActivity allows users to tie points on bitmap images to geo
 * coordinates on Google Maps.
 *
 * @author Marko Teittinen
 */
public class TiePointActivity extends Activity implements MapEventsReceiver, LocationListener, SensorEventListener {
    protected MapView map;
    protected Polygon mDestinationPolygon;
    public static ArrayList<POI> mPOIs; //made static to pass between activities
    RadiusMarkerClusterer mPoiMarkers;
    AutoCompleteTextView poiTagText;
    protected FolderOverlay mKmlOverlay; //root container of overlays from KML reading
    public static KmlDocument mKmlDocument; //made static to pass between activities
    public static Stack<KmlFeature> mKmlStack; //passed between activities, top is the current KmlFeature to edit.
    public static KmlFolder mKmlClipboard; //passed between activities. Folder for multiple items selection.
    protected static final int POIS_REQUEST = 2;
    private static final String EXTRA_PREFIX = "osm.custommaps";
    public static final String BITMAP_DATA = EXTRA_PREFIX + ".BitmapData";
    public static final String IMAGE_POINT = EXTRA_PREFIX + ".ImagePoint";
    public static final String RESTORE_SETTINGS = EXTRA_PREFIX + ".RestoreSettings";
    public static final String GEO_POINT_E6 = EXTRA_PREFIX + ".GeoPointE6";
    static final String graphHopperApiKey = "AMFmC5P8s958tcjfFRJmefNboJ5H0HN6PLFyvdm3";
    static final String mapQuestApiKey = "Fmjtd%7Cluubn10zn9%2C8s%3Do5-90rnq6";
    static final String flickrApiKey = "c39be46304a6c6efda8bc066c185cd7e";
    static final String geonamesAccount = "mkergall";
    static final String userAgent = "OsmNavigator/1.0";
    static String SHARED_PREFS_APPKEY = "OSMNavigator";
    static String PREF_LOCATIONS_KEY = "PREF_LOCATIONS";
    private static final String LOG_TAG = "Custom Maps";
    protected GeoPoint startPoint, destinationPoint;
    protected ArrayList<GeoPoint> viaPoints;
    protected static int START_INDEX = -2, DEST_INDEX = -1;
    protected FolderOverlay mItineraryMarkers;
    //for departure, destination and viapoints
    protected Marker markerStart, markerDestination;
    protected ViaPointInfoWindow mViaPointInfoWindow;
    protected DirectedLocationOverlay myLocationOverlay;
    protected LocationManager mLocationManager;
    private ImageButton mapModeButton;
    private Button doneButton;
    private SeekBar scaleBar;
    private SeekBar transparencyBar;
    private SeekBar rotateBar;
    protected boolean mTrackingMode;
    Button mTrackingModeButton;
    float mAzimuthAngleSpeed = 0.0f;
    //private MyLocationOverlay userLocation;


    private MapImageOverlay imageOverlay;
    private HelpDialogManager helpDialogManager;
    private SharedPreferences mPrefs;
    // protected MyLocationNewOverlay myLocationOverlay;
    private CompassOverlay mCompassOverlay;
    private MinimapOverlay mMinimapOverlay;
    private ScaleBarOverlay mScaleBarOverlay;
    private ResourceProxy mResourceProxy;
    OnlineTileSourceBase WMSTILE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences("OSMNAVIGATOR", MODE_PRIVATE);

        setContentView(R.layout.tiepoints);
        Context context = getApplicationContext();
        DisplayMetrics dm = context.getResources().getDisplayMetrics();

        //map = (MapView) findViewById(R.id.mapviewlocation);
        GenericMapView genericMap = (GenericMapView) findViewById(R.id.mapviewlocation);
        MapTileProviderBasic bitmapProvider = new MapTileProviderBasic(getApplicationContext());
        genericMap.setTileProvider(bitmapProvider);
        map = genericMap.getMapView();
        String tileProviderName = prefs.getString("TILE_PROVIDER", "Mapnik");


        ViewParent p = genericMap.getParent();
        if (p instanceof ViewGroup) {
            ViewGroup layout = (ViewGroup) p;
            LayoutParams layoutParams = genericMap.getLayoutParams();
            try {
                ITileSource tileSource = TileSourceFactory.getTileSource(tileProviderName);
                map.setTileSource(tileSource);
            } catch (IllegalArgumentException e) {
                map.setTileSource(TileSourceFactory.MAPNIK);
            }
            map.setBuiltInZoomControls(true);
            map.setMultiTouchControls(true);
            IMapController mapController = map.getController();
            //To use MapEventsReceiver methods, we add a MapEventsOverlay:

            MapEventsOverlay overlay = new MapEventsOverlay(this, this);
            map.getOverlays().add(overlay);

            //map prefs:
            mapController.setZoom(prefs.getInt("MAP_ZOOM_LEVEL", 5));
            mapController.setCenter(new GeoPoint((double) prefs.getFloat("MAP_CENTER_LAT", 48.5f),
                    (double) prefs.getFloat("MAP_CENTER_LON", 2.5f)));

            mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            myLocationOverlay = new DirectedLocationOverlay(this);
            map.getOverlays().add(myLocationOverlay);

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
            //TODO: restore other aspects of myLocationOverlay...
            startPoint = savedInstanceState.getParcelable("start");
            destinationPoint = savedInstanceState.getParcelable("destination");
            viaPoints = savedInstanceState.getParcelableArrayList("viapoints");


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


            layout.removeView(genericMap);
            layout.addView(genericMap, layoutParams);


            mScaleBarOverlay = new ScaleBarOverlay(context);
            mScaleBarOverlay.setCentred(true);
            mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 10);
        }
        prepareUI();
        map.setBuiltInZoomControls(true);
        //mapView.setReticleDrawMode(MapView.ReticleDrawMode.DRAW_RETICLE_NEVER);

        Bundle extras = getIntent().getExtras();
        int[] center = extras.getIntArray(IMAGE_POINT);
        byte[] pngImage = extras.getByteArray(BITMAP_DATA);
        Bitmap image = BitmapFactory.decodeByteArray(pngImage, 0, pngImage.length);
        imageOverlay=new

                MapImageOverlay(context);imageOverlay.setOverlayImage(image,center[0],center[1]);
        map.getOverlays().add(imageOverlay);

        if(extras.getBoolean(RESTORE_SETTINGS,false)){
            PreferenceHelper helpedPrefs = new PreferenceHelper();
            writeTransparencyUi(helpedPrefs.getTransparency());
            writeScaleUi(helpedPrefs.getScale());
            writeRotationUi(helpedPrefs.getRotation());
        }else{
            writeTransparencyUi(50);
            imageOverlay.setTransparency(50);
            writeScaleUi(1.0f);
            writeRotationUi(0);
        }

        helpDialogManager = new HelpDialogManager(this, HelpDialogManager.HELP_TIE_POINT,getString(R.string.geo_point_help));}

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
            mTrackingModeButton.setKeepScreenOn(false);}}




    public void updateUIWithPolygon(ArrayList<GeoPoint> polygon, String name) {
        List<Overlay> mapOverlays = map.getOverlays();
        int location = -1;
        if (mDestinationPolygon != null)
            location = mapOverlays.indexOf(mDestinationPolygon);
        mDestinationPolygon = new Polygon(this);
        mDestinationPolygon.setFillColor(0x15FF0080);
        mDestinationPolygon.setStrokeColor(0x800000FF);
        mDestinationPolygon.setStrokeWidth(5.0f);
        mDestinationPolygon.setTitle(name);
        BoundingBoxE6 bb = null;
        if (polygon != null) {
            mDestinationPolygon.setPoints(polygon);
            bb = BoundingBoxE6.fromGeoPoints(polygon);
        }
        if (location != -1)
            mapOverlays.set(location, mDestinationPolygon);
        else
            mapOverlays.add(1, mDestinationPolygon); //insert just above the MapEventsOverlay.
        setViewOn(bb);
        map.invalidate();
    }


    void savePrefs() {
        SharedPreferences prefs = getSharedPreferences("OSMNAVIGATOR", MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putInt("MAP_ZOOM_LEVEL", map.getZoomLevel());
        GeoPoint c = (GeoPoint) map.getMapCenter();
        ed.putFloat("MAP_CENTER_LAT", (float) c.getLatitude());
        ed.putFloat("MAP_CENTER_LON", (float) c.getLongitude());
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

    void setViewOn(BoundingBoxE6 bb) {
        if (bb != null) {
            map.zoomToBoundingBox(bb);
        }
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
        outState.putParcelable("start", startPoint);
        outState.putParcelable("destination", destinationPoint);
        outState.putParcelableArrayList("viapoints", viaPoints);
        // GUI widget states are automatically stored, no need to add anything
    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        helpDialogManager.onRestoreInstanceState(savedInstanceState);
    }


    private void returnGeoPoint(GeoPoint location) {
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
        setResult(RESULT_OK, result);
        finish();
    }


// --------------------------------------------------------------------------
// Options menu

private static final int MENU_USER_LOCATION = 1;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, MENU_USER_LOCATION, Menu.NONE, "My location").setIcon(
                android.R.drawable.ic_menu_mylocation);
        helpDialogManager.onCreateOptionsMenu(menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case MENU_USER_LOCATION:
                myLocationOverlay.getLocation(); //TODO Might Break This
                return true;
            default:
                helpDialogManager.onOptionsItemSelected(item);
                return true;
        }
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

static float mAzimuthOrientation = 0.0f;
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
            //Toast.makeText(this, pLoc.getProvider()+" dT="+dT, Toast.LENGTH_SHORT).show();
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
            /*
            double d = prevLocation.distanceTo(newLocation);
			mSpeed = d/dT*1000.0; // m/s
			mSpeed = mSpeed * 3.6; //km/h
			*/
            mSpeed = pLoc.getSpeed() * 3.6;
            long speedInt = Math.round(mSpeed);
            //TextView speedTxt = (TextView) findViewById(R.id.speed);
            //speedTxt.setText(speedInt + " km/h");

            //TODO: check if speed is not too small
            if (mSpeed >= 0.1) {
                //mAzimuthAngleSpeed = (float)prevLocation.bearingTo(newLocation);
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
                    /* TODO Filter to implement...
                    float azimuth = event.values[0];
					if (Math.abs(azimuth-mAzimuthOrientation)>2.0f){
						mAzimuthOrientation = azimuth;
						myLocationOverlay.setBearing(mAzimuthOrientation);
						if (mTrackingMode)
							map.setMapOrientation(-mAzimuthOrientation);
						else
							map.invalidate();
					}
					*/
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

private final Runnable centerOnUserLocation = new Runnable() {
    @Override
    public void run() {
        if (mTrackingMode) {
            mTrackingModeButton.setBackgroundResource(osm.custommaps.R.drawable.btn_tracking_on);
            if (myLocationOverlay.isEnabled() && myLocationOverlay.getLocation() != null) {
                map.getController().animateTo(myLocationOverlay.getLocation());

                map.setMapOrientation(-mAzimuthAngleSpeed);
                mTrackingModeButton.setKeepScreenOn(true);
            } else {
                mTrackingModeButton.setBackgroundResource(osm.custommaps.R.drawable.btn_tracking_off);
                map.setMapOrientation(0.0f);
                mTrackingModeButton.setKeepScreenOn(false);
            }
        }
    }};




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
        rotateBar = (SeekBar) findViewById(R.id.rotateBar);
        scaleBar = (SeekBar) findViewById(R.id.scaleBar);
        transparencyBar = (SeekBar) findViewById(R.id.transparencyBar);

        // Toggle between map and satellite view
        mapModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO       mapView.getOverlayManager().setSatellite(!mapView.isSatellite());
            }
        });

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {   //TODO had to edit my cast to geopoint again
                returnGeoPoint((GeoPoint) map.getMapCenter());
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
        });}

    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        InfoWindow.closeAllInfoWindowsOn(map);
        return true;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        return false;
    }


    /**
         * Reverse Geocoding
         */

    /*public String getAddress(GeoPoint p) {
        GeocoderNominatim geocoder = new GeocoderNominatim(this, userAgent);
        String theAddress;
        try {
            double dLatitude = p.getLatitude();
            double dLongitude = p.getLongitude();
            List<Address> addresses = geocoder.getFromLocation(dLatitude, dLongitude, 1);
            StringBuilder sb = new StringBuilder();
            if (addresses.size() > 0) {
                Address address = addresses.get(0);
                int n = address.getMaxAddressLineIndex();
                for (int i = 0; i <= n; i++) {
                    if (i != 0)
                        sb.append(", ");
                    sb.append(address.getAddressLine(i));
                }
                theAddress = sb.toString();
            } else {
                theAddress = null;
            }
        } catch (IOException e) {
            theAddress = null;
        }
        if (theAddress != null) {
            return theAddress;
        } else {
            return "";
        }
    }*/




    /*private class GeocodingTask extends AsyncTask<Object, Void, List<Address>> {
        int mIndex;

        protected List<Address> doInBackground(Object... params) {
            String locationAddress = (String) params[0];
            mIndex = (Integer) params[1];
            GeocoderNominatim geocoder = new GeocoderNominatim(getApplicationContext(), userAgent);
            geocoder.setOptions(true); //ask for enclosing polygon (if any)
            try {
                BoundingBoxE6 viewbox = map.getBoundingBox();
                List<Address> foundAdresses = geocoder.getFromLocationName(locationAddress, 1,
                        viewbox.getLatSouthE6() * 1E-6, viewbox.getLonEastE6() * 1E-6,
                        viewbox.getLatNorthE6() * 1E-6, viewbox.getLonWestE6() * 1E-6, false);
                return foundAdresses;
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(List<Address> foundAdresses) {
            if (foundAdresses == null) {
                Toast.makeText(getApplicationContext(), "Geocoding error", Toast.LENGTH_SHORT).show();
            } else if (foundAdresses.size() == 0) { //if no address found, display an error
                Toast.makeText(getApplicationContext(), "Address not found.", Toast.LENGTH_SHORT).show();
            } else {
                Address address = foundAdresses.get(0); //get first address
                String addressDisplayName = address.getExtras().getString("display_name");
                if (mIndex == START_INDEX) {
                    startPoint = new GeoPoint(address.getLatitude(), address.getLongitude());
                    markerStart = updateItineraryMarker(markerStart, startPoint, START_INDEX,
                            osm.custommaps.R.string.departure, osm.custommaps.R.drawable.marker_departure, -1, addressDisplayName);
                    map.getController().setCenter(startPoint);
                } else if (mIndex == DEST_INDEX) {
                    destinationPoint = new GeoPoint(address.getLatitude(), address.getLongitude());
                    markerDestination = updateItineraryMarker(markerDestination, destinationPoint, DEST_INDEX,
                            osm.custommaps.R.string.destination, osm.custommaps.R.drawable.marker_destination, -1, addressDisplayName);
                    map.getController().setCenter(destinationPoint);
                }
                getRoadAsync();
                //get and display enclosing polygon:
                Bundle extras = address.getExtras();
                if (extras != null && extras.containsKey("polygonpoints")) {
                    ArrayList<GeoPoint> polygon = extras.getParcelableArrayList("polygonpoints");
                    //Log.d("DEBUG", "polygon:"+polygon.size());
                    updateUIWithPolygon(polygon, addressDisplayName);
                } else {
                    updateUIWithPolygon(null, "");
                }
            }
        }
    }*/

    // -            -------------------------------------------------------------------------
    // Dialog management


    //Methods From Other  go here
    //-------------------------------------------------------------------------


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
// MapActivity status methods required by Google license


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






