package ca.nylus.nylusgps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

public class StartUpServiceReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            //SharedPreferences prefs = context.getSharedPreferences("myPrefs",
            //        Context.MODE_PRIVATE);
            //String url = prefs.getString("url", "");
            //String deviceId = prefs.getString("deviceId", "");
//
            //if(!url.equals("") && !deviceId.equals("")){
            //    if (Build.VERSION.SDK_INT >= 26) {
            //        Intent serviceIntent = new Intent(context, BackgroundService.class);
            //        context.startForegroundService(serviceIntent);
            //    }else{
            //        Intent serviceIntent = new Intent(context, BackgroundService.class);
            //        context.startService(serviceIntent);
            //    }
            //}

            Intent activityIntent = new Intent(context, MainActivity.class);
            activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(activityIntent);
        }
    }
}
