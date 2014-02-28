package se.powerapp.mymapapp;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.ActivityRecognitionClient;

public class MyReceiver extends BroadcastReceiver {
    public static final String ACTION_GEOFENCE_TOAST = "se.powerapp.mymapapp.GEOFENCE_TOAST";

    public MyReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null) {
            String action = intent.getAction();
            if(ACTION_GEOFENCE_TOAST.equals(action)) {
                Toast.makeText(context, "Your geofence was triggered!", Toast.LENGTH_SHORT).show();
                createNotification(context);
            }
        }
    }

    public void createNotification(Context context) {


        PendingIntent notifyIntent =
                PendingIntent.getActivity(
                        context, 0,
                        new Intent(context,MainActivity.class), Intent.FLAG_ACTIVITY_NEW_TASK);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentTitle("Geolocation:")
                .setContentText("You entered a geofence!")
                .setSmallIcon(R.drawable.ic_launcher);


        builder.setContentIntent(notifyIntent);
        builder.setAutoCancel(true);
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(1, builder.build());



    }

}