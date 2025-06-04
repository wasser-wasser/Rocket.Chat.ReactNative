package fdroid_up.rocket.reactnative.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.Context.MODE_PRIVATE
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import expo.modules.kotlin.modules.Module
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import com.google.gson.Gson
import com.facebook.react.bridge.ReactApplicationContext
import kotlinx.serialization.json.*
// import kotlinx.serialization.json.Json
// import kotlinx.serialization.json.boolean
// import kotlinx.serialization.json.int
// import kotlinx.serialization.json.jsonObject
// import kotlinx.serialization.json.jsonPrimitive
// import kotlinx.serialization.json.booleanOrNull
// import kotlinx.serialization.json.long
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.PushService
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage


// import fdroid_up.rocket.reactnative.notification.UPUtils.Notifier
import fdroid_up.rocket.reactnative.notification.decodeMessage
import fdroid_up.rocket.reactnative.notification.vapidImplementedForSdk

import fdroid_up.rocket.reactnative.notification.CustomPushNotification
import fdroid_up.rocket.reactnative.notification.UnifiedPushServerRegister

class PushServiceImpl : PushService() {
    private val context = this
    val TAG = "PushServiceImpl"
    private var _module: Module? = null

    fun setModule(m: Module?) {
        _module = m
    }

    private fun sendPushEvent(action: String, data: Bundle) {
        val payload = Bundle()
        payload.putBundle("data", data)
        payload.putString("action", action)
        Log.d(TAG, "sendPushEvent $action $data")

        // This is only needed if using EXPO
        val module = _module
        if (module != null) {
            module.sendEvent("message", payload)
        } else {
            Log.e(TAG, "sendPushEvent called without a reference to the expo module")
        }
    }

    private fun sendErrorEvent(err: Throwable) {
        val data = Bundle()
        data.putString("message", err.message)
        data.putString("stackTrace", err.stackTraceToString())
        sendPushEvent("error", data)
    }

