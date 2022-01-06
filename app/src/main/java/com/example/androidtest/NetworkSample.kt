package com.example.androidtest

import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.telephony.TelephonyManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.text.format.Formatter
import java.lang.Exception
import java.lang.StringBuilder
import java.net.NetworkInterface
import java.net.InetAddress
import java.util.*

object NetworkSample {
    private const val NETWORK_STATE_OFFLINE = "Offline"
    private const val NETWORK_STATE_UNKNOWN = "Unknown"
    private const val NETWORK_STATE_WIFI = "Wifi"
    private const val NETWORK_STATE_MOBILE = "Mobile"
    private const val NETWORK_STATE_2G = "2g"
    private const val NETWORK_STATE_3G = "3g"
    private const val NETWORK_STATE_LTE = "Lte"
    fun isConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var activeNetwork: NetworkInfo? = null
        if (cm != null) {
            activeNetwork = cm.activeNetworkInfo
        }
        return activeNetwork != null && activeNetwork.isConnected
    }

    private fun isWifiConnected(context: Context?): Boolean {
        if (context == null) {
            return false
        }
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetworkInfo
        return (activeNetwork != null && activeNetwork.isConnectedOrConnecting
                && activeNetwork.type == ConnectivityManager.TYPE_WIFI)
    }

    fun getNetworkTypeName(context: Context?): String {
        if (context == null) {
            return NETWORK_STATE_UNKNOWN
        }
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager ?: return NETWORK_STATE_UNKNOWN
        val networkInfo = connectivityManager.activeNetworkInfo ?: return NETWORK_STATE_UNKNOWN
        if (networkInfo.isConnected) {
            if (getNetworkType(context) == NETWORK_STATE_WIFI) {
                return NETWORK_STATE_WIFI
            } else if (getNetworkType(context) == NETWORK_STATE_MOBILE) {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager ?: return NETWORK_STATE_MOBILE
                val networkType = telephonyManager.networkType
                return when (networkType) {
                    TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyManager.NETWORK_TYPE_IDEN -> NETWORK_STATE_2G
                    TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyManager.NETWORK_TYPE_HSPAP -> NETWORK_STATE_3G
                    TelephonyManager.NETWORK_TYPE_LTE -> NETWORK_STATE_LTE
                    else -> NETWORK_STATE_MOBILE
                }
            }
        } else {
            return NETWORK_STATE_UNKNOWN
        }
        return NETWORK_STATE_UNKNOWN
    }

    private fun getNetworkType(context: Context?): String {
        if (context == null) {
            return NETWORK_STATE_UNKNOWN
        }
        val connectivityManager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            ?: return NETWORK_STATE_UNKNOWN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) ?: return NETWORK_STATE_UNKNOWN
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return NETWORK_STATE_WIFI
            } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return NETWORK_STATE_MOBILE
            }
        } else {
            val activeNetwork = connectivityManager.activeNetworkInfo ?: return NETWORK_STATE_UNKNOWN
            if (activeNetwork.type == ConnectivityManager.TYPE_WIFI) {
                return NETWORK_STATE_WIFI
            } else if (activeNetwork.type == ConnectivityManager.TYPE_MOBILE) {
                return NETWORK_STATE_MOBILE
            }
        }
        return NETWORK_STATE_UNKNOWN
    }

    fun getWifiSsid(context: Context): String? {
        if (!isWifiConnected(context)) {
            return null
        }
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager ?: return null
        val wifiInfo = wifiManager.connectionInfo ?: return null
        return wifiInfo.ssid
    }

    @SuppressLint("HardwareIds")
    fun getWifiMacAddress(context: Context): String? {
        val mng = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager ?: return null
        val info = mng.connectionInfo
        return info.macAddress
    }

    /**
     * @return return of String like WIFI, MOBILE:UMTS. return OFFLINE if is not connected
     */
    fun getConnectedNetworkInfoName(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager ?: return NETWORK_STATE_UNKNOWN
        val networkInfo = cm.activeNetworkInfo ?: return NETWORK_STATE_UNKNOWN
        if (!networkInfo.isConnected) {
            return NETWORK_STATE_OFFLINE
        }
        val sb = StringBuilder()
        val typeName = networkInfo.typeName
        val subTypeName = networkInfo.subtypeName
        sb.append(typeName)
        if (subTypeName == null || subTypeName.length == 0) {
            return sb.toString()
        }
        sb.append(":").append(networkInfo.subtypeName)
        return sb.toString()
    }

    fun getClientIp(context: Context): String {
        var isWifiConn = false
        var clientIpAddress = ""
        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        for (network in connMgr.allNetworks) {
            val networkInfo = connMgr.getNetworkInfo(network)
            if (networkInfo != null && networkInfo.type == ConnectivityManager.TYPE_WIFI) {
                isWifiConn = isWifiConn or networkInfo.isConnected
            }
        }
        if (isWifiConn) {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (wifiManager != null) {
                val ipAddress = wifiManager.connectionInfo.ipAddress
                clientIpAddress = Formatter.formatIpAddress(ipAddress)
            }
        } else {
            clientIpAddress = mobileIPAddress
        }
        return clientIpAddress
    }

    //Do noting
    private val mobileIPAddress: String
        private get() {
            try {
                val interfaces: List<NetworkInterface> = Collections.list(NetworkInterface.getNetworkInterfaces())
                for (intf in interfaces) {
                    val addrs: List<InetAddress> = Collections.list(intf.inetAddresses)
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress) {
                            return addr.hostAddress
                        }
                    }
                }
            } catch (e: Exception) {
                //Do noting
            }
            return ""
        }
}