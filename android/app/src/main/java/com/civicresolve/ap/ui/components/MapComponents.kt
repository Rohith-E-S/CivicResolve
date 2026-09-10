package com.civicresolve.ap.ui.components

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private val voyagerSource = XYTileSource(
    "CartoVoyager", 0, 19, 256, ".png",
    arrayOf("https://a.basemaps.cartocdn.com/rastertiles/voyager/", "https://b.basemaps.cartocdn.com/rastertiles/voyager/", "https://c.basemaps.cartocdn.com/rastertiles/voyager/"),
    "© OSM © CARTO"
)
private val darkSource = XYTileSource(
    "CartoDark", 0, 19, 256, ".png",
    arrayOf("https://a.basemaps.cartocdn.com/dark_all/", "https://b.basemaps.cartocdn.com/dark_all/", "https://c.basemaps.cartocdn.com/dark_all/"),
    "© OSM © CARTO"
)

@Composable
fun ComplaintMap(lat: String, lng: String, modifier: Modifier = Modifier) {
    val latD = lat.toDoubleOrNull() ?: 0.0
    val lngD = lng.toDoubleOrNull() ?: 0.0
    if (latD == 0.0 && lngD == 0.0) {
        Box(modifier = modifier.height(220.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Text("No location", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        return
    }
    val context = LocalContext.current
    AndroidView(
        modifier = modifier.height(260.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
        factory = {
            MapView(context).apply {
                setTileSource(voyagerSource)
                setMultiTouchControls(true)
                controller.setZoom(15.0)
                controller.setCenter(GeoPoint(latD, lngD))
            }
        },
        update = { map ->
            val point = GeoPoint(latD, lngD)
            val marker = map.overlays.filterIsInstance<Marker>().firstOrNull()
            if (marker == null) {
                val m = Marker(map)
                m.position = point
                m.title = "Complaint Location"
                map.overlays.add(m)
            } else {
                marker.position = point
            }
            map.controller.setCenter(point)
            map.invalidate()
        }
    )
}

@Composable
fun MapPicker(
    latitude: String,
    longitude: String,
    onChange: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val latD = latitude.toDoubleOrNull() ?: 20.5937
    val lngD = longitude.toDoubleOrNull() ?: 78.9629
    var mapView by remember { mutableStateOf<MapView?>(null) }

    Box(modifier = modifier.height(300.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                MapView(context).apply {
                    setTileSource(voyagerSource)
                    setMultiTouchControls(true)
                    controller.setZoom(17.5)
                    controller.setCenter(GeoPoint(latD, lngD))
                    setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_UP) {
                            val proj = projection
                            val geo = proj.fromPixels(event.x.toInt(), event.y.toInt()) as GeoPoint
                            onChange(String.format("%.6f", geo.latitude), String.format("%.6f", geo.longitude))
                        }
                        false
                    }
                    mapView = this
                }
            },
            update = { map ->
                // Recentre when the pin is set externally (e.g. "Use current
                // location") so the crosshair matches the submitted coords
                val target = GeoPoint(latD, lngD)
                val center = map.mapCenter
                val rad = Math.PI / 180.0
                val dLat = (target.latitude - center.latitude) * rad
                val dLng = (target.longitude - center.longitude) * rad
                val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(center.latitude * rad) * Math.cos(target.latitude * rad) *
                    Math.sin(dLng / 2) * Math.sin(dLng / 2)
                if (6371000.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) > 1.0) {
                    map.controller.animateTo(target)
                }
            }
        )
        // Crosshair
        Box(modifier = Modifier.align(Alignment.Center).size(36.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.Red).border(2.dp, Color.White, CircleShape)
            )
        }
        Text(
            "Drag map to adjust pin",
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp).clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(alpha = 0.7f)).padding(horizontal = 12.dp, vertical = 5.dp),
            color = Color.White,
            fontSize = 11.sp
        )
        IconButton(
            onClick = {
                // use last known? parent should handle recenter via fused location
                mapView?.let { mv ->
                    val center = mv.mapCenter as GeoPoint
                    onChange(String.format("%.6f", center.latitude), String.format("%.6f", center.longitude))
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
        ) {
            Icon(Icons.Outlined.MyLocation, null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ExploreMapSimple(
    complaints: List<com.civicresolve.ap.data.model.Complaint>,
    scope: String,
    onSelect: (com.civicresolve.ap.data.model.Complaint) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val valid = complaints.filter { it.latitude.toDoubleOrNull() != null && it.longitude.toDoubleOrNull() != null && it.latitude != "0" }
    if (valid.isEmpty()) {
        Box(modifier = modifier.height(520.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF18181B)), contentAlignment = Alignment.Center) {
            Text("No surveillance dots in view. Adjust filters or switch to Global.", color = Color(0xFFA1A1AA), fontSize = 12.sp)
        }
        return
    }
    val avgLat = valid.mapNotNull { it.latitude.toDoubleOrNull() }.average()
    val avgLng = valid.mapNotNull { it.longitude.toDoubleOrNull() }.average()
    val zoom = if (scope == "MY_DISTRICT") 12.0 else 7.0

    AndroidView(
        modifier = modifier.height(520.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Color(0xFF3F3F46), RoundedCornerShape(12.dp)),
        factory = {
            MapView(context).apply {
                setTileSource(darkSource)
                setMultiTouchControls(true)
                controller.setZoom(zoom)
                controller.setCenter(GeoPoint(avgLat, avgLng))
            }
        },
        update = { map ->
            // Rebuild markers on every data change (refresh, district/global
            // toggle) — the factory block only runs once
            map.overlays.clear()
            valid.forEach { c ->
                val m = Marker(map)
                m.position = GeoPoint(c.latitude.toDouble(), c.longitude.toDouble())
                m.title = c.description.take(40)
                m.setOnMarkerClickListener { _, _ -> onSelect(c); true }
                map.overlays.add(m)
            }
            map.controller.setCenter(GeoPoint(avgLat, avgLng))
            map.controller.setZoom(zoom)
            map.invalidate()
        }
    )
}
