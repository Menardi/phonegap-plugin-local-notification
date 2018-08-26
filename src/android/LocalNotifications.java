
package com.adobe.phonegap.notification;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.AlarmManager;
import android.content.Context;
import android.provider.Settings;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.net.Uri;
import android.os.Build;

/**
* This class exposes methods in Cordova that can be called from JavaScript.
*/
public class LocalNotifications extends CordovaPlugin {

    private static final String TAG = "LocalNotifications";

    public static CallbackContext notificationContext;

     /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback context from which we were invoked.
     */
    @SuppressLint("NewApi")
    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "in local notifications: "+action);
        Context context = cordova.getActivity();

        if (action.equals("show")) {
            for(int i = 0; i < args.length(); i++) {
                showNotification(args.getJSONObject(i));
            }

            PluginResult result = new PluginResult(PluginResult.Status.OK, "show");
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
        } else if (action.equals("close")) {
            String tag = args.getJSONObject(0).getString("tag");
            // NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(context);
            mNotificationManager.cancel(tag, 0);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent notificationBroadcastReceiverIntent = new Intent(context, NotificationBroadcastReceiver.class);
            int requestCode = tag.hashCode();
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, notificationBroadcastReceiverIntent, PendingIntent.FLAG_NO_CREATE);

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
                Log.d(TAG, "alarm cancelled: " + requestCode);
            } else {
                Log.d(TAG, "alarm not found: " + requestCode);
            }

            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
        } else if (action.equals("hasPermission")) {
            NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(context);

            if (mNotificationManager.areNotificationsEnabled()) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, "granted"));
            } else {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, "denied"));
            }
        } else if (action.equals("requestPermission")) {
            // Not implemented on Android - try "settings"
        } else if (action.equals("settings")) {
            Intent intent = new Intent();

            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1){
                intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                intent.putExtra("android.provider.extra.APP_PACKAGE", context.getPackageName());
            } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                intent.putExtra("app_package", context.getPackageName());
                intent.putExtra("app_uid", context.getApplicationInfo().uid);
            } else {
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
            }
            context.startActivity(intent);
        } else if (action.equals("listen")) {
            notificationContext = callbackContext;
        } else {
            Log.d(TAG, "return false");
            return false;
        }
        return true;
    }

    private void showNotification(JSONObject args) throws JSONException {
        // Get args
        long when = args.getLong("when");
        if (when == 0) {
            when = System.currentTimeMillis();
        }

        Log.v(TAG, "schedule notification now=" + System.currentTimeMillis() + " when=" + when + " args=" + args.toString());

        Context context = cordova.getActivity();
        String tag = args.getString("tag");

        int requestCode = tag.hashCode();

        Intent notificationBroadcastReceiverIntent = new Intent(context, NotificationBroadcastReceiver.class);
        notificationBroadcastReceiverIntent.putExtra("args", args.toString());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, notificationBroadcastReceiverIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, when, pendingIntent);
//        AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, when, pendingIntent);
    }
}