    override fun onMessage(message: PushMessage, instance: String) {
        Log.d(TAG, "recieved message ... processing")
        // val data = Bundle()
        val params = decodeMessage(message.content.toString(Charsets.UTF_8))

        Log.d(TAG, "onMessage -- recieved message  . decoded ... $params")
        val jsonElement = Json.parseToJsonElement(String(message.content))
        val obj = jsonElement.jsonObject
        // convert JSONObject to bundle
        val data = jsonObjectToBundle(obj)

        val messageContent = jsonElement.jsonObject["message"]?.jsonPrimitive?.content
        val titleContent = jsonElement.jsonObject["title"]?.jsonPrimitive?.content
        val notIdContent = jsonElement.jsonObject["notId"]?.jsonPrimitive?.content

        if (messageContent != null) {data.putString("message", messageContent)}
        if (messageContent != null) { data.putString("title", titleContent)}
        if (messageContent != null) {data.putString("notId", notIdContent)}
        

        
        data.putBoolean("decrypted", message.decrypted)
        data.putString("instance", instance)
        Log.d(TAG, "sending \"message\" action with data: $data")
        CustomPushNotification(context, data)
        sendPushEvent("message", data)
        showNotification(String(message.content))

        if (message.decrypted) {
            Log.d(TAG, "show DECRYPTED  \"message\""+ String(message.content) )
            kotlin.runCatching {
                showNotification(String(message.content))
            }.onFailure { err ->
                Log.e(TAG, "Error displaying notification: $err")
                sendErrorEvent(err)
            }
        }

    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showNotification(message: String) {
        Log.d(TAG, "making notification for  ndg: $message")
        createNotificationChannel()

        val data = kotlin.runCatching {
            val json = Json.parseToJsonElement(message)
            json.jsonObject
        }.onFailure { err ->
            Log.e(TAG, "Error parsing notification JSON object: $err")
            sendErrorEvent(err)
        }.getOrNull()

        // res is null is there was a failure in the `runCatching` block
        if (data == null) {
            return
        }

        val id = data["id"]?.jsonPrimitive?.long
        val url = data["url"]?.jsonPrimitive?.content
        val title = data["title"]?.jsonPrimitive?.content
        val body = data["body"]?.jsonPrimitive?.content
        val imageUrl = data["imageUrl"]?.jsonPrimitive?.content
        val count = data["number"]?.jsonPrimitive?.int
        val silent = data["silent"]?.jsonPrimitive?.boolean
        Log.d(TAG, "making notification for !!! url $url")
        // rocketchat://room/abc123
        Log.d(TAG, "making notification for  body $body")
        Log.d(TAG, "making notification for  title $title")
        
        // //
        // types: Record<string, string> = {
        //     c: 'channel',
        //     d: 'direct',
        //     p: 'group',
        //     l: 'channels'
        // };
        // let roomName = type === SubscriptionType.DIRECT ? sender.username : name;
        // if (type === SubscriptionType.OMNICHANNEL) {
        //     roomName = sender.name;
        // }


        if (id == null) {
            Log.w(TAG, "Not sending notification without 'id' in json body")
            return
        }

        val icon = applicationContext.applicationInfo.icon
        val channel = getNotificationChannelId()
        val notification =
            NotificationCompat.Builder(this, channel)
                .setSmallIcon(icon!!)
                .setContentTitle(title)
                .setContentText(body)
                .setTicker(title)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(getOpenUrlIntent(url))
                .setAutoCancel(true)

        if (silent != null) {
            notification.setSilent(silent)
        }

        if (count != null) {
            notification.setNumber(count)
        }

        if (imageUrl !== null) {
            runBlocking {
                val bitmap = urlToBitmap(imageUrl)
                notification.setLargeIcon(bitmap)
            }
        }

        NotificationManagerCompat.from(this).notify(id.toInt(), notification.build())
    }

    private suspend fun urlToBitmap(url: String): Bitmap {
        val bitmap = withContext(Dispatchers.IO) {
            val connection = URL(url).openConnection() as HttpURLConnection

            connection.doInput = true
            connection.connect()

            val bitmap = BitmapFactory.decodeStream(connection.inputStream)
            connection.disconnect()

            return@withContext bitmap
        }
        return bitmap
    }


    fun jsonObjectToBundle(jsonObject: JsonObject): Bundle {
        val bundle = Bundle()
        for ((key, value) in jsonObject) {
            when (value) {
                is JsonPrimitive -> {
                    when {
                        value.isString -> bundle.putString(key, value.content)
                        value.booleanOrNull != null -> bundle.putBoolean(key, value.boolean)
                        value.longOrNull != null -> {
                            val longValue = value.long
                            if (longValue in Int.MIN_VALUE..Int.MAX_VALUE) {
                                bundle.putInt(key, longValue.toInt())
                            } else {
                                bundle.putLong(key, longValue)
                            }
                        }
                        value.doubleOrNull != null -> bundle.putDouble(key, value.double)
                        else -> throw IllegalArgumentException("Unsupported primitive type for key: $key")
                    }
                }
                is JsonObject -> bundle.putBundle(key, jsonObjectToBundle(value))
                is JsonArray -> {
                    // Optional: Add support for JsonArray here
                    throw IllegalArgumentException("JsonArray is not supported for key: $key")
                }
                JsonNull -> bundle.putString(key, null)
            }
        }
        return bundle
    }

    private fun getNotificationChannelId(): String {
        val id = applicationContext.packageName
        val channel = "$id:unified_push_channel"
        return channel
    }

    private fun getAppName(): String {
        val pm = applicationContext.packageManager
        val info = applicationContext.applicationInfo
        return pm.getApplicationLabel(info).toString()
    }

    private fun getNotificationChannelName(): String {
        val appName = getAppName()
        val text = "$appName UP Notifications"
        return text
    }

    private fun getNotificationChannelDescription(): String {
        val appName = getAppName()
        val text = "UnifiedPush Notification Channel for $appName"
        return text
    }

    private fun createNotificationChannel() {
        Log.d(TAG, "creating notificationChannel")
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val id = getNotificationChannelId()
            val name = getNotificationChannelName()
            val descriptionText = getNotificationChannelDescription()
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel =
                NotificationChannel(id, name, importance).apply {
                    description = descriptionText
                }
            // Register the channel with the system.
            NotificationManagerCompat.from(this@PushServiceImpl).createNotificationChannel(channel)
        }
    }

    private fun getOpenUrlIntent(url: String?): PendingIntent {
        Log.d(TAG, "getOpenUrlIntent  ($url)")
        var intent = applicationContext.packageManager.getLaunchIntentForPackage(
            applicationContext.packageName
        )

        if (url != null) {
            intent = Intent(Intent.ACTION_VIEW, url.toUri().normalizeScheme())
        }

        intent?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    override fun onNewEndpoint(endpoint: PushEndpoint, instance: String) {
        val data = Bundle().apply {
            putString("endpoint_url", endpoint.url)
            putString("pubKey", endpoint.pubKeySet?.pubKey)
            putString("auth", endpoint.pubKeySet?.auth)
            putString("instance", instance)
        }
        // val loadedEjson: Ejson = Gson().fromJson("{}", Ejson::class.java)
        Log.d(TAG, "unifiedpush - sending \"registered\" action with loadedjson: $data")
        val prefs = context.getSharedPreferences("UnifiedPushPrefs", MODE_PRIVATE)
        prefs.edit().putString("push_server_url", endpoint.url).apply()

        sendPushEvent("registered", data)
    }

    override fun onRegistrationFailed(reason: FailedReason, instance: String) {
        val data = Bundle()
        data.putString("reason", reason.name)
        data.putString("instance", instance)
        Log.d(TAG, "sending \"registrationFailed\" action with data: $data")
        sendPushEvent("registrationFailed", data)
    }

    override fun onUnregistered(instance: String) {
        val data = Bundle()
        data.putString("instance", instance)
        Log.d(TAG, "sending \"unregistered\" action with data: $data")
        sendPushEvent("unregistered", data)
    }
}