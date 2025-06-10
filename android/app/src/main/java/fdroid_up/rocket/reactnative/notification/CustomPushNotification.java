package fdroid_up.rocket.reactnative.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Person;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.net.Uri;
import androidx.annotation.Nullable;

import org.json.JSONObject;
import java.util.Set;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.facebook.react.bridge.ReactApplicationContext;
import com.google.gson.Gson;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.lang.ClassCastException;
import fdroid_up.rocket.reactnative.notification.ReplyBroadcast;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import fdroid_up.rocket.reactnative.R;

public class CustomPushNotification {
    private static final String MODULE_NAME = "fdroid_up.rocket.reactnative.notification.CustomPushNotification";
    public static ReactApplicationContext reactApplicationContext;
    private final Context mContext;
    private final NotificationManager notificationManager;
    private Bundle mNotificationData;
    private static WritableMap cachedNotification = null;

    public static String KEY_REPLY = "KEY_REPLY";
    public static String NOTIFICATION_ID = "NOTIFICATION_ID";

    private static final Map<String, List<Bundle>> notificationMessages = new HashMap<>();
    private static boolean isInitialized = false;

    public CustomPushNotification(Context context, Bundle notificationData) {
        Log.w(MODULE_NAME, "CustomPushNotification instantiate");
        this.mContext = context;
        this.mNotificationData = notificationData;
        this.notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        this.handleNotification();
    }

    public static String bundleToString(Bundle bundle) {
        if (bundle == null) return "{}";
        try {
            JSONObject json = new JSONObject();
            Set<String> keys = bundle.keySet();
            for (String key : keys) {
                Object value = bundle.get(key);
                json.put(key, value != null ? value.toString() : JSONObject.NULL);
            }
            return json.toString();
        } catch (Exception e) {
            return "{}"; // fallback in case of error
        }
    }

    private void notificationLoad(Ejson ejson, Callback callback) {
        LoadNotification loadNotification = new LoadNotification();
        loadNotification.load(reactApplicationContext, ejson, callback);
    }


    public static void notifyJSReady() {
        isInitialized = true;
        checkAndSendCachedNotification();
    }

    public static void setReactApplicationContext(ReactApplicationContext context) {
        reactApplicationContext = context;
        checkAndSendCachedNotification();
    }
    public static void sendPushToJs(WritableMap pushPayload) {
        Log.w(MODULE_NAME, "sendPushToJs ("+pushPayload+")--> :"+reactApplicationContext.hasActiveCatalystInstance()+"   "+ isInitialized+ isJSReady());

        // if (reactApplicationContext.hasActiveCatalystInstance() && isInitialized) {
        if (isJSReady() && isInitialized) {
            sendEvent("onNotification", pushPayload);
        } else {
            cachedNotification = pushPayload;
        }
    }
    private static boolean isJSReady() {
        return reactApplicationContext != null &&
               reactApplicationContext.hasActiveCatalystInstance() &&
               isInitialized;
    }
    private static void sendEvent(String eventName, WritableMap params) {
        Log.w(MODULE_NAME, "sendEvent ("+eventName+")--> :"+params+" --- "+ reactApplicationContext +  reactApplicationContext.hasActiveCatalystInstance());
        // if (reactApplicationContext != null && reactApplicationContext.hasActiveCatalystInstance()) {
        if (isInitialized) { //isJSReady()
            reactApplicationContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
        }
    }
    private static void checkAndSendCachedNotification() {
        if (cachedNotification != null &&
            reactApplicationContext != null &&
            reactApplicationContext.hasActiveCatalystInstance() &&
            isInitialized) {
            sendEvent("onNotification", cachedNotification);
            cachedNotification = null;
        }
    }




