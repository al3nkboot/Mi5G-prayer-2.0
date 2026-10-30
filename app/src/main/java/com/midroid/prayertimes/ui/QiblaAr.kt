package com.midroid.prayertimes.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.midroid.prayertimes.R
import com.midroid.prayertimes.domain.Place
import com.midroid.prayertimes.domain.Qibla
import kotlin.math.abs
import kotlin.math.roundToInt

/** Camera overlay for the Qibla. Nothing is recorded or sent. */
@Composable
fun QiblaAr(place: Place, qibla: Double, heading: Heading?, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val hasCamera = remember { ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    Column(modifier.testTag("qibla_ar")) {
        if (!hasCamera) {
            Text(stringResource(R.string.qibla_ar_no_camera), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            return@Column
        }
        if (!granted) {
            Text(stringResource(R.string.qibla_ar_need), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Button(onClick = { ask.launch(Manifest.permission.CAMERA) }, modifier = Modifier.padding(top = 12.dp).testTag("qibla_ar_grant")) {
                Text(stringResource(R.string.qibla_ar_grant))
            }
            return@Column
        }
        val h = heading
        val facing = h != null && Qibla.isFacing(qibla, h.trueDeg)
        val delta = if (h == null) 0.0 else Qibla.delta(qibla, h.trueDeg)
        Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f)) {
            AndroidView(
                factory = { CameraPreview(it) },
                modifier = Modifier.matchParentSize(),
                onRelease = { it.release() },
            )
            Canvas(Modifier.matchParentSize()) {
                val cx = size.width / 2f + (delta / 40.0).coerceIn(-1.0, 1.0).toFloat() * size.width * 0.42f
                val cy = size.height * 0.42f
                drawLine(Color.White, Offset(size.width / 2f, size.height * 0.92f), Offset(cx, cy), 6f)
                drawCircle(if (facing) Gold else Color.White, 28f, Offset(cx, cy), style = Stroke(6f))
                drawRect(Color(0xFF1B1C1A), Offset(cx - 10f, cy - 10f), androidx.compose.ui.geometry.Size(20f, 20f))
            }
        }
        val line = when {
            h == null -> stringResource(R.string.qibla_waiting)
            facing -> stringResource(R.string.qibla_facing)
            else -> stringResource(R.string.qibla_turn, abs(delta).roundToInt())
        }
        Text(line, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Text(stringResource(R.string.qibla_ar_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        if (place.lat.isNaN()) Text("")
    }
}

private class CameraPreview(ctx: Context) : TextureView(ctx), TextureView.SurfaceTextureListener {
    private val thread = HandlerThread("qibla-cam").also { it.start() }
    private val handler = Handler(thread.looper)
    private var released = false
    private var device: CameraDevice? = null
    private var session: CameraCaptureSession? = null

    init { surfaceTextureListener = this }

    fun release() {
        if (released) return
        released = true
        runCatching { session?.close() }
        runCatching { device?.close() }
        session = null
        device = null
        thread.quitSafely()
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) = open()
    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = Unit
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    private fun open() {
        val mgr = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = runCatching { mgr.cameraIdList.firstOrNull() }.getOrNull() ?: return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return
        runCatching {
            mgr.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    device = camera
                    val st = surfaceTexture ?: return
                    st.setDefaultBufferSize(width.coerceAtLeast(1), height.coerceAtLeast(1))
                    val surface = Surface(st)
                    runCatching {
                        camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(s: CameraCaptureSession) {
                                session = s
                                val req = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply { addTarget(surface) }
                                runCatching { s.setRepeatingRequest(req.build(), null, handler) }
                            }
                            override fun onConfigureFailed(s: CameraCaptureSession) = Unit
                        }, handler)
                    }
                }
                override fun onDisconnected(camera: CameraDevice) { camera.close() }
                override fun onError(camera: CameraDevice, error: Int) { camera.close() }
            }, handler)
        }
    }
}
