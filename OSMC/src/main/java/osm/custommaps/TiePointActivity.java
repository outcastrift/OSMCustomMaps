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

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.ResourceProxyImpl;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MinimapOverlay;
import org.osmdroid.views.overlay.MyLocationOverlay;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import osm.custommaps.HelpDialogManager;
import osm.custommaps.MapApiKeys;
import osm.custommaps.R;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * TiePointActivity allows users to tie points on bitmap images to geo
 * coordinates on Google Maps.
 *
 * @author Marko Teittinen
 */
public class TiePointActivity extends FragmentActivity implements CustomMapsConstants {
  private static final String EXTRA_PREFIX = "osm.custommaps";
  public static final String BITMAP_DATA = EXTRA_PREFIX + ".BitmapData";
  public static final String IMAGE_POINT = EXTRA_PREFIX + ".ImagePoint";
  public static final String RESTORE_SETTINGS = EXTRA_PREFIX + ".RestoreSettings";
  public static final String GEO_POINT_E6 = EXTRA_PREFIX + ".GeoPointE6";

  private static final String LOG_TAG = "Custom Maps";

  private MapView mapView;
  private ImageButton mapModeButton;
  private Button doneButton;
  private SeekBar scaleBar;
  private SeekBar transparencyBar;
  private SeekBar rotateBar;
  private MyLocationOverlay userLocation;


  private MapImageOverlay imageOverlay;
  private HelpDialogManager helpDialogManager;
  private SharedPreferences mPrefs;
  private MyLocationNewOverlay mLocationOverlay;
  private CompassOverlay mCompassOverlay;
  private MinimapOverlay mMinimapOverlay;
  private ScaleBarOverlay mScaleBarOverlay;
  private ResourceProxy mResourceProxy;

