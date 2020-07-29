package com.alfanshter.menampilkangeolocation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.os.Message
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.alfanshter.menampilkangeolocation.Utils.AppConstants
import com.alfanshter.menampilkangeolocation.Utils.GpsUtils
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

@Suppress("DEPRECATED_IDENTITY_EQUALS")
class MainActivity : AppCompatActivity() {
    private var wayLatitude = 0.0
    private  var wayLongitude:kotlin.Double = 0.0
    lateinit var locationRequest : LocationRequest
    lateinit var locationCallback : LocationCallback
    private var isContinue = false
    private var isGPS = false
    private var LOCATION_REQUEST = 1000
    private var GPS_REQUEST = 1001
    var result: String? = null
    lateinit var address : Address
    private var stringBuilder: java.lang.StringBuilder? = null

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 10 * 1000.toLong() // 10 seconds

        locationRequest.fastestInterval = 5 * 1000.toLong() // 5 seconds


        GpsUtils(this).turnGPSOn(object : GpsUtils.onGpsListener {
            override fun gpsStatus(isGPSEnable: Boolean) {
                // turn on GPS
                isGPS = isGPSEnable
            }
        })

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult == null) {
                    return
                }
                for (location in locationResult.locations) {
                    if (location != null) {
                        wayLatitude = location.latitude
                        wayLongitude = location.longitude
                        if (!isContinue) {
                            txtLocation.text = String.format(
                                Locale.US,
                                "%s - %s",
                                wayLatitude,
                                wayLongitude
                            )
                        } else {
                            stringBuilder!!.append(wayLatitude)
                            stringBuilder!!.append("-")
                            stringBuilder!!.append(wayLongitude)
                            stringBuilder!!.append("\n\n")
                            txtContinueLocation.text = stringBuilder.toString()
                        }
                        if (!isContinue && mFusedLocationClient != null) {
                            mFusedLocationClient.removeLocationUpdates(locationCallback)
                        }
                    }
                }
            }
        }
        if (!isGPS) {
            Toast.makeText(this, "Please turn on GPS", Toast.LENGTH_SHORT).show()
            return
        }
        isContinue = false
        getLocation()
        btnLocation.setOnClickListener { v: View? -> }

        btnContinueLocation.setOnClickListener { v: View? ->
            if (!isGPS) {
                Toast.makeText(this, "Please turn on GPS", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            isContinue = true
            stringBuilder = java.lang.StringBuilder()
            getLocation()
        }
    }

    private fun getLocation() {
        var geocoder: Geocoder
        var addressList = ArrayList<Address>()
        geocoder = Geocoder(applicationContext, Locale.getDefault())

        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) !== PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) !== PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                AppConstants.LOCATION_REQUEST
            )

            ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    LOCATION_REQUEST
            )
        } else {
            if (isContinue) {
                mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            } else {
                mFusedLocationClient.lastLocation
                        .addOnSuccessListener(this@MainActivity) { location ->
                            if (location != null) {
                                wayLatitude = location.getLatitude()
                                wayLongitude = location.getLongitude()
                                txtLocation.text = String.format(
                                    Locale.US,
                                    "%s - %s",
                                    wayLatitude,
                                    wayLongitude
                                )
                                addressList = geocoder.getFromLocation(
                                        location.latitude,
                                        location.longitude,
                                        1
                                ) as ArrayList<Address>
                                if (addressList != null && addressList.size > 0) {
                                    address = addressList[0]
                                    stringBuilder = StringBuilder()
                                    for (i in 0 until address.maxAddressLineIndex) {
                                        stringBuilder!!.append(address.getAddressLine(i)).append("\n")
                                    }
                                    stringBuilder!!.append(address.locality).append(",")
                                    stringBuilder!!.append(address.postalCode).append(",")
                                    stringBuilder!!.append(address.countryName)
                                    result = stringBuilder.toString()
                                    alamat.text = result.toString()
                                }
                                val message = Message.obtain()
                                if (result != null) {
                                    message.what = 1
                                    val bundle = Bundle()
                                    bundle.putString("address", result)
                                    message.data = bundle
                                }
                            } else {
                                mFusedLocationClient.requestLocationUpdates(
                                        locationRequest,
                                        locationCallback,
                                        null
                                )
                            }
                        }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1000 -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    if (isContinue) {
                        if (ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return
                        }
                        mFusedLocationClient.requestLocationUpdates(
                            locationRequest,
                            locationCallback,
                            null
                        )
                    } else {
                        mFusedLocationClient.lastLocation
                            .addOnSuccessListener(this@MainActivity) { location ->
                                if (location != null) {
                                    wayLatitude = location.latitude
                                    wayLongitude = location.longitude
                                    txtLocation.text = String.format(
                                        Locale.US,
                                        "%s - %s",
                                        wayLatitude,
                                        wayLongitude
                                    )
                                } else {
                                    mFusedLocationClient.requestLocationUpdates(
                                        locationRequest,
                                        locationCallback,
                                        null
                                    )
                                }
                            }
                    }
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GPS_REQUEST) {
                isGPS = true // flag maintain before get location
            }
        }
    }

}