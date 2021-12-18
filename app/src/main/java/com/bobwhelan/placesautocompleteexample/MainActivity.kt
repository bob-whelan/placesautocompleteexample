package com.bobwhelan.placesautocompleteexample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.bobwhelan.placesautocompleteexample.ui.GoogleMaps
import com.bobwhelan.placesautocompleteexample.ui.theme.PlacesAutocompleteExampleTheme
import com.google.android.gms.maps.model.LatLng

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlacesAutocompleteExampleTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    GoogleMaps(LatLng(17.385, 78.4867), 18f)
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    PlacesAutocompleteExampleTheme {
        GoogleMaps(LatLng(17.385, 78.4867), 18f)
    }
}