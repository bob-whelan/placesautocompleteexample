package com.bobwhelan.placesautocompleteexample.ui

import android.app.Activity
import android.content.Context
import android.location.Geocoder
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.bobwhelan.placesautocompleteexample.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.maps.android.ktx.awaitMap
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun GoogleMaps(
    stateLatLng: LatLng,
    zoom: Float
) {
    val context = LocalContext.current
    val mapView = rememberMapViewWithLifeCycle()
    val loc = LatLng(stateLatLng.latitude, stateLatLng.longitude)
    val addressString = remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Places.initialize(context, "YOUR API KEY HERE")

    val placeAutoCompleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode != Activity.RESULT_OK) {
            return@rememberLauncherForActivityResult
        }
        val place = Autocomplete.getPlaceFromIntent(it.data!!)
        // do what you want with the place - here we can move the camera and add a marker
        scope.launch {
            val googleMap = mapView.awaitMap()
            val markerIcon = BitmapDescriptorFactory.fromResource(R.drawable.marker)
            place.latLng.let { location ->
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        location,
                        18.0f
                    )
                )
                addressString.value = place.address ?: ""
                setMarker(
                    googleMap = googleMap,
                    location = location,
                    markerIcon = markerIcon,
                    title = if (place.name != null) place.name else addressString.value,
                    snippet = if (place.name != null) addressString.value else null
                )
            }
        }
    }
    Box {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            val cameraPosition = remember(loc) {
                LatLng(loc.latitude, loc.longitude)
            }
            LaunchedEffect(mapView) {
                val googleMap = mapView.awaitMap()
                googleMap.addMarker(MarkerOptions().position(cameraPosition))
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(cameraPosition))
            }
            AndroidView({ mapView }) { mapView ->
                // Reading zoom so that AndroidView recomposes when it changes. The getMapAsync lambda
                // is stored for later, Compose doesn't recognize state reads
                val mapZoom = zoom
                scope.launch {
                    val googleMap = mapView.awaitMap()
                    // Move camera to the same place to trigger the zoom update
                    with(googleMap) {
                        this.mapType = 1
                        this.uiSettings.isZoomControlsEnabled = true
                        this.uiSettings.isCompassEnabled = true
                        this.moveCamera(CameraUpdateFactory.newLatLngZoom(cameraPosition, mapZoom))
                        this.setOnMapLongClickListener { location ->
                            googleMap.clear()
                            googleMap.addMarker(
                                MarkerOptions()
                                    .position(location)
                                    .draggable(true)
                            )
                        }
                    }
                }
            }
        }
        IconButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 12.dp),
            onClick = {
                // Set the fields to specify which types of place data to
                // return after the user has made a selection.
                val fields =
                    listOf(Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
                // Start the autocomplete intent.
                val intent =
                    Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                        .build(context)
                // Use the launcher to fire the intent
                placeAutoCompleteLauncher.launch(
                    intent,
                    ActivityOptionsCompat.makeBasic()
                )
            }
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = "Search",
                tint = Color.Unspecified
            )
        }
    }
}


@Composable
fun rememberMapViewWithLifeCycle(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            id = R.id.map
        }
    }
    val lifeCycleObserver = rememberMapLifecycleObserver(mapView)
    val lifeCycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifeCycle) {
        lifeCycle.addObserver(lifeCycleObserver)
        onDispose {
            lifeCycle.removeObserver(lifeCycleObserver)
        }
    }
    return mapView
}

@Composable
fun rememberMapLifecycleObserver(mapView: MapView): LifecycleEventObserver =
    remember(mapView) {
        LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> throw IllegalStateException()
            }
        }
    }


fun getAddress(context: Context, location: LatLng): String {
    val geocoder = Geocoder(context, Locale.ENGLISH)
    val addresses =
        geocoder.getFromLocation(location.latitude, location.longitude, 1)
    addresses[0]?.let { address0 ->
        return address0.getAddressLine(0).removeSuffix(", USA")
    }
    return ""
}

fun setMarker(
    googleMap: GoogleMap,
    location: LatLng,
    markerIcon: BitmapDescriptor,
    title: String,
    snippet: String?
) {
    googleMap.clear()
    if (snippet == null) {
        googleMap.addMarker(
            MarkerOptions()
                .position(location)
                .icon(markerIcon)
                .title(title)
                .draggable(true)
        )?.showInfoWindow()
    } else {
        googleMap.addMarker(
            MarkerOptions()
                .position(location)
                .icon(markerIcon)
                .title(title)
                .snippet(snippet)
                .draggable(true)
        )?.showInfoWindow()
    }
}
