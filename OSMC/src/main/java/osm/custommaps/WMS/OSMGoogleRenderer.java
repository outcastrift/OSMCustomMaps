package osm.custommaps.WMS;

/**
 * Created by Sam on 11-Aug-15.
 */
import org.osmdroid.ResourceProxy;
import org.osmdroid.ResourceProxy.string;
import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;

public class OSMGoogleRenderer extends OnlineTileSourceBase {

    private final int mOrdinal;

    public OSMGoogleRenderer(String aName, final string aResourceId, int aZoomMinLevel,
                                int aZoomMaxLevel, int aMaptileZoom, String aImageFilenameEnding, int ordinal,
                                String ...aBaseUrl) {
        super(aName, aResourceId, aZoomMinLevel, aZoomMaxLevel, aMaptileZoom, aImageFilenameEnding, aBaseUrl);
        mOrdinal = ordinal;
    }

    @Override
    public int ordinal() {
        return mOrdinal;
    }

    @Override
    public String localizedName(ResourceProxy proxy) {
        return name();
    }

    @Override
    public String getTileURLString(
            MapTile aTile) {
        return new StringBuilder().append(getBaseUrl()).append("x=").append(aTile.getX())
                .append("&y=").append(aTile.getY()).append("&z=").append(aTile.getZoomLevel())
                .toString();
    }

}