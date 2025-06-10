package fdroid_up.rocket.reactnative.notification;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;

import java.util.List;
import java.util.ArrayList;

import org.unifiedpush.android.connector.UnifiedPush;
// import static org.unifiedpush.android.connector.ConstantsKt;
import static org.unifiedpush.android.connector.ConstantsKt.INSTANCE_DEFAULT;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import org.unifiedpush.android.connector.keys.DefaultKeyManager;
import fdroid_up.rocket.reactnative.notification.CustomPushNotification;

public class UnifiedPushModule extends ReactContextBaseJavaModule {

    private static final String MODULE_NAME = "UnifiedPush";
    private static final String PREFS_NAME = "UnifiedPushPrefs";
    private static final String LAST_MESSAGE_KEY = "last_message";
    private static final String RC_SERVER_KEY = "rc_server";
    private final ReactApplicationContext reactContext;
    
    @Override
    public void initialize() {
        super.initialize();
        CustomPushNotification.setReactApplicationContext(getReactApplicationContext());
    }

    @ReactMethod
    public void markJSReady() {
        CustomPushNotification.notifyJSReady();
        this.initialize();
    }

    @ReactMethod
    public void sendNotification(String ntfy_url) {
        WritableMap payload = Arguments.createMap();
        String jsonBody = String.format("{\"UP_REGISTER\":\"UP_REGISTER\", \"ntfy_url\":\"%s\"}", 
        ntfy_url);
        payload.putString("message", jsonBody);
        CustomPushNotification.sendPushToJs(payload);
    }

    public UnifiedPushModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        Context context = reactContext.getApplicationContext();

