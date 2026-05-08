package com.example.complaintportal.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.complaintportal.data.model.Complaint
import com.example.complaintportal.ui.theme.MorphingStatusBadge
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.res.stringResource
import com.example.complaintportal.R

enum class MapScope {
    MY_REPORTS,
    MY_DISTRICT,
    GLOBAL_FEED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OsmDashboardMap(
    complaints: List<Complaint>,
    onComplaintClick: (String) -> Unit,
    userLocation: GeoPoint? = null,
    scope: MapScope = MapScope.MY_REPORTS,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var selectedComplaint by remember { mutableStateOf<Complaint?>(null) }
    val sheetState = rememberModalBottomSheetState()
    
    // Non-reactive flag: won't trigger recomposition when set, but persists across recompositions
    val centered = remember { booleanArrayOf(false) }
    // Track last scope to detect tab switches
    val lastScope = remember { arrayOf(scope) }

    // CartoDB Voyager — reliable OSM-based tiles, no blocking policy, no API key needed
    val cartoTiles = remember {
        XYTileSource(
            "CartoDB",
            0, 19, 256, ".png",
            arrayOf(
                "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
                "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
                "https://c.basemaps.cartocdn.com/rastertiles/voyager/"
            ),
            "\u00a9 OpenStreetMap contributors \u00a9 CARTO"
        )
    }

    // Remember MapView instance to survive recomposition
    val mapView = remember {
        val config = org.osmdroid.config.Configuration.getInstance()
        config.load(context, context.getSharedPreferences("osmdroid", 0))
        config.userAgentValue = "CivicResolver/1.0 (com.example.complaintportal)"
        // NOTE: Do NOT delete tile cache here — it prevents downloaded tiles from being stored

        MapView(context).apply {
            setTileSource(cartoTiles)
            setMultiTouchControls(true)
            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
            
            minZoomLevel = 10.0
            maxZoomLevel = 19.0
            controller.setZoom(13.0)

            // Prevent parent Column/Scaffold from stealing vertical drag events
            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    android.view.MotionEvent.ACTION_DOWN,
                    android.view.MotionEvent.ACTION_MOVE ->
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                    android.view.MotionEvent.ACTION_UP,
                    android.view.MotionEvent.ACTION_CANCEL ->
                        v.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false // false = let MapView still handle the event normally
            }
        }
    }

    // Lifecycle handling
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Intercept ALL scroll/fling events so the parent Column never scrolls the map up/down
    val mapNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                available // consume everything
            override suspend fun onPreFling(available: Velocity): Velocity =
                available // consume fling too
        }
    }

    Box(modifier = modifier.fillMaxSize().clipToBounds().nestedScroll(mapNestedScrollConnection)) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { mv ->
                mv.overlays.clear()
                
                val validComplaints = complaints.filter { 
                    val lat = it.latitude.toDoubleOrNull()
                    val lng = it.longitude.toDoubleOrNull()
                    lat != null && lng != null && 
                    lat in 6.0..38.0 && lng in 68.0..98.0
                }

                validComplaints.forEach { complaint ->
                    val lat = complaint.latitude.toDouble()
                    val lng = complaint.longitude.toDouble()
                    
                    val marker = Marker(mv)
                    marker.position = GeoPoint(lat, lng)
                    marker.title = complaint.category.replace("_", " ").replaceFirstChar { it.uppercase() }
                    marker.snippet = "${complaint.city}, ${complaint.state}"
                    
                    val markerColor = when (complaint.status.lowercase()) {
                        "new" -> android.graphics.Color.RED
                        "in_progress" -> android.graphics.Color.parseColor("#FFB74D")
                        "resolved" -> android.graphics.Color.parseColor("#81C784")
                        else -> android.graphics.Color.GRAY
                    }
                    
                    marker.icon?.setTint(markerColor)
                    
                    marker.setOnMarkerClickListener { m, _ ->
                        selectedComplaint = complaint
                        m.showInfoWindow()
                        mv.controller.animateTo(m.position, 16.0, 1000L)
                        true
                    }
                    mv.overlays.add(marker)
                }

                // Center on data centroid once complaints arrive (non-reactive flag avoids recomposition loop)
                if (!centered[0] && validComplaints.isNotEmpty()) {
                    val centerLat = validComplaints.map { it.latitude.toDouble() }.average()
                    val centerLng = validComplaints.map { it.longitude.toDouble() }.average()
                    val targetZoom = when (scope) {
                        MapScope.MY_REPORTS -> 13.0
                        MapScope.MY_DISTRICT -> 13.5
                        MapScope.GLOBAL_FEED -> 11.5
                    }
                    mv.controller.setCenter(GeoPoint(centerLat, centerLng))
                    mv.controller.setZoom(targetZoom)
                    centered[0] = true
                }

                // Handle scope tab switch — re-zoom without re-centering
                if (centered[0] && lastScope[0] != scope) {
                    val targetZoom = when (scope) {
                        MapScope.MY_REPORTS -> 13.0
                        MapScope.MY_DISTRICT -> 13.5
                        MapScope.GLOBAL_FEED -> 11.5
                    }
                    mv.controller.zoomTo(targetZoom)
                    lastScope[0] = scope
                }

                mv.invalidate()
            }
        )

        // Overlay chip showing count
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${complaints.size} issues visible",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (selectedComplaint != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedComplaint = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            selectedComplaint?.let { complaint ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = complaint.category.replace("_", " ").replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${complaint.landmark}, ${complaint.city}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        MorphingStatusBadge(status = complaint.status)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (!complaint.beforeImageUrl.isNullOrBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(complaint.beforeImageUrl),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    Text(
                        text = complaint.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            onComplaintClick(complaint.id)
                            selectedComplaint = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.view_details), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
