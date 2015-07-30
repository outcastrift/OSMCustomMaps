package osm.custommaps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Location;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.NonAcceleratedOverlay;
import org.osmdroid.views.overlay.Overlay;

import java.util.List;

/**
 * Created by Sam on 7/29/2015.
 */
public class BobTheBuilder extends NonAcceleratedOverlay {
    private List<Point> imagePoints;
    private List<GeoPoint> geoPoints;
    private List<Point> screenPoints;
    private Bitmap image;
    private Bitmap newImage;
    private Paint transparency;
    private Paint pointPaint;
    private Matrix imageMatrix;
    private GeoPoint lastDrawnLocation = null;
    private float lastDrawnZoom = -1;



    public BobTheBuilder(Context ctx) {
        super(ctx);
    }

    @Override
    protected void onDraw(Canvas canvas, MapView mapView, boolean shadow) {

                if (!shadow) {
                    IGeoPoint mapCenter =  mapView.getMapCenter();
                    float zoom = mapView.getProjection().metersToEquatorPixels(1.0f);
                    if (zoom != lastDrawnZoom || !mapCenter.equals(lastDrawnLocation)) {

                            Projection projection = mapView.getProjection();
                            int i = 0;
                            for (GeoPoint gp : geoPoints) {
                                Point sp = screenPoints.get(i++);
                                projection.toPixels(gp, sp);
                                lastDrawnZoom = zoom;
                                lastDrawnLocation = (GeoPoint) mapCenter;
                            }


                    }
                    // Draw translucent map image
                    canvas.drawBitmap(image, imageMatrix, transparency);
                    // Highlight specified tie points with circles
                    for (Point p : screenPoints) {
                        canvas.drawCircle(p.x, p.y, 10, pointPaint);
                    }
                }
            }
        }

