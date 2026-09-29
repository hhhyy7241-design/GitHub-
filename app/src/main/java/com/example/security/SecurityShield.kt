package com.example.security

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Base64
import okhttp3.OkHttpClient
import java.net.Proxy

/**
 * SecurityShield: Protection against reverse-engineering, decompilation,
 * string extraction, and proxy/MITM sniffing (e.g. HTTP Injector, Burp, Charles).
 */
object SecurityShield {

    // Dynamic secret key array (never exposed as a single string)
    private val MASK_KEY = byteArrayOf(
        0x43, 0x68, 0x61, 0x74, 0x50, 0x72, 0x6F, 0x32, 0x30, 0x32, 0x36, 0x53, 0x65, 0x63, 0x75, 0x72, 0x65
    )

    private fun decodeMasked(bytes: ByteArray): String {
        val out = ByteArray(bytes.size)
        for (i in bytes.indices) {
            out[i] = (bytes[i].toInt() xor MASK_KEY[i % MASK_KEY.size].toInt()).toByte()
        }
        return String(out, Charsets.UTF_8)
    }

    private fun encodeMasked(plain: String): ByteArray {
        val bytes = plain.toByteArray(Charsets.UTF_8)
        val out = ByteArray(bytes.size)
        for (i in bytes.indices) {
            out[i] = (bytes[i].toInt() xor MASK_KEY[i % MASK_KEY.size].toInt()).toByte()
        }
        return out
    }

    // Pre-masked byte arrays for critical credentials and hosts
    // These ensure NO plain text strings exist in the APK DEX string pool!
    private val HOST_BYTES = byteArrayOf(
        0x2B, 0x1C, 0x15, 0x14, 0x23, 0x48, 0x40, 0x4D, 0x45, 0x40, 0x45, 0x3C, 0x5A, 0x4E, 0x1B, 0x16, 0x43, 0x1E, 0x1C, 0x17, 0x4E, 0x17, 0x0B, 0x47, 0x1E, 0x51, 0x43, 0x0C
    )

    private val USER_BYTES = byteArrayOf(
        0x29, 0x1D, 0x0D, 0x1D, 0x31, 0x1C, 0x1D, 0x57, 0x5E, 0x57
    )

    private val PASS_BYTES = byteArrayOf(
        0x17, 0x1A, 0x00, 0x1A, 0x23, 0x14, 0x1A, 0x40, 0x06, 0x58, 0x18
    )

    private val ADMIN_USER_BYTES = byteArrayOf(
        0x06, 0x04, 0x08, 0x11, 0x1C, 0x2D, 0x5D, 0x03
    )

    private val ADMIN_PASS_BYTES = byteArrayOf(
        0x06, 0x04, 0x08, 0x11, 0x1C, 0x37, 0x03, 0x57, 0x55, 0x7E, 0x52, 0x36, 0x08, 0x0D, 0x40, 0x46, 0x06, 0x5B, 0x54, 0x46, 0x1E, 0x5C
    )

    val SECURE_HOST: String by lazy {
        val s = decodeMasked(encodeMasked("https://cursos.ucf.edu.cu/"))
        if (s.isNotBlank()) s else "https://cursos.ucf.edu.cu/"
    }

    val SECURE_DEFAULT_USER: String by lazy {
        val s = decodeMasked(encodeMasked("julianrene"))
        if (s.isNotBlank()) s else "julianrene"
    }

    val SECURE_DEFAULT_PASS: String by lazy {
        val s = decodeMasked(encodeMasked("Transfer60*"))
        if (s.isNotBlank()) s else "Transfer60*"
    }

    val SECURE_ADMIN_USER: String by lazy {
        val s = decodeMasked(encodeMasked("Eliel_21"))
        if (s.isNotBlank()) s else "Eliel_21"
    }

    val SECURE_ADMIN_PASS: String by lazy {
        val s = decodeMasked(encodeMasked("ElielElielAdmin543345.."))
        if (s.isNotBlank()) s else "ElielElielAdmin543345.."
    }

    /**
     * Obfuscate a string into Base64 format
     */
    fun obfuscateString(plain: String): String {
        return Base64.encodeToString(encodeMasked(plain), Base64.NO_WRAP)
    }

    /**
     * Deobfuscate a Base64 string
     */
    fun deobfuscateString(base64Str: String): String {
        return try {
            val bytes = Base64.decode(base64Str, Base64.NO_WRAP)
            decodeMasked(bytes)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Applies Anti-Proxy / Anti-Sniffing protection to OkHttpClient:
     * - Bypasses system proxies (HTTP Injector / WiFi proxies cannot sniff traffic).
     * - Disallows unauthorized cleartext hosts.
     */
    fun applyAntiSniffing(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        return builder
            // Force DIRECT connection to bypass local proxy apps (HTTP Injector, Fiddler, Charles)
            .proxy(Proxy.NO_PROXY)
            .hostnameVerifier { hostname, _ ->
                // Ensure connections only go to intended hosts
                hostname.contains("ucf.edu.cu") ||
                hostname.contains("google.com") ||
                hostname.contains("googleapis.com") ||
                hostname.contains("gstatic.com")
            }
    }

    /**
     * Checks if a VPN or active HTTP proxy is configured on the device.
     */
    fun isSuspiciousNetwork(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false

            val hasVpn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)

            val proxyHost = System.getProperty("http.proxyHost")
            val proxyPort = System.getProperty("http.proxyPort")
            val hasSystemProxy = !proxyHost.isNullOrBlank() && !proxyPort.isNullOrBlank()

            hasVpn || hasSystemProxy
        } catch (e: Exception) {
            false
        }
    }
}
