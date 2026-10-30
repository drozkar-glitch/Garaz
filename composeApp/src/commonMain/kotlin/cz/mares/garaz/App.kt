package cz.mares.garaz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import kotlin.random.Random

private data class GaragePalette(
    val isDark: Boolean,
    val bg: Color,
    val card: Color,
    val cardSub: Color,
    val border: Color,
    val accent: Color,
    val greenOk: Color,
    val greenBg: Color,
    val amberWarn: Color,
    val redAlert: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val onAccent: Color
)

private val DarkPalette = GaragePalette(
    isDark = true,
    bg = Color(0xFF0F172A),
    card = Color(0xFF1E293B),
    cardSub = Color(0xFF152033),
    border = Color(0xFF334155),
    accent = Color(0xFF38BDF8),
    greenOk = Color(0xFF10B981),
    greenBg = Color(0xFF064E3B),
    amberWarn = Color(0xFFF59E0B),
    redAlert = Color(0xFFEF4444),
    textPrimary = Color(0xFFF8FAFC),
    textMuted = Color(0xFF94A3B8),
    onAccent = Color(0xFF0F172A)
)

private val LightPalette = GaragePalette(
    isDark = false,
    bg = Color(0xFFF1F5F9),
    card = Color(0xFFFFFFFF),
    cardSub = Color(0xFFF8FAFC),
    border = Color(0xFFCBD5E1),
    accent = Color(0xFF0284C7),
    greenOk = Color(0xFF059669),
    greenBg = Color(0xFFD1FAE5),
    amberWarn = Color(0xFFD97706),
    redAlert = Color(0xFFDC2626),
    textPrimary = Color(0xFF0F172A),
    textMuted = Color(0xFF475569),
    onAccent = Color(0xFFFFFFFF)
)

private val LocalGaragePalette = compositionLocalOf { DarkPalette }

private data class ServiceAlertItem(
    val vehicle: Vehicle,
    val part: PartItem,
    val lastRecord: ReplacementRecord,
    val remainingKm: Int
)

private fun computeAlertsForVehicle(vehicle: Vehicle): List<ServiceAlertItem> {
    val allParts = GarageCatalog.allPartsForVehicle(vehicle)
    val alerts = mutableListOf<ServiceAlertItem>()
    for (part in allParts) {
        if (part.defaultIntervalKm <= 0) continue
        val lastRec = vehicle.records
            .filter { it.partId == part.id }
            .maxByOrNull { it.mileageKm } ?: continue
        val driven = (vehicle.currentMileageKm - lastRec.mileageKm).coerceAtLeast(0)
        val remaining = part.defaultIntervalKm - driven
        if (remaining <= 3000) {
            alerts.add(ServiceAlertItem(vehicle, part, lastRec, remaining))
        }
    }
    return alerts.sortedBy { it.remainingKm }
}

private fun notifyIfServiceDue(vehicles: List<Vehicle>) {
    val allAlerts = vehicles.flatMap { computeAlertsForVehicle(it) }
    if (allAlerts.isEmpty()) return

    val overdueCount = allAlerts.count { it.remainingKm < 0 }
    val title = if (overdueCount > 0) {
        "⚠️ Překročen servisní interval ($overdueCount×)"
    } else {
        "⏱ Blíží se servisní interval (${allAlerts.size}×)"
    }
    val summary = allAlerts.take(4).joinToString("\n") { item ->
        val status = if (item.remainingKm < 0) {
            "překročeno o ${formatKm(-item.remainingKm)}"
        } else {
            "zbývá ${formatKm(item.remainingKm)}"
        }
        "${item.vehicle.name}: ${item.part.name} ($status)"
    }
    GaragePlatform.showServiceNotification(title, summary)
}

