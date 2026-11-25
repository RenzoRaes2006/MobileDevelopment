package be.ap.laatsteronde.ui.map

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import be.ap.laatsteronde.data.Coordinates
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

private const val DEFAULT_ZOOM = 17.0

/**
 * An OpenStreetMap map with at most one marker.
 *
 * - [position] is where the marker stands; null means no marker.
 * - [centerOn] is where the map looks. It moves there when [centerOn] or [recenterKey] changes,
 *   so the map does not jump back while the person looks around.
 * - A tap on the map, or (with [draggable]) a drag of the marker, is reported to [onPositionPicked].
 *   Without [onPositionPicked] the map only shows.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun MarkerMap(
    position: Coordinates?,
    centerOn: Coordinates,
    @DrawableRes markerIcon: Int,
    modifier: Modifier = Modifier,
    /** True for a pin whose tip points at the place (the bottom middle), false for a dot (the middle). */
    pinStyle: Boolean = false,
    draggable: Boolean = false,
    recenterKey: Int = 0,
    onPositionPicked: ((Coordinates) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnPicked by rememberUpdatedState(onPositionPicked)

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            // The designs have no zoom buttons; zooming works with two fingers.
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(DEFAULT_ZOOM)
            controller.setCenter(GeoPoint(centerOn.latitude, centerOn.longitude))
            // On a screen that scrolls, a drag on the map must move the map and not the page.
            setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        view.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
        }
    }

    val marker = remember(mapView) {
        Marker(mapView).apply {
            setInfoWindow(null) // no popup bubble
            setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                override fun onMarkerDrag(marker: Marker) = Unit
                override fun onMarkerDragStart(marker: Marker) = Unit
                override fun onMarkerDragEnd(marker: Marker) {
                    currentOnPicked?.invoke(Coordinates(marker.position.latitude, marker.position.longitude))
                }
            })
        }
    }

    // Taps on the map. The overlay is added once and always reads the newest callback.
    DisposableEffect(mapView) {
        val tapOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(point: GeoPoint): Boolean {
                val callback = currentOnPicked ?: return false
                callback(Coordinates(point.latitude, point.longitude))
                return true
            }

            override fun longPressHelper(point: GeoPoint): Boolean = false
        })
        mapView.overlays.add(0, tapOverlay)
        onDispose { mapView.overlays.remove(tapOverlay) }
    }

    // The map follows the lifecycle of the screen, as osmdroid asks.
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    SideEffect {
        marker.isDraggable = draggable
        marker.icon = ContextCompat.getDrawable(context, markerIcon)
        marker.setAnchor(
            Marker.ANCHOR_CENTER,
            if (pinStyle) Marker.ANCHOR_BOTTOM else Marker.ANCHOR_CENTER
        )
    }

    LaunchedEffect(mapView, position) {
        if (position == null) {
            mapView.overlays.remove(marker)
        } else {
            marker.position = GeoPoint(position.latitude, position.longitude)
            if (marker !in mapView.overlays) mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    LaunchedEffect(mapView, centerOn, recenterKey) {
        mapView.controller.animateTo(GeoPoint(centerOn.latitude, centerOn.longitude))
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}
