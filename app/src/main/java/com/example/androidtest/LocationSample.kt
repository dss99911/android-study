package com.example.androidtest

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.HandlerThread
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.annotation.MainThread
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean


/**
 * https://developer.android.com/training/location
 */
class LocationSample private constructor(private val mContext: Context) : ConnectionCallbacks, OnConnectionFailedListener {
    private var mGoogleApiClient: GoogleApiClient? = null
    private var mLocationRequest: LocationRequest? = null
    private var mThread: HandlerThread? = null
    private var location: Location? = null
    override fun onConnected(bundle: Bundle?) {
        startHandlerThread()
        requestLocation()
    }

    override fun onConnectionSuspended(i: Int) {
        terminateHandlerThread()
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        terminateHandlerThread()
    }

    fun requestLocation() {
        logi("requestLocation")
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        if (isGoogleApiClientConnected) {
            try {
                LocationServices
                    .getFusedLocationProviderClient(app)
                    .requestLocationUpdates(mLocationRequest, mCallback, mThread!!.looper)
            } catch (e: SecurityException) {
                e.printStackTrace()
            } catch (e: NullPointerException) {
                e.printStackTrace()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        } else {
            initialize()
        }
    }

    fun requestAccurateLocation(activity: Activity, requestCode: Int, listener: LocationListener) {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            listener.onFailure(SecurityException())
            return
        }
        if (!isGoogleApiClientConnected) {
            initialize()
            listener.onFailure(RuntimeException("google api not connected"))
            return
        }
        try {
            val locationRequest = LocationRequest.create()
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            checkLocationSetting(activity, locationRequest, requestCode, object : LocationSettingListener {
                override fun onSuccess(response: LocationSettingsResponse?) {
                    //the `onLocationAvailability()` can be invoked with availability false after `onLocationResult` on some rare case..
                    //in that case, don't call onFailure

                    //the `onLocationAvailability()` can be invoked with availability false after `onLocationResult` on some rare case..
                    //in that case, don't call onFailure
                    //in background, onLocationAvailability is false. and when changed to foreground, onLocationResult is invoked.
                    val isSuccess = AtomicBoolean(false)

                    val fusedLocationProviderClient = LocationServices
                        .getFusedLocationProviderClient(app)
                    fusedLocationProviderClient
                        .requestLocationUpdates(locationRequest, object : LocationCallback() {
                            override fun onLocationResult(locationResult: LocationResult) {
                                isSuccess.set(true)

                                fusedLocationProviderClient.removeLocationUpdates(this)
                                listener.onSuccess(locationResult.lastLocation)
                            }

                            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                                if (!locationAvailability.isLocationAvailable && !isSuccess.get()) {
                                    listener.onFailure(RuntimeException("location not available"))
                                }
                            }
                        }, Looper.getMainLooper())
                }

                override fun onFailure(e: Exception?) {
                    listener.onFailure(e)
                }
            })
        } catch (e: SecurityException) {
            listener.onFailure(e)
        } catch (e: NullPointerException) {
            listener.onFailure(e)
        } catch (e: IllegalStateException) {
            listener.onFailure(e)
        }
    }

    @SuppressLint("MissingPermission")
    fun requestAccurateLocation() {
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val fusedLocationProviderClient = LocationServices
            .getFusedLocationProviderClient(app)
        fusedLocationProviderClient
            .requestLocationUpdates(locationRequest, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    Log.i("HYUN", "LOCATION = ${locationResult.lastLocation}")
                }

                override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                    Log.i("HYUN", "LOCATION = ${locationAvailability.isLocationAvailable}")
                }
            }, Looper.getMainLooper())
    }


    private interface LocationSettingListener {
        fun onSuccess(response: LocationSettingsResponse?)
        fun onFailure(e: Exception?)
    }

    interface LocationListener {
        fun onSuccess(location: Location?)
        fun onFailure(e: Exception?)
    }

    private fun checkLocationSetting(activity: Activity, locationRequest: LocationRequest, requestCode: Int, listener: LocationSettingListener) {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(activity)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener(activity) { response: LocationSettingsResponse? -> listener.onSuccess(response) }
        task.addOnFailureListener(activity) { e: Exception? ->
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(
                        activity,
                        requestCode
                    )
                } catch (sendEx: SendIntentException) {
                    listener.onFailure(e)
                }
            } else {
                listener.onFailure(e)
            }
        }
    }


    /**
     * android.permission.ACCESS_COARSE_LOCATION – API가 Wi-Fi 또는 이동통신 데이터(또는 모두)를 사용하여 기기의 위치를 판별할 수 있습니다. API는 도시 블록 1개 정도의 오차로 위치를 반환합니다.
     * android.permission.ACCESS_FINE_LOCATION – API가 이용 가능한 위치 제공자에서 최대한 정확한 위치를 판별할 수 있습니다. 위치 제공자에는 GPS(Global Positioning System)와 Wi-Fi, 이동통신 데이터가 포함됩니다.
     */
    private val mCallback: LocationCallback = object : LocationCallback() {
        /**
         * 위치를 끄면, false
         */
        override fun onLocationAvailability(locationAvailability: LocationAvailability) {
            logi("onLocationAvailability : $locationAvailability")
            if (location == null && locationAvailability.isLocationAvailable) {
                getLocation()
            }
        }

        /**
         * 위치가 켜져있을 때만 호출됨
         */
        override fun onLocationResult(locationResult: LocationResult) {
            logi("onLocationResult")
            location = locationResult.lastLocation
            val ret: String?
            if (location != null) {
                ret = makeLocationText(location)
                Log.i("HYUN", "LOCATION = $ret")
            } else {
                Log.i("HYUN", "LOCATION is null")
            }
        }
    }
    private val isGoogleApiClientConnected: Boolean
        private get() = mGoogleApiClient != null && mGoogleApiClient!!.isConnected

    @MainThread
    fun initialize() {
        logi("initialize")
        if (mGoogleApiClient == null) {
            val context: Context = app
            mGoogleApiClient = GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()
        }
        if (mGoogleApiClient!!.isConnected) {
            mGoogleApiClient!!.reconnect()
        } else {
            mGoogleApiClient!!.connect()
        }
        if (mLocationRequest == null) {
            mLocationRequest = LocationRequest.create()
            mLocationRequest?.setInterval(TimeUnit.MINUTES.toMillis(5))
            mLocationRequest?.setFastestInterval(TimeUnit.MINUTES.toMillis(5))
            mLocationRequest?.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        }
    }

    /**
     * location이 없어도 호출됨. 이 때, location 은 null.
     */
    @SuppressLint("MissingPermission")
    fun singleCall() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext)

        fusedLocationClient.getLastLocation()
            .addOnSuccessListener{ location ->
                logi("onSuccess=$location")

            }
            .addOnCanceledListener {
                logi("onCancel")
            }
            .addOnFailureListener { location ->
                logi("onFailure=$location")
            }
    }

    private fun startHandlerThread() {
        if (mThread == null) {
            mThread = HandlerThread(LocationSample::class.simpleName)
            mThread!!.start()
        }
    }

    private fun terminateHandlerThread() {
        if (mThread != null) {
            mThread!!.quitSafely()
            mThread = null
        }
    }

    private fun getLocation() {
        val locationManager = mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            logi("No Location Permission")
            return
        }
        val lastKnownGPSLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        putLocation(lastKnownGPSLocation, "GPS")
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val lastKnownNetworkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        putLocation(lastKnownNetworkLocation, "Network")
        val lastKnownPassiveLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        putLocation(lastKnownPassiveLocation, "Passive")

    }

    fun getLocationLog() {
        logi("loc enabled2=${isLocationEnabled()}")
        val locationManager = mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
//        val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
//        val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
//        val passiveLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
//        logi("gps=$gpsLocation,network=$networkLocation,passive=$passiveLocation")
        val gpsLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkLocationEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        val passiveLocationEnabled = locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)
        logi("enabled gps=$gpsLocationEnabled,network=$networkLocationEnabled,passive=$passiveLocationEnabled")