    public void handleNotification() {
        Bundle bundle = this.mNotificationData;
        // Hat der Benachrichtigungsserver zusätzliche Authentifizierung angefordert, z.B. für das Weiterleiten von Nachrichten vom Server?
        if ("UP_REGISTER".equals(bundle.getString("UP_REGISTER"))) {
            String registration_url = bundle.getString("ntfy_url","");
            if (registration_url.length() < 9){
                return;
            }
            WritableMap payload = Arguments.createMap();
            String jsonBody = String.format("{\"UP_REGISTER\":\"UP_REGISTER\", \"ntfy_url\":\"%s\", \"ntfy_auth_style\": \"%s\"}", 
            bundle.getString("ntfy_url"), bundle.getString("ntfy_auth_style"));
            payload.putString("message",jsonBody);
            sendPushToJs(payload);
        }

        // Ejson loadedEjson = new Gson().fromJson(bundle.getString("ejson", "{}"), Ejson.class);
        Ejson loadedEjson = new Gson().fromJson(bundle.getString("ejson", "{}"), Ejson.class);
        Log.w(MODULE_NAME, "handleNotification loadedEjson sender:"+loadedEjson.sender);
        Log.w(MODULE_NAME, "handleNotification loadedEjson userId:"+loadedEjson.userId());
        Log.w(MODULE_NAME, "handleNotification loadedEjson message-id-only: "+loadedEjson.notificationType);
        
        String notId = bundle.getString("notId", "1");

        // get message if not present
        if (loadedEjson.notificationType != null && loadedEjson.notificationType.equals("message-id-only")) {
            // KEYS:
            //     userId
            //     token
            //     messageId

            //     ---- this will then be used to get the data from the server --> then we need to pull it in :/
            notificationLoad(loadedEjson, new Callback() {
                @Override
                public void call(@Nullable Bundle bundle) {
                    if (bundle != null) {
                        Log.w(MODULE_NAME, "notificationLoad loadedEjson:"+bundle);
                        //FIXME pass this data on 
                    }
                }
            });
        }

        if (notificationMessages.get(notId) == null) {
            notificationMessages.put(notId, new ArrayList<>());
        }

        if (loadedEjson.msg != null) {
            String decrypted = Encryption.shared.decryptMessage(loadedEjson, reactApplicationContext);
            if (decrypted != null) {
                bundle.putString("message", decrypted);
            }
        }

        bundle.putLong("time", new Date().getTime());
        bundle.putString("username", loadedEjson.sender != null ? loadedEjson.sender.username : bundle.getString("title"));
        bundle.putString("senderId", loadedEjson.sender != null ? loadedEjson.sender._id : "1");
        bundle.putString("avatarUri", loadedEjson.getAvatarUri());

        notificationMessages.get(notId).add(bundle);
        Log.w(MODULE_NAME, "handleNotification notId:"+notId);
        postNotification(Integer.parseInt(notId));
        Log.w(MODULE_NAME, bundleToString(this.mNotificationData) );
        // sendEvent("onNotification", bundleToString(this.mNotificationData) );
    }
    
    public static void clearMessages(int notId) {
        notificationMessages.remove(Integer.toString(notId));
    }

    // private void postNotification(int notificationId) {
    //     //FIXME TODO at least please clean this. shoulc do differently dont know how though - plz help make this intent for a new push notification
    //     Intent intent = new Intent(this.mContext, ReplyBroadcast.class); // Define actual target activity if needed
    //     // PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    //             PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);


    //     Log.w(MODULE_NAME, "Notification.Builder postNotification notificationId:"+notificationId);
    //     Notification.Builder builder = getNotificationBuilder(pendingIntent);
    //     notificationManager.notify(notificationId, builder.build());
    // }

