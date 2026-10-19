package com.usuallycoding.portfolio.admin.presentation

import com.usuallycoding.portfolio.admin.domain.AdminRepository
import org.koin.compose.koinInject
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.usuallycoding.portfolio.ProjectSummaryDto
import com.usuallycoding.portfolio.ui.ErrorView
import com.usuallycoding.portfolio.ui.Load
import com.usuallycoding.portfolio.ui.LoadingView
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes

/** The projects section: the ordered list, or the editor for one project. */
@Composable
fun ProjectsScreen(vm: ProjectsViewModel, modifier: Modifier = Modifier) {
    val target = vm.editor
    if (target == null) {
        ProjectsList(vm, modifier)
    } else {
        val repository = koinInject<AdminRepository>()
        val editor = viewModel(key = "project-editor-${target.generation}") { ProjectEditorViewModel(repository, target.id) }
        ProjectEditorScreen(editor, onClose = vm::closeEditor, modifier = modifier)
    }
}

@Composable
private fun ProjectsList(vm: ProjectsViewModel, modifier: Modifier) {
    when (val state = vm.loadState) {
        Load.Loading -> LoadingView(modifier)
        is Load.Failed -> ErrorView(state.message, onRetry = vm::load, modifier = modifier)
        is Load.Loaded -> Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            val projects = state.value
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Projects", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Visitors see public projects in this order.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(onClick = { vm.open(null) }) { Text("New project") }
                }
                vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (projects.isEmpty()) Text("No projects yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(projects, key = { _, p -> p.id }) { index, project ->
                        ProjectRow(
                            project,
                            canMoveUp = index > 0 && !vm.busy,
                            canMoveDown = index < projects.lastIndex && !vm.busy,
                            onUp = { vm.move(index, -1) },
                            onDown = { vm.move(index, +1) },
                            onEdit = { vm.open(project.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectRow(
    project: ProjectSummaryDto,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onEdit: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AdminImage(project.thumbnail, Modifier.width(96.dp).aspectRatio(16f / 9f), description = "${project.title} thumbnail")
            Column(Modifier.weight(1f)) {
                Text(project.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(project.createdAt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                if (project.isPublic) "Public" else "Private",
                style = MaterialTheme.typography.labelMedium,
                color = if (project.isPublic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onUp, enabled = canMoveUp) { Text("Up") }
            TextButton(onClick = onDown, enabled = canMoveDown) { Text("Down") }
            TextButton(onClick = onEdit) { Text("Edit") }
        }
    }
}

@Composable
fun ProjectEditorScreen(vm: ProjectEditorViewModel, onClose: () -> Unit, modifier: Modifier = Modifier) {
    when (val state = vm.loadState) {
        Load.Loading -> LoadingView(modifier)
        is Load.Failed -> Column(modifier) {
            TextButton(onClick = onClose) { Text("Back to projects") }
            ErrorView(state.message, onRetry = vm::load)
        }
        is Load.Loaded -> ProjectEditorForm(vm, onClose, modifier)
    }
}

@Composable
private fun ProjectEditorForm(vm: ProjectEditorViewModel, onClose: () -> Unit, modifier: Modifier) {
    var confirmDelete by remember { mutableStateOf(false) }
    val items by vm.queue.items.collectAsState()

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 840.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onClose) { Text("Back to projects") }
            }
            Text(if (vm.projectId == null) "New project" else "Edit project", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(vm.title, { vm.title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                vm.createdAt, { vm.createdAt = it },
                label = { Text("Creation date") }, singleLine = true,
                isError = !vm.dateIsValid,
                supportingText = { Text(if (vm.dateIsValid) "yyyy-MM-dd" else "Use the format yyyy-MM-dd, e.g. 2026-01-31") },
                modifier = Modifier.width(240.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Switch(vm.isPublic, { vm.isPublic = it })
                Text(if (vm.isPublic) "Public: visitors can see this project" else "Private: only you can see this project")
            }
            OutlinedTextField(vm.summary, { vm.summary = it }, label = { Text("Summary") }, minLines = 3, modifier = Modifier.fillMaxWidth())

            ImageField("Thumbnail", CropAspect.Thumbnail, vm.thumbnail, onChange = { vm.thumbnail = it }, previewWidth = 320.dp)

            GalleryEditor(vm, items)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { vm.save { } }, enabled = vm.canSave) { Text(if (vm.saving) "Saving…" else "Save") }
                if (vm.projectId != null) {
                    OutlinedButton(onClick = { confirmDelete = true }, enabled = !vm.saving) {
                        Text("Delete project", color = MaterialTheme.colorScheme.error)
                    }
                }
                vm.message?.let { Text(it, color = if (vm.messageIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }
            }
            if (vm.queue.isBusy) Text("Waiting for image uploads to finish before saving…", style = MaterialTheme.typography.bodySmall)
            if (vm.queue.failedCount > 0) {
                Text(
                    "${vm.queue.failedCount} image(s) failed to upload and will not be saved. Retry or remove them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete \"${vm.title}\"?") },
            text = { Text("This removes the project and its image list. It cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.delete(onClose) }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GalleryEditor(vm: ProjectEditorViewModel, items: List<UploadItem>) {
    val picker = rememberFilePickerLauncher(type = FileKitType.Image, mode = FileKitMode.Multiple()) { files ->
        if (!files.isNullOrEmpty()) vm.addFiles(files.map { file -> PendingFile(file.name) { file.readBytes() } })
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Images", style = MaterialTheme.typography.titleMedium)
            Text("${items.size} in gallery", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(onClick = { picker.launch() }) { Text("Add images…") }
        Text(
            "Select as many as you like. They upload a few at a time; use ← and → to set their order.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items.forEachIndexed { index, item ->
                GalleryCard(
                    item = item,
                    isFirst = index == 0,
                    isLast = index == items.lastIndex,
                    onEarlier = { vm.queue.move(item.key, -1) },
                    onLater = { vm.queue.move(item.key, +1) },
                    onRemove = { vm.queue.remove(item.key) },
                    onRetry = { vm.queue.retry(item.key) },
                )
            }
        }
    }
}

@Composable
private fun GalleryCard(
    item: UploadItem,
    isFirst: Boolean,
    isLast: Boolean,
    onEarlier: () -> Unit,
    onLater: () -> Unit,
    onRemove: () -> Unit,
    onRetry: () -> Unit,
) {
    Card(Modifier.width(150.dp)) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.BottomCenter) {
                AdminImage(item.image, Modifier.matchParentSize(), description = item.name)
                when (val status = item.status) {
                    UploadStatus.Queued -> Text("Waiting…", style = MaterialTheme.typography.labelSmall)
                    UploadStatus.Preparing -> Text("Preparing…", style = MaterialTheme.typography.labelSmall)
                    is UploadStatus.Uploading -> LinearProgressIndicator(progress = { status.progress }, modifier = Modifier.fillMaxWidth())
                    is UploadStatus.Failed, UploadStatus.Done -> {}
                }
            }
            Text(item.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            (item.status as? UploadStatus.Failed)?.let { failed ->
                Text(failed.message, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, maxLines = 3)
                TextButton(onClick = onRetry) { Text("Retry") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onEarlier, enabled = !isFirst) { Text("←") }
                TextButton(onClick = onLater, enabled = !isLast) { Text("→") }
            }
            TextButton(onClick = onRemove, modifier = Modifier.fillMaxWidth()) { Text("Remove", color = MaterialTheme.colorScheme.error, maxLines = 1) }
        }
    }
}