        List<String> currentDistributors = UnifiedPush.getDistributors(context);
        if (currentDistributors.isEmpty()) {
            Log.d(MODULE_NAME, "No distributors available");
            // onRegistrationFailed()
            return;
        }
        for (String distributor : currentDistributors) {
            Log.d("UnifiedPush", "Distributor: " + distributor);
        }
    }

    @NonNull
    @Override
    public String getName() {
        return MODULE_NAME;
    }

    // Get cached notification message from SharedPreferences
    @ReactMethod
    public void getCachedNotification(Promise promise) {
        try {
            Log.e(MODULE_NAME, "getting cached notification");
            SharedPreferences prefs = reactContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String message = prefs.getString(LAST_MESSAGE_KEY, null);
            promise.resolve(message);
        } catch (Exception e) {
            Log.e(MODULE_NAME, "Failed to get cached notification", e);
            promise.reject("UNIFIEDPUSH_ERROR", "Failed to get cached notification", e);
        }
    }

    // Save a message to SharedPreferences
    public static void cacheMessage(Context context, String message) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(LAST_MESSAGE_KEY, message).apply();
        Log.d(MODULE_NAME, "Cached message: " + message);
    }

    @ReactMethod
    public void sendRegistration(String rc_server, String userId, String userToken,  Promise promise) {
    String push_server_url = "";
        try {
            Log.e(MODULE_NAME, "getting cached UP-gateway endpoint url");
            SharedPreferences prefs = reactContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            push_server_url = prefs.getString("push_server_url", null);  
            prefs.edit().putString(RC_SERVER_KEY, rc_server).apply();
        } catch (Exception e) {
            Log.e(MODULE_NAME, "Failed to get push_server_url", e);
        }

        Context context = reactContext.getApplicationContext();
        Log.d(MODULE_NAME, "CsendRegisteration: ... userId:"+ userId+  "... rc_server:" + rc_server );
        // FIXME add UP GATEWAY PATH
        String jsonBody = String.format("{\"userId\":\"%s\", \"userToken\":\"%s\", \"rc_server\":\"%s\", \"UP_push_server\":\"%s\"}", 
            userId, userToken, rc_server, push_server_url);
        Log.d(MODULE_NAME, "jsonBody:  " + jsonBody );
        
        UnifiedPushServerRegister.sendPost(context, rc_server+ "/up-proxy/register", jsonBody, new UnifiedPushServerRegister.Callback() {
            @Override
            public void onResponse(String result) {
                Log.e(MODULE_NAME, "UP REGISTER onResponse "+ result);
                promise.resolve(result);  // Send response back to JavaScript
            }

            @Override
            public void onError(Exception error) {
                promise.reject("POST_ERROR", error);  // Send error back to JavaScript
            }
        });
    }

    @ReactMethod
    public void removeRegistration(String rc_server, String userId, String userToken,  Promise promise) {
    String push_server_url = "";
        try {
            Log.e(MODULE_NAME, "getting cached UP-gateway endpoint url");
            SharedPreferences prefs = reactContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            push_server_url = prefs.getString("push_server_url", null);  
            prefs.edit().putString(RC_SERVER_KEY, rc_server).apply();
        } catch (Exception e) {
            Log.e(MODULE_NAME, "Failed to get push_server_url", e);
        }

        Context context = reactContext.getApplicationContext();
        Log.d(MODULE_NAME, "CsendRegisteration: ... userId:"+ userId+  "... rc_server:" + rc_server );
        // FIXME add UP GATEWAY PATH
        String jsonBody = String.format("{\"userId\":\"%s\", \"userToken\":\"%s\", \"rc_server\":\"%s\", \"UP_push_server\":\"%s\"}", 
            userId, userToken, rc_server, push_server_url);
        Log.d(MODULE_NAME, "jsonBody:  " + jsonBody );
        
        UnifiedPushServerRegister.sendPost(context, rc_server+ "/up-proxy/removeRegistration", jsonBody, new UnifiedPushServerRegister.Callback() {
            @Override
            public void onResponse(String result) {
                Log.e(MODULE_NAME, "UP REGISTER onResponse "+ result);
                promise.resolve(result);  // Send response back to JavaScript
            }

            @Override
            public void onError(Exception error) {
                promise.reject("POST_ERROR", error);  // Send error back to JavaScript
            }
        });
    }

    // Allow JS to trigger registration manually
    @ReactMethod
    public void registerApp(Promise promise) {
        try {
            Context context = reactContext.getApplicationContext();
            // Attempt to use default distributor and register
            List<String> currentDistributors = UnifiedPush.getDistributors(context);
            UnifiedPush.tryUseCurrentOrDefaultDistributor(context, success -> {
                Log.d(MODULE_NAME, "Using distributor...");
                if (success) {
                    Log.d(MODULE_NAME, "Using distributor successfully");
                    UnifiedPush.register(context, INSTANCE_DEFAULT, "rocket.chat",  null);
                } else if (!currentDistributors.isEmpty()) {
                    Log.w(MODULE_NAME, "Trying to use distributor "+ currentDistributors.get(0));
                    UnifiedPush.saveDistributor(context, currentDistributors.get(0));
                    UnifiedPush.register(
                        context,
                        INSTANCE_DEFAULT,
                        "fdroid_up.rocket.reactnative",
                        null
                    );
                    // "TODO: KEY vom Server holen"
                    
                    Log.w(MODULE_NAME, "Failed to use distributor");
                }
                return null;
            });

            promise.resolve("registered");
        } catch (Exception e) {
            Log.e(MODULE_NAME, "Registration failed", e);
            promise.reject("REGISTER_FAIL", "UnifiedPush registration failed", e);
        }
    }

    @ReactMethod
public void registerAppWithId(String appId, Promise promise) {
    try {
        Context context = reactContext.getApplicationContext();

        List<String> currentDistributors = UnifiedPush.getDistributors(context);
        UnifiedPush.tryUseCurrentOrDefaultDistributor(context, success -> {
            Log.d(MODULE_NAME, "Using distributor...");
            if (success) {
                Log.d(MODULE_NAME, "Using distributor successfully");
                UnifiedPush.register(context, INSTANCE_DEFAULT, appId, null);
            } else if (!currentDistributors.isEmpty()) {
                Log.w(MODULE_NAME, "Trying to use distributor "+ currentDistributors.get(0));
                UnifiedPush.saveDistributor(context, currentDistributors.get(0));
                UnifiedPush.register(context, INSTANCE_DEFAULT, appId, null);
                Log.w(MODULE_NAME, "Failed to use distributor");
            }
            return null;
        });

        promise.resolve("registered with id: " + appId);
    } catch (Exception e) {
        Log.e(MODULE_NAME, "Registration with ID failed", e);
        promise.reject("REGISTER_WITH_ID_FAIL", "UnifiedPush registration with ID failed", e);
    }
}
}
