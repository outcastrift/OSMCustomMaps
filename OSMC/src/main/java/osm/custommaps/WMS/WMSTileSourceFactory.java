package osm.custommaps.WMS;

import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.tilesource.CloudmadeTileSource;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Sam on 8/5/2015.
 */
public class WMSTileSourceFactory extends TileSourceFactory {
        public WMSTileSourceFactory() {
        }

        public static ITileSource getTileSource(String aName) throws IllegalArgumentException {
            Iterator i$ = mTileSources.iterator();

            ITileSource tileSource;
            do {
                if(!i$.hasNext()) {
                    throw new IllegalArgumentException("No such tile source: " + aName);
                }

                tileSource = (ITileSource)i$.next();
            } while(!tileSource.name().equals(aName));

            return tileSource;
        }

        public static boolean containsTileSource(String aName) {
            Iterator i$ = mTileSources.iterator();

            ITileSource tileSource;
            do {
                if(!i$.hasNext()) {
                    return false;
                }

                tileSource = (ITileSource)i$.next();
            } while(!tileSource.name().equals(aName));

            return true;
        }

        public static ITileSource getTileSource(int aOrdinal) throws IllegalArgumentException {
            Iterator i$ = mTileSources.iterator();

            ITileSource tileSource;
            do {
                if(!i$.hasNext()) {
                    throw new IllegalArgumentException("No tile source at position: " + aOrdinal);
                }

                tileSource = (ITileSource)i$.next();
            } while(tileSource.ordinal() != aOrdinal);

            return tileSource;
        }

    public static final OnlineTileSourceBase MAPNIK;
    public static final OnlineTileSourceBase CYCLEMAP;
    public static final OnlineTileSourceBase PUBLIC_TRANSPORT;
    public static final OnlineTileSourceBase MAPQUESTOSM;
    public static final OnlineTileSourceBase MAPQUESTAERIAL;
    public static final OnlineTileSourceBase MAPQUESTAERIAL_US;
    public static final OnlineTileSourceBase DEFAULT_TILE_SOURCE;
    public static final OnlineTileSourceBase CLOUDMADESTANDARDTILES;
    public static final OnlineTileSourceBase CLOUDMADESMALLTILES;
    public static final OnlineTileSourceBase FIETS_OVERLAY_NL;
    public static final OnlineTileSourceBase BASE_OVERLAY_NL;
    public static final OnlineTileSourceBase ROADS_OVERLAY_NL;
    public static final OnlineTileSourceBase GOOGLE_SATELLITE;
    public static final OnlineTileSourceBase GOOGLE_MAPS;
    public static final OnlineTileSourceBase GOOGLE_TERRAIN;
    public static final OnlineTileSourceBase BING_MAPS;
    public static final OnlineTileSourceBase BING_EARTH;
    public static final OnlineTileSourceBase BING_HYBRID;
    public static final OnlineTileSourceBase YAHOO_MAPS;
    public static final OnlineTileSourceBase YAHOO_MAPS_SATELLITE;
    private static ArrayList<ITileSource> mTileSources;
        public static ArrayList<ITileSource> getTileSources() {
            return mTileSources;
        }

        public static void addTileSource(ITileSource mTileSource) {
            mTileSources.add(mTileSource);
        }

