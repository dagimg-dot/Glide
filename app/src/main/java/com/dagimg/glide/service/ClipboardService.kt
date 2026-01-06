package com.dagimg.glide.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.dagimg.glide.MainActivity
import com.dagimg.glide.R
import com.dagimg.glide.data.ClipboardRepository
import com.dagimg.glide.overlay.ClipboardPanelView
import com.dagimg.glide.overlay.EdgeHandleView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that manages the clipboard listener and overlay views.
 * Runs continuously when enabled to capture clipboard changes and show edge panel.
 */
class ClipboardService : Service() {
    companion object {
        private const val TAG = "ClipboardService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "glide_service_channel"

        /**
         * Start the clipboard service
         */
        fun start(context: Context) {
            val intent = Intent(context, ClipboardService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stop the clipboard service
         */
        fun stop(context: Context) {
            context.stopService(Intent(context, ClipboardService::class.java))
        }

        /**
         * Check if service is running
         */
        fun isRunning(context: Context): Boolean {
            val prefs = context.getSharedPreferences("glide_prefs", Context.MODE_PRIVATE)
            return prefs.getBoolean("service_enabled", false)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var repository: ClipboardRepository
    private lateinit var windowManager: WindowManager
    private lateinit var clipboardManager: ClipboardManager

    private var edgeHandle: EdgeHandleView? = null
    private var clipboardPanel: ClipboardPanelView? = null

    private val clipboardListener =
        ClipboardManager.OnPrimaryClipChangedListener {
            handleClipboardChange()
        }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")

        repository = ClipboardRepository(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // Register clipboard listener
        clipboardManager.addPrimaryClipChangedListener(clipboardListener)

        // Create overlay views
        createEdgeHandle()
        createClipboardPanel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        Log.d(TAG, "Service onStartCommand")

        // Create notification channel and start foreground
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        // Save enabled state
        getSharedPreferences("glide_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("service_enabled", true)
            .apply()

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy")

        // Unregister clipboard listener
        clipboardManager.removePrimaryClipChangedListener(clipboardListener)

        // Remove overlay views
        removeEdgeHandle()
        removeClipboardPanel()

        // Save disabled state
        getSharedPreferences("glide_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("service_enabled", false)
            .apply()

        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Handle clipboard content change
     */
    private fun handleClipboardChange() {
        val clip = clipboardManager.primaryClip ?: return
        if (clip.itemCount == 0) return

        val item = clip.getItemAt(0)

        serviceScope.launch {
            // Try to get text content
            val text = item.text?.toString()
            if (!text.isNullOrBlank()) {
                repository.addText(text)
                Log.d(TAG, "Captured text: ${text.take(50)}...")
                return@launch
            }

            // Try to get image content
            val uri = item.uri
            if (uri != null) {
                try {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                        if (bitmap != null) {
                            repository.addImage(bitmap)
                            Log.d(TAG, "Captured image from URI")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to capture image", e)
                }
            }
        }
    }

    /**
     * Create the edge handle overlay view
     */
    private fun createEdgeHandle() {
        edgeHandle =
            EdgeHandleView(this) {
                // Toggle panel visibility
                togglePanel()
            }

        val params =
            WindowManager
                .LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT,
                ).apply {
                    gravity = Gravity.END or Gravity.CENTER_VERTICAL
                    x = 0
                    y = 0
                }

        try {
            windowManager.addView(edgeHandle, params)
            Log.d(TAG, "Edge handle added")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add edge handle", e)
        }
    }

    /**
     * Create the clipboard panel overlay view (initially hidden)
     */
    private fun createClipboardPanel() {
        clipboardPanel =
            ClipboardPanelView(this, repository) {
                // Close panel callback
                hidePanel()
            }
        clipboardPanel?.visibility = android.view.View.GONE

        val displayMetrics = resources.displayMetrics
        val panelWidth = (displayMetrics.widthPixels * 0.85).toInt()

        val params =
            WindowManager
                .LayoutParams(
                    panelWidth,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT,
                ).apply {
                    gravity = Gravity.END
                    x = 0
                    y = 0
                }

        try {
            windowManager.addView(clipboardPanel, params)
            Log.d(TAG, "Clipboard panel added (hidden)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add clipboard panel", e)
        }
    }

    /**
     * Toggle panel visibility
     */
    private fun togglePanel() {
        if (clipboardPanel?.visibility == android.view.View.VISIBLE) {
            hidePanel()
        } else {
            showPanel()
        }
    }

    /**
     * Show the clipboard panel with animation
     */
    fun showPanel() {
        clipboardPanel?.apply {
            visibility = android.view.View.VISIBLE
            // Animate slide in from right
            translationX = width.toFloat()
            animate()
                .translationX(0f)
                .setDuration(250)
                .start()
        }
        // Hide edge handle while panel is open
        edgeHandle?.visibility = android.view.View.GONE
    }

    /**
     * Hide the clipboard panel with animation
     */
    fun hidePanel() {
        clipboardPanel?.apply {
            animate()
                .translationX(width.toFloat())
                .setDuration(200)
                .withEndAction {
                    visibility = android.view.View.GONE
                }.start()
        }
        // Show edge handle again
        edgeHandle?.visibility = android.view.View.VISIBLE
    }

    /**
     * Remove edge handle from window
     */
    private fun removeEdgeHandle() {
        edgeHandle?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        edgeHandle = null
    }

    /**
     * Remove clipboard panel from window
     */
    private fun removeClipboardPanel() {
        clipboardPanel?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        clipboardPanel = null
    }

    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = getString(R.string.notification_channel_description)
                    setShowBadge(false)
                }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Create the foreground service notification
     */
    private fun createNotification(): Notification {
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_edit) // TODO: Replace with proper icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}
