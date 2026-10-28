package be.ap.laatsteronde

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import be.ap.laatsteronde.ui.theme.OSMTheme
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.io.File

private val AP_HOGESCHOOL = GeoPoint(51.230167, 4.416129)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                Surface(modifier = Modifier.fillMaxSize()) {
                    MapScreen(modifier = Modifier.safeDrawingPadding())
                }
            }
        }
    }
}

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val mapView = rememberMapView()

    AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxSize()
    )

    // TODO: add your own functionality here (search field, markers, ...)
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
