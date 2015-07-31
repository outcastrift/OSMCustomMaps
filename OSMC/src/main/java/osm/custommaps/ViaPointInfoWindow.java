package osm.custommaps;

import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.MarkerInfoWindow;
import org.osmdroid.views.MapView;
import android.view.View;
import android.widget.Button;

import osm.custommaps.*;
import osm.custommaps.MapActivity;

/**
 * A customized InfoWindow handling "itinerary" points (start, destination and via-points). 
 * We inherit from MarkerInfoWindow as it already provides most of what we want. 
 * And we just add support for a "remove" button. 
 * 
 * @author M.Kergall
 */
public class ViaPointInfoWindow extends MarkerInfoWindow {

	int mSelectedPoint;
	
	public ViaPointInfoWindow(int layoutResId, MapView mapView) {
		super(layoutResId, mapView);
		Button btnDelete = (Button)(mView.findViewById(osm.custommaps.R.id.bubble_delete));
		btnDelete.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				//Call the removePoint method on MapActivity.
				//TODO: find a cleaner way to do that!
				osm.custommaps.MapActivity mapActivity = (MapActivity)view.getContext();
				mapActivity.removePoint(mSelectedPoint);
				close();
			}
		});
	}

	@Override public void onOpen(Object item) {
		Marker eItem = (Marker)item;
		mSelectedPoint = (Integer)eItem.getRelatedObject();
		super.onOpen(item);
	}

}
