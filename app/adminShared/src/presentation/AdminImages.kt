package com.usuallycoding.portfolio.admin.presentation

import com.usuallycoding.portfolio.admin.domain.AdminRepository
import org.koin.compose.koinInject
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.usuallycoding.portfolio.ImageDto
import com.usuallycoding.portfolio.ui.PortfolioImage
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** An image stored on the server, shown through the session so private and staged images load too. */
@Composable
fun AdminImage(image: ImageDto?, modifier: Modifier = Modifier, thumbnail: Boolean = true, description: String? = null) {
    val repository = koinInject<AdminRepository>()
    PortfolioImage(
        url = image?.let { repository.imageUrl(it.fileName, thumbnail) },
        contentDescription = description,
        modifier = modifier,
    )
}

/** The crop frame with zoom and drag. The image is drawn so exactly the area that will be uploaded is visible. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CropPanel(source: SourceImage, aspect: CropAspect, state: CropState, modifier: Modifier = Modifier) {
    val bitmap = remember(source) { source.image.toComposeImageBitmap() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val frameWidth = minOf(maxWidth, 520.dp)
            Box(
                Modifier
                    .width(frameWidth)
                    .aspectRatio(aspect.width.toFloat() / aspect.height)
                    .clipToBounds()
                    .border(2.dp, MaterialTheme.colorScheme.primary)
                    .pointerInput(state) {
                        detectTransformGestures { _, pan, zoomChange, _ ->
                            val pixelsPerSourcePixel = size.width / state.cropWidth
                            state.zoomBy(zoomChange)
                            state.panBy(-pan.x / pixelsPerSourcePixel, -pan.y / pixelsPerSourcePixel)
                        }
                    }
                    .onPointerEvent(PointerEventType.Scroll) { event ->
                        val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                        state.zoomBy(if (delta < 0) 1.1f else 1f / 1.1f)
                    },
            ) {
                Canvas(Modifier.matchParentSize()) {
                    val r = state.rect()
                    drawImage(
                        image = bitmap,
                        srcOffset = IntOffset(r.left.toInt(), r.top.toInt()),
                        srcSize = IntSize(r.width.toInt().coerceAtLeast(1), r.height.toInt().coerceAtLeast(1)),
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                    )
                }
            }
        }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Zoom", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = state.zoom,
                onValueChange = state::zoomTo,
                valueRange = CropState.MIN_ZOOM..CropState.MAX_ZOOM,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            )
            TextButton(onClick = state::reset) { Text("Reset") }
        }
        Text(
            "Drag to move, scroll or pinch to zoom. The framed ${aspect.label} area is what gets uploaded.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun CropDialog(source: SourceImage, aspect: CropAspect, onConfirm: (CropRect) -> Unit, onCancel: () -> Unit) {
    val state = remember(source, aspect) { CropState(source.width, source.height, aspect) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Crop image (${aspect.label})") },
        text = { CropPanel(source, aspect, state) },
        confirmButton = { TextButton(onClick = { onConfirm(state.rect()) }) { Text("Crop & upload") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    )
}

/**
 * A single image slot (profile picture, cover, icon, thumbnail): shows the current image, lets the owner
 * pick a file, crop it to [aspect], and uploads the result. [onChange] receives the new image or null on removal.
 */
@Composable
fun ImageField(
    label: String,
    aspect: CropAspect,
    image: ImageDto?,
    onChange: (ImageDto?) -> Unit,
    modifier: Modifier = Modifier,
    previewWidth: androidx.compose.ui.unit.Dp = 160.dp,
) {
    val repository = koinInject<AdminRepository>()
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf<SourceImage?>(null) }
    var fileName by remember { mutableStateOf("image") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberFilePickerLauncher(type = FileKitType.Image) { file ->
        if (file != null) {
            error = null
            scope.launch {
                try {
                    val bytes = file.readBytes()
                    fileName = file.name.substringBeforeLast('.')
                    source = ImageProcessing.decode(bytes) ?: run { error = "That file is not an image this app can read."; null }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    error = "Could not read that file."
                }
            }
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Box(
            Modifier.width(previewWidth).aspectRatio(aspect.width.toFloat() / aspect.height)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            AdminImage(image, Modifier.matchParentSize(), description = label)
            if (busy) CircularProgressIndicator()
        }
        Text("Required ratio ${aspect.label}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { picker.launch() }, enabled = !busy) { Text(if (image == null) "Choose image…" else "Replace…") }
            if (image != null) TextButton(onClick = { onChange(null) }, enabled = !busy) { Text("Remove") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }

    source?.let { decoded ->
        CropDialog(
            source = decoded,
            aspect = aspect,
            onCancel = { source = null },
            onConfirm = { crop ->
                source = null
                busy = true
                error = null
                scope.launch {
                    try {
                        val prepared = withContext(Dispatchers.Default) { ImageProcessing.prepare(decoded, crop, aspect) }
                        val uploaded = repository.uploadImage(prepared.bytes, "$fileName.${prepared.extension}", prepared.contentType)
                        onChange(uploaded)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        error = e.adminMessage()
                    } finally {
                        busy = false
                    }
                }
            },
        )
    }
}