@Composable
fun App() {
    val systemDark = isSystemInDarkTheme()
    var themePref by remember {
        mutableStateOf(GaragePlatform.loadString("garaz_theme_mode_v1", "0").toIntOrNull() ?: 0)
    }
    val useDark = when (themePref) {
        1 -> false
        2 -> true
        else -> systemDark
    }
    val palette = if (useDark) DarkPalette else LightPalette

    var vehicles by remember { mutableStateOf(GarageStorage.loadVehicles()) }
    var selectedVehicleId by remember { mutableStateOf(vehicles.firstOrNull()?.id ?: "") }
    val currentVehicle = vehicles.find { it.id == selectedVehicleId } ?: vehicles.firstOrNull()

    var expandedCategoryIds by remember { mutableStateOf(emptySet<String>()) }
    var expandedSubfolderIds by remember { mutableStateOf(emptySet<String>()) }

    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf(0) }

    var partToMarkReplaced by remember { mutableStateOf<PartItem?>(null) }
    var partForHistoryDetail by remember { mutableStateOf<PartItem?>(null) }
    var partForIntervalEdit by remember { mutableStateOf<PartItem?>(null) }
    var showEditVehicleDialog by remember { mutableStateOf(false) }
    var isAddingNewVehicle by remember { mutableStateOf(false) }
    var showQuickMileageDialog by remember { mutableStateOf(false) }
    var showSmartParserDialog by remember { mutableStateOf(false) }
    var showAllHistoryDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var subfolderForCatalogFetch by remember { mutableStateOf<Pair<CategoryDef, SubfolderDef>?>(null) }
    var subfolderForCustomPart by remember { mutableStateOf<Pair<CategoryDef, SubfolderDef>?>(null) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (GarageStorage.checkAndRunWeeklyBackupIfDue(vehicles)) {
            statusBanner = "☁️ Týdenní záloha na Google Drive byla automaticky aktualizována."
        }
        notifyIfServiceDue(vehicles)
    }

    fun updateVehicles(newList: List<Vehicle>, checkNotifications: Boolean = false) {
        vehicles = newList
        GarageStorage.saveVehicles(newList)
        if (GarageStorage.checkAndRunWeeklyBackupIfDue(newList)) {
            statusBanner = "☁️ Týdenní záloha na Google Drive byla automaticky aktualizována."
        }
        if (checkNotifications) {
            notifyIfServiceDue(newList)
        }
    }

    fun updateCurrentVehicle(checkNotifications: Boolean = false, transform: (Vehicle) -> Vehicle) {
        val cur = currentVehicle ?: return
        val updated = vehicles.map { if (it.id == cur.id) transform(it) else it }
        updateVehicles(updated, checkNotifications)
    }

    fun savePartInterval(part: PartItem, newIntervalKm: Int) {
        updateCurrentVehicle(checkNotifications = true) { v ->
            val updatedMap = v.customIntervals.toMutableMap()
            updatedMap[part.id] = newIntervalKm.coerceAtLeast(0)
            val updatedCustomParts = v.customParts.map { cp ->
                if (cp.id == part.id) cp.copy(defaultIntervalKm = newIntervalKm.coerceAtLeast(0)) else cp
            }
            v.copy(
                customIntervals = updatedMap,
                customParts = updatedCustomParts
            )
        }
    }

    val materialColors = if (useDark) {
        darkColorScheme(
            primary = palette.accent,
            background = palette.bg,
            surface = palette.card,
            onPrimary = palette.onAccent,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            background = palette.bg,
            surface = palette.card,
            onPrimary = palette.onAccent,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary
        )
    }

    CompositionLocalProvider(LocalGaragePalette provides palette) {
        MaterialTheme(colorScheme = materialColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.bg)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🚘 GARÁŽ",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.accent,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmallTopButton("📜 Historie") { showAllHistoryDialog = true }
                        SmallTopButton("⚙️ Nastavení") { showSettingsDialog = true }
                    }
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    vehicles.forEach { car ->
                        val isSel = car.id == currentVehicle?.id
                        val carAlerts = remember(car) { computeAlertsForVehicle(car) }
                        val hasOverdue = carAlerts.any { it.remainingKm < 0 }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) palette.accent.copy(alpha = 0.18f) else palette.card)
                                .border(
                                    width = if (isSel) 2.dp else 1.dp,
                                    color = when {
                                        hasOverdue -> palette.redAlert
                                        carAlerts.isNotEmpty() -> palette.amberWarn
                                        isSel -> palette.accent
                                        else -> palette.border
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedVehicleId = car.id }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = car.name,
                                        fontSize = 13.sp,
                                        lineHeight = 16.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) palette.accent else palette.textPrimary
                                    )
                                    if (carAlerts.isNotEmpty()) {
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "⚠ ${carAlerts.size}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (hasOverdue) palette.redAlert else palette.amberWarn
                                        )
                                    }
                                }
                                Text(
                                    text = "${formatKm(car.currentMileageKm)} • ${car.engineCode.ifBlank { "VIN" }}",
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    color = palette.textMuted
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.card)
                            .border(1.dp, palette.border, RoundedCornerShape(10.dp))
                            .clickable {
                                isAddingNewVehicle = true
                                showEditVehicleDialog = true
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "+ Přidat auto",
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false,
                            fontWeight = FontWeight.Bold,
                            color = palette.accent
                        )
                    }
                }

                if (statusBanner != null) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.greenBg)
                            .border(1.dp, palette.greenOk, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(statusBanner ?: "", fontSize = 12.sp, color = palette.textPrimary, modifier = Modifier.weight(1f))
                        Text(
                            "✕",
                            fontSize = 14.sp,
                            color = palette.textPrimary,
                            modifier = Modifier.clickable { statusBanner = null }.padding(start = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (currentVehicle != null) {
                    val allParts = remember(currentVehicle) { GarageCatalog.allPartsForVehicle(currentVehicle) }
                    val replacedPartIds = remember(currentVehicle.records) {
                        currentVehicle.records.map { it.partId }.toSet()
                    }
                    val currentVehicleAlerts = remember(currentVehicle) {
                        computeAlertsForVehicle(currentVehicle)
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            VehicleSummaryCard(
                                vehicle = currentVehicle,
                                replacedCount = replacedPartIds.size,
                                totalPartsCount = allParts.size,
                                onEditVinOrVehicle = {
                                    isAddingNewVehicle = false
                                    showEditVehicleDialog = true
                                },
                                onQuickKmClick = { showQuickMileageDialog = true },
                                onSmartParseClick = { showSmartParserDialog = true }
                            )
                        }

                        if (currentVehicleAlerts.isNotEmpty()) {
                            item {
                                ServiceAlertsBannerCard(
                                    alerts = currentVehicleAlerts,
                                    onMarkReplaced = { part -> partToMarkReplaced = part },
                                    onEditInterval = { part -> partForIntervalEdit = part }
                                )
                            }
                        }

                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(palette.card)
                                    .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = {
                                        Text(
                                            "Hledat díl (čep, tlumič, olej, vstřik...)",
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = palette.textMuted
                                        )
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        if (searchQuery.isNotBlank()) {
                                            Text(
                                                "✕",
                                                color = palette.textMuted,
                                                modifier = Modifier.clickable { searchQuery = "" }.padding(8.dp)
                                            )
                                        }
                                    }
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChipBtn("Všechny díly (${allParts.size})", filterMode == 0) { filterMode = 0 }
                                    FilterChipBtn("✓ Vyměněné (${replacedPartIds.size})", filterMode == 1) { filterMode = 1 }
                                    FilterChipBtn("⏱ Intervaly", filterMode == 2) { filterMode = 2 }
                                    if (currentVehicleAlerts.isNotEmpty()) {
                                        FilterChipBtn("⚠ K výměně (${currentVehicleAlerts.size})", filterMode == 3) { filterMode = 3 }
                                    }
                                }
                            }
                        }

                        if (searchQuery.isNotBlank() || filterMode != 0) {
                            val alertPartIds = currentVehicleAlerts.map { it.part.id }.toSet()
                            val normQuery = GarageCatalog.removeDiacritics(searchQuery.trim().lowercase())
                            val filteredParts = allParts.filter { part ->
                                val matchesSearch = if (normQuery.isBlank()) true else {
                                    val pName = GarageCatalog.removeDiacritics(part.name.lowercase())
                                    val sName = GarageCatalog.removeDiacritics(part.subfolderName.lowercase())
                                    pName.contains(normQuery) || sName.contains(normQuery)
                                }
                                val matchesFilter = when (filterMode) {
                                    1 -> replacedPartIds.contains(part.id)
                                    2 -> part.defaultIntervalKm > 0
                                    3 -> alertPartIds.contains(part.id)
                                    else -> true
                                }
                                matchesSearch && matchesFilter
                            }

                            item {
                                Text(
                                    "Nalezené položky (${filteredParts.size}):",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.accent,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }

                            items(filteredParts, key = { it.id }) { part ->
                                val partRecords = currentVehicle.records
                                    .filter { it.partId == part.id }
                                    .sortedByDescending { it.mileageKm }
                                PartRowCard(
                                    part = part,
                                    vehicleMileage = currentVehicle.currentMileageKm,
                                    records = partRecords,
                                    showCategoryPath = true,
                                    onMarkReplaced = { partToMarkReplaced = part },
                                    onShowHistory = { partForHistoryDetail = part },
                                    onEditInterval = { partForIntervalEdit = part }
                                )
                            }
                        } else {
                            items(GarageCatalog.categories, key = { it.id }) { cat ->
                                val catParts = allParts.filter { it.categoryId == cat.id }
                                val catReplacedCount = catParts.count { replacedPartIds.contains(it.id) }
                                val catAlertsCount = currentVehicleAlerts.count { it.part.categoryId == cat.id }
                                val isCatExpanded = expandedCategoryIds.contains(cat.id)

                                CategoryTreeCard(
                                    category = cat,
                                    allCategoryParts = catParts,
                                    replacedCount = catReplacedCount,
                                    alertsCount = catAlertsCount,
                                    isExpanded = isCatExpanded,
                                    expandedSubfolderIds = expandedSubfolderIds,
                                    vehicle = currentVehicle,
                                    onToggleCategory = {
                                        expandedCategoryIds = if (isCatExpanded) {
                                            expandedCategoryIds - cat.id
                                        } else {
                                            expandedCategoryIds + cat.id
                                        }
                                    },
                                    onToggleSubfolder = { subId ->
                                        expandedSubfolderIds = if (expandedSubfolderIds.contains(subId)) {
                                            expandedSubfolderIds - subId
                                        } else {
                                            expandedSubfolderIds + subId
                                        }
                                    },
                                    onMarkPartReplaced = { part -> partToMarkReplaced = part },
                                    onShowPartHistory = { part -> partForHistoryDetail = part },
                                    onEditPartInterval = { part -> partForIntervalEdit = part },
                                    onFetchExtraParts = { sub -> subfolderForCatalogFetch = cat to sub },
                                    onAddCustomPart = { sub -> subfolderForCustomPart = cat to sub }
                                )
                            }
                        }

                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }

            val targetPart = partToMarkReplaced
            if (targetPart != null && currentVehicle != null) {
                val latestPart = GarageCatalog.allPartsForVehicle(currentVehicle).find { it.id == targetPart.id } ?: targetPart
                MarkPartReplacedDialog(
                    part = latestPart,
                    vehicle = currentVehicle,
                    onDismiss = { partToMarkReplaced = null },
                    onConfirm = { km, date, brand, note, intervalKm ->
                        val newRec = ReplacementRecord(
                            id = "rec_${Random.nextInt(100000, 999999)}",
                            partId = latestPart.id,
                            date = date,
                            mileageKm = km,
                            brandOrSpec = brand,
                            note = note
                        )
                        updateCurrentVehicle(checkNotifications = true) { v ->
                            val updatedKm = maxOf(v.currentMileageKm, km)
                            val updatedIntervals = v.customIntervals.toMutableMap()
                            updatedIntervals[latestPart.id] = intervalKm.coerceAtLeast(0)
                            val updatedCustomParts = v.customParts.map { cp ->
                                if (cp.id == latestPart.id) cp.copy(defaultIntervalKm = intervalKm.coerceAtLeast(0)) else cp
                            }
                            v.copy(
                                currentMileageKm = updatedKm,
                                records = listOf(newRec) + v.records,
                                customIntervals = updatedIntervals,
                                customParts = updatedCustomParts
                            )
                        }
                        statusBanner = "✓ Označeno jako vyměněné: ${latestPart.name} (${formatKm(km)})"
                        partToMarkReplaced = null
                    }
                )
            }

            val historyPart = partForHistoryDetail
            if (historyPart != null && currentVehicle != null) {
                val latestPart = GarageCatalog.allPartsForVehicle(currentVehicle).find { it.id == historyPart.id } ?: historyPart
                PartHistoryDialog(
                    part = latestPart,
                    vehicle = currentVehicle,
                    onDismiss = { partForHistoryDetail = null },
                    onAddAnother = {
                        partForHistoryDetail = null
                        partToMarkReplaced = latestPart
                    },
                    onSaveInterval = { newIntervalKm ->
                        savePartInterval(latestPart, newIntervalKm)
                        statusBanner = if (newIntervalKm > 0) {
                            "⏱ Interval pro „${latestPart.name}“ nastaven na ${formatKm(newIntervalKm)}."
                        } else {
                            "⏱ Hlídání intervalu pro „${latestPart.name}“ bylo vypnuto."
                        }
                    },
                    onDeleteRecord = { recId ->
                        updateCurrentVehicle { v ->
                            v.copy(records = v.records.filterNot { it.id == recId })
                        }
                    }
                )
            }

            val intervalPart = partForIntervalEdit
            if (intervalPart != null && currentVehicle != null) {
                val latestPart = GarageCatalog.allPartsForVehicle(currentVehicle).find { it.id == intervalPart.id } ?: intervalPart
                EditPartIntervalDialog(
                    part = latestPart,
                    onDismiss = { partForIntervalEdit = null },
                    onSave = { newIntervalKm ->
                        savePartInterval(latestPart, newIntervalKm)
                        partForIntervalEdit = null
                        statusBanner = if (newIntervalKm > 0) {
                            "⏱ Interval pro „${latestPart.name}“ nastaven na ${formatKm(newIntervalKm)}."
                        } else {
                            "⏱ Hlídání intervalu pro „${latestPart.name}“ bylo vypnuto."
                        }
                    }
                )
            }

            if (showEditVehicleDialog) {
                val editingCar = if (isAddingNewVehicle) {
                    Vehicle(
                        id = "car_${Random.nextInt(10000, 99999)}",
                        name = "Nové vozidlo",
                        vin = "",
                        plate = "",
                        year = 2018,
                        engineCode = "",
                        engineDesc = "",
                        powerKw = 110,
                        gearbox = "DSG / Manuál",
                        color = "",
                        stkUntil = "",
                        tiresInfo = "",
                        currentMileageKm = 150000
                    )
                } else {
                    currentVehicle
                }

                if (editingCar != null) {
                    EditVehicleVinDialog(
                        initialVehicle = editingCar,
                        isNew = isAddingNewVehicle,
                        canDelete = vehicles.size > 1 && !isAddingNewVehicle,
                        onDismiss = { showEditVehicleDialog = false },
                        onSave = { savedCar ->
                            if (isAddingNewVehicle) {
                                val newList = vehicles + savedCar
                                updateVehicles(newList, checkNotifications = true)
                                selectedVehicleId = savedCar.id
                            } else {
                                updateVehicles(
                                    vehicles.map { if (it.id == savedCar.id) savedCar else it },
                                    checkNotifications = true
                                )
                            }
                            showEditVehicleDialog = false
                            statusBanner = "Karta vozidla ${savedCar.name} uložena."
                        },
                        onDelete = {
                            val remaining = vehicles.filterNot { it.id == editingCar.id }
                            updateVehicles(remaining)
                            selectedVehicleId = remaining.firstOrNull()?.id ?: ""
                            showEditVehicleDialog = false
                        }
                    )
                }
            }

            if (showQuickMileageDialog && currentVehicle != null) {
                QuickMileageDialog(
                    currentKm = currentVehicle.currentMileageKm,
                    vehicleName = currentVehicle.name,
                    onDismiss = { showQuickMileageDialog = false },
                    onSave = { newKm ->
                        updateCurrentVehicle(checkNotifications = true) { it.copy(currentMileageKm = newKm) }
                        showQuickMileageDialog = false
                        statusBanner = "Stav tachometru aktualizován na ${formatKm(newKm)}."
                    }
                )
            }

            if (showSmartParserDialog && currentVehicle != null) {
                SmartServiceParserDialog(
                    vehicle = currentVehicle,
                    onDismiss = { showSmartParserDialog = false },
                    onConfirmMultiple = { selectedParts, km, date, brand, note ->
                        val newRecords = selectedParts.map { p ->
                            ReplacementRecord(
                                id = "rec_${Random.nextInt(100000, 999999)}_${p.id}",
                                partId = p.id,
                                date = date,
                                mileageKm = km,
                                brandOrSpec = brand,
                                note = note
                            )
                        }
                        updateCurrentVehicle(checkNotifications = true) { v ->
                            v.copy(
                                currentMileageKm = maxOf(v.currentMileageKm, km),
                                records = newRecords + v.records
                            )
                        }
                        showSmartParserDialog = false
                        statusBanner = "✓ Hromadně zapsáno ${newRecords.size} vyměněných položek (${formatKm(km)})."
                    }
                )
            }

            val fetchTarget = subfolderForCatalogFetch
            if (fetchTarget != null && currentVehicle != null) {
                CatalogPartsPickerDialog(
                    category = fetchTarget.first,
                    subfolder = fetchTarget.second,
                    existingParts = GarageCatalog.allPartsForVehicle(currentVehicle),
                    onDismiss = { subfolderForCatalogFetch = null },
                    onAddSelectedParts = { partNames, markImmediately ->
                        val createdParts = partNames.map { pName ->
                            PartItem(
                                id = "cust_${Random.nextInt(100000, 999999)}",
                                categoryId = fetchTarget.first.id,
                                categoryName = fetchTarget.first.title,
                                subfolderId = fetchTarget.second.id,
                                subfolderName = fetchTarget.second.title,
                                name = pName,
                                isCustom = true
                            )
                        }
                        val autoRecords = if (markImmediately) {
                            createdParts.map { cp ->
                                ReplacementRecord(
                                    id = "rec_${Random.nextInt(100000, 999999)}",
                                    partId = cp.id,
                                    date = GaragePlatform.currentDateString(),
                                    mileageKm = currentVehicle.currentMileageKm,
                                    brandOrSpec = "",
                                    note = "Označeno při načtení z katalogu"
                                )
                            }
                        } else emptyList()

                        updateCurrentVehicle { v ->
                            v.copy(
                                customParts = v.customParts + createdParts,
                                records = autoRecords + v.records
                            )
                        }
                        subfolderForCatalogFetch = null
                        statusBanner = "Přidáno ${createdParts.size} dílů do složky ${fetchTarget.second.title}."
                    }
                )
            }

            val customTarget = subfolderForCustomPart
            if (customTarget != null && currentVehicle != null) {
                AddCustomPartDialog(
                    category = customTarget.first,
                    subfolder = customTarget.second,
                    onDismiss = { subfolderForCustomPart = null },
                    onConfirm = { name, pos, intervalKm, spec, markNow ->
                        val newPart = PartItem(
                            id = "cust_${Random.nextInt(100000, 999999)}",
                            categoryId = customTarget.first.id,
                            categoryName = customTarget.first.title,
                            subfolderId = customTarget.second.id,
                            subfolderName = customTarget.second.title,
                            name = name,
                            positionTag = pos,
                            defaultIntervalKm = intervalKm,
                            specHint = spec,
                            isCustom = true
                        )
                        val newRecs = if (markNow) {
                            listOf(
                                ReplacementRecord(
                                    id = "rec_${Random.nextInt(100000, 999999)}",
                                    partId = newPart.id,
                                    date = GaragePlatform.currentDateString(),
                                    mileageKm = currentVehicle.currentMileageKm
                                )
                            )
                        } else emptyList()

                        updateCurrentVehicle { v ->
                            v.copy(
                                customParts = v.customParts + newPart,
                                records = newRecs + v.records
                            )
                        }
                        subfolderForCustomPart = null
                        statusBanner = "Díl „$name“ přidán do složky ${customTarget.second.title}."
                    }
                )
            }

            if (showAllHistoryDialog && currentVehicle != null) {
                AllVehicleHistoryDialog(
                    vehicle = currentVehicle,
                    onDismiss = { showAllHistoryDialog = false },
                    onDeleteRecord = { recId ->
                        updateCurrentVehicle { v ->
                            v.copy(records = v.records.filterNot { it.id == recId })
                        }
                    }
                )
            }

            if (showSettingsDialog) {
                SettingsAndBackupDialog(
                    vehicles = vehicles,
                    currentThemePref = themePref,
                    onThemePrefChange = { newMode ->
                        themePref = newMode
                        GaragePlatform.saveString("garaz_theme_mode_v1", newMode.toString())
                    },
                    onDismiss = { showSettingsDialog = false },
                    onRestoreVehicles = { restored ->
                        updateVehicles(restored, checkNotifications = true)
                        selectedVehicleId = restored.firstOrNull()?.id ?: ""
                        showSettingsDialog = false
                        statusBanner = "Databáze garáže byla úspěšně obnovena (${restored.size} vozidel)."
                    }
                )
            }
        }
    }
}