  /*In your on create method add this code after creating your MapView:

  // Add tiles layer with custom WMS tile source
  final WMSMapTileProviderBasic tileProvider = new WMSMapTileProviderBasic(getApplicationContext());
  final ITileSource tileSource = new WMSTileSource("wmsserver", null, 3, 18, 256, ".png",
          getString(R.string.your_map_request));
  tileProvider.setTileSource(tileSource);
  final TilesOverlay tilesOverlay = new TilesOverlay(tileProvider, this.getBaseContext());

  tilesOverlay.setLoadingBackgroundColor(Color.TRANSPARENT);

  mapView.getOverlays().add(tilesOverlay);


  your map_request_string will looks something like this:

  https://xxx.xxx.xx.xx/geoserver/gwc/service/wms?LAYERS=base_map&amp;FORMAT=image/jpeg&amp;SERVICE=WMS&amp;VERSION=1.1.1&amp;REQUEST=GetMap&amp;STYLES=&amp;SRS=EPSG:900913&amp;WIDTH=256&amp;HEIGHT=256&amp;BBOX=
*/

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.tiepoints);
    Context context = getApplicationContext();
    DisplayMetrics dm = context.getResources().getDisplayMetrics();
    mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);


    mapView = (MapView) findViewById(R.id.mapviewlocation);
   // mapView = (MapView) findViewById(R.id.mapviewlocation);
    ViewParent p = mapView.getParent();
    if (p instanceof ViewGroup) {
      ViewGroup layout = (ViewGroup) p;
      LayoutParams layoutParams = mapView.getLayoutParams();
      try {
        mResourceProxy = new ResourceProxyImpl(getApplicationContext());

        layout.removeView(mapView);
        layout.addView(mapView, layoutParams);
        //mapView = new MapView(context, 256, new DefaultResourceProxyImpl(context), null, null);

        this.mCompassOverlay = new CompassOverlay(context, new InternalCompassOrientationProvider(context),
                mapView);
        this.mLocationOverlay = new MyLocationNewOverlay(context, new GpsMyLocationProvider(context),
                mapView);

        mMinimapOverlay = new MinimapOverlay(context, mapView.getTileRequestCompleteHandler());
        mMinimapOverlay.setWidth(dm.widthPixels / 5);
        mMinimapOverlay.setHeight(dm.heightPixels / 5);

        mScaleBarOverlay = new ScaleBarOverlay(context);
        mScaleBarOverlay.setCentred(true);
        mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 10);


        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.getOverlays().add(this.mLocationOverlay);
        mapView.getOverlays().add(this.mCompassOverlay);
        mapView.getOverlays().add(this.mMinimapOverlay);
        mapView.getOverlays().add(this.mScaleBarOverlay);
        mapView.setMinZoomLevel(3);
        mapView.getZoomLevel(true);


        mapView.getController().setZoom(mPrefs.getInt(PREFS_ZOOM_LEVEL, 1));
        mapView.scrollTo(mPrefs.getInt(PREFS_SCROLL_X, 0), mPrefs.getInt(PREFS_SCROLL_Y, 0));

        mLocationOverlay.enableMyLocation();
        mCompassOverlay.enableCompass();
        mapView.setEnabled(true);
      } catch (IllegalArgumentException ex) {
        Log.e(LOG_TAG, "Failed to create a map matching the signature key");
        setContentView(mapView);
        return;
      }
    }
    userLocation = new MyLocationOverlay(this, mapView);

    prepareUI();

    mapView.setBuiltInZoomControls(true);
    //mapView.setReticleDrawMode(MapView.ReticleDrawMode.DRAW_RETICLE_NEVER);

    Bundle extras = getIntent().getExtras();
    int[] center = extras.getIntArray(IMAGE_POINT);
    byte[] pngImage = extras.getByteArray(BITMAP_DATA);
    Bitmap image = BitmapFactory.decodeByteArray(pngImage, 0, pngImage.length);
    imageOverlay = new MapImageOverlay(context);
    imageOverlay.setOverlayImage(image, center[0], center[1]);
    mapView.getOverlays().add(imageOverlay);

    if (extras.getBoolean(RESTORE_SETTINGS, false)) {
      PreferenceHelper prefs = new PreferenceHelper();
      writeTransparencyUi(prefs.getTransparency());
      writeScaleUi(prefs.getScale());
      writeRotationUi(prefs.getRotation());
    } else {
      writeTransparencyUi(50);
      imageOverlay.setTransparency(50);
      writeScaleUi(1.0f);
      writeRotationUi(0);
    }

    helpDialogManager = new HelpDialogManager(this, HelpDialogManager.HELP_TIE_POINT,
      "Zoom and pan Google map to match the center of the thumbnail.\n\n" + //
      "Use the sliders to rotate and scale the thumbnail, and to adjust its transparency.\n\n" +
      "Map button toggles map view and satellite view.");
  }

  @Override
  protected void onResume() {
    super.onResume();

    userLocation.enableMyLocation();
    Bundle extras = getIntent().getExtras();
    final String tileSourceName = mPrefs.getString(PREFS_TILE_SOURCE,
            TileSourceFactory.DEFAULT_TILE_SOURCE.name());
    try {
      final ITileSource tileSource = TileSourceFactory.getTileSource(tileSourceName);
      mapView.setTileSource(tileSource);
    } catch (final IllegalArgumentException ignore) {
    }
    if (mPrefs.getBoolean(PREFS_SHOW_LOCATION, false)) {
      this.mLocationOverlay.enableMyLocation();
    }
    if (mPrefs.getBoolean(PREFS_SHOW_COMPASS, false)) {
      this.mCompassOverlay.enableCompass();
    }
    if (extras.containsKey(GEO_POINT_E6)) {
      // Editing a tiepoint that was previously placed, center it on view
      int[] geoLocationE6 = extras.getIntArray(GEO_POINT_E6);
      mapView.getController().setCenter(new GeoPoint(geoLocationE6[0], geoLocationE6[1]));
      // Prevent resetting of map center point on device orientation change
      extras.remove(GEO_POINT_E6);
    }
    imageOverlay.setTransparency(readTransparencyUi());
    imageOverlay.setScale(readScaleUi());
    imageOverlay.setRotate(readRotationUi());
    mapView.postInvalidate();
    helpDialogManager.onResume();

  }

  @Override
  protected void onPause() {
    helpDialogManager.onPause();
    mLocationOverlay.disableMyLocation();

    userLocation.disableMyLocation();
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
    int[] geoPoint = new int[] {location.getLatitudeE6(), location.getLongitudeE6()};
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
        mLocationOverlay.enableMyLocation(); //TODO Might Break This
        return true;
      default:
        helpDialogManager.onOptionsItemSelected(item);
        return true;
    }
  }

  // --------------------------------------------------------------------------
  // Dialog management

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
      GeoPoint userGeo = userLocation.getMyLocation();
      if (userGeo != null) {
        mapView.getController().animateTo(userGeo);
      }
    }
  };

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
        returnGeoPoint((GeoPoint) mapView.getMapCenter());
      }
    });

    rotateBar.setOnSeekBarChangeListener(new ImageOverlayAdjuster() {
      @Override
      protected void updateImageOverlay(int value) {
        imageOverlay.setRotate(value - 180);
        mapView.invalidate();
      }
    });

    scaleBar.setOnSeekBarChangeListener(new ImageOverlayAdjuster() {
      @Override
      protected void updateImageOverlay(int value) {
        imageOverlay.setScale(1.0f + (value / 10f));
        mapView.invalidate();
      }
    });

    transparencyBar.setOnSeekBarChangeListener(new ImageOverlayAdjuster() {
      @Override
      protected void updateImageOverlay(int value) {
        imageOverlay.setTransparency(value);
        mapView.invalidate();
      }
    });
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
  // MapActivity status methods required by Google license

  /*@Override
  protected boolean isRouteDisplayed() {  //TODO no longer needed
    return false;
  }*/

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
