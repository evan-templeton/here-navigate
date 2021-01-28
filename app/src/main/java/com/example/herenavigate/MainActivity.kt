package com.example.herenavigate

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.here.sdk.core.GeoCoordinates
import com.here.sdk.core.errors.InstantiationErrorException
import com.here.sdk.mapview.MapScheme
import com.here.sdk.mapview.MapView
import com.here.sdk.routing.*

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var mapView: MapView
    private lateinit var locationCallback: LocationCallback
    private lateinit var waypoints: List<Waypoint>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        askLocationPermission()

        mapView = findViewById(R.id.map_view)
        mapView.onCreate(savedInstanceState)
        mapView.setOnReadyListener { // This will be called each time after this activity is resumed.
            // It will not be called before the first map scene was loaded.
            // Any code that requires map data may not work as expected beforehand.
            Log.d("HereMaps", "HERE Rendering Engine attached.")
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {

                    val startCoords = GeoCoordinates(location.latitude, location.longitude)
                    val endCoords = GeoCoordinates(40.439112680705165, -79.9971769423376)

                    val startWaypoint = Waypoint(startCoords)
                    val destWaypoint = Waypoint(endCoords)
                    waypoints = listOf(startWaypoint, destWaypoint)

                    calculateRoute(waypoints)
                }
            }
        }
    }

    private fun calculateRoute(waypoints: List<Waypoint>) {
        try {
            val routingEngine = RoutingEngine()
            routingEngine.calculateRoute(waypoints, TruckOptions()) { routingError, routes ->
                if (routingError == null) {
                    val route = routes!![0]
                    Log.d("Route Duration: ", route.durationInSeconds.toString())
                    Log.d("Route Length: ", route.lengthInMeters.toString())
//                                showRouteDetails(route)
//                                showRouteOnMap(route)
//                                logRouteViolations(route)
                } else {
                    Log.d("Route error: ", routingError.toString())
                }
            }
        } catch (e: InstantiationErrorException) {
            throw RuntimeException("Initialization of RoutingEngine failed: " + e.error.name)
        }
    }

    private fun askLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //permission granted
                fusedLocationProviderClient.lastLocation.addOnSuccessListener {
                    // Got last known location. In some rare situations this can be null.
                    }
                loadMapScene()
            }
        }
    }

    private fun loadMapScene() {
        // Load a scene from the HERE SDK to render the map with a map scheme.
        mapView.mapScene.loadScene(
                MapScheme.NORMAL_DAY
        ) { mapError ->
            if (mapError == null) {
                val distanceInMeters = (1000 * 10).toDouble()
                mapView.camera.lookAt(
                        GeoCoordinates(52.530932, 13.384915), distanceInMeters
                )
            } else {
                Log.d("HereMaps Error", "Loading map failed: mapError: " + mapError.name)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        startLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create()
        locationRequest.priority = 102
        locationRequest.interval = 5000
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

}