//        logi("time gps=${gpsLocation?.time},network=${networkLocation?.time},passive=${passiveLocation?.time}")

    }

    private fun putLocation(lastKnownLocation: Location?, logKeyword: String) {
        if (lastKnownLocation != null) {
            val ret = makeLocationText(lastKnownLocation)
            logi("Location : $ret")
        }
    }

    companion object {
        private var mInstance: LocationSample? = null
        fun getInstance(context: Context): LocationSample {
            if (mInstance == null) {
                synchronized(LocationSample::class.java) {
                    if (mInstance == null) {
                        mInstance = LocationSample(context)
                    }
                }
            }
            return mInstance!!
        }

        fun makeLocationText(location: Location?): String? {
            return if (location == null) {
                null
            } else location.latitude.toString() + "," + location.longitude
        }
    }

    fun isLocationEnabled() = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
            var locationMode = 0
            try {
                locationMode = Settings.Secure.getInt(mContext.contentResolver, Settings.Secure.LOCATION_MODE)
            } catch (e: Settings.SettingNotFoundException) {
                e.printStackTrace()
            }
            locationMode != Settings.Secure.LOCATION_MODE_OFF

        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
            val locationManager = mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.isLocationEnabled
        }
        else -> {
            val locationProviders: String = Settings.Secure.getString(mContext.contentResolver, Settings.Secure.LOCATION_MODE)
            !TextUtils.isEmpty(locationProviders)
        }
    }
}