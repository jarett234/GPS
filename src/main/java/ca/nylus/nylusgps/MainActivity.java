package ca.nylus.nylusgps;
import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.io.FileWriter;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";
    private String _url = "";
    private String _loggingurl = "";
    private String _deviceId = "";
    private IOTMessageManager _messageManager;
    private LoggingManager _loggingManager;

    @BindView(R.id.btn_start_tracking)
    Button btnStartTracking;

    @BindView(R.id.btn_stop_tracking)
    Button btnStopTracking;

    @BindView(R.id.txt_status)
    TextView txtStatus;

    @BindView(R.id.txt_start_tracking_btn_label)
    TextView txtStartTrackingLabel;

    @BindView(R.id.txt_stop_tracking_btn_label)
    TextView txtStopTrackingLabel;

    @BindView(R.id.main_container)
    LinearLayout mainContainer;

    public BackgroundService gpsService;
    public boolean mTracking = false;

    @BindView(R.id.urlTextInput)
    TextView urlInput;

    @BindView(R.id.deviceIdInput)
    TextView deviceInput;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        sharedPreferences = getPreferences(MODE_PRIVATE);


        _url = sharedPreferences.getString("url", "");
        _deviceId = sharedPreferences.getString("deviceId", "");
        _loggingurl = sharedPreferences.getString("loggingurl", "");

        try{
            if(_messageManager != null){
                _messageManager.Close();
            }
            _messageManager = new IOTMessageManager(_loggingurl, getApplicationContext());
            _loggingManager = new LoggingManager(_messageManager, _deviceId);
        }catch(Exception e){
            //Do nothing do not want logger service to fail app.
        }

        urlInput.setText(_url);
        deviceInput.setText(_deviceId);

        if(isMyServiceRunning(BackgroundService.class)){
            try{
                Intent myService = new Intent(MainActivity.this, BackgroundService.class);
                stopService(myService);
                unbindService(serviceConnection);
            }catch (Exception e){
                _loggingManager.SendLog("stopping service issue occurred:  " + _deviceId + " Message: " + e.getMessage());
            }
            _loggingManager.SendLog("Stopping service for:  " + _deviceId);
        }

        if(!_url.equals("") && !_deviceId.equals("")){
            final Intent intent = new Intent(this.getApplication(), BackgroundService.class);
            intent.putExtra("url", _url);
            intent.putExtra("deviceId", _deviceId);
            intent.putExtra("loggingurl", _loggingurl);
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                this.getApplication().startForegroundService(intent);
                _loggingManager.SendLog("Starting foreground service for:  " + _deviceId);
            }else{
                this.getApplication().startService(intent);
                _loggingManager.SendLog("Starting service for:  " + _deviceId);
            }

            this.getApplication().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

            _loggingManager.SendLog("Binding service for:  " + _deviceId);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        _loggingManager.SendLog("onDestroy app shutting down for:  " + _deviceId);
    }

    @Override
    protected void onActivityResult(int requessCode, int resultCode, Intent data) {
        try{
            if(requessCode == 999 && resultCode == RESULT_OK){
                String url = data.getStringExtra("url");
                String deviceId = data.getStringExtra("deviceId");
                _loggingurl = data.getStringExtra("loggingurl");
                urlInput.setText(url);
                deviceInput.setText(deviceId);
                saveSettingsButtonClick();
            }
        }catch (Exception e){
            _loggingManager.SendLog("onActivityResult Error Occurred for:  " + _deviceId);
            _loggingManager.SendLog("onActivityResult Error Message for:  " + _deviceId + " Message: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_start_tracking)
    public void startLocationButtonClick() {
        try{
            _loggingManager.SendLog("startLocationButtonClick started tracking for:  " + _deviceId);

            if(!isMyServiceRunning(BackgroundService.class)){
                String url = sharedPreferences.getString("url", "");
                String deviceId = sharedPreferences.getString("deviceId", "");
                if(!url.equals("") && !deviceId.equals("")){
                    final Intent intent = new Intent(this.getApplication(), BackgroundService.class);
                    intent.putExtra("url", url);
                    intent.putExtra("deviceId", deviceId);
                    intent.putExtra("loggingurl", _loggingurl);

                    if (android.os.Build.VERSION.SDK_INT >= 26) {
                        this.getApplication().startForegroundService(intent);
                    }else{
                        this.getApplication().startService(intent);
                    }

                    this.getApplication().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
                }
            }else{
                try{
                    Intent myService = new Intent(MainActivity.this, BackgroundService.class);
                    stopService(myService);
                    unbindService(serviceConnection);
                }catch (Exception e){
                    _loggingManager.SendLog("stopping service issue occurred:  " + _deviceId + " Message: " + e.getMessage());
                }

                String url = sharedPreferences.getString("url", "");
                String deviceId = sharedPreferences.getString("deviceId", "");

                final Intent intent = new Intent(this.getApplication(), BackgroundService.class);
                intent.putExtra("url", url);
                intent.putExtra("deviceId", deviceId);
                intent.putExtra("loggingurl", _loggingurl);

                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    this.getApplication().startForegroundService(intent);
                }else{
                    this.getApplication().startService(intent);
                }

                this.getApplication().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            }

            Dexter.withActivity(this)
                    .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    .withListener(new PermissionListener() {
                        @Override
                        public void onPermissionGranted(PermissionGrantedResponse response) {

                            _loggingManager.SendLog("startLocationButtonClick permission granted (LOCATION) for:  " + _deviceId);

                            gpsService.startTracking();
                            mTracking = true;
                            toggleButtons();
                        }

                        @Override
                        public void onPermissionDenied(PermissionDeniedResponse response) {
                            _loggingManager.SendLog("startLocationButtonClick permission denied (LOCATION) for:  " + _deviceId);

                            if (response.isPermanentlyDenied()) {
                                openSettings();
                            }
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                            token.continuePermissionRequest();
                        }
                    }).check();

        }catch(Exception e){
            _loggingManager.SendLog("startLocationButtonClick Error Occurred for:  " + _deviceId);
            _loggingManager.SendLog("startLocationButtonClick Error Message for:  " + _deviceId + " Message: " + e.getMessage());
        }
    }

    @OnClick(R.id.btn_stop_tracking)
    public void stopLocationButtonClick() {
        try{
            _loggingManager.SendLog("stopLocationButtonClick stopped tracking for:  " + _deviceId);

            mTracking = false;
            gpsService.stopTracking();
            toggleButtons();
            if(isMyServiceRunning(BackgroundService.class)){
                Intent myService = new Intent(MainActivity.this, BackgroundService.class);
                stopService(myService);
                unbindService(serviceConnection);
            }
        }catch(Exception e){
            _loggingManager.SendLog("stopLocationButtonClick Error Occurred for:  " + _deviceId);
            _loggingManager.SendLog("stopLocationButtonClick Error Message for:  " + _deviceId + " Message: " + e.getMessage());
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void setUrlInput(String str){
        urlInput.setText(str);
    }

    @OnClick(R.id.btn_qr_code)
    public void scanQrCodeButtonClick() {
        try{
            Dexter.withActivity(this)
                    .withPermission(Manifest.permission.CAMERA)
                    .withListener(new PermissionListener() {
                        @Override
                        public void onPermissionGranted(PermissionGrantedResponse response) {
                            Intent intent = new Intent(MainActivity.this, ScanActivity.class);
                            intent.putExtra("url", _url);
                            intent.putExtra("deviceId", _deviceId);
                            intent.putExtra("loggingurl", _loggingurl);
                            startActivityForResult(intent, 999);

                            _loggingManager.SendLog("scanQrCodeButtonClick permission granted (CAMERA) for:  " + _deviceId);
                        }

                        @Override
                        public void onPermissionDenied(PermissionDeniedResponse response) {
                            if (response.isPermanentlyDenied()) {
                                openSettings();
                            }
                            Log.i(TAG, "Permission Denied");

                            _loggingManager.SendLog("scanQrCodeButtonClick permission denied (CAMERA) for:  " + _deviceId);
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                            token.continuePermissionRequest();
                            Log.i(TAG, "onPermissionRationaleShouldBeShown");
                        }
                    }).check();
        }catch(Exception e){
            _loggingManager.SendLog("scanQrCodeButtonClick Error Occurred for:  " + _deviceId);
            _loggingManager.SendLog("scanQrCodeButtonClick Error Message for:  " + _deviceId + " Message: " + e.getMessage());
        }
    }

    //@OnClick(R.id.btn_save_settings)
    public void saveSettingsButtonClick() {
        try{

            try{
                if(_messageManager != null){
                    _messageManager.Close();
                }
                _messageManager = new IOTMessageManager(_loggingurl, getApplicationContext());
                _loggingManager = new LoggingManager(_messageManager, _deviceId);
            }catch(Exception e){
                //Do nothing do not want logger service to fail app.
            }

            _loggingManager.SendLog("saveSettingsButtonClick saving settings for:  " + _deviceId);
            Snackbar.make(mainContainer, "Settings saved.", Snackbar.LENGTH_LONG).show();

            try{
                _deviceId = deviceInput.getText().toString();
                _url = urlInput.getText().toString();
            }catch(Exception e){
                //Do nothing do not want logger service to fail app.
            }

            _loggingManager.SendLog("saveSettingsButtonClick saving settings for:  " + _deviceId + ". Changed deviceId from: " + _deviceId + " to: " + deviceInput.getText().toString());
            _loggingManager.SendLog("saveSettingsButtonClick saving settings for:  " + _deviceId + ". Changed url from: " + _url + " to: " + urlInput.getText().toString());

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("url", urlInput.getText().toString());
            editor.putString("deviceId", deviceInput.getText().toString());
            editor.putString("loggingurl", _loggingurl);

            if(!urlInput.getText().toString().equals("") && !deviceInput.getText().toString().equals("") && !_loggingurl.equals("")){
                final Intent intent = new Intent(this.getApplication(), BackgroundService.class);
                intent.putExtra("url", urlInput.getText().toString());
                intent.putExtra("deviceId", deviceInput.getText().toString());
                intent.putExtra("loggingurl", _loggingurl);

                if(isMyServiceRunning(BackgroundService.class)){
                    try{
                        Intent myService = new Intent(MainActivity.this, BackgroundService.class);
                        stopService(myService);
                        unbindService(serviceConnection);
                    }catch (Exception e){
                        _loggingManager.SendLog("stopping service issue occurred:  " + _deviceId + " Message: " + e.getMessage());
                    }
                }

                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    this.getApplication().startForegroundService(intent);
                }else{
                    this.getApplication().startService(intent);
                }

                this.getApplication().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            }else{
                mTracking = false;
                gpsService.stopTracking();
                toggleButtons();
                if(isMyServiceRunning(BackgroundService.class)){
                    try{
                        Intent myService = new Intent(MainActivity.this, BackgroundService.class);
                        stopService(myService);
                        unbindService(serviceConnection);
                    }catch (Exception e){
                        _loggingManager.SendLog("stopping service issue occurred:  " + _deviceId + " Message: " + e.getMessage());
                    }
                }
            }

            editor.commit();
        }catch(Exception e){
            _loggingManager.SendLog("saveSettingsButtonClick Error Occurred for:  " + _deviceId);
            _loggingManager.SendLog("saveSettingsButtonClick Error Message for:  " + _deviceId + " Message: " + e.getMessage());
        }
    }

    private void toggleButtons() {
        btnStartTracking.setEnabled(!mTracking);
        btnStopTracking.setEnabled(mTracking);
        txtStatus.setText( (mTracking) ? "Tracking On" : "GPS Ready" );
        if(mTracking){
            btnStartTracking.setVisibility(View.GONE);
            txtStartTrackingLabel.setVisibility(View.GONE);
            btnStopTracking.setVisibility(View.VISIBLE);
            txtStopTrackingLabel.setVisibility(View.VISIBLE);
        } else {
            btnStopTracking.setVisibility(View.GONE);
            txtStopTrackingLabel.setVisibility(View.GONE);
            btnStartTracking.setVisibility(View.VISIBLE);
            txtStartTrackingLabel.setVisibility(View.VISIBLE);
        }
    }

    private void openSettings() {
        Intent intent = new Intent();
        intent.setAction( Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            String name = className.getClassName();
            if (name.endsWith("BackgroundService")) {
                gpsService = ((BackgroundService.LocationServiceBinder) service).getService();
                btnStartTracking.setEnabled(true);
                txtStatus.setText("GPS Ready");

                try{
                    String url = sharedPreferences.getString("url", "");
                    String deviceId = sharedPreferences.getString("deviceId", "");
                    if(!url.equals("") && !deviceId.equals("")){
                        startLocationButtonClick();
                    }
                }catch (Exception e){
                    _loggingManager.SendLog("onServiceConnected Error Occurred for:  " + _deviceId);
                    _loggingManager.SendLog("onServiceConnected Error Message for:  " + _deviceId + " Message: " + e.getMessage());
                }
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            if (className.getClassName().equals("BackgroundService")) {
                gpsService = null;
            }
        }


    };
}
