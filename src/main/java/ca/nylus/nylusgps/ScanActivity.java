package ca.nylus.nylusgps;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;

import java.util.Arrays;
import java.util.List;

import me.dm7.barcodescanner.zbar.ZBarScannerView;

public class ScanActivity extends AppCompatActivity implements ZBarScannerView.ResultHandler {
    private ZBarScannerView mScannerView;

    //camera permission is needed.

    private String _url = "";
    private String _deviceId = "";
    private String _loggingurl = "";

    @Override
    public void onCreate(Bundle state) {
        _url = getIntent().getStringExtra("url");
        _deviceId = getIntent().getStringExtra("deviceId");
        _loggingurl = getIntent().getStringExtra("loggingurl");
        super.onCreate(state);
        mScannerView = new ZBarScannerView(this);    // Programmatically initialize the scanner view
        setContentView(mScannerView);                // Set the scanner view as the content view
    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
        mScannerView.startCamera();          // Start camera on resume
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();           // Stop camera on pause
    }

    @Override
    public void onBackPressed() {
        Intent i = new Intent();
        i.putExtra("url", _url);
        i.putExtra("deviceId", _deviceId);
        i.putExtra("loggingurl", _loggingurl);
        setResult(RESULT_OK, i);
        finish();
        mScannerView.stopCamera();           // Stop camera on pause
    }

    @Override
    public void handleResult(me.dm7.barcodescanner.zbar.Result result) {
        // Do something with the result here
        Log.v("Scan Results", result.getContents()); // Prints scan results
        Log.v("Scan Format", result.getBarcodeFormat().getName()); // Prints the scan format (qrcode, pdf417 etc.)

        List<String> items = Arrays.asList(result.getContents().split(","));
        for(String item : items){
            List<String> subList = Arrays.asList(item.split(":="));
            if(subList.size() == 2){
                if(subList.get(0).equals("url")){
                    _url = subList.get(1);
                }
                if(subList.get(0).equals("deviceid")){
                    _deviceId = subList.get(1);
                }
                if(subList.get(0).equals("loggingurl")){
                    _loggingurl = subList.get(1);
                }
            }
        }

        onBackPressed();

        // If you would like to resume scanning, call this method below:
        //mScannerView.resumeCameraPreview(this);
    }
}
