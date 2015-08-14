package osm.custommaps.WMS;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.MapTileRequestState;
import org.osmdroid.tileprovider.modules.IFilesystemCache;
import org.osmdroid.tileprovider.modules.INetworkAvailablityCheck;
import org.osmdroid.tileprovider.modules.MapTileDownloader;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase.LowMemoryException;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.util.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * This code is mostly coppied from MapTileDownloader with changes
 * mainly in WMSTileLoader.
 *
 * @author Steve Potell -- spotell@t-sciences.com
 * */
public class WMSMapTileDownloader extends MapTileModuleProviderBase {

    // ===========================================================
    // Constants
    // ===========================================================

    private static final Logger logger = LoggerFactory.getLogger(MapTileDownloader.class);
    private static int MAXIMUM_ZOOMLEVEL = 0;

    // ===========================================================
    // Fields
    // ===========================================================

    private final IFilesystemCache mFilesystemCache;

    private OnlineTileSourceBase mTileSource;

    private final INetworkAvailablityCheck mNetworkAvailablityCheck;

    // ===========================================================
    // Constructors
    // ===========================================================

    public WMSMapTileDownloader(final ITileSource pTileSource) {
        this(pTileSource, null, null);
    }

    public WMSMapTileDownloader(final ITileSource pTileSource, final IFilesystemCache pFilesystemCache) {
        this(pTileSource, pFilesystemCache, null);
    }

