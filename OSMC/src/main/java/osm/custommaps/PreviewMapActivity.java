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


import osm.custommaps.CustomMaps;
import osm.custommaps.HelpDialogManager;
import osm.custommaps.ImageHelper;
import osm.custommaps.MapApiKeys;
import osm.custommaps.PtSizeFixer;
import osm.custommaps.R;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.os.Build;
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
import android.widget.Toast;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MinimapOverlay;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

/**
 * PreviewMapActivity shows the created map aligned on top of Google Map.
 *
 * @author Marko Teittinen
 */
public class PreviewMapActivity extends FragmentActivity implements CustomMapsConstants{
  private static final String EXTRA_PREFIX = "osm.custommaps";
  public static final String BITMAP_FILE = EXTRA_PREFIX + ".BitmapFile";
  public static final String IMAGE_POINTS = EXTRA_PREFIX + ".ImagePoints";
  public static final String TIEPOINTS = EXTRA_PREFIX + ".Tiepoints";
  public static final String CORNER_GEO_POINTS = EXTRA_PREFIX + ".CornerGeoPoints";
  public static final boolean DEBUGMODE = false;

  public static final int NOT_SET = Integer.MIN_VALUE;

  public static final String PREFS_NAME = "org.andnav.osm.prefs";
  public static final String PREFS_TILE_SOURCE = "tilesource";
  public static final String PREFS_SCROLL_X = "scrollX";
  public static final String PREFS_SCROLL_Y = "scrollY";
  public static final String PREFS_ZOOM_LEVEL = "zoomLevel";
  public static final String PREFS_SHOW_LOCATION = "showLocation";
  public static final String PREFS_SHOW_COMPASS = "showCompass";

  private static final String LOG_TAG = "Custom Maps";

  private MapView mapView;
  private Button saveButton;
  private ImageButton mapModeButton;
  private SeekBar transparencyBar;
  private Canvas canvas;
  private WarpedImageOverlay imageOverlay;


  private Matrix imageToGeo;
  private GeoPoint mapImageCenter;
  private List<GeoPoint> imageCornerGeoPoints;
  private int latSpanE6;
  private int lonSpanE6;
  private SharedPreferences mPrefs;
  private MyLocationNewOverlay mLocationOverlay;
  private CompassOverlay mCompassOverlay;
  private MinimapOverlay mMinimapOverlay;
  private ScaleBarOverlay mScaleBarOverlay;
  private ResourceProxy mResourceProxy;

  private HelpDialogManager helpDialogManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Context context = getApplicationContext();
    setContentView(R.layout.createpreview);
    DisplayMetrics dm = context.getResources().getDisplayMetrics();
    mapView  = (MapView) findViewById(R.id.mapviewlocation);
    // mapView = (MapView) findViewById(R.id.mapviewlocation);
    ViewParent p = mapView.getParent();
    if (p instanceof ViewGroup) {
      ViewGroup layout = (ViewGroup) p;
      LayoutParams layoutParams = mapView.getLayoutParams();
      try {
        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

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
        Log.e(CustomMaps.LOG_TAG, "Failed to create a map matching the signature key");
        Button saveButton = (Button) findViewById(R.id.save);
        saveButton.setEnabled(false);
        return;
      }
    }

    prepareUI();
    if (PtSizeFixer.isFixNeeded((Activity) null)) {
      PtSizeFixer.fixView(saveButton.getRootView());
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      // Update actionbar title to match selected locale
     getActionBar().setTitle(R.string.create_map_name);
    }

    mapView.setBuiltInZoomControls(true);
   // mapView.setReticleDrawMode(MapView.ReticleDrawMode.DRAW_RETICLE_NEVER);

    // Create overlay
    String fileName = getIntent().getStringExtra(BITMAP_FILE);
    Bitmap mapImage = ImageHelper.loadImage(fileName, true);
    if (mapImage == null) {
      // Failed to load image, cancel activity
      Toast.makeText(this, R.string.editor_image_load_failed, Toast.LENGTH_LONG).show();
      setResult(RESULT_CANCELED);
      finish();
      return;
    }

    imageOverlay = new WarpedImageOverlay(context, mapImage);

    List<Point> imagePoints;
    imagePoints = new ArrayList<Point>();
    int[] imagePointArray = getIntent().getIntArrayExtra(IMAGE_POINTS);
    for (int i = 0; i + 1 < imagePointArray.length; i += 2) {
      imagePoints.add(new Point(imagePointArray[i], imagePointArray[i + 1]));
    }
    List<GeoPoint> geoPoints = new ArrayList<GeoPoint>();
    int[] geoPointArray = getIntent().getIntArrayExtra(TIEPOINTS);
    for (int i = 0; i + 1 < geoPointArray.length; i += 2) {
      geoPoints.add(new GeoPoint(geoPointArray[i], geoPointArray[i + 1]));
    }
    imageOverlay.setTiepoints(imagePoints, geoPoints);
    mapView.getOverlays().add(imageOverlay);

    transparencyBar.setProgress(50);
    imageOverlay.setTransparency(50);

    helpDialogManager = new HelpDialogManager(this, HelpDialogManager.HELP_PREVIEW_CREATE,
            getString(R.string.preview_help));

