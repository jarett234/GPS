package ca.nylus.nylusgps;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Observable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.Message;

import org.json.JSONObject;

import java.io.OutputStream;

public class BackgroundService extends Service {
    private static final String CHANNEL_ID = "2";
    private final LocationServiceBinder binder = new LocationServiceBinder();
    private final String TAG = "BackgroundService";
    private final String MESSAGE_TYPE = "telemetry";
    private LocationManager mLocationManager;
    private NotificationManager notificationManager;
    private String url;
    private String deviceId;
    private String loggingurl;
    private final int LOCATION_INTERVAL = 30000;
    private final int LOCATION_DISTANCE = 0;
    private DeviceClient client;
    IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;
    private static final int METHOD_SUCCESS = 200;
    public static final int METHOD_THROWS = 403;
    private static final int METHOD_NOT_DEFINED = 404;
    private Message sendMessage;
    private int msgSentCount = 0;
    private String msgStr;
    private int msgReceivedCount = 0;
    private int sendMessagesInterval = 5000;
    private IOTMessageManager _messageManager;
    private IOTMessageManager _messageManagerLogging;
    private LoggingManager _loggingManager;
    private PowerManager.WakeLock _mWakeLock;

    public BackgroundService() {
        //Nothing needs to be done here
    }

    @Override
    public IBinder onBind(Intent intent) {
        url = intent.getStringExtra("url");
        deviceId = intent.getStringExtra("deviceId");
        loggingurl = intent.getStringExtra("loggingurl");
        if(_messageManager != null){
            _messageManager.Close();
        }
        _messageManager = new IOTMessageManager(url, getApplicationContext());
        //_messageManagerLogging = new IOTMessageManager(loggingurl, getApplicationContext());
        _loggingManager = new LoggingManager(_messageManager, deviceId);
        return binder;
    }

    private final LocationListener gpsLocationListener = new LocationListener()
    {
        private final String TAG = "LocationListener";

        @Override
        public void onLocationChanged(Location location)
        {
            try {
                RequestTask requestTask = new RequestTask();
                Log.i(TAG, "LocationChanged: "+location);
                String[] params = new String[20];
                params[0] = url;
                params[1] = String.valueOf(location.getLatitude());
                params[2] = String.valueOf(location.getLongitude());
                params[3] = String.valueOf(location.getSpeed());
                params[4] = deviceId;
                requestTask.execute(params);
            } catch (Exception ex) {
                _loggingManager.SendLog("onLocationChanged for background service error occurred for:  " + deviceId + ".  Error message: " + ex.getMessage());
            }
        }

        @Override
        public void onProviderDisabled(String provider)
        {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider)
        {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            Log.e(TAG, "onStatusChanged: " + status);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        url = intent.getStringExtra("url");
        deviceId = intent.getStringExtra("deviceId");
        loggingurl = intent.getStringExtra("loggingurl");
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        _mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyApp::MyWakelockTag");
        if ((_mWakeLock != null) &&           // we have a WakeLock
                (_mWakeLock.isHeld() == false)) {  // but we don't hold it
            _mWakeLock.acquire();
        }

        try{
            if(_messageManager != null){
                _messageManager.Close();
            }
            _messageManager = new IOTMessageManager(url, getApplicationContext());
            //_messageManagerLogging = new IOTMessageManager(loggingurl, getApplicationContext());
            _loggingManager = new LoggingManager(_messageManager, deviceId);
        }catch (Exception e){
            _loggingManager.SendLog("Exception in onStartCommand: " + e.getMessage());
        }
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate()
    {
        Log.i(TAG, "onCreate");
        startForeground(12345678, getNotification());
    }

    @Override
    public void onDestroy()
    {
        if (_mWakeLock.isHeld()) _mWakeLock.release();
        super.onDestroy();
        if (mLocationManager != null) {
            try {
                mLocationManager.removeUpdates(gpsLocationListener);
            } catch (Exception ex) {
                _loggingManager.SendLog("Exception in onDestroy. Fail to remove location listners, ignore" + ex.getMessage());
            }
        }
    }

    private void initializeLocationManager() {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    public void startTracking() {
        if(android.os.Build.VERSION.SDK_INT >= 26){
            Notification.Builder builder = new Notification.Builder(getApplicationContext(), "channel_01").setAutoCancel(true);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            builder.setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("Active")
                    .setContentText("Tracking")
                    .setStyle(new Notification.BigTextStyle()
                            .bigText("Tracking"));
            notificationManager.notify(12345678, builder.build());
        }
        else{
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("Active")
                    .setContentText("Tracking")
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("Tracking"))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            notificationManager.notify(12345678, builder.build());
        }

        initializeLocationManager();
        //mLocationListener = new LocationListener(LocationManager.GPS_PROVIDER);

        try {
            mLocationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, gpsLocationListener );

        } catch (java.lang.SecurityException ex) {
            _loggingManager.SendLog("Exception in background service: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            _loggingManager.SendLog("Exception in background service: " + ex.getMessage());
        }
    }

    public void stopTracking() {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            Notification.Builder builder = new Notification.Builder(getApplicationContext(), "channel_01").setAutoCancel(true);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            builder.setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("Active")
                    .setContentText("Not Tracking")
                    .setStyle(new Notification.BigTextStyle()
                            .bigText("Not Tracking"));
            notificationManager.notify(12345678, builder.build());
        }
        else{
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("Active")
                    .setContentText("Not Tracking")
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("Not Tracking"))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            notificationManager.notify(12345678, builder.build());
        }
        this.onDestroy();
    }

    private Notification getNotification() {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel("channel_01", "Nylus Channel", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            Notification.Builder builder = new Notification.Builder(getApplicationContext(), "channel_01").setAutoCancel(true);
            builder.setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("Active")
                    .setContentText("Not Tracking")
                    .setStyle(new Notification.BigTextStyle()
                            .bigText("Not Tracking"));
                    //.setPriority(NotificationCompat.PRIORITY_DEFAULT);
            return builder.build();
        }else{
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("Active")
                    .setContentText("Not Tracking")
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("Not Tracking"))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            return builder.build();
        }
    }

    public class LocationServiceBinder extends Binder {
        public BackgroundService getService() {
            return BackgroundService.this;
        }
    }

    public class RequestTask extends AsyncTask<String, String, String> {

        public RequestTask(){
            //set context variables if required
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            String urlString = params[0];
            String latitude = params[1];
            String longitude = params[2];
            String speed = params[3];
            String deviceId = params[4];
            OutputStream out = null;

            try
            {
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("speed", speed);
                jsonParam.put("latitude", latitude);
                jsonParam.put("longitude", longitude);
                jsonParam.put("messagetype", MESSAGE_TYPE);
                msgStr = jsonParam.toString();
                _messageManager.SendMessage(msgStr);
            }
            catch (Exception e)
            {
                System.err.println("Exception while sending event: " + e);
                _loggingManager.SendLog("Exception while sending event: " + e);
            }
            return "Text";
        }
    }
}

