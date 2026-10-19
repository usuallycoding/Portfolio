package com.usuallycoding.portfolio.admin.presentation

import com.usuallycoding.portfolio.admin.domain.AdminRepository
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usuallycoding.portfolio.ImageDto
import com.usuallycoding.portfolio.ProfileDto
import com.usuallycoding.portfolio.ProfileInput
import com.usuallycoding.portfolio.ui.ErrorView
import com.usuallycoding.portfolio.ui.Load
import com.usuallycoding.portfolio.ui.LoadingView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Edits the profile. Images are uploaded as soon as they are chosen but only attached when saved. */
class ProfileViewModel(private val repository: AdminRepository) : ViewModel() {
    var loadState by mutableStateOf<Load<Unit>>(Load.Loading)
        private set

    var name by mutableStateOf("")
    var description by mutableStateOf("")
    var profileImage by mutableStateOf<ImageDto?>(null)
    var coverImage by mutableStateOf<ImageDto?>(null)

    var saving by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var messageIsError by mutableStateOf(false)
        private set

    private var saved: ProfileDto? = null

    val hasChanges: Boolean
        get() {
            val s = saved ?: return false
            return name != s.name || description != s.description || profileImage != s.profileImage || coverImage != s.coverImage
        }

    init {
        load()
    }

    fun load() {
        loadState = Load.Loading
        viewModelScope.launch {
            try {
                apply(repository.profile())
                loadState = Load.Loaded(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                loadState = Load.Failed(e.adminMessage())
            }
        }
    }

    fun discard() {
        saved?.let(::apply)
        message = null
    }

    fun save() {
        if (saving) return
        saving = true
        message = null
        viewModelScope.launch {
            try {
                val result = repository.updateProfile(ProfileInput(name, description, profileImage?.id, coverImage?.id))
                apply(result)
                message = "Saved."
                messageIsError = false
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

    private fun apply(profile: ProfileDto) {
        saved = profile
        name = profile.name
        description = profile.description
        profileImage = profile.profileImage
        coverImage = profile.coverImage
    }
}

@Composable
fun ProfileScreen(vm: ProfileViewModel, modifier: Modifier = Modifier) {
    when (val state = vm.loadState) {
        Load.Loading -> LoadingView(modifier)
        is Load.Failed -> ErrorView(state.message, onRetry = vm::load, modifier = modifier)
        is Load.Loaded -> ProfileForm(vm, modifier)
    }
}

@Composable
private fun ProfileForm(vm: ProfileViewModel, modifier: Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("Profile", style = MaterialTheme.typography.headlineSmall)
            Text(
                "The profile is always public. Visitors see placeholders until you fill it in.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = vm.name,
                onValueChange = { vm.name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = vm.description,
                onValueChange = { vm.description = it },
                label = { Text("Description") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            ImageField("Profile picture", CropAspect.Square, vm.profileImage, onChange = { vm.profileImage = it })
            ImageField("Cover image", CropAspect.Cover, vm.coverImage, onChange = { vm.coverImage = it }, previewWidth = 360.dp)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = vm::save, enabled = !vm.saving) { Text(if (vm.saving) "Saving…" else "Save") }
                OutlinedButton(onClick = vm::discard, enabled = vm.hasChanges && !vm.saving) { Text("Discard changes") }
                vm.message?.let {
                    Text(it, color = if (vm.messageIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }
            }
            if (vm.hasChanges && vm.message == null) {
                Text("You have unsaved changes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
