package fdroid_up.rocket.reactnative.notification

import android.os.Build
import android.util.Base64
import java.net.URLDecoder
import java.security.KeyFactory
import java.security.Security
import java.security.interfaces.ECPublicKey
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import org.bouncycastle.jce.ECPointUtil
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import org.bouncycastle.jce.provider.BouncyCastleProvider

// Register Bouncy Castle as a security provider
private val registerBouncyCastleProvider = Security.addProvider(BouncyCastleProvider())

/**
 * Check if VAPID is implemented for the SDK version.
 */
fun vapidImplementedForSdk(): Boolean {
    return Build.VERSION.SDK_INT >= 23
}

/**
 * Decode a Base64 URL-safe string to a byte array.
 */
fun String.b64decode(): ByteArray {
    return Base64.decode(this, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}

/**
 * Encode a byte array to a Base64 URL-safe string.
 */
fun ByteArray.b64encode(): String {
    return Base64.encodeToString(this, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}

/**
 * Decode a Base64-encoded uncompressed EC point to an ECPublicKey.
 */
fun String.decodePubKey(): ECPublicKey {
    val encodedPoint = this.b64decode()

    // Retrieve the curve parameters for secp256r1 (also known as prime256v1)
    val bcSpec: ECNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec("secp256r1")

    // Convert Bouncy Castle's curve parameters to Java's ECNamedCurveSpec
    val ecSpec = ECNamedCurveSpec(
        bcSpec.name,
        bcSpec.curve,
        bcSpec.g,
        bcSpec.n,
        bcSpec.h,
        bcSpec.seed
    )

    // Decode the point using Bouncy Castle's ECPointUtil
    val point: ECPoint = ECPointUtil.decodePoint(ecSpec.curve, encodedPoint)

    // Create a public key specification with the decoded point
    val pubSpec = ECPublicKeySpec(point, ecSpec)

    // Generate the public key from the specification
    val keyFactory = KeyFactory.getInstance("EC", "BC")
    return keyFactory.generatePublic(pubSpec) as ECPublicKey
}

/**
 * Encode an ECPublicKey to a Base64-encoded uncompressed EC point.
 */
fun ECPublicKey.encode(): String {
    val encodedPoint = this.w.let { point ->
        val x = point.affineX.toByteArray().let { it.padStart(32) }
        val y = point.affineY.toByteArray().let { it.padStart(32) }
        byteArrayOf(0x04) + x + y
    }
    return encodedPoint.b64encode()
}

/**
 * Pad a byte array to the specified length with leading zeros.
 */
fun ByteArray.padStart(length: Int): ByteArray {
    if (this.size >= length) return this
    val result = ByteArray(length)
    System.arraycopy(this, 0, result, length - this.size, this.size)
    return result
}

/**
 * Generate test page URL for parameters.
 */
fun genTestPageUrl(endpoint: String, p256dh: String, auth: String, vapid: String, showVapid: Boolean): String {
    var url = "https://unifiedpush.org/test_wp.html#endpoint=$endpoint&p256dh=$p256dh&auth=$auth"
    if (showVapid) {
        url += "&vapid=$vapid"
    }
    return url
}

private const val COULD_NOT_DECRYPT = "Could not decrypt content."

/**
 * Decode message as a `application/x-www-form-urlencoded` message.
 *
 * If the message doesn't contain "title=" and "message=", the full body is considered as the message.
 * If the message contains unknown characters, the message is [COULD_NOT_DECRYPT].
 *
 * @return a map of key => value.
 * "title=myTitle&message=myContent" will return a Map {"title"=>"myTitle", "message"=>"myContent"}
 */
internal fun decodeMessage(message: String): Map<String, String> {
    val params = try {
        message.split("&").associate {
            val (key, value) = it.split("=", limit = 2)
            URLDecoder.decode(key, "UTF-8") to URLDecoder.decode(value, "UTF-8")
        }
    } catch (e: Exception) {
        notDecodedMap(message)
    }

    return if (params.containsKey("title") && params.containsKey("message")) {
        params
    } else {
        notDecodedMap(message)
    }
}

private fun notDecodedMap(message: String): Map<String, String> {
    return if (message.all { it.isDefined() && !it.isISOControl() }) {
        mapOf("message" to message)
    } else {
        mapOf("message" to COULD_NOT_DECRYPT)
    }
}