    // Compute geo location of map image center
    if (!imageOverlay.computeImageWarp(mapView)) {
      IGeoPoint mapImageCenter = mapView.getMapCenter(); //Todo casted to Geopoint
      latSpanE6 = mapView.getLatitudeSpan();
      lonSpanE6 = mapView.getLongitudeSpan();
      return;
    }
    // Get matrix for converting image points to geo points
    imageToGeo = new Matrix();
    imageToGeo.set(imageOverlay.computeImageToGeoMatrix(mapView));
    // Convert center point to geo
    Point center = new Point(mapImage.getWidth() / 2, mapImage.getHeight() / 2);
    mapImageCenter = imageToGeoPoint(imageToGeo, center);
    // Find lat and lon spans
    computeImageCornerGeoPoints();
    int minLatE6 = Integer.MAX_VALUE;
    int maxLatE6 = Integer.MIN_VALUE;
    int minLonE6 = Integer.MAX_VALUE;
    int maxLonE6 = Integer.MIN_VALUE;
    for (GeoPoint gp : imageCornerGeoPoints) {
      minLatE6 = Math.min(minLatE6, gp.getLatitudeE6());
      maxLatE6 = Math.max(maxLatE6, gp.getLatitudeE6());
      minLonE6 = Math.min(minLonE6, gp.getLongitudeE6());
      maxLonE6 = Math.max(maxLonE6, gp.getLongitudeE6());
    }
    latSpanE6 = maxLatE6 - minLatE6;
    lonSpanE6 = maxLonE6 - minLonE6;
  }

  @Override
  protected void onResume() {
    super.onResume();
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
    if (mapView != null) {
      mapView.getController().setCenter(mapImageCenter);
      mapView.getController().zoomToSpan(latSpanE6, lonSpanE6);
      helpDialogManager.onResume();
    }
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    if (imageOverlay != null) {
      imageOverlay.setTransparency(transparencyBar.getProgress());
      helpDialogManager.onRestoreInstanceState(savedInstanceState);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    if (helpDialogManager != null) {
      helpDialogManager.onSaveInstanceState(outState);
    }
    super.onSaveInstanceState(outState);
  }

  public void computeAndReturnTiepoints() {
    if (imageToGeo == null) {
      imageOverlay.computeImageWarp(mapView);
      imageToGeo = imageOverlay.computeImageToGeoMatrix(mapView);
      computeImageCornerGeoPoints();
    }
    int[] cornerGeoPoints = new int[8];
    int i = 0;
    for (GeoPoint gp : imageCornerGeoPoints) {
      cornerGeoPoints[i++] = gp.getLatitudeE6();
      cornerGeoPoints[i++] = gp.getLongitudeE6();
    }
    Intent result = getIntent();
    result.putExtra(CORNER_GEO_POINTS, cornerGeoPoints);
    setResult(RESULT_OK, result);
    mLocationOverlay.disableMyLocation();
    mCompassOverlay.disableCompass();
    mapView.setMultiTouchControls(false);
    mapView.getOverlays().remove(this.mLocationOverlay);
    mapView.getOverlays().remove(this.mCompassOverlay);
    mapView.getOverlays().remove(this.mMinimapOverlay);
    mapView.getOverlays().remove(this.mScaleBarOverlay);
    mapView.setEnabled(false);
    finish();
  }

  @Override
  protected void onPause() {
    if (helpDialogManager != null) {
      helpDialogManager.onPause();
    }
    if (isFinishing() && imageOverlay != null) {
      Bitmap image = imageOverlay.getImage();
      if (image != null && !image.isRecycled()) {
        image.recycle();
        imageOverlay.setImage(null);
      }
    }
    super.onPause();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    if (helpDialogManager == null) {
      return false;
    }
    helpDialogManager.onCreateOptionsMenu(menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    helpDialogManager.onOptionsItemSelected(item);
    return true;
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
  // Image overlay helper methods

  private void computeImageCornerGeoPoints() {
    Bitmap mapImage = imageOverlay.getImage();
    // List corners starting from lower left going counter clockwise
    List<Point> corners = new ArrayList<Point>();
    corners.add(new Point(0, mapImage.getHeight()));
    corners.add(new Point(mapImage.getWidth(), mapImage.getHeight()));
    corners.add(new Point(mapImage.getWidth(), 0));
    corners.add(new Point(0, 0));
    // Convert to geo points
    imageCornerGeoPoints = new ArrayList<GeoPoint>();
    for (Point pt : corners) {
      GeoPoint geoPoint = imageToGeoPoint(imageToGeo, pt);
      imageCornerGeoPoints.add(geoPoint);
    }
  }

  private GeoPoint imageToGeoPoint(Matrix converter, Point imagePoint) {
    float[] coords = new float[] {imagePoint.x, imagePoint.y};
    converter.mapPoints(coords);
    return new GeoPoint(Math.round(coords[1] * 1E6f), Math.round(coords[0] * 1E6f));
  }

  // --------------------------------------------------------------------------
  // Prepare UI elements

  private void prepareUI() {
    saveButton = (Button) findViewById(R.id.save);
    mapModeButton = (ImageButton) findViewById(R.id.mapmode);
    transparencyBar = (SeekBar) findViewById(R.id.transparencyBar);

    saveButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        computeAndReturnTiepoints();
      }
    });

    mapModeButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        return;
        //mapView.setSatellite(!mapView.isSatellite()); //todo fix this later
      }
    });

    transparencyBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      private void updateImageOverlay(int value) {
        imageOverlay.setTransparency(value);
        mapView.invalidate();
      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        updateImageOverlay(seekBar.getProgress());
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
        updateImageOverlay(seekBar.getProgress());
      }

      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
          updateImageOverlay(seekBar.getProgress());
        }
      }
    });
  }

  // --------------------------------------------------------------------------
  // MapActivity status methods required by Google license


}
