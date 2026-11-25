package be.ap.laatsteronde

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import be.ap.laatsteronde.data.Coordinates
import be.ap.laatsteronde.ui.AppNavigation
import be.ap.laatsteronde.ui.theme.OSMTheme
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

private val AP_HOGESCHOOL = GeoPoint(51.230167, 4.416129)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draws behind the system bars. The app is always light, so the bar icons are dark
        // even when the phone itself is in dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        Configuration.getInstance().apply {
            load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
            // The OpenStreetMap tile servers require a User-Agent that identifies the app.
            userAgentValue = packageName
            // App-specific folders: no storage permissions needed.
            osmdroidBasePath = File(filesDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }

        setContent {
            OSMTheme {
                // Use the theme's background color (cream / dark green), not the white card surface.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(modifier = Modifier.safeDrawingPadding())
                }
            }
        }
    }
}

/** The map with one pin on [location]; the map moves to the pin when the location changes. */
@Composable
fun MapScreen(location: Coordinates, modifier: Modifier = Modifier) {
    val mapView = rememberMapView()
    val pin = remember(mapView) {
        Marker(mapView).apply { setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxSize()
    )

    LaunchedEffect(mapView, location) {
        val point = GeoPoint(location.latitude, location.longitude)
        pin.position = point
        if (pin !in mapView.overlays) mapView.overlays.add(pin)
        mapView.controller.animateTo(point)
        mapView.invalidate()
    }
}

/**
 * Creates one MapView that follows the lifecycle of the screen:
 * onResume/onPause together with the activity, onDetach when the screen disappears.
 */
@Composable
fun rememberMapView(): MapView {
    val context = LocalContext.current

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(17.0)
            controller.setCenter(AP_HOGESCHOOL)
        }
    }
    return mapView
}
