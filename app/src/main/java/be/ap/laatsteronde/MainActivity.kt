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
import androidx.compose.ui.Modifier
import be.ap.laatsteronde.ui.AppNavigation
import be.ap.laatsteronde.ui.theme.OSMTheme
import org.osmdroid.config.Configuration
import java.io.File

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