        static {
            final int size = TileSourceFactory.getTileSources().size();

            MAPNIK = new XYTileSource("Mapnik", ResourceProxy.string.mapnik, 0, 18, 256, ".png", new String[]{"http://a.tile.openstreetmap.org/", "http://b.tile.openstreetmap.org/", "http://c.tile.openstreetmap.org/"});
            CYCLEMAP = new XYTileSource("CycleMap", ResourceProxy.string.cyclemap, 0, 17, 256, ".png", new String[]{"http://a.tile.opencyclemap.org/cycle/", "http://b.tile.opencyclemap.org/cycle/", "http://c.tile.opencyclemap.org/cycle/"});
            PUBLIC_TRANSPORT = new XYTileSource("OSMPublicTransport", ResourceProxy.string.public_transport, 0, 17, 256, ".png", new String[]{"http://openptmap.org/tiles/"});
            MAPQUESTOSM = new XYTileSource("MapquestOSM", ResourceProxy.string.mapquest_osm, 0, 18, 256, ".jpg", new String[]{"http://otile1.mqcdn.com/tiles/1.0.0/map/", "http://otile2.mqcdn.com/tiles/1.0.0/map/", "http://otile3.mqcdn.com/tiles/1.0.0/map/", "http://otile4.mqcdn.com/tiles/1.0.0/map/"});
            MAPQUESTAERIAL = new XYTileSource("MapquestAerial", ResourceProxy.string.mapquest_aerial, 0, 11, 256, ".jpg", new String[]{"http://otile1.mqcdn.com/tiles/1.0.0/sat/", "http://otile2.mqcdn.com/tiles/1.0.0/sat/", "http://otile3.mqcdn.com/tiles/1.0.0/sat/", "http://otile4.mqcdn.com/tiles/1.0.0/sat/"});
            MAPQUESTAERIAL_US = new XYTileSource("MapquestAerialUSA", ResourceProxy.string.mapquest_aerial, 0, 18, 256, ".jpg", new String[]{"http://otile1.mqcdn.com/tiles/1.0.0/sat/", "http://otile2.mqcdn.com/tiles/1.0.0/sat/", "http://otile3.mqcdn.com/tiles/1.0.0/sat/", "http://otile4.mqcdn.com/tiles/1.0.0/sat/"});
            DEFAULT_TILE_SOURCE = MAPNIK;
            CLOUDMADESTANDARDTILES = new CloudmadeTileSource("CloudMadeStandardTiles", ResourceProxy.string.cloudmade_standard, 0, 18, 256, ".png", new String[]{"http://a.tile.cloudmade.com/%s/%d/%d/%d/%d/%d%s?token=%s", "http://b.tile.cloudmade.com/%s/%d/%d/%d/%d/%d%s?token=%s", "http://c.tile.cloudmade.com/%s/%d/%d/%d/%d/%d%s?token=%s"});
            CLOUDMADESMALLTILES = new CloudmadeTileSource("CloudMadeSmallTiles", ResourceProxy.string.cloudmade_small, 0, 21, 64, ".png", new String[]{"http://a.tile.cloudmade.com/%s/%d/%d/%d/%d/%d%s?token=%s", "http://b.tile.cloudmade.com/%s/%d/%d/%d/%d/%d%s?token=%s", "http://c.tile.cloudmade.com/%s/%d/%d/%d/%d/%d%s?token=%s"});
            FIETS_OVERLAY_NL = new XYTileSource("Fiets", ResourceProxy.string.fiets_nl, 3, 18, 256, ".png", new String[]{"http://overlay.openstreetmap.nl/openfietskaart-overlay/"});
            BASE_OVERLAY_NL = new XYTileSource("BaseNL", ResourceProxy.string.base_nl, 0, 18, 256, ".png", new String[]{"http://overlay.openstreetmap.nl/basemap/"});
            ROADS_OVERLAY_NL = new XYTileSource("RoadsNL", ResourceProxy.string.roads_nl, 0, 18, 256, ".png", new String[]{"http://overlay.openstreetmap.nl/roads/"});
            GOOGLE_SATELLITE = new OSMGoogleRenderer("Google Maps Satellite", ResourceProxy.string.unknown,0,20,256,".png",size + 1," http://mt0.google.com/vt/lyrs=s@127,h@127&hl=en&");   //http://mt1.google.com/vt/lyrs=s&amp;x={$x}&amp;y={$y}&amp;z={$z}, http://mt0.google.com/vt/lyrs=s@127,h@127&hl=en&
            GOOGLE_MAPS = new OSMGoogleRenderer("Google Maps", ResourceProxy.string.unknown,0,19,256,".png",size,"http://mt0.google.com/vt/lyrs=m@127&");
            GOOGLE_TERRAIN = new OSMGoogleRenderer("Google Maps Terrain", ResourceProxy.string.unknown,0,15,256,".jpg",size + 2,"http://mt0.google.com/vt/lyrs=t@127,r@127&");
            YAHOO_MAPS = new OSMMapYahooRenderer("Yahoo Maps", ResourceProxy.string.unknown,0,17,256,".jpg",size + 3,"http://maps.yimg.com/hw/tile?");
            YAHOO_MAPS_SATELLITE = new OSMMapYahooRenderer("Yahoo Maps Satellite", ResourceProxy.string.unknown,0,17,256,".jpg",size + 4,"http://maps.yimg.com/ae/ximg?");
            BING_MAPS = new OSMMapMicrosoftRenderer("Microsoft Maps", ResourceProxy.string.unknown,0,19,256,".png",size + 5,"http://r0.ortho.tiles.virtualearth.net/tiles/r");
            BING_EARTH = new OSMMapMicrosoftRenderer("Microsoft Earth", ResourceProxy.string.unknown,0,19,256,".jpg",size + 6,"http://a0.ortho.tiles.virtualearth.net/tiles/a");
            BING_HYBRID = new OSMMapMicrosoftRenderer("Microsoft Hybrid", ResourceProxy.string.unknown,0,19,256,".jpg",size + 7,"http://h0.ortho.tiles.virtualearth.net/tiles/h");
            mTileSources = new ArrayList();
            mTileSources.add(MAPNIK);
            mTileSources.add(CYCLEMAP);
            mTileSources.add(PUBLIC_TRANSPORT);
            mTileSources.add(MAPQUESTOSM);
            mTileSources.add(MAPQUESTAERIAL);
            mTileSources.add(GOOGLE_MAPS);
            mTileSources.add(GOOGLE_SATELLITE);
            mTileSources.add(GOOGLE_TERRAIN);
            mTileSources.add(BING_EARTH);
            mTileSources.add(BING_HYBRID);
            mTileSources.add(BING_MAPS);
            mTileSources.add(YAHOO_MAPS);
            mTileSources.add(YAHOO_MAPS_SATELLITE);
        }

