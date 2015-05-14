package hu.markcool.mygps;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

import ssl.SSLSocketFactoryEx;

public class GpsService extends Service {

    private static final String TAG = "GpsService ";

    private LocationManager mLocationManager = null;

    // The minimum time between updates in milliseconds
    private static final int MIN_TIME_BW_UPDATES = 1000 * 30; // 1 minute

    // The minimum distance to change Updates in meters
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 50f;


    private String deviceId;
    private double latitude, longitude;

    // Define a listener that responds to location updates
    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;

        public LocationListener(String provider) {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.e(TAG, "onLocationChanged: " + location);

//            Toast.makeText(getApplicationContext(), "Location Changed, now:" + location, Toast.LENGTH_SHORT).show();

            mLastLocation.set(location);

            if (location == null)  return;



            Location dest = new Location(location); //取得現在位置
            latitude = dest.getLatitude();
            longitude = dest.getLongitude();

            Toast.makeText(getApplicationContext(), "Location Changed, now latitude:" + latitude +
                    ", longitude:" + longitude, Toast.LENGTH_SHORT).show();

            // save cloud DB
            Thread t1=new Thread(postData);
            t1.start();



        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }

    LocationListener[] mLocationListeners = new LocationListener[]{
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {

        Log.e(TAG, "onCreate");

        // get device id
        GetDeviceId gdi = new GetDeviceId();
        deviceId = gdi.getDeviceId(this);


        initializeLocationManager();
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES,
                    mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.e(TAG, "onDestroy");

        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }

    private void initializeLocationManager() {

        Log.e(TAG, "initializeLocationManager");

        if (mLocationManager == null) {
            // Acquire a reference to the system Location Manager
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }


    private Runnable postData = new Runnable () {
        public void run() {


            System.out.println("latitude:" + String.valueOf(latitude));
            System.out.println("longitude:" + String.valueOf(longitude));
            System.out.println("deviceId:" + deviceId);

            // params post use (not json format)
            List<NameValuePair> params = new ArrayList<>();

            params.add(new BasicNameValuePair("latitude", String.valueOf(latitude)));
            params.add(new BasicNameValuePair("longitude", String.valueOf(longitude)));
            params.add(new BasicNameValuePair("device_id", deviceId));


            htmlFormPost(params);

        }
    };

    // process html form post (not image or file post)
    private String htmlFormPost(List<NameValuePair> params) {

        String httpLink = "http://52.74.67.51/gps_tracking/gps_updata.php";

        String resultString = null;

        // Instantiate the custom HttpClient
        HttpClient httpClient = getNewHttpClient();

        // Create a new HttpClient and Post Header
        // HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(httpLink);
        HttpResponse response;

        try {

            // Bound to request Entry
            httpPost.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));

            System.out.println("httpPost :" + httpPost);

            // Execute HTTP Post Request
            response = httpClient.execute(httpPost);

            System.out.println("response code :" + response.getStatusLine().getStatusCode());

            if (response.getStatusLine().getStatusCode() == 200) {
                // get response string, JSON format data
                resultString = EntityUtils.toString(response.getEntity());

            } else {
                resultString = null;
                System.out.println("response not ok, status :" + response.getStatusLine().getStatusCode());

            }


        } catch (Exception e) {

            System.out.println("HttpPost Exception : " + e.toString());

        } finally {
            httpClient.getConnectionManager().shutdown();
        }

        System.out.println("resultString : " + resultString);

        return resultString;
    }


    // ssl process
    private HttpClient getNewHttpClient() {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            SSLSocketFactory sf = new SSLSocketFactoryEx(trustStore);
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
    }
}