    private void postNotification(int notificationId) {
        Log.w(MODULE_NAME, "postNotification: Building notification with ID " + notificationId);

        // Intent to open the app when notification is tapped
        Intent openAppIntent = mContext.getPackageManager()
            .getLaunchIntentForPackage(mContext.getPackageName());
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(
            mContext,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = getNotificationBuilder(contentIntent);
        notificationManager.notify(notificationId, builder.build());
    }

    private PendingIntent getOpenUrlIntent(String url) {

        //////////////////
        /// 
        /// ///////////////
        Log.d(MODULE_NAME, "getOpenUrlIntent customNotification (" + url + ")");
        Intent intent = this.mContext.getPackageManager().getLaunchIntentForPackage(this.mContext.getPackageName());

        if (url != null) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url).normalizeScheme());
        }

        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }

        return PendingIntent.getActivity(this.mContext, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }


    private Notification.Builder getNotificationBuilder(PendingIntent intent) {
        Notification.Builder notification = new Notification.Builder(mContext);
        Bundle bundle = this.mNotificationData;
        Log.w(MODULE_NAME, "Notification.Builder getNotificationBuilder bundle:"+bundle);
        String notId = bundle.getString("notId", "1");
        String title = bundle.getString("title");
        String message = bundle.getString("message");
        String url = bundle.getString("url");
        Log.w(MODULE_NAME, "Notification.Builder getNotificationBuilder message:"+message);
        Log.w(MODULE_NAME, "Notification.Builder getNotificationBuilder title:"+title);

        notification
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(intent)
            .setPriority(Notification.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .setContentIntent(getOpenUrlIntent(url))
            .setAutoCancel(true);

        // .setContentIntent(getOpenUrlIntent(this.mContext, url))


        Ejson ejson = new Gson().fromJson(bundle.getString("ejson", "{}"), Ejson.class);

        Integer notificationId = Integer.parseInt(notId);
        notificationColor(notification);
        notificationChannel(notification);
        notificationIcons(notification, bundle);
        notificationDismiss(notification, notificationId);
        notificationStyle(notification, notificationId, bundle);
        notificationReply(notification, notificationId, bundle);

        return notification;
    }

    private void notificationColor(Notification.Builder notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notification.setColor(mContext.getColor(R.color.notification_text));
        }
    }

    private void notificationChannel(Notification.Builder notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "rocketchatrn_channel_01";
            String CHANNEL_NAME = "All";

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);

            notification.setChannelId(CHANNEL_ID);
        }
    }

    private Bitmap getAvatar(String uri) {
        try {
            return Glide.with(mContext)
                    .asBitmap()
                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(10)))
                    .load(uri)
                    .submit(100, 100)
                    .get();
        } catch (ExecutionException | InterruptedException e) {
            return largeIcon();
        }
    }

    private Bitmap largeIcon() {
        final Resources res = mContext.getResources();
        String packageName = mContext.getPackageName();
        int largeIconResId = res.getIdentifier("ic_notification", "drawable", packageName);
        return BitmapFactory.decodeResource(res, largeIconResId);
    }

    private void notificationIcons(Notification.Builder notification, Bundle bundle) {
        final Resources res = mContext.getResources();
        String packageName = mContext.getPackageName();
        int smallIconResId = res.getIdentifier("ic_notification", "drawable", packageName);
        Ejson ejson = new Gson().fromJson(bundle.getString("ejson", "{}"), Ejson.class);

        notification.setSmallIcon(smallIconResId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            notification.setLargeIcon(getAvatar(ejson.getAvatarUri()));
        }
    }

    private void notificationStyle(Notification.Builder notification, int notId, Bundle bundle) {
        List<Bundle> bundles = notificationMessages.get(Integer.toString(notId));

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Notification.InboxStyle messageStyle = new Notification.InboxStyle();
            if (bundles != null) {
                for (Bundle data : bundles) {
                    messageStyle.addLine(data.getString("message"));
                }
            }
            notification.setStyle(messageStyle);
        } else {
            Notification.MessagingStyle messageStyle;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                messageStyle = new Notification.MessagingStyle("");
            } else {
                messageStyle = new Notification.MessagingStyle(new Person.Builder().setName("").build());
            }

            messageStyle.setConversationTitle(bundle.getString("title"));

            if (bundles != null) {
                for (Bundle data : bundles) {
                    long timestamp = data.getLong("time");
                    String message = data.getString("message");
                    String senderId = data.getString("senderId");
                    String avatarUri = data.getString("avatarUri");
                    Ejson ejson = new Gson().fromJson(data.getString("ejson", "{}"), Ejson.class);

                    String m = extractMessage(message, ejson);

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                        messageStyle.addMessage(m, timestamp, ejson.senderName);
                    } else {
                        Bitmap avatar = getAvatar(avatarUri);
                        Person.Builder sender = new Person.Builder().setKey(senderId).setName(ejson.senderName);
                        if (avatar != null) {
                            sender.setIcon(Icon.createWithBitmap(avatar));
                        }
                        messageStyle.addMessage(m, timestamp, sender.build());
                    }
                }
            }

            notification.setStyle(messageStyle);
        }
    }

    private String extractMessage(String message, Ejson ejson) {
        if (ejson.type != null && !ejson.type.equals("d")) {
            int pos = message.indexOf(":");
            return message.substring(pos == -1 ? 0 : pos + 2);
        }
        return message;
    }

    private void notificationReply(Notification.Builder notification, int notificationId, Bundle bundle) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;

        String label = "Reply";
        final Resources res = mContext.getResources();
        String packageName = mContext.getPackageName();
        int smallIconResId = res.getIdentifier("ic_notification", "drawable", packageName);

        Intent replyIntent = new Intent(mContext, ReplyBroadcast.class);
        replyIntent.setAction(KEY_REPLY);
        replyIntent.putExtra("pushNotification", bundle);

        PendingIntent replyPendingIntent = PendingIntent.getBroadcast(
                mContext,
                notificationId,
                replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );

        RemoteInput remoteInput = new RemoteInput.Builder(KEY_REPLY).setLabel(label).build();

        Notification.Action replyAction = new Notification.Action.Builder(smallIconResId, label, replyPendingIntent)
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .build();

        notification.setShowWhen(true).addAction(replyAction);
    }

    private void notificationDismiss(Notification.Builder notification, int notificationId) {
        Intent intent = new Intent(mContext, DismissNotification.class);
        intent.putExtra(NOTIFICATION_ID, notificationId);

        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(mContext, notificationId, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        notification.setDeleteIntent(dismissPendingIntent);
    }
}