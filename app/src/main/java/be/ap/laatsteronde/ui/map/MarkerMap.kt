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

/** A shop on the map: a label with the name of the shop ([label]) at [position]. [id] says which shop was tapped. */
data class MapPin(val id: String, val position: Coordinates, val label: String)

/**
 * An OpenStreetMap map with one marker (the position of the customer or the pin of a shop) and,
 * if wanted, name labels for shops ([pins]).
 *
 * - [position] is where the marker stands; null means no marker.
 * - [pins] are name labels of shops; the one with [selectedPinId] is green. A tap on a label goes to
 *   [onPinClick], a tap on the map itself to [onMapTap] (to close a card).
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
    pins: List<MapPin> = emptyList(),
    selectedPinId: String? = null,
    onPinClick: ((String) -> Unit)? = null,
    onMapTap: (() -> Unit)? = null,
    onPositionPicked: ((Coordinates) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnPicked by rememberUpdatedState(onPositionPicked)
    val currentOnMapTap by rememberUpdatedState(onMapTap)
    val currentOnPinClick by rememberUpdatedState(onPinClick)

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
                currentOnMapTap?.invoke()
                val callback = currentOnPicked ?: return currentOnMapTap != null
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

    // The name labels of the shops. They are made again when the shops or the chosen shop change;
    // the chosen one is added last, so it is drawn on top.
    val pinMarkers = remember(mapView) { mutableListOf<Marker>() }
    LaunchedEffect(mapView, pins, selectedPinId) {
        pinMarkers.forEach { mapView.overlays.remove(it) }
        pinMarkers.clear()
        pins.sortedBy { it.id == selectedPinId }.forEach { pin ->
            val pinMarker = Marker(mapView).apply {
                setInfoWindow(null)
                this.position = GeoPoint(pin.position.latitude, pin.position.longitude) // "this": the parameter [position] has the same name
                icon = shopMarkerDrawable(context, pin.label, selected = pin.id == selectedPinId)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setOnMarkerClickListener { _, _ ->
                    currentOnPinClick?.invoke(pin.id)
                    true
                }
            }
            mapView.overlays.add(pinMarker)
            pinMarkers += pinMarker
        }
        mapView.invalidate()
    }

    LaunchedEffect(mapView, centerOn, recenterKey) {
        mapView.controller.animateTo(GeoPoint(centerOn.latitude, centerOn.longitude))
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}
