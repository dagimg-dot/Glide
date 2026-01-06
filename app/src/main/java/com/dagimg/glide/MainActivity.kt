package com.dagimg.glide

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dagimg.glide.service.ClipboardService
import com.dagimg.glide.service.GlideAccessibilityService
import com.dagimg.glide.ui.theme.GlideTheme

class MainActivity : ComponentActivity() {
    private var isServiceEnabled by mutableStateOf(false)
    private var hasOverlayPermission by mutableStateOf(false)
    private var hasAccessibilityPermission by mutableStateOf(false)
    private var hasNotificationPermission by mutableStateOf(false)

    private val overlayPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) {
            checkPermissions()
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) {
            checkPermissions()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Load saved state
        val prefs = getSharedPreferences("glide_prefs", Context.MODE_PRIVATE)
        isServiceEnabled = prefs.getBoolean("service_enabled", false)

        setContent {
            GlideTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MainScreen(
                        isEnabled = isServiceEnabled,
                        hasOverlayPermission = hasOverlayPermission,
                        hasAccessibilityPermission = hasAccessibilityPermission,
                        hasNotificationPermission = hasNotificationPermission,
                        onToggle = { toggleService() },
                        onRequestOverlayPermission = { requestOverlayPermission() },
                        onRequestAccessibilityPermission = { openAccessibilitySettings() },
                        onRequestNotificationPermission = { requestNotificationPermission() },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun checkPermissions() {
        hasOverlayPermission = Settings.canDrawOverlays(this)
        hasAccessibilityPermission = GlideAccessibilityService.isRunning()
        hasNotificationPermission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
    }

    private fun toggleService() {
        if (!isServiceEnabled) {
            // Check required permissions before enabling
            if (!hasOverlayPermission) {
                requestOverlayPermission()
                return
            }
            if (!hasAccessibilityPermission) {
                openAccessibilitySettings()
                return
            }

            // Start service
            ClipboardService.start(this)
            isServiceEnabled = true
        } else {
            // Stop service
            ClipboardService.stop(this)
            isServiceEnabled = false
        }

        // Save state
        getSharedPreferences("glide_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("service_enabled", isServiceEnabled)
            .apply()
    }

    private fun requestOverlayPermission() {
        val intent =
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            )
        overlayPermissionLauncher.launch(intent)
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
fun MainScreen(
    isEnabled: Boolean,
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    hasNotificationPermission: Boolean,
    onToggle: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
) {
    val allPermissionsGranted = hasOverlayPermission && hasAccessibilityPermission

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color(0xFF0D0D0D),
                                    Color(0xFF1A1A1A),
                                ),
                        ),
                ).padding(24.dp)
                .statusBarsPadding(),
    ) {
        // Header
        Text(
            text = "Glide",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )

        Text(
            text = "Clipboard Edge Panel",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Main Toggle Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = if (isEnabled) Color(0xFF1E3A2F) else Color(0xFF1E1E1E),
                ),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Edge Panel",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Text(
                        text = if (isEnabled) "Running" else "Disabled",
                        fontSize = 14.sp,
                        color = if (isEnabled) Color(0xFF4ADE80) else Color.Gray,
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { onToggle() },
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF4ADE80),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF2A2A2A),
                        ),
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Permissions Section
        Text(
            text = "Required Permissions",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        // Overlay Permission
        PermissionCard(
            title = "Display Over Apps",
            description = "Required to show the edge panel",
            isGranted = hasOverlayPermission,
            onClick = onRequestOverlayPermission,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Accessibility Permission
        PermissionCard(
            title = "Accessibility Service",
            description = "Required for clipboard monitoring on Android 10+",
            isGranted = hasAccessibilityPermission,
            onClick = onRequestAccessibilityPermission,
        )

        // Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Spacer(modifier = Modifier.height(8.dp))

            PermissionCard(
                title = "Notifications",
                description = "Show service running notification",
                isGranted = hasNotificationPermission,
                onClick = onRequestNotificationPermission,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Status indicator
        if (!allPermissionsGranted) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = Color(0xFF3D2E1E),
                    ),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Grant all permissions to enable Glide",
                        color = Color(0xFFFBBF24),
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isGranted) Color(0xFF1E2E1E) else Color(0xFF1E1E1E),
        label = "bg",
    )

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                )
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color.Gray,
                )
            }

            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isGranted) {
                                Color(0xFF4ADE80).copy(alpha = 0.2f)
                            } else {
                                Color.Gray.copy(alpha = 0.2f)
                            },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF4ADE80) else Color.Gray,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
