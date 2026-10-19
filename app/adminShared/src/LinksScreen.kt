package com.usuallycoding.portfolio.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usuallycoding.portfolio.ImageDto
import com.usuallycoding.portfolio.SocialLinkDto
import com.usuallycoding.portfolio.SocialLinkInput
import com.usuallycoding.portfolio.ui.ErrorView
import com.usuallycoding.portfolio.ui.Load
import com.usuallycoding.portfolio.ui.LoadingView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** The social links, in the order visitors see them. Every change is saved to the server straight away. */
class LinksViewModel(private val session: AdminSession) : ViewModel() {
    var loadState by mutableStateOf<Load<List<SocialLinkDto>>>(Load.Loading)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        load()
    }

    private val links: List<SocialLinkDto> get() = (loadState as? Load.Loaded)?.value.orEmpty()

    fun load() {
        loadState = Load.Loading
        viewModelScope.launch {
            loadState = try {
                Load.Loaded(session.call { adminLinks() })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Load.Failed(e.adminMessage())
            }
        }
    }

    fun clearError() {
        error = null
    }

    fun move(index: Int, delta: Int) {
        val reordered = links.moved(index, delta)
        if (reordered === links) return
        run { reorderLinks(reordered.map { it.id }); adminLinks() }
    }

    fun save(id: Long?, input: SocialLinkInput, onDone: () -> Unit = {}) = run(onDone) {
        if (id == null) createLink(input) else updateLink(id, input)
        adminLinks()
    }

    fun delete(id: Long) = run { deleteLink(id); adminLinks() }

    /** Runs a change that ends by returning the fresh list of links, and shows that list. */
    private fun run(onDone: () -> Unit = {}, block: suspend com.usuallycoding.portfolio.PortfolioApi.() -> List<SocialLinkDto>) {
        if (busy) return
        busy = true
        error = null
        viewModelScope.launch {
            try {
                loadState = Load.Loaded(session.call(block))
                onDone()
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

private class LinkDraft(val id: Long?, name: String, address: String, isPublic: Boolean, icon: ImageDto?) {
    var name by mutableStateOf(name)
    var address by mutableStateOf(address)
    var isPublic by mutableStateOf(isPublic)
    var icon by mutableStateOf(icon)

    fun toInput() = SocialLinkInput(name, address, isPublic, icon?.id)
}

@Composable
fun LinksScreen(vm: LinksViewModel, modifier: Modifier = Modifier) {
    when (val state = vm.loadState) {
        Load.Loading -> LoadingView(modifier)
        is Load.Failed -> ErrorView(state.message, onRetry = vm::load, modifier = modifier)
        is Load.Loaded -> LinksList(vm, state.value, modifier)
    }
}

@Composable
private fun LinksList(vm: LinksViewModel, links: List<SocialLinkDto>, modifier: Modifier) {
    var editing by remember { mutableStateOf<LinkDraft?>(null) }
    var deleting by remember { mutableStateOf<SocialLinkDto?>(null) }

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Social links", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Visitors see public links in this order.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = { editing = LinkDraft(null, "", "https://", false, null) }) { Text("Add link") }
            }
            vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (links.isEmpty()) {
                Text("No links yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(links, key = { _, link -> link.id }) { index, link ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AdminImage(link.icon, Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)), description = "${link.name} icon")
                            Column(Modifier.weight(1f)) {
                                Text(link.name, style = MaterialTheme.typography.titleMedium)
                                Text(link.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            Text(
                                if (link.isPublic) "Public" else "Private",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (link.isPublic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(onClick = { vm.move(index, -1) }, enabled = index > 0 && !vm.busy) { Text("Up") }
                            TextButton(onClick = { vm.move(index, +1) }, enabled = index < links.lastIndex && !vm.busy) { Text("Down") }
                            TextButton(onClick = { editing = LinkDraft(link.id, link.name, link.address, link.isPublic, link.icon) }) { Text("Edit") }
                            TextButton(onClick = { deleting = link }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }

    editing?.let { draft ->
        AlertDialog(
            onDismissRequest = { editing = null; vm.clearError() },
            title = { Text(if (draft.id == null) "Add link" else "Edit link") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(draft.name, { draft.name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        draft.address, { draft.address = it }, label = { Text("Address") }, singleLine = true,
                        supportingText = { Text("http, https, mailto or tel") }, modifier = Modifier.fillMaxWidth(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Switch(draft.isPublic, { draft.isPublic = it })
                        Text(if (draft.isPublic) "Public: visitors can see it" else "Private: only you can see it")
                    }
                    ImageField("Icon", CropAspect.Square, draft.icon, onChange = { draft.icon = it }, previewWidth = 96.dp)
                    vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.save(draft.id, draft.toInput()) { editing = null } }, enabled = !vm.busy) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null; vm.clearError() }) { Text("Cancel") } },
        )
    }

    deleting?.let { link ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete \"${link.name}\"?") },
            text = { Text("This removes the link. It cannot be undone.") },
            confirmButton = { TextButton(onClick = { vm.delete(link.id); deleting = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}
