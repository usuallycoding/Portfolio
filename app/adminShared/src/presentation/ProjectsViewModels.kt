package com.usuallycoding.portfolio.admin.presentation

import com.usuallycoding.portfolio.admin.domain.AdminRepository
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usuallycoding.portfolio.ImageDto
import com.usuallycoding.portfolio.ProjectDto
import com.usuallycoding.portfolio.ProjectInput
import com.usuallycoding.portfolio.ProjectSummaryDto
import com.usuallycoding.portfolio.ui.Load
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Which project the editor is open for; [id] is null for a new project. [generation] makes every opening a fresh editor. */
data class EditorTarget(val id: Long?, val generation: Int)

/** The ordered project list, and which project (if any) is open for editing. */
class ProjectsViewModel(private val repository: AdminRepository) : ViewModel() {
    var loadState by mutableStateOf<Load<List<ProjectSummaryDto>>>(Load.Loading)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    /** Kept here, not in the screen, so an open editor survives a re-login. */
    var editor by mutableStateOf<EditorTarget?>(null)
        private set
    private var generation = 0

    init {
        load()
    }

    fun load() {
        loadState = Load.Loading
        viewModelScope.launch {
            loadState = try {
                Load.Loaded(repository.projects())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Load.Failed(e.adminMessage())
            }
        }
    }

    fun open(id: Long?) {
        editor = EditorTarget(id, ++generation)
    }

    fun closeEditor() {
        editor = null
        load()
    }

    fun move(index: Int, delta: Int) {
        val current = (loadState as? Load.Loaded)?.value ?: return
        val reordered = current.moved(index, delta)
        if (reordered === current || busy) return
        busy = true
        error = null
        viewModelScope.launch {
            try {
                loadState = Load.Loaded(repository.reorderProjects(reordered.map { it.id }))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.adminMessage()
            } finally {
                busy = false
            }
        }
    }
}

/**
 * Edits one project. Image uploads happen as files are chosen (see [queue]); nothing is attached to the
 * project until [save], so abandoning the form leaves the project untouched.
 */
class ProjectEditorViewModel(private val repository: AdminRepository, projectId: Long?) : ViewModel() {
    /** The project being edited; set after the first save of a new project, so saving again updates it. */
    var projectId: Long? = projectId
        private set

    var loadState by mutableStateOf<Load<Unit>>(if (projectId == null) Load.Loaded(Unit) else Load.Loading)
        private set

    var title by mutableStateOf("")
    var createdAt by mutableStateOf(if (projectId == null) todayIso() else "")
    var isPublic by mutableStateOf(false)
    var summary by mutableStateOf("")
    var thumbnail by mutableStateOf<ImageDto?>(null)

    var saving by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var messageIsError by mutableStateOf(false)
        private set

    val queue = UploadQueue(
        scope = viewModelScope,
        concurrency = 3,
        prepare = { name, bytes ->
            withContext(Dispatchers.Default) {
                val source = ImageProcessing.decode(bytes) ?: throw NotAnImageException(name)
                ImageProcessing.prepare(source)
            }
        },
        upload = { name, image, onProgress ->
            repository.uploadImage(image.bytes, "${name.substringBeforeLast('.')}.${image.extension}", image.contentType) { sent, total ->
                if (total != null && total > 0) onProgress((sent.toFloat() / total).coerceIn(0f, 1f))
            }
        },
    )

    val dateIsValid: Boolean get() = isValidIsoDate(createdAt)
    val canSave: Boolean get() = title.isNotBlank() && dateIsValid && !queue.isBusy && !saving

    init {
        if (projectId != null) load()
    }

    fun load() {
        val id = projectId ?: return
        loadState = Load.Loading
        viewModelScope.launch {
            try {
                apply(repository.project(id))
                loadState = Load.Loaded(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                loadState = Load.Failed(e.adminMessage())
            }
        }
    }

    fun addFiles(files: List<PendingFile>) = queue.enqueue(files)

    fun save(onSaved: () -> Unit) {
        if (!canSave) return
        saving = true
        message = null
        viewModelScope.launch {
            try {
                val input = ProjectInput(
                    title = title,
                    createdAt = createdAt,
                    isPublic = isPublic,
                    summary = summary,
                    thumbnailId = thumbnail?.id,
                    imageIds = queue.uploadedImages().map { it.id },
                )
                val id = projectId
                val saved = if (id == null) repository.createProject(input) else repository.updateProject(id, input)
                projectId = saved.id
                // Keep the gallery as it is: failed or still-running uploads must survive a save so they can be retried.
                apply(saved, includeImages = false)
                message = "Saved."
                messageIsError = false
                onSaved()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = e.adminMessage()
                messageIsError = true
            } finally {
                saving = false
            }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = projectId ?: return
        if (saving) return
        saving = true
        viewModelScope.launch {
            try {
                repository.deleteProject(id)
                onDeleted()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = e.adminMessage()
                messageIsError = true
            } finally {
                saving = false
            }
        }
    }

    private fun apply(project: ProjectDto, includeImages: Boolean = true) {
        title = project.title
        createdAt = project.createdAt
        isPublic = project.isPublic
        summary = project.summary
        thumbnail = project.thumbnail
        if (includeImages) queue.setExisting(project.images)
    }
}
