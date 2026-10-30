package cz.mares.garaz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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

private val BgDark = Color(0xFF0F172A)
private val CardDark = Color(0xFF1E293B)
private val CardDarker = Color(0xFF152033)
private val BorderSlate = Color(0xFF334155)
private val AccentSky = Color(0xFF38BDF8)
private val GreenOk = Color(0xFF10B981)
private val GreenBg = Color(0xFF064E3B)
private val AmberWarn = Color(0xFFF59E0B)
private val RedAlert = Color(0xFFEF4444)
private val TextWhite = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF94A3B8)

@Composable
fun App() {
    val scope = rememberCoroutineScope()
    var vehicles by remember { mutableStateOf(GarageStorage.loadVehicles()) }
    var selectedVehicleId by remember { mutableStateOf(vehicles.firstOrNull()?.id ?: "") }
    val currentVehicle = vehicles.find { it.id == selectedVehicleId } ?: vehicles.firstOrNull()

    var expandedCategoryIds by remember { mutableStateOf(setOf("motor")) }
    var expandedSubfolderIds by remember { mutableStateOf(setOf("m_naplne", "m_filtry")) }

    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf(0) } // 0 = Vse, 1 = Vymenene, 2 = Intervaly

    // Dialog states
    var partToMarkReplaced by remember { mutableStateOf<PartItem?>(null) }
    var partForHistoryDetail by remember { mutableStateOf<PartItem?>(null) }
    var showEditVehicleDialog by remember { mutableStateOf(false) }
    var isAddingNewVehicle by remember { mutableStateOf(false) }
    var showQuickMileageDialog by remember { mutableStateOf(false) }
    var showSmartParserDialog by remember { mutableStateOf(false) }
    var showAllHistoryDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var subfolderForCatalogFetch by remember { mutableStateOf<Pair<CategoryDef, SubfolderDef>?>(null) }
    var subfolderForCustomPart by remember { mutableStateOf<Pair<CategoryDef, SubfolderDef>?>(null) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    fun updateVehicles(newList: List<Vehicle>) {
        vehicles = newList
        GarageStorage.saveVehicles(newList)
    }

    fun updateCurrentVehicle(transform: (Vehicle) -> Vehicle) {
        val cur = currentVehicle ?: return
        val updated = vehicles.map { if (it.id == cur.id) transform(it) else it }
        updateVehicles(updated)
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AccentSky,
            background = BgDark,
            surface = CardDark,
            onPrimary = Color.Black,
            onBackground = TextWhite,
            onSurface = TextWhite
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
                .padding(top = 38.dp, start = 12.dp, end = 12.dp, bottom = 8.dp)
        ) {
            // Horni lista aplikace
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🚘 GARÁŽ", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = AccentSky)
                    Spacer(Modifier.width(8.dp))
                    Text("Katalog & Servis podle VIN", fontSize = 12.sp, color = TextMuted)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallTopButton("📜 Historie") { showAllHistoryDialog = true }
                    SmallTopButton("⚙️ Nastavení") { showSettingsDialog = true }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Prepinac aut v garazi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                vehicles.forEach { car ->
                    val isSel = car.id == currentVehicle?.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) AccentSky.copy(alpha = 0.2f) else CardDark)
                            .border(
                                width = if (isSel) 2.dp else 1.dp,
                                color = if (isSel) AccentSky else BorderSlate,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedVehicleId = car.id }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text(
                                text = car.name,
                                fontSize = 13.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) AccentSky else TextWhite
                            )
                            Text(
                                text = "${formatKm(car.currentMileageKm)} • ${car.engineCode.ifBlank { "VIN" }}",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardDark)
                        .border(1.dp, BorderSlate, RoundedCornerShape(10.dp))
                        .clickable {
                            isAddingNewVehicle = true
                            showEditVehicleDialog = true
                        }
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Text("+ Přidat auto", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentSky)
                }
            }

            if (statusBanner != null) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(GreenBg)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(statusBanner ?: "", fontSize = 12.sp, color = TextWhite, modifier = Modifier.weight(1f))
                    Text(
                        "✕",
                        fontSize = 14.sp,
                        color = TextWhite,
                        modifier = Modifier.clickable { statusBanner = null }.padding(start = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            if (currentVehicle != null) {
                val allParts = remember(currentVehicle) { GarageCatalog.allPartsForVehicle(currentVehicle) }
                val replacedPartIds = remember(currentVehicle.records) {
                    currentVehicle.records.map { it.partId }.toSet()
                }

                // Seznam vsech polozek v hlavnim scrollu
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Karta vybraneho vozidla
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

                    // Vyhledavani a filtry
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardDark)
                                .padding(10.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Hledat díl (např. čep, tlumič, olej, vstřik, žhavič...)", fontSize = 13.sp, color = TextMuted) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        Text(
                                            "✕",
                                            color = TextMuted,
                                            modifier = Modifier.clickable { searchQuery = "" }.padding(8.dp)
                                        )
                                    }
                                }
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChipBtn("Všechny díly (${allParts.size})", filterMode == 0) { filterMode = 0 }
                                FilterChipBtn("✓ Vyměněné (${replacedPartIds.size})", filterMode == 1) { filterMode = 1 }
                                FilterChipBtn("⏱ Hlídání intervalů", filterMode == 2) { filterMode = 2 }
                            }
                        }
                    }

                    // Pokud uzivatel vyhledava nebo filtruje, zobrazime primo vyfiltrovane dily
                    if (searchQuery.isNotBlank() || filterMode != 0) {
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
                                else -> true
                            }
                            matchesSearch && matchesFilter
                        }

                        item {
                            Text(
                                "Nalezené položky (${filteredParts.size}):",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentSky,
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
                                onShowHistory = { partForHistoryDetail = part }
                            )
                        }
                    } else {
                        // Stromova struktura: Kategorie -> Podslozky -> Konkretni dily
                        items(GarageCatalog.categories, key = { it.id }) { cat ->
                            val catParts = allParts.filter { it.categoryId == cat.id }
                            val catReplacedCount = catParts.count { replacedPartIds.contains(it.id) }
                            val isCatExpanded = expandedCategoryIds.contains(cat.id)

                            CategoryTreeCard(
                                category = cat,
                                allCategoryParts = catParts,
                                replacedCount = catReplacedCount,
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
                                onFetchExtraParts = { sub -> subfolderForCatalogFetch = cat to sub },
                                onAddCustomPart = { sub -> subfolderForCustomPart = cat to sub }
                            )
                        }
                    }

                    item { Spacer(Modifier.height(28.dp)) }
                }
            }
        }

        // 1. Dialog pro oznaceni konkretniho dilu jako VYMENENO
        val targetPart = partToMarkReplaced
        if (targetPart != null && currentVehicle != null) {
            MarkPartReplacedDialog(
                part = targetPart,
                vehicle = currentVehicle,
                onDismiss = { partToMarkReplaced = null },
                onConfirm = { km, date, brand, note ->
                    val newRec = ReplacementRecord(
                        id = "rec_${Random.nextInt(100000, 999999)}",
                        partId = targetPart.id,
                        date = date,
                        mileageKm = km,
                        brandOrSpec = brand,
                        note = note
                    )
                    updateCurrentVehicle { v ->
                        val updatedKm = maxOf(v.currentMileageKm, km)
                        v.copy(
                            currentMileageKm = updatedKm,
                            records = listOf(newRec) + v.records
                        )
                    }
                    statusBanner = "✓ Označeno jako vyměněné: ${targetPart.name} (${formatKm(km)})"
                    partToMarkReplaced = null
                }
            )
        }

        // 2. Dialog historie konkretniho dilu (s moznosti smazat zaznam)
        val historyPart = partForHistoryDetail
        if (historyPart != null && currentVehicle != null) {
            PartHistoryDialog(
                part = historyPart,
                vehicle = currentVehicle,
                onDismiss = { partForHistoryDetail = null },
                onAddAnother = {
                    partForHistoryDetail = null
                    partToMarkReplaced = historyPart
                },
                onDeleteRecord = { recId ->
                    updateCurrentVehicle { v ->
                        v.copy(records = v.records.filterNot { it.id == recId })
                    }
                }
            )
        }

        // 3. Dialog pro nacteni/upravu auta podle VIN (dataovozidlech.cz)
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
                            updateVehicles(newList)
                            selectedVehicleId = savedCar.id
                        } else {
                            updateVehicles(vehicles.map { if (it.id == savedCar.id) savedCar else it })
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

        // 4. Rychla uprava stavu tachometru
        if (showQuickMileageDialog && currentVehicle != null) {
            QuickMileageDialog(
                currentKm = currentVehicle.currentMileageKm,
                vehicleName = currentVehicle.name,
                onDismiss = { showQuickMileageDialog = false },
                onSave = { newKm ->
                    updateCurrentVehicle { it.copy(currentMileageKm = newKm) }
                    showQuickMileageDialog = false
                    statusBanner = "Stav tachometru aktualizován na ${formatKm(newKm)}."
                }
            )
        }

        // 5. Chytre rozklicovani hromadneho servisu z textu
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
                    updateCurrentVehicle { v ->
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

        // 6. Nacitani dalsich dilu z online katalogu do podslozky
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

        // 7. Pridani vlastniho dilu do podslozky
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

        // 8. Kompletni chronologicka historie auta
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

        // 9. Nastaveni API klice MDCR a zalohovani databaze
        if (showSettingsDialog) {
            SettingsAndBackupDialog(
                vehicles = vehicles,
                onDismiss = { showSettingsDialog = false },
                onRestoreVehicles = { restored ->
                    updateVehicles(restored)
                    selectedVehicleId = restored.firstOrNull()?.id ?: ""
                    showSettingsDialog = false
                    statusBanner = "Databáze garáže byla úspěšně obnovena (${restored.size} vozidel)."
                }
            )
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardDark)
            .border(1.dp, BorderSlate, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(vehicle.name, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextWhite)
                Spacer(Modifier.height(2.dp))
                Text(
                    "VIN: ${vehicle.vin.ifBlank { "nezadáno" }} • SPZ: ${vehicle.plate.ifBlank { "-" }}",
                    fontSize = 12.sp,
                    color = AccentSky,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Motor: ${vehicle.engineCode} (${vehicle.engineDesc}, ${vehicle.powerKw} kW) • ${vehicle.gearbox}",
                    fontSize = 12.sp,
                    color = TextMuted
                )
                if (vehicle.stkUntil.isNotBlank() || vehicle.tiresInfo.isNotBlank()) {
                    Text(
                        "STK do: ${vehicle.stkUntil.ifBlank { "-" }} • Pneu: ${vehicle.tiresInfo.ifBlank { "-" }}",
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardDarker)
                    .border(1.dp, AccentSky.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clickable { onEditVinOrVehicle() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("🔍 VIN / Upravit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentSky)
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Aktualni stav km
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardDarker)
                    .border(1.dp, BorderSlate, RoundedCornerShape(10.dp))
                    .clickable { onQuickKmClick() }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Tachometr (klepni pro úpravu)", fontSize = 10.sp, color = TextMuted)
                    Text(formatKm(vehicle.currentMileageKm), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = TextWhite)
                }
                Text("✎", fontSize = 16.sp, color = AccentSky)
            }

            // Prehled vymenenych dilu
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(GreenBg.copy(alpha = 0.55f))
                    .border(1.dp, GreenOk.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Vyměněno dílů", fontSize = 10.sp, color = TextWhite)
                Text("$replacedCount / $totalPartsCount", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = GreenOk)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Tlacitko pro chytre rozklicovani servisu
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(AccentSky.copy(alpha = 0.16f))
                .border(1.dp, AccentSky, RoundedCornerShape(10.dp))
                .clickable { onSmartParseClick() }
                .padding(vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "⚡ Zapsat větší servis (Automaticky rozklíčovat položky z textu)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AccentSky
            )
        }
    }
}

@Composable
private fun CategoryTreeCard(
    category: CategoryDef,
    allCategoryParts: List<PartItem>,
    replacedCount: Int,
    isExpanded: Boolean,
    expandedSubfolderIds: Set<String>,
    vehicle: Vehicle,
    onToggleCategory: () -> Unit,
    onToggleSubfolder: (String) -> Unit,
    onMarkPartReplaced: (PartItem) -> Unit,
    onShowPartHistory: (PartItem) -> Unit,
    onFetchExtraParts: (SubfolderDef) -> Unit,
    onAddCustomPart: (SubfolderDef) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .border(1.dp, if (isExpanded) AccentSky.copy(alpha = 0.6f) else BorderSlate, RoundedCornerShape(12.dp))
    ) {
        // Hlavicka hlavni sekce (napr. Motor, Podvozek, Brzdy)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleCategory() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(category.icon, fontSize = 20.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(category.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    Text(
                        "${category.subfolders.size} podsložek • ${allCategoryParts.size} dílů",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (replacedCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(GreenBg)
                            .border(1.dp, GreenOk, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("✓ $replacedCount vyměněno", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenOk)
                    }
                }
                Text(if (isExpanded) "▲" else "▼", fontSize = 14.sp, color = AccentSky)
            }
        }

        // Podslozky uvnitr kategorie
        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardDarker)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                category.subfolders.forEach { sub ->
                    val subParts = allCategoryParts.filter { it.subfolderId == sub.id }
                    val subReplaced = subParts.count { p -> vehicle.records.any { it.partId == p.id } }
                    val isSubExpanded = expandedSubfolderIds.contains(sub.id)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardDark)
                            .border(
                                1.dp,
                                if (isSubExpanded) AccentSky.copy(alpha = 0.4f) else BorderSlate,
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleSubfolder(sub.id) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text(if (isSubExpanded) "📂" else "📁", fontSize = 15.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    sub.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextWhite
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    if (subReplaced > 0) "✓ $subReplaced/${subParts.size}" else "${subParts.size} dílů",
                                    fontSize = 11.sp,
                                    fontWeight = if (subReplaced > 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (subReplaced > 0) GreenOk else TextMuted
                                )
                                Text(if (isSubExpanded) "−" else "+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentSky)
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
                                        onShowHistory = { onShowPartHistory(part) }
                                    )
                                }

                                // Tlacitka pro dotazeni dilu z katalogu nebo pridani vlastniho dilu
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
                                            .background(CardDarker)
                                            .border(1.dp, AccentSky.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .clickable { onFetchExtraParts(sub) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("📥 Načíst další díly z katalogu", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentSky)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(CardDarker)
                                            .border(1.dp, BorderSlate, RoundedCornerShape(8.dp))
                                            .clickable { onAddCustomPart(sub) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("+ Vlastní díl", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
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
    onShowHistory: () -> Unit
) {
    val lastRec = records.firstOrNull()
    val isReplaced = lastRec != null
    val drivenSinceLast = if (lastRec != null) (vehicleMileage - lastRec.mileageKm).coerceAtLeast(0) else 0
    val remainingKm = if (lastRec != null && part.defaultIntervalKm > 0) {
        part.defaultIntervalKm - drivenSinceLast
    } else null

    val borderColor = when {
        remainingKm != null && remainingKm < 0 -> RedAlert
        remainingKm != null && remainingKm <= 3000 -> AmberWarn
        isReplaced -> GreenOk.copy(alpha = 0.8f)
        else -> BorderSlate
    }

    val bgColor = if (isReplaced) GreenBg.copy(alpha = 0.28f) else CardDarker

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable {
                if (isReplaced) onShowHistory() else onMarkReplaced()
            }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            if (showCategoryPath) {
                Text(
                    "${part.categoryName} → ${part.subfolderName}",
                    fontSize = 10.sp,
                    color = AccentSky
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (part.positionTag.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(AccentSky.copy(alpha = 0.2f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(part.positionTag, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = AccentSky)
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = part.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            if (part.specHint.isNotBlank() || part.defaultIntervalKm > 0) {
                val intervalTxt = if (part.defaultIntervalKm > 0) "Interval: ${formatKm(part.defaultIntervalKm)}" else ""
                val combined = listOf(part.specHint, intervalTxt).filter { it.isNotBlank() }.joinToString(" • ")
                Text(combined, fontSize = 11.sp, color = TextMuted)
            }

            if (lastRec != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "✓ Vyměněno: ${formatKm(lastRec.mileageKm)} (${lastRec.date}) • najeto ${formatKm(drivenSinceLast)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GreenOk
                )
                if (lastRec.brandOrSpec.isNotBlank() || lastRec.note.isNotBlank()) {
                    val info = listOf(lastRec.brandOrSpec, lastRec.note).filter { it.isNotBlank() }.joinToString(" – ")
                    Text("Díl / pozn.: $info", fontSize = 11.sp, color = TextWhite.copy(alpha = 0.85f))
                }
                if (remainingKm != null) {
                    val statusTxt = if (remainingKm >= 0) {
                        "Do další výměny zbývá: ${formatKm(remainingKm)}"
                    } else {
                        "⚠ Překročen interval o ${formatKm(-remainingKm)}!"
                    }
                    val statusCol = when {
                        remainingKm < 0 -> RedAlert
                        remainingKm <= 3000 -> AmberWarn
                        else -> AccentSky
                    }
                    Text(statusTxt, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusCol)
                }
            }
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isReplaced) GreenOk else AccentSky)
                    .clickable { onMarkReplaced() }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Text(
                    text = if (isReplaced) "✓ + Vyměnit" else "✓ Vyměněno",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
            }
            if (records.isNotEmpty()) {
                Text(
                    text = "Historie (${records.size}x)",
                    fontSize = 10.sp,
                    color = AccentSky,
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
    onConfirm: (km: Int, date: String, brand: String, note: String) -> Unit
) {
    var kmText by remember { mutableStateOf(vehicle.currentMileageKm.toString()) }
    var dateText by remember { mutableStateOf(GaragePlatform.currentDateString()) }
    var brandText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardDark)
                .border(1.dp, AccentSky, RoundedCornerShape(16.dp))
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("✓ Označit díl jako vyměněný", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = GreenOk)
            Text("${part.categoryName} → ${part.subfolderName}", fontSize = 11.sp, color = AccentSky)
            Text(part.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            if (part.specHint.isNotBlank()) {
                Text("Specifikace: ${part.specHint}", fontSize = 12.sp, color = TextMuted)
            }

            OutlinedTextField(
                value = kmText,
                onValueChange = { kmText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Stav tachometru při výměně (km)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = dateText,
                onValueChange = { dateText = it },
                label = { Text("Datum výměny (DD.MM.RRRR)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

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
                    Text("Zrušit", color = TextMuted)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val km = kmText.toIntOrNull() ?: vehicle.currentMileageKm
                        onConfirm(km, dateText.ifBlank { GaragePlatform.currentDateString() }, brandText.trim(), noteText.trim())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenOk)
                ) {
                    Text("✓ Uložit výměnu", color = Color.Black, fontWeight = FontWeight.Bold)
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
    onDeleteRecord: (String) -> Unit
) {
    val records = vehicle.records.filter { it.partId == part.id }.sortedByDescending { it.mileageKm }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardDark)
                .border(1.dp, BorderSlate, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(part.name, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextWhite)
            Text("Historie výměn tohoto dílu (${records.size}x)", fontSize = 12.sp, color = AccentSky)

            if (records.isEmpty()) {
                Text("Zatím žádný záznam o výměně.", fontSize = 13.sp, color = TextMuted)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    records.forEach { r ->
                        val driven = (vehicle.currentMileageKm - r.mileageKm).coerceAtLeast(0)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CardDarker)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${formatKm(r.mileageKm)} • ${r.date}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenOk
                                )
                                Text("Najeto od této výměny: ${formatKm(driven)}", fontSize = 11.sp, color = TextMuted)
                                if (r.brandOrSpec.isNotBlank()) {
                                    Text("Díl: ${r.brandOrSpec}", fontSize = 12.sp, color = TextWhite)
                                }
                                if (r.note.isNotBlank()) {
                                    Text("Pozn.: ${r.note}", fontSize = 12.sp, color = TextMuted)
                                }
                            }
                            Text(
                                "🗑 Smazat",
                                fontSize = 11.sp,
                                color = RedAlert,
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
                    Text("Zavřít", color = TextMuted)
                }
                Button(
                    onClick = onAddAnother,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenOk)
                ) {
                    Text("+ Zapsat novou výměnu", color = Color.Black, fontWeight = FontWeight.Bold)
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
                .background(CardDark)
                .border(1.dp, AccentSky, RoundedCornerShape(16.dp))
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (isNew) "Přidat vozidlo podle VIN" else "Karta vozidla & Načtení VIN",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AccentSky
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
                colors = ButtonDefaults.buttonColors(containerColor = AccentSky)
            ) {
                Text(
                    if (loadingVin) "Načítám..." else "🔍 Načíst vozidlo a motor podle VIN",
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (vinMessage != null) {
                Text(vinMessage ?: "", fontSize = 11.sp, color = GreenOk)
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
                label = { Text("Zapsané pneu a ráfky z TP") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canDelete) {
                    TextButton(onClick = onDelete) {
                        Text("Smazat auto", color = RedAlert)
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                Row {
                    TextButton(onClick = onDismiss) {
                        Text("Zrušit", color = TextMuted)
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
                        colors = ButtonDefaults.buttonColors(containerColor = GreenOk)
                    ) {
                        Text("Uložit vozidlo", color = Color.Black, fontWeight = FontWeight.Bold)
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
    var kmText by remember { mutableStateOf(currentKm.toString()) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardDark)
                .border(1.dp, AccentSky, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Aktualizovat stav tachometru", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Text(vehicleName, fontSize = 12.sp, color = AccentSky)
            OutlinedTextField(
                value = kmText,
                onValueChange = { kmText = it.filter { c -> c.isDigit() } },
                label = { Text("Aktuální stav km") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Zrušit", color = TextMuted) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onSave(kmText.toIntOrNull() ?: currentKm) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentSky)
                ) {
                    Text("Uložit km", color = Color.Black, fontWeight = FontWeight.Bold)
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
                .background(CardDark)
                .border(1.dp, AccentSky, RoundedCornerShape(16.dp))
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("⚡ Chytré rozklíčování servisu", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = AccentSky)
            Text(
                "Napiš jednou větou, co se na autě dělalo (např. „výměna oleje a všech filtrů, přední kotouče a destičky, levý čep řízení“). Aplikace sama najde příslušné díly ve stromu:",
                fontSize = 11.sp,
                color = TextMuted
            )

            OutlinedTextField(
                value = rawText,
                onValueChange = {
                    rawText = it
                    val matched = GarageCatalog.parseServiceTextToParts(it, allParts)
                    selectedIds = matched.map { p -> p.id }.toSet()
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
                color = GreenOk
            )

            val matchedParts = allParts.filter { selectedIds.contains(it.id) }
            if (matchedParts.isEmpty()) {
                Text("Zatím žádné položky – napiš text výše.", fontSize = 12.sp, color = TextMuted)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    matchedParts.forEach { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(GreenBg.copy(alpha = 0.5f))
                                .clickable { selectedIds = selectedIds - p.id }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✓ ${p.name} (${p.subfolderName})", fontSize = 12.sp, color = TextWhite, modifier = Modifier.weight(1f))
                            Text("✕", fontSize = 12.sp, color = TextMuted)
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Zrušit", color = TextMuted) }
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
                    colors = ButtonDefaults.buttonColors(containerColor = GreenOk)
                ) {
                    Text("✓ Označit vše (${selectedIds.size})", color = Color.Black, fontWeight = FontWeight.Bold)
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
                .background(CardDark)
                .border(1.dp, AccentSky, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("📥 Katalog dílů: ${subfolder.title}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = AccentSky)
            Text("Vyber díly, které chceš přidat do této podsložky:", fontSize = 12.sp, color = TextMuted)

            if (loading) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentSky)
                }
            } else if (fetchedNames.isEmpty()) {
                Text("Všechny dostupné díly z katalogu už v této složce máš.", fontSize = 13.sp, color = GreenOk)
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
                                .background(if (isSel) AccentSky.copy(alpha = 0.2f) else CardDarker)
                                .border(1.dp, if (isSel) AccentSky else BorderSlate, RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedNames = if (isSel) selectedNames - name else selectedNames + name
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isSel) "☑" else "☐", fontSize = 15.sp, color = if (isSel) AccentSky else TextMuted)
                            Spacer(Modifier.width(8.dp))
                            Text(name, fontSize = 13.sp, color = TextWhite)
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
                    Text(if (markImmediately) "☑" else "☐", fontSize = 16.sp, color = GreenOk)
                    Spacer(Modifier.width(8.dp))
                    Text("Rovnou je označit jako vyměněné při aktuálních km", fontSize = 12.sp, color = TextWhite)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Zavřít", color = TextMuted) }
                if (fetchedNames.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onAddSelectedParts(selectedNames.toList(), markImmediately) },
                        enabled = selectedNames.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentSky)
                    ) {
                        Text("Přidat vybrané (${selectedNames.size})", color = Color.Black, fontWeight = FontWeight.Bold)
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
                .background(CardDark)
                .border(1.dp, AccentSky, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("+ Přidat vlastní díl", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = AccentSky)
            Text("${category.title} → ${subfolder.title}", fontSize = 12.sp, color = TextMuted)

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
                Text(if (markNow) "☑" else "☐", fontSize = 16.sp, color = GreenOk)
                Spacer(Modifier.width(8.dp))
                Text("Rovnou označit jako vyměněný", fontSize = 13.sp, color = TextWhite)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Zrušit", color = TextMuted) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onConfirm(name.trim(), pos.trim(), intervalText.toIntOrNull() ?: 0, spec.trim(), markNow)
                        }
                    },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenOk)
                ) {
                    Text("Uložit díl", color = Color.Black, fontWeight = FontWeight.Bold)
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
    val allPartsMap = remember(vehicle) {
        GarageCatalog.allPartsForVehicle(vehicle).associateBy { it.id }
    }
    val sortedRecords = vehicle.records.sortedByDescending { it.mileageKm }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardDark)
                .border(1.dp, AccentSky, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("📜 Servisní knížka: ${vehicle.name}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = AccentSky)
            Text("Celkem záznamů o výměně: ${sortedRecords.size}", fontSize = 12.sp, color = TextMuted)

            if (sortedRecords.isEmpty()) {
                Text("Zatím nebyl označen žádný vyměněný díl.", fontSize = 13.sp, color = TextMuted)
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
                                .background(CardDarker)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    part?.name ?: rec.partId,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Text(
                                    "${formatKm(rec.mileageKm)} • ${rec.date}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GreenOk
                                )
                                if (rec.brandOrSpec.isNotBlank() || rec.note.isNotBlank()) {
                                    Text(
                                        listOf(rec.brandOrSpec, rec.note).filter { it.isNotBlank() }.joinToString(" – "),
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                            Text(
                                "🗑",
                                fontSize = 14.sp,
                                color = RedAlert,
                                modifier = Modifier.clickable { onDeleteRecord(rec.id) }.padding(6.dp)
                            )
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = AccentSky)) {
                    Text("Zavřít", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SettingsAndBackupDialog(
    vehicles: List<Vehicle>,
    onDismiss: () -> Unit,
    onRestoreVehicles: (List<Vehicle>) -> Unit
) {
    var apiKey by remember { mutableStateOf(GarageStorage.loadApiKey()) }
    var backupText by remember { mutableStateOf("") }
    var infoMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardDark)
                .border(1.dp, AccentSky, RoundedCornerShape(16.dp))
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("⚙️ Nastavení & Záloha", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = AccentSky)

            Text(
                "API klíč z portálu dataovozidlech.cz (volitelné – pro automatické stahování kompletních dat z velkého TP):",
                fontSize = 11.sp,
                color = TextMuted
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

            HorizontalDivider(color = BorderSlate)

            Text("Záloha a přenos databází mezi telefony:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val exported = GarageStorage.serializeVehicles(vehicles)
                        backupText = exported
                        GaragePlatform.copyToClipboard(exported)
                        infoMsg = "✓ Záloha zkopírována do schránky."
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentSky)
                ) {
                    Text("Kopírovat zálohu", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                    colors = ButtonDefaults.buttonColors(containerColor = GreenOk)
                ) {
                    Text("Obnovit ze zálohy", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            OutlinedTextField(
                value = backupText,
                onValueChange = { backupText = it },
                label = { Text("Data zálohy (pro export / import)") },
                modifier = Modifier.fillMaxWidth().height(90.dp)
            )

            if (infoMsg != null) {
                Text(infoMsg ?: "", fontSize = 12.sp, color = GreenOk)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Hotovo", color = AccentSky, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun SmallTopButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CardDark)
            .border(1.dp, BorderSlate, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
    }
}

@Composable
private fun FilterChipBtn(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) AccentSky.copy(alpha = 0.22f) else CardDarker)
            .border(1.dp, if (active) AccentSky else BorderSlate, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) AccentSky else TextMuted
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