    public WMSMapTileDownloader(final ITileSource pTileSource,
                                final IFilesystemCache pFilesystemCache,
                                final INetworkAvailablityCheck pNetworkAvailablityCheck) {
        super(NUMBER_OF_TILE_DOWNLOAD_THREADS, TILE_DOWNLOAD_MAXIMUM_QUEUE_SIZE);

        mFilesystemCache = pFilesystemCache;
        mNetworkAvailablityCheck = pNetworkAvailablityCheck;
        setTileSource(pTileSource);
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    public ITileSource getTileSource() {
        return mTileSource;
    }

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================

    @Override
    protected String getName() {
        return "WMS Online Tile Download Provider";
    }

    @Override
    protected String getThreadGroupName() {
        return "downloader";
    }

    @Override
    protected Runnable getTileLoader() {
        return new WMSTileLoader();
    }

    @Override
    public boolean getUsesDataConnection() {
        return true;
    }

    @Override
    public int getMinimumZoomLevel() {
        return (mTileSource != null ? mTileSource.getMinimumZoomLevel() : MINIMUM_ZOOMLEVEL);
    }

    @Override
    public int getMaximumZoomLevel() {
        return (mTileSource != null ? mTileSource.getMaximumZoomLevel() : MAXIMUM_ZOOMLEVEL);
    }

    @Override
    public void setTileSource(ITileSource tileSource) {
        // We are only interested in OnlineTileSourceBase tile sources
        if (tileSource instanceof OnlineTileSourceBase) {
            mTileSource = (OnlineTileSourceBase) tileSource;
        } else {
            // Otherwise shut down the tile downloader
            mTileSource = null;
        }
    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    private class WMSTileLoader extends MapTileModuleProviderBase.TileLoader {

        @Override
        public Drawable loadTile(final MapTileRequestState aState) throws CantContinueException {

            if (mTileSource == null) {
                return null;
            }

            InputStream in = null;
            OutputStream out = null;
            final MapTile tile = aState.getMapTile();

            try {

                if (mNetworkAvailablityCheck != null
                        && !mNetworkAvailablityCheck.getNetworkAvailable()) {
                    if (DEBUGMODE) {
                        logger.debug("WMSMapTileDownloader -- Skipping " + getName() + " due to NetworkAvailabliltyCheck.");
                    }
                    return null;
                }

                final String tileURLString = mTileSource.getTileURLString(tile);

                if (DEBUGMODE) {
                    logger.debug("WMSMapTileDownloader -- Downloading Maptile from url: " + tileURLString);
                }

                if (TextUtils.isEmpty(tileURLString)) {
                    return null;
                }

                // The main changes are here dealing with credentials.
                // create the credentials with username and password
                CredentialsProvider credProvider = new BasicCredentialsProvider();
                credProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                        new UsernamePasswordCredentials("", ""));

                // get the client
                HttpClient httpClient = getNewHttpClient();
                ((AbstractHttpClient) httpClient).setCredentialsProvider(credProvider);

                logger.debug("WMSMapTileDownloader -- tileURLString ------------------------- " + tileURLString);

                final HttpUriRequest head = new HttpGet(tileURLString);
                final HttpResponse response = httpClient.execute(head);

                // Check to see if we got success
                final org.apache.http.StatusLine line = response.getStatusLine();
                if (line.getStatusCode() != 200) {
                    logger.warn("WMSMapTileDownloader -- Problem downloading MapTile: " + tile + " HTTP response: " + line);
                    return null;
                }

                final HttpEntity entity = response.getEntity();
                if (entity == null) {
                    logger.warn("WMSMapTileDownloader -- No content downloading MapTile: " + tile);
                    return null;
                }
                in = entity.getContent();

                final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
                out = new BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE);
                StreamUtils.copy(in, out);
                out.flush();
                final byte[] data = dataStream.toByteArray();
                final ByteArrayInputStream byteStream = new ByteArrayInputStream(data);

                // Save the data to the filesystem cache
                if (mFilesystemCache != null) {
                    mFilesystemCache.saveFile(mTileSource, tile, byteStream);
                    byteStream.reset();
                }
                final Drawable result = mTileSource.getDrawable(byteStream);

                return result;
            } catch (final UnknownHostException e) {
                // no network connection so empty the queue
                logger.warn("WMSMapTileDownloader -- UnknownHostException downloading MapTile: " + tile + " : " + e);
                throw new CantContinueException(e);
            } catch (final LowMemoryException e) {
                // low memory so empty the queue
                logger.warn("WMSMapTileDownloader -- LowMemoryException downloading MapTile: " + tile + " : " + e);
                throw new CantContinueException(e);
            } catch (final FileNotFoundException e) {
                logger.warn("WMSMapTileDownloader -- Tile not found: " + tile + " : " + e);
            } catch (final IOException e) {
                logger.warn("WMSMapTileDownloader -- IOException downloading MapTile: " + tile + " : " + e);
            } catch (final Throwable e) {
                logger.error("WMSMapTileDownloader -- Error downloading MapTile: " + tile, e);
            } finally {
                StreamUtils.closeStream(in);
                StreamUtils.closeStream(out);
            }

            return null;
        }

       /* @Override //TODO Figure this out, dont understand how he did this.
        protected void tileLoaded(final MapTileRequestState pState, final Drawable pDrawable) {
            removeTileFromQueues(pState.getMapTile());
            // don't return the tile because we'll wait for the fs provider to ask for it
            // this prevent flickering when a load of delayed downloads complete for tiles
            // that we might not even be interested in any more
            pState.getCallback().mapTileRequestCompleted(pState, null);
        }*/

    } // end WMSTileLoader

    // TODO: fix the cert to prompt and store.

    /**
     * This class creates the HttpClient that we will use.
     *
     * @return the HttpClient that will trust all certs.
     */
    public HttpClient getNewHttpClient() {

        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

            return new DefaultHttpClient(ccm, params);
        } catch (Exception e) {
            return new DefaultHttpClient();
        }

    } // end getNewHttpClient

    /**
     * Socket factory for trusting all certs.
     */
    public class MySSLSocketFactory extends SSLSocketFactory {

        SSLContext sslContext = SSLContext.getInstance("TLS");

        public MySSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
            super(truststore);

            TrustManager tm = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            sslContext.init(null, new TrustManager[] { tm }, null);
        }

        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
            return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
        }

        @Override
        public Socket createSocket() throws IOException {
            return sslContext.getSocketFactory().createSocket();
        }

    } // end MySSLSocketFactory

} // end WMSMapTileDownloader
