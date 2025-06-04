package fdroid_up.rocket.reactnative.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ReplyBroadcast extends BroadcastReceiver {
    private Context mContext;
    private Bundle bundle;
    private NotificationManager notificationManager;

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            final CharSequence message = getReplyMessage(intent);
            if (message == null) {
                return;
            }

            mContext = context;
            bundle = intent.getExtras(); // Directly using intent extras

            if (bundle == null) {
                return;
            }

            String notId = bundle.getString("notId");
            if (notId == null) {
                Log.w("ReplyBroadcast", "Notification ID missing in intent extras.");
                return;
            }

            String ejsonStr = bundle.getString("ejson", "{}");
            Ejson ejson = new Gson().fromJson(ejsonStr, Ejson.class);

            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            replyToMessage(ejson, Integer.parseInt(notId), message);
        }
    }

    protected void replyToMessage(final Ejson ejson, final int notId, final CharSequence message) {
        String serverURL = ejson.serverURL();
        String rid = ejson.rid;

        if (serverURL == null || rid == null) {
            return;
        }

        final OkHttpClient client = new OkHttpClient();
        final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        String json = buildMessage(rid, message.toString(), ejson);

        CustomPushNotification.clearMessages(notId);

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .header("x-auth-token", ejson.token())
                .header("x-user-id", ejson.userId())
                .url(serverURL + "/api/v1/chat.sendMessage")
                .post(body)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("RCNotification", "Reply FAILED exception: " + e.getMessage());
                onReplyFailed(notificationManager, notId);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d("RCNotification", "Reply SUCCESS");
                    onReplySuccess(notificationManager, notId);
                } else {
                    Log.i("RCNotification", "Reply FAILED: status " + response.code());
                    onReplyFailed(notificationManager, notId);
                }
            }
        });
    }

    private String getMessageId() {
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 17; i++) {
            builder.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return builder.toString();
    }

    protected String buildMessage(String rid, String message, Ejson ejson) {
        Gson gson = new GsonBuilder().create();
        String id = getMessageId();
        String encrypted = Encryption.shared.encryptMessage(message, id, ejson);

        Map<String, Object> msgMap = new HashMap<>();
        msgMap.put("_id", id);
        msgMap.put("rid", rid);
        msgMap.put("msg", encrypted);
        if (!encrypted.equals(message)) {
            msgMap.put("t", "e2e");
        }
        if (ejson.tmid != null) {
            msgMap.put("tmid", ejson.tmid);
        }

        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("message", msgMap);

        return gson.toJson(wrapper);
    }

    protected void onReplyFailed(NotificationManager notificationManager, int notId) {
        String CHANNEL_ID = "CHANNEL_ID_REPLY_FAILED";
        final Resources res = mContext.getResources();
        int smallIconResId = res.getIdentifier("ic_notification", "drawable", mContext.getPackageName());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Reply Failure", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(mContext, CHANNEL_ID)
                : new Notification.Builder(mContext);

        Notification notification = builder
                .setContentTitle("Failed to reply to message.")
                .setSmallIcon(smallIconResId)
                .build();

        notificationManager.notify(notId, notification);
    }

    protected void onReplySuccess(NotificationManager notificationManager, int notId) {
        notificationManager.cancel(notId);
    }

    private CharSequence getReplyMessage(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        return remoteInput != null ? remoteInput.getCharSequence(CustomPushNotification.KEY_REPLY) : null;
    }
}
