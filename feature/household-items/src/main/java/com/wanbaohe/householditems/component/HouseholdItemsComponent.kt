package com.wanbaohe.householditems.component

import com.arkivanov.decompose.ComponentContext
import com.shifenmiao.database.household.repo.HouseholdRepository
import com.t8rin.imagetoolbox.core.domain.coroutines.DispatchersHolder
import com.t8rin.imagetoolbox.core.ui.utils.BaseComponent
import com.t8rin.imagetoolbox.core.ui.utils.navigation.Screen
import com.wanbaohe.householditems.model.ExpiryStatus
import com.wanbaohe.householditems.model.HouseholdItemUi
import com.wanbaohe.householditems.model.HouseholdItemsUiState
import com.wanbaohe.householditems.model.HouseholdLocationUi
import com.wanbaohe.householditems.model.buildLocationTree
import com.wanbaohe.householditems.service.HouseholdService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 家庭物品主页 Component — 仅做 UI 状态编排。
 *
 * - 写操作全部走 [HouseholdService](AI 工具同样走 Service,共享业务/审计日志)
 * - 表单状态机由 [ItemEditor] 接管
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HouseholdItemsComponent @AssistedInject internal constructor(
    @Assisted componentContext: ComponentContext,
    @Assisted val onGoBack: () -> Unit,
    @Assisted val onNavigate: (Screen) -> Unit,
    dispatchersHolder: DispatchersHolder,
    private val repository: HouseholdRepository,
    private val service: HouseholdService,
) : BaseComponent(dispatchersHolder, componentContext) {

    private val _uiState = MutableStateFlow(HouseholdItemsUiState())
    val uiState: StateFlow<HouseholdItemsUiState> = _uiState

    private val itemEditor = ItemEditor()
    private val searchQuery = MutableStateFlow("")

    init {
        observeLocations()
        observeItems()
    }

    // ─────────── 搜索 / 浏览 ───────────

    fun onSearchQueryChange(value: String) {
        searchQuery.value = value
        _uiState.update { it.copy(searchQuery = value) }
    }

    fun enterLocation(locationId: String) {
        _uiState.update { it.copy(currentLocationId = locationId) }
    }

    fun navigateToBreadcrumb(locationId: String?) {
        _uiState.update { it.copy(currentLocationId = locationId) }
    }

    // ─────────── 物品表单 ───────────

    fun startAddItem() {
        _uiState.update { itemEditor.startAdd(it, it.currentLocationId) }
    }

    fun startEditItem(item: HouseholdItemUi) {
        _uiState.update { itemEditor.startEdit(it, item) }
    }

    fun hideItemEditor() {
        _uiState.update { itemEditor.hide(it) }
    }

    fun onEditorNameChange(value: String) = _uiState.update { itemEditor.changeName(it, value) }
    fun onEditorCategoryChange(value: String) = _uiState.update { itemEditor.changeCategory(it, value) }
    fun onEditorLocationChange(locationId: String?) = _uiState.update { itemEditor.changeLocation(it, locationId) }
    fun onEditorExpireDateChange(date: LocalDate?) = _uiState.update { itemEditor.changeExpireDate(it, date) }
    fun onEditorPhotoPicked(uri: String) = _uiState.update { itemEditor.changePhoto(it, uri) }
    fun onEditorPhotoRemoved() = _uiState.update { itemEditor.removePhoto(it) }
    fun onEditorNoteChange(value: String) = _uiState.update { itemEditor.changeNote(it, value) }

    fun submitItem() {
        val state = _uiState.value
        val input = itemEditor.buildInput(state) ?: return
        val editingId = state.editingItemId
        componentScope.launch {
            val result = if (editingId == null) {
                service.addItem(input, HouseholdService.ACTOR_USER, HouseholdService.SOURCE_UI)
                    .map { }
            } else {
                service.updateItem(editingId, input, HouseholdService.ACTOR_USER, HouseholdService.SOURCE_UI)
            }
            result.onSuccess { hideItemEditor() }
        }
    }

    fun deleteItem(itemId: String) {
        componentScope.launch {
            service.deleteItem(itemId, HouseholdService.ACTOR_USER, HouseholdService.SOURCE_UI)
        }
    }

    // ─────────── 位置管理 ───────────

    fun openLocationManager() = _uiState.update { it.copy(showLocationManager = true) }

    fun closeLocationManager() = _uiState.update {
        it.copy(showLocationManager = false, locationDeleteBlocked = false)
    }

    fun consumeLocationDeleteBlocked() = _uiState.update { it.copy(locationDeleteBlocked = false) }

    fun addLocation(name: String, parentId: String?) {
        if (name.isBlank()) return
        componentScope.launch {
            service.addLocation(name, parentId, HouseholdService.ACTOR_USER, HouseholdService.SOURCE_UI)
        }
    }

    fun renameLocation(locationId: String, newName: String) {
        if (newName.isBlank()) return
        componentScope.launch {
            service.renameLocation(locationId, newName, HouseholdService.ACTOR_USER, HouseholdService.SOURCE_UI)
        }
    }

    fun deleteLocation(locationId: String) {
        componentScope.launch {
            service.deleteLocation(locationId, HouseholdService.ACTOR_USER, HouseholdService.SOURCE_UI)
                .onFailure {
                    _uiState.update { state -> state.copy(locationDeleteBlocked = true) }
                }
        }
    }

    // ─────────── 内部 ───────────

    private fun observeLocations() {
        repository.observeLocations()
            .onEach { list ->
                val ui = list.map { HouseholdLocationUi(id = it.id, name = it.name, parentId = it.parentId) }
                _uiState.update { state ->
                    state.copy(
                        locations = ui,
                        locationTree = buildLocationTree(ui),
                        // 当前位置被删除时回到根
                        currentLocationId = state.currentLocationId
                            ?.takeIf { id -> ui.any { it.id == id } },
                    )
                }
            }
            .launchIn(componentScope)
    }

    private fun observeItems() {
        searchQuery
            .flatMapLatest { query ->
                if (query.isBlank()) repository.observeItems() else repository.search(query.trim())
            }
            .map { items ->
                items.map { item ->
                    val expireDate = item.expireAt?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    val daysToExpire = expireDate?.let {
                        ChronoUnit.DAYS.between(LocalDate.now(), it)
                    }
                    HouseholdItemUi(
                        id = item.id,
                        name = item.name,
                        category = item.category.orEmpty(),
                        locationId = item.locationId,
                        locationPath = repository.buildLocationPath(item.locationId),
                        expireDate = expireDate,
                        photoPath = item.photoPath,
                        note = item.note.orEmpty(),
                        expiryStatus = expiryStatusOf(daysToExpire),
                        daysToExpire = daysToExpire,
                    )
                }
            }
            .onEach { list ->
                _uiState.update { state ->
                    state.copy(
                        items = list,
                        expiredItems = list.filter { it.expiryStatus == ExpiryStatus.EXPIRED },
                        expiringSoonItems = list.filter { it.expiryStatus == ExpiryStatus.EXPIRING_SOON },
                    )
                }
            }
            .launchIn(componentScope)
    }

    private fun expiryStatusOf(daysToExpire: Long?): ExpiryStatus = when {
        daysToExpire == null -> ExpiryStatus.NONE
        daysToExpire < 0 -> ExpiryStatus.EXPIRED
        daysToExpire <= EXPIRING_SOON_DAYS -> ExpiryStatus.EXPIRING_SOON
        else -> ExpiryStatus.FRESH
    }

    @AssistedFactory
    fun interface Factory {
        operator fun invoke(
            componentContext: ComponentContext,
            onGoBack: () -> Unit,
            onNavigate: (Screen) -> Unit,
        ): HouseholdItemsComponent
    }

    private companion object {
        const val EXPIRING_SOON_DAYS = 30L
    }
}