    /**
     *  TileSourceFactory.addTileSource(new OSMMapGoogleRenderer("Google Maps",ResourceProxy.string.unknown,0,19,256,".png",size,"http://mt0.google.com/vt/lyrs=m@127&"));
     TileSourceFactory.addTileSource(new OSMMapGoogleRenderer("Google Maps Satellite",ResourceProxy.string.unknown,0,19,256,".jpg",size + 1,"http://mt0.google.com/vt/lyrs=s@127,h@127&"));
     TileSourceFactory.addTileSource(new OSMMapGoogleRenderer("Google Maps Terrain",ResourceProxy.string.unknown,0,15,256,".jpg",size + 2,"http://mt0.google.com/vt/lyrs=t@127,r@127&"));
     TileSourceFactory.addTileSource(new OSMMapYahooRenderer("Yahoo Maps",ResourceProxy.string.unknown,0,17,256,".jpg",size + 3,"http://maps.yimg.com/hw/tile?"));
     TileSourceFactory.addTileSource(new OSMMapYahooRenderer("Yahoo Maps Satellite",ResourceProxy.string.unknown,0,17,256,".jpg",size + 4,"http://maps.yimg.com/ae/ximg?"));
     TileSourceFactory.addTileSource(new OSMMapMicrosoftRenderer("Microsoft Maps",ResourceProxy.string.unknown,0,19,256,".png",size + 5,"http://r0.ortho.tiles.virtualearth.net/tiles/r"));
     TileSourceFactory.addTileSource(new OSMMapMicrosoftRenderer("Microsoft Earth",ResourceProxy.string.unknown,0,19,256,".jpg",size + 6,"http://a0.ortho.tiles.virtualearth.net/tiles/a"));
     TileSourceFactory.addTileSource(new OSMMapMicrosoftRenderer("Microsoft Hybrid",ResourceProxy.string.unknown,0,19,256,".jpg",size + 7,"http://h0.ortho.tiles.virtualearth.net/tiles/h"));

     */
    }