@Composable
private fun ServiceAlertsBannerCard(
    alerts: List<ServiceAlertItem>,
    onMarkReplaced: (PartItem) -> Unit,
    onEditInterval: (PartItem) -> Unit
) {
    val p = LocalGaragePalette.current
    val hasOverdue = alerts.any { it.remainingKm < 0 }
    val borderCol = if (hasOverdue) p.redAlert else p.amberWarn
    val headerCol = if (hasOverdue) p.redAlert else p.amberWarn

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(borderCol.copy(alpha = if (p.isDark) 0.16f else 0.12f))
            .border(1.5.dp, borderCol, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = if (hasOverdue) {
                "🚨 Upozornění: Překročený servisní interval (${alerts.size})"
            } else {
                "⚠️ Blíží se pravidelný servis (${alerts.size})"
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = headerCol
        )

        alerts.forEach { item ->
            val isOver = item.remainingKm < 0
            val itemCol = if (isOver) p.redAlert else p.amberWarn
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(p.card)
                    .border(1.dp, itemCol.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 9.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = item.part.name,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = p.textPrimary
                    )
                    Text(
                        text = if (isOver) {
                            "⚠ Překročeno o ${formatKm(-item.remainingKm)}! (interval ${formatKm(item.part.defaultIntervalKm)})"
                        } else {
                            "⏱ Do výměny zbývá: ${formatKm(item.remainingKm)} (interval ${formatKm(item.part.defaultIntervalKm)})"
                        },
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = itemCol,
                        modifier = Modifier.clickable { onEditInterval(item.part) }
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(p.greenOk)
                        .clickable { onMarkReplaced(item.part) }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "✓ Vyměnit",
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false,
                        fontWeight = FontWeight.ExtraBold,
                        color = p.onAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun VehicleSummaryCard(
    vehicle: Vehicle,
    replacedCount: Int,
    totalPartsCount: Int,
    onEditVinOrVehicle: () -> Unit,
    onQuickKmClick: () -> Unit,
    onSmartParseClick: () -> Unit
) {
    val p = LocalGaragePalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(p.card)
            .border(1.dp, p.border, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = vehicle.name,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = p.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(p.cardSub)
                    .border(1.dp, p.accent.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .clickable { onEditVinOrVehicle() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "🔍 VIN / Upravit",
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    softWrap = false,
                    fontWeight = FontWeight.Bold,
                    color = p.accent
                )
            }
        }

        Text(
            text = "VIN: ${vehicle.vin.ifBlank { "nezadáno" }} • SPZ: ${vehicle.plate.ifBlank { "-" }} • STK do: ${vehicle.stkUntil.ifBlank { "-" }}",
            fontSize = 12.sp,
            lineHeight = 15.sp,
            color = p.accent,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Motor: ${vehicle.engineCode} (${vehicle.engineDesc}, ${vehicle.powerKw} kW) • ${vehicle.gearbox}",
            fontSize = 11.sp,
            lineHeight = 14.sp,
            color = p.textMuted,
            modifier = Modifier.fillMaxWidth()
        )
        if (vehicle.tiresInfo.isNotBlank()) {
            Text(
                text = "Pneu a ráfky v TP: ${vehicle.tiresInfo}",
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = p.textMuted,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(p.cardSub)
                    .border(1.dp, p.border, RoundedCornerShape(9.dp))
                    .clickable { onQuickKmClick() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Tachometr (upravit)", fontSize = 10.sp, lineHeight = 12.sp, color = p.textMuted)
                    Text(
                        text = formatKm(vehicle.currentMileageKm),
                        fontSize = 14.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = p.textPrimary
                    )
                }
                Text("✎", fontSize = 15.sp, color = p.accent)
            }

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(p.greenBg)
                    .border(1.dp, p.greenOk.copy(alpha = 0.6f), RoundedCornerShape(9.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Vyměněno dílů", fontSize = 10.sp, lineHeight = 12.sp, color = p.textPrimary)
                Text(
                    text = "$replacedCount / $totalPartsCount",
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = p.greenOk
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(9.dp))
                .background(p.accent.copy(alpha = 0.14f))
                .border(1.dp, p.accent, RoundedCornerShape(9.dp))
                .clickable { onSmartParseClick() }
                .padding(vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⚡ Zapsat větší servis (Rozklíčovat položky z textu)",
                fontSize = 12.sp,
                lineHeight = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold,
                color = p.accent
            )
        }
    }
}

@Composable
private fun CategoryTreeCard(
    category: CategoryDef,
    allCategoryParts: List<PartItem>,
    replacedCount: Int,
    alertsCount: Int,
    isExpanded: Boolean,
    expandedSubfolderIds: Set<String>,
    vehicle: Vehicle,
    onToggleCategory: () -> Unit,
    onToggleSubfolder: (String) -> Unit,
    onMarkPartReplaced: (PartItem) -> Unit,
    onShowPartHistory: (PartItem) -> Unit,
    onEditPartInterval: (PartItem) -> Unit,
    onFetchExtraParts: (SubfolderDef) -> Unit,
    onAddCustomPart: (SubfolderDef) -> Unit
) {
    val p = LocalGaragePalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(p.card)
            .border(
                1.dp,
                when {
                    alertsCount > 0 -> p.amberWarn
                    isExpanded -> p.accent.copy(alpha = 0.6f)
                    else -> p.border
                },
                RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleCategory() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(category.icon, fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = category.title,
                        fontSize = 15.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = p.textPrimary
                    )
                    Text(
                        text = "${category.subfolders.size} podsložek • ${allCategoryParts.size} dílů",
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = p.textMuted
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (alertsCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(p.amberWarn.copy(alpha = 0.2f))
                            .border(1.dp, p.amberWarn, RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "⚠ $alertsCount",
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            fontWeight = FontWeight.ExtraBold,
                            color = p.amberWarn
                        )
                    }
                }
                if (replacedCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(p.greenBg)
                            .border(1.dp, p.greenOk, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "✓ $replacedCount",
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            fontWeight = FontWeight.Bold,
                            color = p.greenOk
                        )
                    }
                }
                Text(if (isExpanded) "▲" else "▼", fontSize = 13.sp, color = p.accent)
            }
        }

        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(p.cardSub)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                category.subfolders.forEach { sub ->
                    val subParts = allCategoryParts.filter { it.subfolderId == sub.id }
                    val subReplaced = subParts.count { part -> vehicle.records.any { it.partId == part.id } }
                    val isSubExpanded = expandedSubfolderIds.contains(sub.id)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(p.card)
                            .border(
                                1.dp,
                                if (isSubExpanded) p.accent.copy(alpha = 0.4f) else p.border,
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleSubfolder(sub.id) }
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text(if (isSubExpanded) "📂" else "📁", fontSize = 14.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = sub.title,
                                    fontSize = 13.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = p.textPrimary
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (subReplaced > 0) "✓ $subReplaced/${subParts.size}" else "${subParts.size} dílů",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    fontWeight = if (subReplaced > 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (subReplaced > 0) p.greenOk else p.textMuted
                                )
                                Text(if (isSubExpanded) "−" else "+", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = p.accent)
                            }
                        }

                        if (isSubExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                subParts.forEach { part ->
                                    val partRecords = vehicle.records
                                        .filter { it.partId == part.id }
                                        .sortedByDescending { it.mileageKm }
                                    PartRowCard(
                                        part = part,
                                        vehicleMileage = vehicle.currentMileageKm,
                                        records = partRecords,
                                        showCategoryPath = false,
                                        onMarkReplaced = { onMarkPartReplaced(part) },
                                        onShowHistory = { onShowPartHistory(part) },
                                        onEditInterval = { onEditPartInterval(part) }
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp, bottom = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(p.cardSub)
                                            .border(1.dp, p.accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .clickable { onFetchExtraParts(sub) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "📥 Načíst další díly z katalogu",
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            fontWeight = FontWeight.Bold,
                                            color = p.accent
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(p.cardSub)
                                            .border(1.dp, p.border, RoundedCornerShape(8.dp))
                                            .clickable { onAddCustomPart(sub) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+ Vlastní díl",
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            fontWeight = FontWeight.Bold,
                                            color = p.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PartRowCard(
    part: PartItem,
    vehicleMileage: Int,
    records: List<ReplacementRecord>,
    showCategoryPath: Boolean,
    onMarkReplaced: () -> Unit,
    onShowHistory: () -> Unit,
    onEditInterval: () -> Unit
) {
    val p = LocalGaragePalette.current
    val lastRec = records.firstOrNull()
    val isReplaced = lastRec != null
    val drivenSinceLast = if (lastRec != null) (vehicleMileage - lastRec.mileageKm).coerceAtLeast(0) else 0
    val remainingKm = if (lastRec != null && part.defaultIntervalKm > 0) {
        part.defaultIntervalKm - drivenSinceLast
    } else null

    val borderColor = when {
        remainingKm != null && remainingKm < 0 -> p.redAlert
        remainingKm != null && remainingKm <= 3000 -> p.amberWarn
        isReplaced -> p.greenOk.copy(alpha = 0.8f)
        else -> p.border
    }

    val bgColor = if (isReplaced) {
        if (p.isDark) p.greenBg.copy(alpha = 0.28f) else p.greenBg.copy(alpha = 0.55f)
    } else {
        p.cardSub
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable {
                if (isReplaced) onShowHistory() else onMarkReplaced()
            }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            if (showCategoryPath) {
                Text(
                    text = "${part.categoryName} → ${part.subfolderName}",
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    color = p.accent
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (part.positionTag.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(p.accent.copy(alpha = 0.18f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(part.positionTag, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = p.accent)
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = part.name,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.textPrimary
                )
            }

            if (part.specHint.isNotBlank()) {
                Text(part.specHint, fontSize = 11.sp, lineHeight = 14.sp, color = p.textMuted)
            }

            Text(
                text = if (part.defaultIntervalKm > 0) {
                    "⏱ Interval: ${formatKm(part.defaultIntervalKm)} (upravit ✎)"
                } else {
                    "⏱ + Nastavit interval (km)"
                },
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = if (part.defaultIntervalKm > 0) FontWeight.SemiBold else FontWeight.Normal,
                color = if (part.defaultIntervalKm > 0) p.accent else p.textMuted,
                modifier = Modifier
                    .clickable { onEditInterval() }
                    .padding(vertical = 2.dp)
            )

            if (lastRec != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "✓ Vyměněno: ${formatKm(lastRec.mileageKm)} (${lastRec.date}) • najeto ${formatKm(drivenSinceLast)}",
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = p.greenOk
                )
                if (lastRec.brandOrSpec.isNotBlank() || lastRec.note.isNotBlank()) {
                    val info = listOf(lastRec.brandOrSpec, lastRec.note).filter { it.isNotBlank() }.joinToString(" – ")
                    Text("Díl / pozn.: $info", fontSize = 11.sp, lineHeight = 14.sp, color = p.textPrimary.copy(alpha = 0.85f))
                }
                if (remainingKm != null) {
                    val statusTxt = if (remainingKm >= 0) {
                        "Do další výměny zbývá: ${formatKm(remainingKm)}"
                    } else {
                        "⚠ Překročen interval o ${formatKm(-remainingKm)}!"
                    }
                    val statusCol = when {
                        remainingKm < 0 -> p.redAlert
                        remainingKm <= 3000 -> p.amberWarn
                        else -> p.accent
                    }
                    Text(statusTxt, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold, color = statusCol)
                }
            }
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isReplaced) p.greenOk else p.accent)
                    .clickable { onMarkReplaced() }
                    .padding(horizontal = 9.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isReplaced) "✓ + Vyměnit" else "✓ Vyměněno",
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false,
                    fontWeight = FontWeight.ExtraBold,
                    color = p.onAccent
                )
            }
            if (records.isNotEmpty()) {
                Text(
                    text = "Historie (${records.size}x)",
                    fontSize = 10.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = p.accent,
                    modifier = Modifier.clickable { onShowHistory() }.padding(2.dp)
                )
            }
        }
    }
}

@Composable
private fun MarkPartReplacedDialog(
    part: PartItem,
    vehicle: Vehicle,
    onDismiss: () -> Unit,
    onConfirm: (km: Int, date: String, brand: String, note: String, intervalKm: Int) -> Unit
) {
    val p = LocalGaragePalette.current
    var kmText by remember { mutableStateOf(vehicle.currentMileageKm.toString()) }
    var dateText by remember { mutableStateOf(GaragePlatform.currentDateString()) }
    var intervalText by remember {
        mutableStateOf(if (part.defaultIntervalKm > 0) part.defaultIntervalKm.toString() else "")
    }
    var brandText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(p.card)
                .border(1.dp, p.accent, RoundedCornerShape(16.dp))
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("✓ Označit díl jako vyměněný", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = p.greenOk)
            Text("${part.categoryName} → ${part.subfolderName}", fontSize = 11.sp, color = p.accent)
            Text(part.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = p.textPrimary)
            if (part.specHint.isNotBlank()) {
                Text("Specifikace: ${part.specHint}", fontSize = 12.sp, color = p.textMuted)
            }

            OutlinedTextField(
                value = kmText,
                onValueChange = { kmText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Stav tachometru při výměně (km)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Datum výměny") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = intervalText,
                    onValueChange = { intervalText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Interval (km, 0=vyp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = brandText,
                onValueChange = { brandText = it },
                label = { Text("Značka / Kód dílu (volitelné, např. MANN, Textar, OEM)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Poznámka (volitelné)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Zrušit", color = p.textMuted)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val km = kmText.toIntOrNull() ?: vehicle.currentMileageKm
                        val intervalKm = intervalText.toIntOrNull() ?: 0
                        onConfirm(
                            km,
                            dateText.ifBlank { GaragePlatform.currentDateString() },
                            brandText.trim(),
                            noteText.trim(),
                            intervalKm
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = p.greenOk)
                ) {
                    Text("✓ Uložit výměnu", color = p.onAccent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EditPartIntervalDialog(
    part: PartItem,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    val p = LocalGaragePalette.current
    var intervalText by remember {
        mutableStateOf(if (part.defaultIntervalKm > 0) part.defaultIntervalKm.toString() else "")
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(p.card)
                .border(1.dp, p.accent, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("⏱ Nastavit servisní interval", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = p.accent)
            Text(part.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = p.textPrimary)
            Text(
                "Zadej po kolika kilometrech se má tento díl měnit (nebo zadej 0 pro vypnutí hlídání intervalu):",
                fontSize = 11.sp,
                color = p.textMuted
            )

            OutlinedTextField(
                value = intervalText,
                onValueChange = { intervalText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Servisní interval v km (např. 15000, 60000, 180000)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(10000, 15000, 30000, 60000, 120000, 150000, 180000, 0).forEach { preset ->
                    val label = if (preset == 0) "Vypnout (0)" else "${preset / 1000} tis."
                    FilterChipBtn(label, intervalText == preset.toString()) {
                        intervalText = preset.toString()
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Zrušit", color = p.textMuted) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onSave(intervalText.toIntOrNull() ?: 0) },
                    colors = ButtonDefaults.buttonColors(containerColor = p.accent)
                ) {
                    Text("Uložit interval", color = p.onAccent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PartHistoryDialog(
    part: PartItem,
    vehicle: Vehicle,
    onDismiss: () -> Unit,
    onAddAnother: () -> Unit,
    onSaveInterval: (Int) -> Unit,
    onDeleteRecord: (String) -> Unit
) {
    val p = LocalGaragePalette.current
    val records = vehicle.records.filter { it.partId == part.id }.sortedByDescending { it.mileageKm }
    var intervalText by remember(part.defaultIntervalKm) {
        mutableStateOf(if (part.defaultIntervalKm > 0) part.defaultIntervalKm.toString() else "")
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(p.card)
                .border(1.dp, p.border, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(part.name, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = p.textPrimary)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = intervalText,
                    onValueChange = { intervalText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Servisní interval (km, 0 = bez intervalu)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { onSaveInterval(intervalText.toIntOrNull() ?: 0) },
                    colors = ButtonDefaults.buttonColors(containerColor = p.accent)
                ) {
                    Text("Uložit km", color = p.onAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Text("Historie výměn tohoto dílu (${records.size}x)", fontSize = 12.sp, color = p.accent)

            if (records.isEmpty()) {
                Text("Zatím žádný záznam o výměně.", fontSize = 13.sp, color = p.textMuted)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    records.forEach { r ->
                        val driven = (vehicle.currentMileageKm - r.mileageKm).coerceAtLeast(0)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(p.cardSub)
                                .border(1.dp, p.border, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${formatKm(r.mileageKm)} • ${r.date}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = p.greenOk
                                )
                                Text("Najeto od této výměny: ${formatKm(driven)}", fontSize = 11.sp, color = p.textMuted)
                                if (r.brandOrSpec.isNotBlank()) {
                                    Text("Díl: ${r.brandOrSpec}", fontSize = 12.sp, color = p.textPrimary)
                                }
                                if (r.note.isNotBlank()) {
                                    Text("Pozn.: ${r.note}", fontSize = 12.sp, color = p.textMuted)
                                }
                            }
                            Text(
                                "🗑 Smazat",
                                fontSize = 11.sp,
                                color = p.redAlert,
                                modifier = Modifier
                                    .clickable { onDeleteRecord(r.id) }
                                    .padding(6.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Zavřít", color = p.textMuted)
                }
                Button(
                    onClick = onAddAnother,
                    colors = ButtonDefaults.buttonColors(containerColor = p.greenOk)
                ) {
                    Text("+ Zapsat novou výměnu", color = p.onAccent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EditVehicleVinDialog(
    initialVehicle: Vehicle,
    isNew: Boolean,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onSave: (Vehicle) -> Unit,
    onDelete: () -> Unit
) {
    val p = LocalGaragePalette.current
    val scope = rememberCoroutineScope()
    var vin by remember { mutableStateOf(initialVehicle.vin) }
    var name by remember { mutableStateOf(initialVehicle.name) }
    var plate by remember { mutableStateOf(initialVehicle.plate) }
    var yearText by remember { mutableStateOf(initialVehicle.year.toString()) }
    var engineCode by remember { mutableStateOf(initialVehicle.engineCode) }
    var engineDesc by remember { mutableStateOf(initialVehicle.engineDesc) }
    var powerText by remember { mutableStateOf(initialVehicle.powerKw.toString()) }
    var gearbox by remember { mutableStateOf(initialVehicle.gearbox) }
    var color by remember { mutableStateOf(initialVehicle.color) }
    var stkUntil by remember { mutableStateOf(initialVehicle.stkUntil) }
    var tiresInfo by remember { mutableStateOf(initialVehicle.tiresInfo) }
    var kmText by remember { mutableStateOf(initialVehicle.currentMileageKm.toString()) }
    var loadingVin by remember { mutableStateOf(false) }
    var vinMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(p.card)
                .border(1.dp, p.accent, RoundedCornerShape(16.dp))
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (isNew) "Přidat vozidlo podle VIN" else "Karta vozidla & Načtení VIN",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = p.accent
            )

            OutlinedTextField(
                value = vin,
                onValueChange = { vin = it.uppercase().trim() },
                label = { Text("17místný VIN kód vozidla") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (vin.length < 10) {
                        vinMessage = "Zadej platný VIN kód (alespoň 10–17 znaků)."
                        return@Button
                    }
                    loadingVin = true
                    vinMessage = "Načítám technická data podle VIN..."
                    scope.launch {
                        val apiKey = GarageStorage.loadApiKey()
                        val decoded = VinAndCatalogService.decodeVin(vin, apiKey)
                        name = decoded.name
                        yearText = decoded.year.toString()
                        engineCode = decoded.engineCode
                        engineDesc = decoded.engineDesc
                        powerText = decoded.powerKw.toString()
                        gearbox = decoded.gearbox
                        color = decoded.color
                        stkUntil = decoded.stkUntil
                        tiresInfo = decoded.tiresInfo
                        vinMessage = decoded.sourceInfo
                        loadingVin = false
                    }
                },
                enabled = !loadingVin,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = p.accent)
            ) {
                Text(
                    if (loadingVin) "Načítám..." else "🔍 Načíst vozidlo a motor podle VIN",
                    color = p.onAccent,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (vinMessage != null) {
                Text(vinMessage ?: "", fontSize = 11.sp, color = p.greenOk)
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Název auta v garáži") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = plate,
                    onValueChange = { plate = it.uppercase() },
                    label = { Text("SPZ / RZ") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = kmText,
                    onValueChange = { kmText = it.filter { c -> c.isDigit() } },
                    label = { Text("Stav tachometru (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = engineCode,
                    onValueChange = { engineCode = it.uppercase() },
                    label = { Text("Kód motoru (např. DFGA)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = powerText,
                    onValueChange = { powerText = it.filter { c -> c.isDigit() } },
                    label = { Text("Výkon (kW)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = engineDesc,
                onValueChange = { engineDesc = it },
                label = { Text("Motorizace (např. 2.0 TDI CR 1968 ccm)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = gearbox,
                    onValueChange = { gearbox = it },
                    label = { Text("Převodovka") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = stkUntil,
                    onValueChange = { stkUntil = it },
                    label = { Text("STK do") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = tiresInfo,
                onValueChange = { tiresInfo = it },
                label = { Text("Zapsané pneu a ráfky z TP (včetně alternativních)") },
                minLines = 2,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canDelete) {
                    TextButton(onClick = onDelete) {
                        Text("Smazat auto", color = p.redAlert)
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                Row {
                    TextButton(onClick = onDismiss) {
                        Text("Zrušit", color = p.textMuted)
                    }
                    Spacer(Modifier.width(6.dp))
                    Button(
                        onClick = {
                            onSave(
                                initialVehicle.copy(
                                    name = name.ifBlank { "Vozidlo" },
                                    vin = vin,
                                    plate = plate,
                                    year = yearText.toIntOrNull() ?: 2018,
                                    engineCode = engineCode,
                                    engineDesc = engineDesc,
                                    powerKw = powerText.toIntOrNull() ?: 110,
                                    gearbox = gearbox,
                                    color = color,
                                    stkUntil = stkUntil,
                                    tiresInfo = tiresInfo,
                                    currentMileageKm = kmText.toIntOrNull() ?: initialVehicle.currentMileageKm
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = p.greenOk)
                    ) {
                        Text("Uložit vozidlo", color = p.onAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickMileageDialog(
    currentKm: Int,
    vehicleName: String,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    val p = LocalGaragePalette.current
    var kmText by remember { mutableStateOf(currentKm.toString()) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(p.card)
                .border(1.dp, p.accent, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Aktualizovat stav tachometru", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = p.textPrimary)
            Text(vehicleName, fontSize = 12.sp, color = p.accent)
            OutlinedTextField(
                value = kmText,
                onValueChange = { kmText = it.filter { c -> c.isDigit() } },
                label = { Text("Aktuální stav km") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Zrušit", color = p.textMuted) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onSave(kmText.toIntOrNull() ?: currentKm) },
                    colors = ButtonDefaults.buttonColors(containerColor = p.accent)
                ) {
                    Text("Uložit km", color = p.onAccent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SmartServiceParserDialog(
    vehicle: Vehicle,
    onDismiss: () -> Unit,
    onConfirmMultiple: (List<PartItem>, Int, String, String, String) -> Unit
) {
    val p = LocalGaragePalette.current
    val allParts = remember(vehicle) { GarageCatalog.allPartsForVehicle(vehicle) }
    var rawText by remember { mutableStateOf("") }
    var kmText by remember { mutableStateOf(vehicle.currentMileageKm.toString()) }
    var dateText by remember { mutableStateOf(GaragePlatform.currentDateString()) }
    var brandText by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(p.card)
                .border(1.dp, p.accent, RoundedCornerShape(16.dp))
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("⚡ Chytré rozklíčování servisu", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = p.accent)
            Text(
                "Napiš jednou větou, co se na autě dělalo (např. „výměna oleje a všech filtrů, přední kotouče a destičky, levý čep řízení“). Aplikace sama najde příslušné díly ve stromu:",
                fontSize = 11.sp,
                color = p.textMuted
            )

            OutlinedTextField(
                value = rawText,
                onValueChange = {
                    rawText = it
                    val matched = GarageCatalog.parseServiceTextToParts(it, allParts)
                    selectedIds = matched.map { item -> item.id }.toSet()
                },
                label = { Text("Popis provedeného servisu") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = kmText,
                    onValueChange = { kmText = it.filter { c -> c.isDigit() } },
                    label = { Text("Stav km") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Datum") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = brandText,
                onValueChange = { brandText = it },
                label = { Text("Značka dílů (volitelné)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                "Rozpoznané položky k označení (${selectedIds.size}):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = p.greenOk
            )

            val matchedParts = allParts.filter { selectedIds.contains(it.id) }
            if (matchedParts.isEmpty()) {
                Text("Zatím žádné položky – napiš text výše.", fontSize = 12.sp, color = p.textMuted)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    matchedParts.forEach { part ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(p.greenBg)
                                .border(1.dp, p.greenOk, RoundedCornerShape(8.dp))
                                .clickable { selectedIds = selectedIds - part.id }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✓ ${part.name} (${part.subfolderName})", fontSize = 12.sp, color = p.textPrimary, modifier = Modifier.weight(1f))
                            Text("✕", fontSize = 12.sp, color = p.textMuted)
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Zrušit", color = p.textMuted) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val finalParts = allParts.filter { selectedIds.contains(it.id) }
                        if (finalParts.isNotEmpty()) {
                            onConfirmMultiple(
                                finalParts,
                                kmText.toIntOrNull() ?: vehicle.currentMileageKm,
                                dateText.ifBlank { GaragePlatform.currentDateString() },
                                brandText.trim(),
                                rawText.trim()
                            )
                        }
                    },
                    enabled = selectedIds.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = p.greenOk)
                ) {
                    Text("✓ Označit vše (${selectedIds.size})", color = p.onAccent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CatalogPartsPickerDialog(
    category: CategoryDef,
    subfolder: SubfolderDef,
    existingParts: List<PartItem>,
    onDismiss: () -> Unit,
    onAddSelectedParts: (List<String>, Boolean) -> Unit
) {
    val p = LocalGaragePalette.current
    var loading by remember { mutableStateOf(true) }
    var fetchedNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedNames by remember { mutableStateOf(setOf<String>()) }
    var markImmediately by remember { mutableStateOf(false) }

    LaunchedEffect(subfolder.id) {
        loading = true
        val existingLower = existingParts.map { it.name.lowercase() }.toSet()
        val rawList = VinAndCatalogService.fetchCatalogPartsForSubfolder(subfolder)
            .filterNot { existingLower.contains(it.lowercase()) }
        fetchedNames = rawList
        selectedNames = rawList.toSet()
        loading = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(p.card)
                .border(1.dp, p.accent, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("📥 Katalog dílů: ${subfolder.title}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = p.accent)
            Text("Vyber díly, které chceš přidat do této podsložky:", fontSize = 12.sp, color = p.textMuted)

            if (loading) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = p.accent)
                }
            } else if (fetchedNames.isEmpty()) {
                Text("Všechny dostupné díly z katalogu už v této složce máš.", fontSize = 13.sp, color = p.greenOk)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    fetchedNames.forEach { name ->
                        val isSel = selectedNames.contains(name)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) p.accent.copy(alpha = 0.18f) else p.cardSub)
                                .border(1.dp, if (isSel) p.accent else p.border, RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedNames = if (isSel) selectedNames - name else selectedNames + name
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isSel) "☑" else "☐", fontSize = 15.sp, color = if (isSel) p.accent else p.textMuted)
                            Spacer(Modifier.width(8.dp))
                            Text(name, fontSize = 13.sp, color = p.textPrimary)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { markImmediately = !markImmediately }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (markImmediately) "☑" else "☐", fontSize = 16.sp, color = p.greenOk)
                    Spacer(Modifier.width(8.dp))
                    Text("Rovnou je označit jako vyměněné při aktuálních km", fontSize = 12.sp, color = p.textPrimary)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Zavřít", color = p.textMuted) }
                if (fetchedNames.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onAddSelectedParts(selectedNames.toList(), markImmediately) },
                        enabled = selectedNames.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = p.accent)
                    ) {
                        Text("Přidat vybrané (${selectedNames.size})", color = p.onAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCustomPartDialog(
    category: CategoryDef,
    subfolder: SubfolderDef,
    onDismiss: () -> Unit,
    onConfirm: (name: String, pos: String, intervalKm: Int, spec: String, markNow: Boolean) -> Unit
) {
    val p = LocalGaragePalette.current
    var name by remember { mutableStateOf("") }
    var pos by remember { mutableStateOf("") }
    var intervalText by remember { mutableStateOf("") }
    var spec by remember { mutableStateOf("") }
    var markNow by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(p.card)
                .border(1.dp, p.accent, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("+ Přidat vlastní díl", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = p.accent)
            Text("${category.title} → ${subfolder.title}", fontSize = 12.sp, color = p.textMuted)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Název dílu") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pos,
                    onValueChange = { pos = it.uppercase() },
                    label = { Text("Pozice (např. L, P)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = intervalText,
                    onValueChange = { intervalText = it.filter { c -> c.isDigit() } },
                    label = { Text("Interval (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = spec,
                onValueChange = { spec = it },
                label = { Text("Specifikace / OEM číslo (volitelné)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.clickable { markNow = !markNow }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (markNow) "☑" else "☐", fontSize = 16.sp, color = p.greenOk)
                Spacer(Modifier.width(8.dp))
                Text("Rovnou označit jako vyměněný", fontSize = 13.sp, color = p.textPrimary)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Zrušit", color = p.textMuted) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onConfirm(name.trim(), pos.trim(), intervalText.toIntOrNull() ?: 0, spec.trim(), markNow)
                        }
                    },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = p.greenOk)
                ) {
                    Text("Uložit díl", color = p.onAccent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AllVehicleHistoryDialog(
    vehicle: Vehicle,
    onDismiss: () -> Unit,
    onDeleteRecord: (String) -> Unit
) {
    val p = LocalGaragePalette.current
    val allPartsMap = remember(vehicle) {
        GarageCatalog.allPartsForVehicle(vehicle).associateBy { it.id }
    }
    val sortedRecords = vehicle.records.sortedByDescending { it.mileageKm }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(p.card)
                .border(1.dp, p.accent, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("📜 Servisní knížka: ${vehicle.name}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = p.accent)
            Text("Celkem záznamů o výměně: ${sortedRecords.size}", fontSize = 12.sp, color = p.textMuted)

            if (sortedRecords.isEmpty()) {
                Text("Zatím nebyl označen žádný vyměněný díl.", fontSize = 13.sp, color = p.textMuted)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sortedRecords.forEach { rec ->
                        val part = allPartsMap[rec.partId]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(p.cardSub)
                                .border(1.dp, p.border, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    part?.name ?: rec.partId,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = p.textPrimary
                                )
                                Text(
                                    "${formatKm(rec.mileageKm)} • ${rec.date}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = p.greenOk
                                )
                                if (rec.brandOrSpec.isNotBlank() || rec.note.isNotBlank()) {
                                    Text(
                                        listOf(rec.brandOrSpec, rec.note).filter { it.isNotBlank() }.joinToString(" – "),
                                        fontSize = 11.sp,
                                        color = p.textMuted
                                    )
                                }
                            }
                            Text(
                                "🗑",
                                fontSize = 14.sp,
                                color = p.redAlert,
                                modifier = Modifier.clickable { onDeleteRecord(rec.id) }.padding(6.dp)
                            )
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = p.accent)) {
                    Text("Zavřít", color = p.onAccent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SettingsAndBackupDialog(
    vehicles: List<Vehicle>,
    currentThemePref: Int,
    onThemePrefChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onRestoreVehicles: (List<Vehicle>) -> Unit
) {
    val p = LocalGaragePalette.current
    var apiKey by remember { mutableStateOf(GarageStorage.loadApiKey()) }
    var cloudUri by remember { mutableStateOf(GarageStorage.loadOneDriveUri()) }
    var weeklyEnabled by remember { mutableStateOf(GarageStorage.isWeeklyBackupEnabled()) }
    var lastBackupDate by remember { mutableStateOf(GarageStorage.loadLastBackupDate()) }
    var backupText by remember { mutableStateOf("") }
    var infoMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(p.card)
                .border(1.dp, p.accent, RoundedCornerShape(16.dp))
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("⚙️ Nastavení & Záloha", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = p.accent)

            Text("Vzhled aplikace (Den / Noc):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = p.textPrimary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChipBtn("📱 Podle systému", currentThemePref == 0) { onThemePrefChange(0) }
                FilterChipBtn("☀️ Den", currentThemePref == 1) { onThemePrefChange(1) }
                FilterChipBtn("🌙 Noc", currentThemePref == 2) { onThemePrefChange(2) }
            }

            HorizontalDivider(color = p.border)

            Text("☁️ Automatická týdenní záloha (Google Drive / Cloud):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = p.textPrimary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        weeklyEnabled = !weeklyEnabled
                        GarageStorage.setWeeklyBackupEnabled(weeklyEnabled)
                    }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (weeklyEnabled) "☑" else "☐", fontSize = 16.sp, color = p.greenOk)
                Spacer(Modifier.width(8.dp))
                Text("Automaticky aktualizovat zálohu 1× týdně (každých 7 dní)", fontSize = 12.sp, color = p.textPrimary)
            }

            Text(
                text = if (cloudUri.isNotBlank()) {
                    "Propojený soubor: ✓ Nastaveno (Google Drive)\nPoslední záloha: $lastBackupDate"
                } else {
                    "Zatím není vybraný záložní soubor na Google Drive."
                },
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = if (cloudUri.isNotBlank()) p.greenOk else p.textMuted
            )

            Button(
                onClick = {
                    val content = GarageStorage.serializeVehicles(vehicles)
                    GaragePlatform.selectBackupFileForSave("garaz_zaloha.txt", content) { selectedUri ->
                        if (!selectedUri.isNullOrBlank()) {
                            GarageStorage.saveOneDriveUri(selectedUri)
                            cloudUri = selectedUri
                            GarageStorage.triggerImmediateOneDriveBackup(vehicles)
                            lastBackupDate = GarageStorage.loadLastBackupDate()
                            infoMsg = "✓ Záložní soubor na Google Drive byl nastaven a uložen."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = p.accent)
            ) {
                Text(
                    text = if (cloudUri.isBlank()) "📂 Vybrat / Vytvořit zálohu na Google Drive" else "📂 Změnit záložní soubor na Google Drive",
                    color = p.onAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (GarageStorage.triggerImmediateOneDriveBackup(vehicles)) {
                            lastBackupDate = GarageStorage.loadLastBackupDate()
                            infoMsg = "✓ Záloha byla okamžitě zapsána na Google Drive."
                        } else {
                            infoMsg = "Nejprve vyber cílový soubor tlačítkem výše."
                        }
                    },
                    enabled = cloudUri.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = p.greenOk)
                ) {
                    Text("☁️ Zálohovat hned", color = p.onAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        GaragePlatform.selectBackupFileForOpen { openedUri, content ->
                            if (!content.isNullOrBlank()) {
                                val parsed = GarageStorage.deserializeVehicles(content)
                                if (parsed.isNotEmpty()) {
                                    if (!openedUri.isNullOrBlank()) {
                                        GarageStorage.saveOneDriveUri(openedUri)
                                        cloudUri = openedUri
                                    }
                                    onRestoreVehicles(parsed)
                                } else {
                                    infoMsg = "Vybraný soubor neobsahuje platnou zálohu Garáže."
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = p.accent)
                ) {
                    Text("📥 Obnovit ze souboru", color = p.onAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            HorizontalDivider(color = p.border)

            Text(
                "API klíč z portálu dataovozidlech.cz (pro stahování dat z velkého TP):",
                fontSize = 11.sp,
                color = p.textMuted
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    GarageStorage.saveApiKey(it)
                },
                label = { Text("API klíč MDČR (dataovozidlech.cz)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(color = p.border)

            Text("Rychlé zkopírování zálohy přes schránku:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = p.textPrimary)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val exported = GarageStorage.serializeVehicles(vehicles)
                        backupText = exported
                        GaragePlatform.copyToClipboard(exported)
                        infoMsg = "✓ Záloha zkopírována do schránky."
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = p.accent)
                ) {
                    Text("Kopírovat text", color = p.onAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Button(
                    onClick = {
                        val parsed = GarageStorage.deserializeVehicles(backupText)
                        if (parsed.isNotEmpty()) {
                            onRestoreVehicles(parsed)
                        } else {
                            infoMsg = "Vložený text neobsahuje platnou zálohu."
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = p.greenOk)
                ) {
                    Text("Obnovit z textu", color = p.onAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            OutlinedTextField(
                value = backupText,
                onValueChange = { backupText = it },
                label = { Text("Textová data zálohy") },
                modifier = Modifier.fillMaxWidth().height(75.dp)
            )

            if (infoMsg != null) {
                Text(infoMsg ?: "", fontSize = 12.sp, color = p.greenOk, fontWeight = FontWeight.SemiBold)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Hotovo", color = p.accent, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun SmallTopButton(text: String, onClick: () -> Unit) {
    val p = LocalGaragePalette.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(p.card)
            .border(1.dp, p.border, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            maxLines = 1,
            softWrap = false,
            fontWeight = FontWeight.SemiBold,
            color = p.textPrimary
        )
    }
}

@Composable
private fun FilterChipBtn(label: String, active: Boolean, onClick: () -> Unit) {
    val p = LocalGaragePalette.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) p.accent.copy(alpha = 0.2f) else p.cardSub)
            .border(1.dp, if (active) p.accent else p.border, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            maxLines = 1,
            softWrap = false,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) p.accent else p.textMuted
        )
    }
}

private fun formatKm(km: Int): String {
    val s = km.toString()
    val sb = StringBuilder()
    for (i in s.indices) {
        if (i > 0 && (s.length - i) % 3 == 0) sb.append(' ')
        sb.append(s[i])
    }
    return "$sb km"
}
