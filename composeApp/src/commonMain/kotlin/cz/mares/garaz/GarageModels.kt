package cz.mares.garaz

import kotlin.math.roundToInt

data class ReplacementRecord(
    val id: String,
    val partId: String,
    val date: String,
    val mileageKm: Int,
    val brandOrSpec: String = "",
    val note: String = ""
)

data class PartItem(
    val id: String,
    val categoryId: String,
    val categoryName: String,
    val subfolderId: String,
    val subfolderName: String,
    val name: String,
    val positionTag: String = "",
    val defaultIntervalKm: Int = 0,
    val specHint: String = "",
    val isCustom: Boolean = false
)

data class Vehicle(
    val id: String,
    val name: String,
    val vin: String,
    val plate: String,
    val year: Int,
    val engineCode: String,
    val engineDesc: String,
    val powerKw: Int,
    val gearbox: String,
    val color: String,
    val stkUntil: String,
    val tiresInfo: String,
    val currentMileageKm: Int,
    val records: List<ReplacementRecord> = emptyList(),
    val customParts: List<PartItem> = emptyList(),
    val customIntervals: Map<String, Int> = emptyMap()
)

data class CategoryDef(
    val id: String,
    val title: String,
    val icon: String,
    val subfolders: List<SubfolderDef>
)

data class SubfolderDef(
    val id: String,
    val title: String,
    val catalogQuery: String
)

data class VinDecodedData(
    val name: String,
    val year: Int,
    val engineCode: String,
    val engineDesc: String,
    val powerKw: Int,
    val gearbox: String,
    val color: String,
    val stkUntil: String,
    val tiresInfo: String,
    val sourceInfo: String
)

object GarageCatalog {

    val categories: List<CategoryDef> = listOf(
        CategoryDef(
            id = "motor",
            title = "Motor",
            icon = "🔧",
            subfolders = listOf(
                SubfolderDef("m_naplne", "Náplně motoru", "motorovy olej"),
                SubfolderDef("m_filtry", "Filtry motoru", "filtr motoru"),
                SubfolderDef("m_rozvody", "Rozvody (hlavní)", "rozvody"),
                SubfolderDef("m_remen", "Drážkový řemen a kladky", "drazkovy remen"),
                SubfolderDef("m_palivo", "Vstřikování a palivová soustava", "vstrikovac"),
                SubfolderDef("m_zhaveni", "Žhavení a zapalování", "zhavici svicka"),
                SubfolderDef("m_sani", "Přeplňování (Turbo) a sání", "turbodmychadlo"),
                SubfolderDef("m_emise", "Emise, EGR, DPF a výfuk", "egr ventil dpf"),
                SubfolderDef("m_chlazeni", "Chlazení motoru", "termostat chladic"),
                SubfolderDef("m_ulozeni", "Uložení a těsnění motoru", "silentblok motoru")
            )
        ),
        CategoryDef(
            id = "prevodovka",
            title = "Převodovka a pohon",
            icon = "⚙️",
            subfolders = listOf(
                SubfolderDef("p_naplne", "Náplně a filtry převodovky", "olej dsg prevodovky"),
                SubfolderDef("p_spojka", "Spojka, dvouhmota a řazení", "dvouhmotovy setrvacnik spojka"),
                SubfolderDef("p_poloosy", "Poloosy, klouby a manžety", "poloosa kloub")
            )
        ),
        CategoryDef(
            id = "brzdy",
            title = "Brzdy",
            icon = "🛑",
            subfolders = listOf(
                SubfolderDef("b_predni", "Přední brzdy", "predni brzdove kotouce desticky"),
                SubfolderDef("b_zadni", "Zadní brzdy a ruční brzda", "zadni brzdove kotouce trmen"),
                SubfolderDef("b_hydraulika", "Brzdová kapalina, hydraulika a ABS", "cidlo abs brzdova hadicka")
            )
        ),
        CategoryDef(
            id = "podvozek",
            title = "Podvozek a řízení",
            icon = "🛞",
            subfolders = listOf(
                SubfolderDef("po_predni_tlumeni", "Přední náprava – tlumiče a pružiny", "predni tlumic pruzina"),
                SubfolderDef("po_predni_ramena", "Přední náprava – ramena, čepy, ložiska", "rameno predni napravy lozisko"),
                SubfolderDef("po_zadni_tlumeni", "Zadní náprava – tlumiče a pružiny", "zadni tlumic pruzina"),
                SubfolderDef("po_zadni_ramena", "Zadní náprava – ramena, silentbloky, ložiska", "zadni rameno lozisko"),
                SubfolderDef("po_rizeni", "Řízení, čepy a geometrie", "cep rizeni tahlo"),
                SubfolderDef("po_kola", "Pneumatiky a kola", "pneumatiky")
            )
        ),
        CategoryDef(
            id = "elektrika",
            title = "Elektrika a osvětlení",
            icon = "⚡",
            subfolders = listOf(
                SubfolderDef("el_zdroj", "Baterie, alternátor a startér", "autobaterie alternator starter"),
                SubfolderDef("el_svetla", "Světlomety, výbojky a žárovky", "vybojka svetlomet zarovka"),
                SubfolderDef("el_senzory", "Čidla, parkovací senzory a jednotky", "parkovaci cidlo senzor")
            )
        ),
        CategoryDef(
            id = "klima",
            title = "Klimatizace a topení",
            icon = "❄️",
            subfolders = listOf(
                SubfolderDef("kl_udrzba", "Kabinový filtr a plnění klimatizace", "kabinovy filtr"),
                SubfolderDef("kl_dily", "Kompresor, chladič a klapky topení", "klimakompresor ventilator topeni")
            )
        ),
        CategoryDef(
            id = "karoserie",
            title = "Karoserie, skla a dveře",
            icon = "🚗",
            subfolders = listOf(
                SubfolderDef("ka_skla", "Stěrače, skla a ostřikovače", "sterace motorek ostrikovacu"),
                SubfolderDef("ka_dvere", "Vzpěry, zámky, stahovačky a svazky", "vzpera kufru zamek dveri")
            )
        )
    )

    val defaultParts: List<PartItem> by lazy {
        val list = mutableListOf<PartItem>()
        fun add(
            catId: String,
            catName: String,
            subId: String,
            subName: String,
            id: String,
            name: String = id,
            pos: String = "",
            intervalKm: Int = 0,
            spec: String = ""
        ) {
            list.add(PartItem(id, catId, catName, subId, subName, name, pos, intervalKm, spec, false))
        }

        // 1. MOTOR
        add("motor", "Motor", "m_naplne", "Náplně motoru", "m_oil", "Motorový olej", "", 15000, "VW 507 00 (5W-30 / 0W-30), 4.7 l")
        add("motor", "Motor", "m_naplne", "Náplně motoru", "m_coolant", "Chladicí kapalina", "", 120000, "VW G12evo / G13, cca 8.0 l")
        add("motor", "Motor", "m_naplne", "Náplně motoru", "m_adblue", "AdBlue kapalina", "", 0, "ISO 22241 (u SCR motorů)")

        add("motor", "Motor", "m_filtry", "Filtry motoru", "m_f_oil", "Olejový filtr (vložka + těsnění)", "", 15000, "Měnit s každým olejem")
        add("motor", "Motor", "m_filtry", "Filtry motoru", "m_f_air", "Vzduchový filtr motoru", "", 30000, "Vložka vzduchového filtru")
        add("motor", "Motor", "m_filtry", "Filtry motoru", "m_f_fuel", "Palivový filtr", "", 60000, "Vložka v tělese palivového filtru")

        add("motor", "Motor", "m_rozvody", "Rozvody (hlavní)", "m_r_set", "Rozvodový řemen / sada rozvodů", "", 180000, "Doporučeno 160–180 tis. km")
        add("motor", "Motor", "m_rozvody", "Rozvody (hlavní)", "m_r_pump", "Vodní pumpa (čerpadlo chlazení)", "", 180000, "Včetně el. regulace u EA288")
        add("motor", "Motor", "m_rozvody", "Rozvody (hlavní)", "m_r_tens", "Napínací kladka rozvodů", "", 180000)
        add("motor", "Motor", "m_rozvody", "Rozvody (hlavní)", "m_r_guide", "Vodicí kladky rozvodů", "", 180000)
        add("motor", "Motor", "m_rozvody", "Rozvody (hlavní)", "m_r_oilbelt", "Řemínek olejového čerpadla", "", 200000)
        add("motor", "Motor", "m_rozvody", "Rozvody (hlavní)", "m_r_seal", "Gufero klikové / vačkové hřídele", "")

        add("motor", "Motor", "m_remen", "Drážkový řemen a kladky", "m_d_belt", "Drážkový (klínový) řemen příslušenství", "", 120000, "Řemen 6PK")
        add("motor", "Motor", "m_remen", "Drážkový řemen a kladky", "m_d_tens", "Napínací kladka drážkového řemene", "", 120000)
        add("motor", "Motor", "m_remen", "Drážkový řemen a kladky", "m_d_alt", "Volnoběžka alternátoru", "")
        add("motor", "Motor", "m_remen", "Drážkový řemen a kladky", "m_d_pulley", "Řemenice klikové hřídele", "")

        add("motor", "Motor", "m_palivo", "Vstřikování a palivová soustava", "m_inj_1", "Vstřikovač – 1. válec", "1.V")
        add("motor", "Motor", "m_palivo", "Vstřikování a palivová soustava", "m_inj_2", "Vstřikovač – 2. válec", "2.V")
        add("motor", "Motor", "m_palivo", "Vstřikování a palivová soustava", "m_inj_3", "Vstřikovač – 3. válec", "3.V")
        add("motor", "Motor", "m_palivo", "Vstřikování a palivová soustava", "m_inj_4", "Vstřikovač – 4. válec", "4.V")
        add("motor", "Motor", "m_palivo", "Vstřikování a palivová soustava", "m_inj_seals", "Měděné podložky a O-kroužky vstřikovačů", "")
        add("motor", "Motor", "m_palivo", "Vstřikování a palivová soustava", "m_hpp", "Vysokotlaké čerpadlo Common-Rail", "")
        add("motor", "Motor", "m_palivo", "Vstřikování a palivová soustava", "m_lp_pump", "Podávací čerpadlo v palivové nádrži", "")
        add("motor", "Motor", "m_palivo", "Vstřikování a palivová soustava", "m_drv", "Regulační ventil tlaku paliva", "")
        add("motor", "Motor", "m_palivo", "Vstřikování a palivová soustava", "m_ret_hose", "Přepadové palivové hadičky", "")

        add("motor", "Motor", "m_zhaveni", "Žhavení a zapalování", "m_glow_1", "Žhavicí / zapalovací svíčka – 1. válec", "1.V")
        add("motor", "Motor", "m_zhaveni", "Žhavení a zapalování", "m_glow_2", "Žhavicí / zapalovací svíčka – 2. válec", "2.V")
        add("motor", "Motor", "m_zhaveni", "Žhavení a zapalování", "m_glow_3", "Žhavicí / zapalovací svíčka – 3. válec", "3.V", 0, "U 2.0 TDI EA288 s čidlem tlaku PSG")
        add("motor", "Motor", "m_zhaveni", "Žhavení a zapalování", "m_glow_4", "Žhavicí / zapalovací svíčka – 4. válec", "4.V")
        add("motor", "Motor", "m_zhaveni", "Žhavení a zapalování", "m_glow_rel", "Řídicí jednotka (relé) žhavení", "")

        add("motor", "Motor", "m_sani", "Přeplňování (Turbo) a sání", "m_turbo", "Turbodmychadlo", "")
        add("motor", "Motor", "m_sani", "Přeplňování (Turbo) a sání", "m_turbo_act", "Regulační baňka / aktuátor turba", "")
        add("motor", "Motor", "m_sani", "Přeplňování (Turbo) a sání", "m_ic", "Intercooler (chladič stlačeného vzduchu)", "")
        add("motor", "Motor", "m_sani", "Přeplňování (Turbo) a sání", "m_th_hose", "Tlakové hadice turba a těsnění", "")
        add("motor", "Motor", "m_sani", "Přeplňování (Turbo) a sání", "m_throttle", "Škrticí klapka sání", "")
        add("motor", "Motor", "m_sani", "Přeplňování (Turbo) a sání", "m_maf", "Váha vzduchu (MAF) / Čidlo tlaku (MAP)", "")

        add("motor", "Motor", "m_emise", "Emise, EGR, DPF a výfuk", "m_egr", "EGR ventil (recirkulace spalin)", "")
        add("motor", "Motor", "m_emise", "Emise, EGR, DPF a výfuk", "m_egr_cool", "Chladič EGR ventilu", "")
        add("motor", "Motor", "m_emise", "Emise, EGR, DPF a výfuk", "m_dpf", "DPF filtr pevných částic (čištění / výměna)", "")
        add("motor", "Motor", "m_emise", "Emise, EGR, DPF a výfuk", "m_dpf_sens", "Snímač diferenčního tlaku DPF (G450)", "")
        add("motor", "Motor", "m_emise", "Emise, EGR, DPF a výfuk", "m_lambda", "Lambda sonda", "")
        add("motor", "Motor", "m_emise", "Emise, EGR, DPF a výfuk", "m_nox", "NOx senzor / Vstřikovač AdBlue", "")
        add("motor", "Motor", "m_emise", "Emise, EGR, DPF a výfuk", "m_egt", "Čidlo teploty výfukových plynů (EGT)", "")
        add("motor", "Motor", "m_emise", "Emise, EGR, DPF a výfuk", "m_flex", "Vlnovec výfuku (pružný díl)", "")
        add("motor", "Motor", "m_emise", "Emise, EGR, DPF a výfuk", "m_exhaust", "Střední a zadní díl výfuku + závěsy", "")

        add("motor", "Motor", "m_chlazeni", "Chlazení motoru", "m_rad", "Hlavní chladič vody", "")
        add("motor", "Motor", "m_chlazeni", "Chlazení motoru", "m_thermo", "Termostat motoru (hlavní)", "")
        add("motor", "Motor", "m_chlazeni", "Chlazení motoru", "m_thermo_dsg", "Termostat chlazení DSG převodovky (malý)", "")
        add("motor", "Motor", "m_chlazeni", "Chlazení motoru", "m_exp", "Expanzní nádobka chladicí kapaliny + víčko", "")
        add("motor", "Motor", "m_chlazeni", "Chlazení motoru", "m_fan", "Ventilátor chladiče (sahara)", "")
        add("motor", "Motor", "m_chlazeni", "Chlazení motoru", "m_oil_cool", "Výměník (chladič) motorového oleje", "")
        add("motor", "Motor", "m_chlazeni", "Chlazení motoru", "m_flange", "Vodní příruby, trubky a hadice chlazení", "")

        add("motor", "Motor", "m_ulozeni", "Uložení a těsnění motoru", "m_mnt_r", "Horní silentblok motoru (pravý)", "P")
        add("motor", "Motor", "m_ulozeni", "Uložení a těsnění motoru", "m_mnt_l", "Silentblok převodovky (levý)", "L")
        add("motor", "Motor", "m_ulozeni", "Uložení a těsnění motoru", "m_mnt_low", "Spodní reakční vzpěra + puky v nápravnici (kost)", "")
        add("motor", "Motor", "m_ulozeni", "Uložení a těsnění motoru", "m_vcover", "Víko ventilů / těsnění víka ventilů", "")
        add("motor", "Motor", "m_ulozeni", "Uložení a těsnění motoru", "m_oilpan", "Olejová vana / přetěsnění + výpustný šroub", "")
        add("motor", "Motor", "m_ulozeni", "Uložení a těsnění motoru", "m_oilsens", "Čidlo hladiny a teploty oleje v olejové vaně", "")

        // 2. PŘEVODOVKA A POHON
        add("prevodovka", "Převodovka a pohon", "p_naplne", "Náplně a filtry převodovky", "p_oil", "Olej v převodovce (DSG / manuál)", "", 60000, "DSG DQ250: 6 l (G 052 182), DQ381: 6 l (G 055 529)")
        add("prevodovka", "Převodovka a pohon", "p_naplne", "Náplně a filtry převodovky", "p_filter", "Filtr DSG převodovky + těsnění baňky", "", 60000, "Měnit s olejem v DSG")
        add("prevodovka", "Převodovka a pohon", "p_naplne", "Náplně a filtry převodovky", "p_mech_oil", "Olej v mechatronice", "", 60000)
        add("prevodovka", "Převodovka a pohon", "p_naplne", "Náplně a filtry převodovky", "p_haldex", "Olej v Haldex spojce + vyčištění sítka", "", 60000, "Pro 4x4 pohon (VW G 060 175)")
        add("prevodovka", "Převodovka a pohon", "p_naplne", "Náplně a filtry převodovky", "p_diff", "Olej v úhlové převodovce / diferenciálu", "", 90000)

        add("prevodovka", "Převodovka a pohon", "p_spojka", "Spojka, dvouhmota a řazení", "p_dmf", "Dvouhmotový setrvačník (LUK / Sachs)", "", 200000)
        add("prevodovka", "Převodovka a pohon", "p_spojka", "Spojka, dvouhmota a řazení", "p_clutch", "Spojková sada (lamela + přítlačák / DSG spojky)", "")
        add("prevodovka", "Převodovka a pohon", "p_spojka", "Spojka, dvouhmota a řazení", "p_bearing", "Vypínací ložisko spojky", "")
        add("prevodovka", "Převodovka a pohon", "p_spojka", "Spojka, dvouhmota a řazení", "p_cyl_main", "Hlavní spojkový válec (u pedálu)", "")
        add("prevodovka", "Převodovka a pohon", "p_spojka", "Spojka, dvouhmota a řazení", "p_cyl_slave", "Pracovní spojkový válec", "")
        add("prevodovka", "Převodovka a pohon", "p_spojka", "Spojka, dvouhmota a řazení", "p_mech_unit", "Mechatronika DSG (oprava / výměna)", "")
        add("prevodovka", "Převodovka a pohon", "p_spojka", "Spojka, dvouhmota a řazení", "p_seal_l", "Gufero převodovky u levé poloosy", "L")
        add("prevodovka", "Převodovka a pohon", "p_spojka", "Spojka, dvouhmota a řazení", "p_seal_r", "Gufero převodovky u pravé poloosy", "P")

        add("prevodovka", "Převodovka a pohon", "p_poloosy", "Poloosy, klouby a manžety", "p_axle_l", "Levá přední poloosa (kompletní)", "L")
        add("prevodovka", "Převodovka a pohon", "p_poloosy", "Poloosy, klouby a manžety", "p_axle_r", "Pravá přední poloosa (kompletní)", "P")
        add("prevodovka", "Převodovka a pohon", "p_poloosy", "Poloosy, klouby a manžety", "p_cv_out_l", "Vnější homokinetický kloub u kola – Levý", "L")
        add("prevodovka", "Převodovka a pohon", "p_poloosy", "Poloosy, klouby a manžety", "p_cv_out_r", "Vnější homokinetický kloub u kola – Pravý", "P")
        add("prevodovka", "Převodovka a pohon", "p_poloosy", "Poloosy, klouby a manžety", "p_cv_in_l", "Vnitřní kloub (tripoid) u převodovky – Levý", "L")
        add("prevodovka", "Převodovka a pohon", "p_poloosy", "Poloosy, klouby a manžety", "p_cv_in_r", "Vnitřní kloub (tripoid) u převodovky – Pravý", "P")
        add("prevodovka", "Převodovka a pohon", "p_poloosy", "Poloosy, klouby a manžety", "p_boot_out_l", "Manžeta poloosy vnější – Levá", "L")
        add("prevodovka", "Převodovka a pohon", "p_poloosy", "Poloosy, klouby a manžety", "p_boot_out_r", "Manžeta poloosy vnější – Pravá", "P")
        add("prevodovka", "Převodovka a pohon", "p_poloosy", "Poloosy, klouby a manžety", "p_boot_in_l", "Manžeta poloosy vnitřní – Levá", "L")
        add("prevodovka", "Převodovka a pohon", "p_poloosy", "Poloosy, klouby a manžety", "p_boot_in_r", "Manžeta poloosy vnitřní – Pravá", "P")

        // 3. BRZDY
        add("brzdy", "Brzdy", "b_predni", "Přední brzdy", "b_f_disc", "Přední brzdové kotouče (sada L+P)", "L+P", 80000, "Typicky 312 mm nebo 340 mm")
        add("brzdy", "Brzdy", "b_predni", "Přední brzdy", "b_f_pad", "Přední brzdové destičky (sada)", "L+P", 45000)
        add("brzdy", "Brzdy", "b_predni", "Přední brzdy", "b_f_cal_l", "Přední brzdový třmen – Levý", "L")
        add("brzdy", "Brzdy", "b_predni", "Přední brzdy", "b_f_cal_r", "Přední brzdový třmen – Pravý", "P")
        add("brzdy", "Brzdy", "b_predni", "Přední brzdy", "b_f_pins_l", "Vodicí čepy a gumičky předního třmenu – Levé", "L")
        add("brzdy", "Brzdy", "b_predni", "Přední brzdy", "b_f_pins_r", "Vodicí čepy a gumičky předního třmenu – Pravé", "P")
        add("brzdy", "Brzdy", "b_predni", "Přední brzdy", "b_f_sh_l", "Krycí plech předního kotouče – Levý", "L")
        add("brzdy", "Brzdy", "b_predni", "Přední brzdy", "b_f_sh_r", "Krycí plech předního kotouče – Pravý", "P")

        add("brzdy", "Brzdy", "b_zadni", "Zadní brzdy a ruční brzda", "b_r_disc", "Zadní brzdové kotouče (sada L+P)", "L+P", 90000, "Typicky 282 mm / 300 mm / 310 mm")
        add("brzdy", "Brzdy", "b_zadni", "Zadní brzdy a ruční brzda", "b_r_pad", "Zadní brzdové destičky (sada)", "L+P", 55000)
        add("brzdy", "Brzdy", "b_zadni", "Zadní brzdy a ruční brzda", "b_r_cal_l", "Zadní brzdový třmen – Levý", "L")
        add("brzdy", "Brzdy", "b_zadni", "Zadní brzdy a ruční brzda", "b_r_cal_r", "Zadní brzdový třmen – Pravý", "P")
        add("brzdy", "Brzdy", "b_zadni", "Zadní brzdy a ruční brzda", "b_r_epb_l", "Motorek el. parkovací brzdy (EPB) – Levý", "L")
        add("brzdy", "Brzdy", "b_zadni", "Zadní brzdy a ruční brzda", "b_r_epb_r", "Motorek el. parkovací brzdy (EPB) – Pravý", "P")
        add("brzdy", "Brzdy", "b_zadni", "Zadní brzdy a ruční brzda", "b_r_cable_l", "Lanovod ruční brzdy – Levý", "L")
        add("brzdy", "Brzdy", "b_zadni", "Zadní brzdy a ruční brzda", "b_r_cable_r", "Lanovod ruční brzdy – Pravý", "P")
        add("brzdy", "Brzdy", "b_zadni", "Zadní brzdy a ruční brzda", "b_r_sh_l", "Krycí plech zadního kotouče – Levý", "L")
        add("brzdy", "Brzdy", "b_zadni", "Zadní brzdy a ruční brzda", "b_r_sh_r", "Krycí plech zadního kotouče – Pravý", "P")

        add("brzdy", "Brzdy", "b_hydraulika", "Brzdová kapalina, hydraulika a ABS", "b_fluid", "Brzdová kapalina (DOT 4 LV)", "", 60000, "Výměna každé 2 roky / 60 tis. km")
        add("brzdy", "Brzdy", "b_hydraulika", "Brzdová kapalina, hydraulika a ABS", "b_hose_fl", "Brzdová hadička přední – Levá", "LP")
        add("brzdy", "Brzdy", "b_hydraulika", "Brzdová kapalina, hydraulika a ABS", "b_hose_fr", "Brzdová hadička přední – Pravá", "PP")
        add("brzdy", "Brzdy", "b_hydraulika", "Brzdová kapalina, hydraulika a ABS", "b_hose_rl", "Brzdová hadička zadní – Levá", "LZ")
        add("brzdy", "Brzdy", "b_hydraulika", "Brzdová kapalina, hydraulika a ABS", "b_hose_rr", "Brzdová hadička zadní – Pravá", "PZ")
        add("brzdy", "Brzdy", "b_hydraulika", "Brzdová kapalina, hydraulika a ABS", "b_abs_fl", "Čidlo ABS otáček kola – Levé přední", "LP")
        add("brzdy", "Brzdy", "b_hydraulika", "Brzdová kapalina, hydraulika a ABS", "b_abs_fr", "Čidlo ABS otáček kola – Pravé přední", "PP")
        add("brzdy", "Brzdy", "b_hydraulika", "Brzdová kapalina, hydraulika a ABS", "b_abs_rl", "Čidlo ABS otáček kola – Levé zadní", "LZ")
        add("brzdy", "Brzdy", "b_hydraulika", "Brzdová kapalina, hydraulika a ABS", "b_abs_rr", "Čidlo ABS otáček kola – Pravé zadní", "PZ")
        add("brzdy", "Brzdy", "b_hydraulika", "Brzdová kapalina, hydraulika a ABS", "b_ring_abs", "Magnetický kroužek ABS / náboj", "")

        // 4. PODVOZEK A ŘÍZENÍ
        add("podvozek", "Podvozek a řízení", "po_predni_tlumeni", "Přední náprava – tlumiče a pružiny", "po_f_sh_l", "Přední tlumič pérování – Levý", "L")
        add("podvozek", "Podvozek a řízení", "po_predni_tlumeni", "Přední náprava – tlumiče a pružiny", "po_f_sh_r", "Přední tlumič pérování – Pravý", "P")
        add("podvozek", "Podvozek a řízení", "po_predni_tlumeni", "Přední náprava – tlumiče a pružiny", "po_f_sp_l", "Přední vinutá pružina – Levá", "L")
        add("podvozek", "Podvozek a řízení", "po_predni_tlumeni", "Přední náprava – tlumiče a pružiny", "po_f_sp_r", "Přední vinutá pružina – Pravá", "P")
        add("podvozek", "Podvozek a řízení", "po_predni_tlumeni", "Přední náprava – tlumiče a pružiny", "po_f_top_l", "Horní uložení předního tlumiče + ložisko – Levé", "L")
        add("podvozek", "Podvozek a řízení", "po_predni_tlumeni", "Přední náprava – tlumiče a pružiny", "po_f_top_r", "Horní uložení předního tlumiče + ložisko – Pravé", "P")
        add("podvozek", "Podvozek a řízení", "po_predni_tlumeni", "Přední náprava – tlumiče a pružiny", "po_f_bump", "Prachovky a dorazy předních tlumičů (sada)", "L+P")

        add("podvozek", "Podvozek a řízení", "po_predni_ramena", "Přední náprava – ramena, čepy, ložiska", "po_f_arm_l", "Spodní příčné rameno přední – Levé", "L")
        add("podvozek", "Podvozek a řízení", "po_predni_ramena", "Přední náprava – ramena, čepy, ložiska", "po_f_arm_r", "Spodní příčné rameno přední – Pravé", "P")
        add("podvozek", "Podvozek a řízení", "po_predni_ramena", "Přední náprava – ramena, čepy, ložiska", "po_f_bush_fl", "Přední silentblok předního ramene – Levý", "L")
        add("podvozek", "Podvozek a řízení", "po_predni_ramena", "Přední náprava – ramena, čepy, ložiska", "po_f_bush_fr", "Přední silentblok předního ramene – Pravý", "P")
        add("podvozek", "Podvozek a řízení", "po_predni_ramena", "Přední náprava – ramena, čepy, ložiska", "po_f_bush_rl", "Zadní silentblok předního ramene (puk/konzole) – Levý", "L")
        add("podvozek", "Podvozek a řízení", "po_predni_ramena", "Přední náprava – ramena, čepy, ložiska", "po_f_bush_rr", "Zadní silentblok předního ramene (puk/konzole) – Pravý", "P")
        add("podvozek", "Podvozek a řízení", "po_predni_ramena", "Přední náprava – ramena, čepy, ložiska", "po_f_ball_l", "Spodní kulový čep ramene – Levý", "L")
        add("podvozek", "Podvozek a řízení", "po_predni_ramena", "Přední náprava – ramena, čepy, ložiska", "po_f_ball_r", "Spodní kulový čep ramene – Pravý", "P")
        add("podvozek", "Podvozek a řízení", "po_predni_ramena", "Přední náprava – ramena, čepy, ložiska", "po_f_stab_l", "Tyčka stabilizátoru přední – Levá", "L")
        add("podvozek", "Podvozek a řízení", "po_predni_ramena", "Přední náprava – ramena, čepy, ložiska", "po_f_stab_r", "Tyčka stabilizátoru přední – Pravá", "P")
        add("podvozek", "Podvozek a řízení", "po_predni_ramena", "Přední náprava – ramena, čepy, ložiska", "po_f_stabbush", "Uložení (gumy) předního stabilizátoru", "L+P")
        add("podvozek", "Podvozek a řízení", "po_predni_ramena", "Přední náprava – ramena, čepy, ložiska", "po_f_hub_l", "Ložisko / náboj předního kola – Levé", "L")
        add("podvozek", "Podvozek a řízení", "po_predni_ramena", "Přední náprava – ramena, čepy, ložiska", "po_f_hub_r", "Ložisko / náboj předního kola – Pravé", "P")

        add("podvozek", "Podvozek a řízení", "po_zadni_tlumeni", "Zadní náprava – tlumiče a pružiny", "po_r_sh_l", "Zadní tlumič pérování – Levý", "L")
        add("podvozek", "Podvozek a řízení", "po_zadni_tlumeni", "Zadní náprava – tlumiče a pružiny", "po_r_sh_r", "Zadní tlumič pérování – Pravý", "P")
        add("podvozek", "Podvozek a řízení", "po_zadni_tlumeni", "Zadní náprava – tlumiče a pružiny", "po_r_sp_l", "Zadní vinutá pružina – Levá", "L")
        add("podvozek", "Podvozek a řízení", "po_zadni_tlumeni", "Zadní náprava – tlumiče a pružiny", "po_r_sp_r", "Zadní vinutá pružina – Pravá", "P")
        add("podvozek", "Podvozek a řízení", "po_zadni_tlumeni", "Zadní náprava – tlumiče a pružiny", "po_r_top_l", "Horní uložení zadního tlumiče – Levé", "L")
        add("podvozek", "Podvozek a řízení", "po_zadni_tlumeni", "Zadní náprava – tlumiče a pružiny", "po_r_top_r", "Horní uložení zadního tlumiče – Pravé", "P")
        add("podvozek", "Podvozek a řízení", "po_zadni_tlumeni", "Zadní náprava – tlumiče a pružiny", "po_r_bump", "Prachovky a dorazy zadních tlumičů (sada)", "L+P")

        add("podvozek", "Podvozek a řízení", "po_zadni_ramena", "Zadní náprava – ramena, silentbloky, ložiska", "po_r_up_l", "Příčné rameno zadní horní – Levé", "L")
        add("podvozek", "Podvozek a řízení", "po_zadni_ramena", "Zadní náprava – ramena, silentbloky, ložiska", "po_r_up_r", "Příčné rameno zadní horní – Pravé", "P")
        add("podvozek", "Podvozek a řízení", "po_zadni_ramena", "Zadní náprava – ramena, silentbloky, ložiska", "po_r_low_l", "Spodní příčné rameno (pod pružinu) – Levé", "L")
        add("podvozek", "Podvozek a řízení", "po_zadni_ramena", "Zadní náprava – ramena, silentbloky, ložiska", "po_r_low_r", "Spodní příčné rameno (pod pružinu) – Pravé", "P")
        add("podvozek", "Podvozek a řízení", "po_zadni_ramena", "Zadní náprava – ramena, silentbloky, ložiska", "po_r_toe_l", "Řídicí tyč sbíhavosti zadní – Levá", "L")
        add("podvozek", "Podvozek a řízení", "po_zadni_ramena", "Zadní náprava – ramena, silentbloky, ložiska", "po_r_toe_r", "Řídicí tyč sbíhavosti zadní – Pravá", "P")
        add("podvozek", "Podvozek a řízení", "po_zadni_ramena", "Zadní náprava – ramena, silentbloky, ložiska", "po_r_trail_l", "Podélné rameno zadní + silentblok – Levé", "L")
        add("podvozek", "Podvozek a řízení", "po_zadni_ramena", "Zadní náprava – ramena, silentbloky, ložiska", "po_r_trail_r", "Podélné rameno zadní + silentblok – Pravé", "P")
        add("podvozek", "Podvozek a řízení", "po_zadni_ramena", "Zadní náprava – ramena, silentbloky, ložiska", "po_r_stab_l", "Tyčka stabilizátoru zadní – Levá", "L")
        add("podvozek", "Podvozek a řízení", "po_zadni_ramena", "Zadní náprava – ramena, silentbloky, ložiska", "po_r_stab_r", "Tyčka stabilizátoru zadní – Pravá", "P")
        add("podvozek", "Podvozek a řízení", "po_zadni_ramena", "Zadní náprava – ramena, silentbloky, ložiska", "po_r_hub_l", "Ložisko / náboj zadního kola – Levé", "L")
        add("podvozek", "Podvozek a řízení", "po_zadni_ramena", "Zadní náprava – ramena, silentbloky, ložiska", "po_r_hub_r", "Ložisko / náboj zadního kola – Pravé", "P")
        add("podvozek", "Podvozek a řízení", "po_zadni_ramena", "Zadní náprava – ramena, silentbloky, ložiska", "po_r_bolts", "Excentrické šrouby zadní nápravy pro geometrii", "L+P")

        add("podvozek", "Podvozek a řízení", "po_rizeni", "Řízení, čepy a geometrie", "po_st_rack", "Hřebenové řízení / převodka řízení", "")
        add("podvozek", "Podvozek a řízení", "po_rizeni", "Řízení, čepy a geometrie", "po_st_out_l", "Kulový čep řízení vnější – Levý", "L")
        add("podvozek", "Podvozek a řízení", "po_rizeni", "Řízení, čepy a geometrie", "po_st_out_r", "Kulový čep řízení vnější – Pravý", "P")
        add("podvozek", "Podvozek a řízení", "po_rizeni", "Řízení, čepy a geometrie", "po_st_in_l", "Axiální táhlo řízení vnitřní – Levé", "L")
        add("podvozek", "Podvozek a řízení", "po_rizeni", "Řízení, čepy a geometrie", "po_st_in_r", "Axiální táhlo řízení vnitřní – Pravé", "P")
        add("podvozek", "Podvozek a řízení", "po_rizeni", "Řízení, čepy a geometrie", "po_st_boot_l", "Manžeta řízení – Levá", "L")
        add("podvozek", "Podvozek a řízení", "po_rizeni", "Řízení, čepy a geometrie", "po_st_boot_r", "Manžeta řízení – Pravá", "P")
        add("podvozek", "Podvozek a řízení", "po_rizeni", "Řízení, čepy a geometrie", "po_geo", "Seřízení geometrie kol (přední + zadní)", "")

        add("podvozek", "Podvozek a řízení", "po_kola", "Pneumatiky a kola", "po_t_sum", "Letní pneumatiky (sada / přezutí)", "")
        add("podvozek", "Podvozek a řízení", "po_kola", "Pneumatiky a kola", "po_t_win", "Zimní pneumatiky (sada / přezutí)", "")
        add("podvozek", "Podvozek a řízení", "po_kola", "Pneumatiky a kola", "po_t_bal", "Vyvážení kol a kontrola ráfků", "")

        // 5. ELEKTRIKA A OSVĚTLENÍ
        add("elektrika", "Elektrika a osvětlení", "el_zdroj", "Baterie, alternátor a startér", "el_bat", "Autobaterie 12V (AGM / EFB + nakódování)", "", 0, "Typicky 68–70 Ah AGM nebo EFB")
        add("elektrika", "Elektrika a osvětlení", "el_zdroj", "Baterie, alternátor a startér", "el_alt", "Alternátor (repase / výměna)", "")
        add("elektrika", "Elektrika a osvětlení", "el_zdroj", "Baterie, alternátor a startér", "el_start", "Startér (repase / výměna)", "")
        add("elektrika", "Elektrika a osvětlení", "el_zdroj", "Baterie, alternátor a startér", "el_keybat", "Baterie v dálkovém ovladači / klíčku (CR2025/CR2032)", "")

        add("elektrika", "Elektrika a osvětlení", "el_svetla", "Světlomety, výbojky a žárovky", "el_xen_l", "Xenonová výbojka (D1S / D3S) / Hlavní žárovka – Levá", "L")
        add("elektrika", "Elektrika a osvětlení", "el_svetla", "Světlomety, výbojky a žárovky", "el_xen_r", "Xenonová výbojka (D1S / D3S) / Hlavní žárovka – Pravá", "P")
        add("elektrika", "Elektrika a osvětlení", "el_svetla", "Světlomety, výbojky a žárovky", "el_bal_l", "Řídicí jednotka (ballast) světlometu – Levá", "L")
        add("elektrika", "Elektrika a osvětlení", "el_svetla", "Světlomety, výbojky a žárovky", "el_bal_r", "Řídicí jednotka (ballast) světlometu – Pravá", "P")
        add("elektrika", "Elektrika a osvětlení", "el_svetla", "Světlomety, výbojky a žárovky", "el_fog_l", "Přední mlhovka / Corner žárovka – Levá", "L")
        add("elektrika", "Elektrika a osvětlení", "el_svetla", "Světlomety, výbojky a žárovky", "el_fog_r", "Přední mlhovka / Corner žárovka – Pravá", "P")
        add("elektrika", "Elektrika a osvětlení", "el_svetla", "Světlomety, výbojky a žárovky", "el_rear_l", "Zadní světlo / žárovky – Levé", "L")
        add("elektrika", "Elektrika a osvětlení", "el_svetla", "Světlomety, výbojky a žárovky", "el_rear_r", "Zadní světlo / žárovky – Pravé", "P")
        add("elektrika", "Elektrika a osvětlení", "el_svetla", "Světlomety, výbojky a žárovky", "el_spz", "Osvětlení SPZ (LED modul / žárovky)", "")

        add("elektrika", "Elektrika a osvětlení", "el_senzory", "Čidla, parkovací senzory a jednotky", "el_pdc_f", "Parkovací senzor (PDC) – Přední", "PŘ")
        add("elektrika", "Elektrika a osvětlení", "el_senzory", "Čidla, parkovací senzory a jednotky", "el_pdc_r", "Parkovací senzor (PDC) – Zadní", "ZAD")
        add("elektrika", "Elektrika a osvětlení", "el_senzory", "Čidla, parkovací senzory a jednotky", "el_lvl_f", "Čidlo naklápění světel (nivelace) – Přední", "PŘ")
        add("elektrika", "Elektrika a osvětlení", "el_senzory", "Čidla, parkovací senzory a jednotky", "el_lvl_r", "Čidlo naklápění světel (nivelace) – Zadní", "ZAD")

        // 6. KLIMATIZACE A TOPENÍ
        add("klima", "Klimatizace a topení", "kl_udrzba", "Kabinový filtr a plnění klimatizace", "kl_cabin", "Kabinový (pylový) filtr s aktivním uhlím", "", 15000, "Měnit ideálně 1x ročně")
        add("klima", "Klimatizace a topení", "kl_udrzba", "Kabinový filtr a plnění klimatizace", "kl_fill", "Doplnění chladiva a oleje klimatizace", "", 60000)
        add("klima", "Klimatizace a topení", "kl_udrzba", "Kabinový filtr a plnění klimatizace", "kl_ozon", "Dezinfekce / ozónování klimatizace", "")

        add("klima", "Klimatizace a topení", "kl_dily", "Kompresor, chladič a klapky topení", "kl_comp", "Klimakompresor (kompresor klimatizace)", "")
        add("klima", "Klimatizace a topení", "kl_dily", "Kompresor, chladič a klapky topení", "kl_cond", "Chladič klimatizace (kondenzátor) + vysoušeč", "")
        add("klima", "Klimatizace a topení", "kl_dily", "Kompresor, chladič a klapky topení", "kl_press", "Tlakový snímač klimatizace", "")
        add("klima", "Klimatizace a topení", "kl_dily", "Kompresor, chladič a klapky topení", "kl_fan", "Kabinkový ventilátor topení + regulátor", "")
        add("klima", "Klimatizace a topení", "kl_dily", "Kompresor, chladič a klapky topení", "kl_rad", "Výměník (radiátor) topení v palubní desce", "")
        add("klima", "Klimatizace a topení", "kl_dily", "Kompresor, chladič a klapky topení", "kl_flap_l", "Servomotorek teplotní klapky topení – Levý", "L")
        add("klima", "Klimatizace a topení", "kl_dily", "Kompresor, chladič a klapky topení", "kl_flap_r", "Servomotorek teplotní klapky topení – Pravý", "P")

        // 7. KAROSERIE, SKLA A DVEŘE
        add("karoserie", "Karoserie, skla a dveře", "ka_skla", "Stěrače, skla a ostřikovače", "ka_wip_f", "Přední stěrače (sada L+P)", "L+P", 30000)
        add("karoserie", "Karoserie, skla a dveře", "ka_skla", "Stěrače, skla a ostřikovače", "ka_wip_r", "Zadní stěrač", "ZAD", 30000)
        add("karoserie", "Karoserie, skla a dveře", "ka_skla", "Stěrače, skla a ostřikovače", "ka_wip_mot", "Motorek a táhla předních stěračů", "")
        add("karoserie", "Karoserie, skla a dveře", "ka_skla", "Stěrače, skla a ostřikovače", "ka_wip_rmot", "Motorek zadního stěrače", "")
        add("karoserie", "Karoserie, skla a dveře", "ka_skla", "Stěrače, skla a ostřikovače", "ka_glass_f", "Čelní sklo", "")
        add("karoserie", "Karoserie, skla a dveře", "ka_skla", "Stěrače, skla a ostřikovače", "ka_wash_p", "Čerpadlo ostřikovačů skel / světlometů", "")

        add("karoserie", "Karoserie, skla a dveře", "ka_dvere", "Vzpěry, zámky, stahovačky a svazky", "ka_str_tl", "Vzpěra 5. dveří / kufru (elektrická / plynová) – Levá", "L")
        add("karoserie", "Karoserie, skla a dveře", "ka_dvere", "Vzpěry, zámky, stahovačky a svazky", "ka_str_tr", "Vzpěra 5. dveří / kufru (elektrická / plynová) – Pravá", "P")
        add("karoserie", "Karoserie, skla a dveře", "ka_dvere", "Vzpěry, zámky, stahovačky a svazky", "ka_str_hood", "Plynová vzpěra přední kapoty", "")
        add("karoserie", "Karoserie, skla a dveře", "ka_dvere", "Vzpěry, zámky, stahovačky a svazky", "ka_lck_fl", "Zámek dveří – Levý přední (řidič)", "LP")
        add("karoserie", "Karoserie, skla a dveře", "ka_dvere", "Vzpěry, zámky, stahovačky a svazky", "ka_lck_fr", "Zámek dveří – Pravý přední (spolujezdec)", "PP")
        add("karoserie", "Karoserie, skla a dveře", "ka_dvere", "Vzpěry, zámky, stahovačky a svazky", "ka_lck_rl", "Zámek dveří – Levý zadní", "LZ")
        add("karoserie", "Karoserie, skla a dveře", "ka_dvere", "Vzpěry, zámky, stahovačky a svazky", "ka_lck_rr", "Zámek dveří – Pravý zadní", "PZ")
        add("karoserie", "Karoserie, skla a dveře", "ka_dvere", "Vzpěry, zámky, stahovačky a svazky", "ka_lck_tr", "Zámek kufru / tlačítko 5. dveří", "ZAD")
        add("karoserie", "Karoserie, skla a dveře", "ka_dvere", "Vzpěry, zámky, stahovačky a svazky", "ka_win_fl", "Stahovačka okna – Levá přední", "LP")
        add("karoserie", "Karoserie, skla a dveře", "ka_dvere", "Vzpěry, zámky, stahovačky a svazky", "ka_win_fr", "Stahovačka okna – Pravá přední", "PP")
        add("karoserie", "Karoserie, skla a dveře", "ka_dvere", "Vzpěry, zámky, stahovačky a svazky", "ka_loom_fl", "Kabelový svazek v průchodce řidičových dveří", "LP")

        list
    }

    fun allPartsForVehicle(vehicle: Vehicle): List<PartItem> {
        return (defaultParts + vehicle.customParts).map { part ->
            val customInt = vehicle.customIntervals[part.id]
            if (customInt != null) part.copy(defaultIntervalKm = customInt) else part
        }
    }

    fun defaultVehicles(): List<Vehicle> = listOf(
        Vehicle(
            id = "car_sup3",
            name = "Škoda Superb III (2018)",
            vin = "TMBJH7NP5J7000001",
            plate = "1AU 0001",
            year = 2018,
            engineCode = "DFGA",
            engineDesc = "2.0 TDI CR (1968 ccm)",
            powerKw = 110,
            gearbox = "Automat DSG",
            color = "Šedá / Černá",
            stkUntil = "10.2026",
            tiresInfo = "215/55 R17, 235/45 R18, 235/40 R19",
            currentMileageKm = 285000
        ),
        Vehicle(
            id = "car_sup2",
            name = "Škoda Superb II (2013)",
            vin = "TMBJE73T2D9000002",
            plate = "2AU 0002",
            year = 2013,
            engineCode = "CFFB",
            engineDesc = "2.0 TDI CR (1968 ccm)",
            powerKw = 103,
            gearbox = "Automat DSG (6st.)",
            color = "Černá metalíza",
            stkUntil = "05.2027",
            tiresInfo = "205/55 R16, 225/45 R17, 225/40 R18",
            currentMileageKm = 365000
        ),
        Vehicle(
            id = "car_3",
            name = "3. Auto v garáži (Uprav podle VIN)",
            vin = "TMBJJ7NE0J0000003",
            plate = "3AU 0003",
            year = 2018,
            engineCode = "CRLB",
            engineDesc = "2.0 TDI CR (1968 ccm)",
            powerKw = 110,
            gearbox = "Manuální / DSG",
            color = "Bílá",
            stkUntil = "08.2027",
            tiresInfo = "205/55 R16, 225/45 R17",
            currentMileageKm = 195000
        )
    )

    val extraCatalogPartsBySubfolder: Map<String, List<String>> = mapOf(
        "m_naplne" to listOf("Aditivum do nafty (VIF)", "Průplach motoru (Engine Flush)", "Brzdový čistič / odmašťovač"),
        "m_filtry" to listOf("Těleso olejového filtru kompletní", "Víčko olejového filtru", "Odvodnění palivového filtru"),
        "m_rozvody" to listOf("Kryt rozvodového řemene", "Šroub klikové hřídele", "Středový ventil přestavovače vačky"),
        "m_remen" to listOf("Volnoběžka klimakompresoru", "Držák příslušenství motoru"),
        "m_palivo" to listOf("Snímač tlaku paliva na railu", "Vysokotlaká palivová trubka", "Čerpadlo přídavného topení Webasto"),
        "m_zhaveni" to listOf("Kabelová lišta žhavicích svíček", "Snímač otáček klikové hřídele (G28)", "Snímač polohy vačkové hřídele (G40)"),
        "m_sani" to listOf("Těsnění sacího potrubí", "Sací potrubí s vířivými klapkami", "Podtlakový ventil N75"),
        "m_emise" to listOf("Trubka EGR ventilu", "Vstřikovač AdBlue (redukčního činidla)", "Nádržka a čerpadlo AdBlue"),
        "m_chlazeni" to listOf("Pomocné elektrické vodní čerpadlo", "Čidlo teploty chladicí kapaliny (G62)", "Příruba chlazení na hlavě válců"),
        "m_ulozeni" to listOf("Těsnění pod hlavou válců", "Gufero vačkové hřídele", "Odlučovač oleje (PCV odvětrání)"),
        "p_naplne" to listOf("Výpustný šroub převodovky s těsněním", "Odvzdušňovací čepička převodovky"),
        "p_spojka" to listOf("Táhla (lanovody) řazení", "Kulisa řazení / volič DSG", "Jehlové ložisko v klikové hřídeli"),
        "p_poloosy" to listOf("Středový šroub poloosy v náboji", "Přírubová hřídel převodovky"),
        "b_predni" to listOf("Držák předního brzdového třmenu – Levý", "Držák předního brzdového třmenu – Pravý", "Sada pístku a těsnění předního třmenu"),
        "b_zadni" to listOf("Držák zadního brzdového třmenu – Levý", "Držák zadního brzdového třmenu – Pravý", "Tlačítko elektrické ruční brzdy + AutoHold"),
        "b_hydraulika" to listOf("Hlavní brzdový válec", "Posilovač brzd", "Podtlaková vývěva brzd"),
        "po_predni_tlumeni" to listOf("Kabeláž adaptivního tlumiče DCC – Levá", "Kabeláž adaptivního tlumiče DCC – Pravá"),
        "po_predni_ramena" to listOf("Nápravnice přední nápravy", "Těhlice předního kola – Levá", "Těhlice předního kola – Pravá"),
        "po_zadni_tlumeni" to listOf("Podložka pod zadní pružinu horní", "Podložka pod zadní pružinu spodní"),
        "po_zadni_ramena" to listOf("Nápravnice zadní nápravy", "Těhlice zadního kola – Levá", "Těhlice zadního kola – Pravá"),
        "po_rizeni" to listOf("Sloupek řízení", "Kroužek airbagu pod volantem"),
        "po_kola" to listOf("Pojistné šrouby kol", "Středové krytky kol Škoda"),
        "el_zdroj" to listOf("Regulátor napětí alternátoru", "Kabel kostry baterie se snímačem IBS"),
        "el_svetla" to listOf("Ostřikovač světlometu – Levý", "Ostřikovač světlometu – Pravý", "Třetí brzdové světlo"),
        "el_senzory" to listOf("Dešťový a světelný senzor", "Řídicí jednotka parkovacích senzorů", "Zadní parkovací kamera"),
        "kl_udrzba" to listOf("Kryt kabinového filtru", "Čištění odtokových kanálků klimatizace"),
        "kl_dily" to listOf("Regulační ventil klimakompresoru", "Expanzní ventil klimatizace", "Servomotorek klapky vnitřní cirkulace"),
        "ka_skla" to listOf("Vyhřívaná tryska ostřikovače – Levá", "Vyhřívaná tryska ostřikovače – Pravá", "Nádobka vody do ostřikovačů"),
        "ka_dvere" to listOf("Stahovačka okna – Levá zadní", "Stahovačka okna – Pravá zadní", "Vnější klika dveří", "Sklo zpětného zrcátka vyhřívané (L / P)")
    )

    fun parseServiceTextToParts(text: String, allParts: List<PartItem>): List<PartItem> {
        val norm = removeDiacritics(text.lowercase())
        val matched = mutableSetOf<PartItem>()

        fun addById(id: String) {
            allParts.find { it.id == id }?.let { matched.add(it) }
        }

        val hasLeft = norm.contains("levy") || norm.contains("leva") || norm.contains("leve") || norm.contains(" l ")
        val hasRight = norm.contains("pravy") || norm.contains("prava") || norm.contains("prave") || norm.contains(" p ")
        val hasFront = norm.contains("predni")
        val hasRear = norm.contains("zadni")

        if (norm.contains("olej")) {
            if (norm.contains("dsg") || norm.contains("prevodov")) {
                addById("p_oil")
                if (norm.contains("filtr")) addById("p_filter")
            } else if (norm.contains("haldex")) {
                addById("p_haldex")
            } else {
                addById("m_oil")
                if (norm.contains("filtr")) addById("m_f_oil")
            }
        }
        if (norm.contains("vsech filtr") || norm.contains("vsechny filtr") || norm.contains("komplet filtr")) {
            addById("m_f_oil")
            addById("m_f_air")
            addById("m_f_fuel")
            addById("kl_cabin")
        }
        if (norm.contains("olejovy filtr")) addById("m_f_oil")
        if (norm.contains("vzduchovy filtr")) addById("m_f_air")
        if (norm.contains("palivovy filtr") || norm.contains("naftovy filtr")) addById("m_f_fuel")
        if (norm.contains("kabinovy") || norm.contains("pylovy")) addById("kl_cabin")

        if (norm.contains("rozvod")) {
            addById("m_r_set")
            addById("m_r_pump")
            addById("m_r_tens")
            addById("m_r_guide")
        }
        if (norm.contains("vodni pump") || norm.contains("cerpadlo chlazen")) addById("m_r_pump")
        if (norm.contains("drazkov") || norm.contains("klinov")) addById("m_d_belt")

        if (norm.contains("kotouc")) {
            if (hasFront || !hasRear) addById("b_f_disc")
            if (hasRear) addById("b_r_disc")
        }
        if (norm.contains("destick") || norm.contains("desky")) {
            if (hasFront || !hasRear) addById("b_f_pad")
            if (hasRear) addById("b_r_pad")
        }
        if (norm.contains("brzdov") && norm.contains("kapalin")) addById("b_fluid")

        if (norm.contains("tlumic")) {
            if (hasFront || !hasRear) {
                if (hasLeft || !hasRight) addById("po_f_sh_l")
                if (hasRight || !hasLeft) addById("po_f_sh_r")
            }
            if (hasRear) {
                if (hasLeft || !hasRight) addById("po_r_sh_l")
                if (hasRight || !hasLeft) addById("po_r_sh_r")
            }
        }
        if (norm.contains("pruzin")) {
            if (hasFront || !hasRear) {
                if (hasLeft || !hasRight) addById("po_f_sp_l")
                if (hasRight || !hasLeft) addById("po_f_sp_r")
            }
            if (hasRear) {
                if (hasLeft || !hasRight) addById("po_r_sp_l")
                if (hasRight || !hasLeft) addById("po_r_sp_r")
            }
        }
        if (norm.contains("stabilizator") || norm.contains("tyck")) {
            if (hasRear) {
                if (hasLeft || !hasRight) addById("po_r_stab_l")
                if (hasRight || !hasLeft) addById("po_r_stab_r")
            } else {
                if (hasLeft || !hasRight) addById("po_f_stab_l")
                if (hasRight || !hasLeft) addById("po_f_stab_r")
            }
        }
        if (norm.contains("cep") && norm.contains("rizeni")) {
            if (hasLeft || !hasRight) addById("po_st_out_l")
            if (hasRight || !hasLeft) addById("po_st_out_r")
        } else if (norm.contains("cep")) {
            if (hasLeft || !hasRight) addById("po_f_ball_l")
            if (hasRight || !hasLeft) addById("po_f_ball_r")
        }
        if (norm.contains("lozisk") && norm.contains("kol")) {
            if (hasRear) {
                if (hasLeft || !hasRight) addById("po_r_hub_l")
                if (hasRight || !hasLeft) addById("po_r_hub_r")
            } else {
                if (hasLeft || !hasRight) addById("po_f_hub_l")
                if (hasRight || !hasLeft) addById("po_f_hub_r")
            }
        }
        if (norm.contains("geometri")) addById("po_geo")
        if (norm.contains("dvouhmot") || norm.contains("setrvacnik")) addById("p_dmf")
        if (norm.contains("spojk")) addById("p_clutch")
        if (norm.contains("bateri") || norm.contains("akumulator")) addById("el_bat")
        if (norm.contains("sterac")) {
            if (hasRear) addById("ka_wip_r") else addById("ka_wip_f")
        }

        allParts.forEach { part ->
            val pNorm = removeDiacritics(part.name.lowercase())
            val words = pNorm.split(" ", "–", "-", "(", ")", "/").filter { it.length >= 5 }
            if (words.size >= 2 && words.take(2).all { norm.contains(it) }) {
                matched.add(part)
            }
        }

        return matched.toList()
    }

    fun removeDiacritics(input: String): String {
        val map = mapOf(
            'á' to 'a', 'č' to 'c', 'ď' to 'd', 'é' to 'e', 'ě' to 'e',
            'í' to 'i', 'ň' to 'n', 'ó' to 'o', 'ř' to 'r', 'š' to 's',
            'ť' to 't', 'ú' to 'u', 'ů' to 'u', 'ý' to 'y', 'ž' to 'z'
        )
        val sb = StringBuilder(input.length)
        for (ch in input) {
            sb.append(map[ch] ?: ch)
        }
        return sb.toString()
    }
}

object VinAndCatalogService {

    suspend fun decodeVin(vinInput: String, apiKey: String): VinDecodedData {
        val vin = vinInput.trim().uppercase()
        val fallbackYear = decodeYearFromVinChar(vin)
        val fallbackMake = decodeMakeFromVin(vin)
        val fallbackModelInfo = decodeModelAndEngineFromVin(vin, fallbackMake, fallbackYear)

        if (apiKey.isNotBlank()) {
            val url = "https://api.dataovozidlech.cz/api/vehicletechnicaldata/v2?vin=$vin"
            val res = GaragePlatform.httpGet(url, mapOf("api_key" to apiKey.trim(), "Accept" to "application/json"))
            if (res.isSuccess) {
                val json = res.getOrNull().orEmpty()
                if (json.contains("TovarniZnacka") || json.contains("ObchodniOznaceni") || json.contains("MotorTyp")) {
                    val regDatum = extractJsonValue(json, "DatumPrvniRegistrace")
                    val rok = regDatum.take(4).toIntOrNull() ?: fallbackYear

                    val rawZnacka = extractJsonValue(json, "TovarniZnacka").ifBlank { fallbackMake }
                    val znacka = formatBrandName(rawZnacka)

                    val rawModel = extractJsonValue(json, "ObchodniOznaceni")
                        .ifBlank { extractJsonValue(json, "ObchodniOzn") }
                        .ifBlank { extractJsonValue(json, "Typ") }
                        .ifBlank { fallbackModelInfo.first }

                    val cleanModel = cleanModelName(znacka, rawModel)

                    val typMotoru = extractJsonValue(json, "MotorTyp")
                        .ifBlank { fallbackModelInfo.second }
                    val vykonStr = extractJsonValue(json, "MotorMaxVykon")
                    val vykonKw = vykonStr.takeWhile { it.isDigit() }.toIntOrNull() ?: fallbackModelInfo.third
                    val objemStr = extractJsonValue(json, "MotorZdvihObjem").takeWhile { it.isDigit() }
                    val palivo = extractJsonValue(json, "Palivo")
                    val motorPopis = formatEngineDescription(typMotoru, objemStr, palivo)

                    val prevodovka = extractJsonValue(json, "PrevodovkaDruh").ifBlank { "Automat / Manuál" }
                    val barva = extractJsonValue(json, "Barva")
                        .ifBlank { extractJsonValue(json, "VozidloKaroserieBarva") }
                        .ifBlank { "Neuvedena" }
                    val stkDo = extractJsonValue(json, "PravidelnaTechnickaProhlidkaDo").take(10)

                    val rawBaseTires = extractJsonValue(json, "NapravyPneuRafky")
                    val rawDalsiZaznamy = extractJsonValue(json, "DalsiZaznamy")
                    val allTiresFormatted = parseAllTiresFromTp(rawBaseTires, rawDalsiZaznamy)

                    val fullTitle = if (cleanModel.isNotBlank()) {
                        "$znacka $cleanModel ($rok)"
                    } else {
                        "$znacka ($rok)"
                    }

                    return VinDecodedData(
                        name = fullTitle,
                        year = rok,
                        engineCode = typMotoru,
                        engineDesc = motorPopis,
                        powerKw = vykonKw,
                        gearbox = prevodovka,
                        color = barva,
                        stkUntil = stkDo.ifBlank { "Neuvedeno" },
                        tiresInfo = allTiresFormatted.ifBlank { "205/55 R16, 225/45 R17, 235/45 R18" },
                        sourceInfo = "Načteno z Registru silničních vozidel MDČR (včetně alternativních pneu z TP)"
                    )
                }
            }
        }

        val fullFallbackTitle = if (fallbackModelInfo.first.isNotBlank()) {
            "$fallbackMake ${fallbackModelInfo.first} ($fallbackYear)"
        } else {
            "$fallbackMake ($fallbackYear)"
        }

        return VinDecodedData(
            name = fullFallbackTitle,
            year = fallbackYear,
            engineCode = fallbackModelInfo.second,
            engineDesc = "TDI / TSI",
            powerKw = fallbackModelInfo.third,
            gearbox = "Automat / Manuál",
            color = "Neuvedena",
            stkUntil = "10.${fallbackYear + 8}",
            tiresInfo = if (fallbackYear >= 2016) "215/55 R17, 235/45 R18, 235/40 R19" else "205/55 R16, 225/45 R17, 225/40 R18",
            sourceInfo = "Rozklíčováno z VIN struktury (vozidlo nebylo nalezeno v online registru MDČR)"
        )
    }

    private fun formatBrandName(raw: String): String {
        val u = raw.trim().uppercase()
        return when {
            u.contains("ŠKODA") || u.contains("SKODA") -> "Škoda"
            u.contains("VOLKSWAGEN") || u == "VW" -> "Volkswagen"
            u.contains("AUDI") -> "Audi"
            u.contains("SEAT") -> "Seat"
            u.contains("CUPRA") -> "Cupra"
            u.contains("BMW") -> "BMW"
            u.contains("MERCEDES") -> "Mercedes-Benz"
            u.contains("FORD") -> "Ford"
            raw.isNotBlank() -> raw.trim().lowercase().replaceFirstChar { it.uppercase() }
            else -> "Vozidlo"
        }
    }

    private fun cleanModelName(brand: String, rawModel: String): String {
        var m = rawModel.trim()
        if (m.uppercase().startsWith(brand.uppercase())) {
            m = m.substring(brand.length).trim()
        }
        return m
    }

    private fun formatEngineDescription(engineCode: String, displacementCcm: String, fuelRaw: String): String {
        val ccm = displacementCcm.toIntOrNull()
        val litersStr = if (ccm != null && ccm > 500) {
            val tenths = (ccm / 100.0).roundToInt()
            "${tenths / 10}.${tenths % 10}"
        } else ""

        val fUp = fuelRaw.uppercase()
        val fuelLabel = when {
            fUp.contains("NM") || fUp.contains("NAFTA") || fUp.contains("DIESEL") -> "TDI"
            fUp.contains("BA") || fUp.contains("BENZ") -> "TSI / TFSI"
            fUp.contains("EL") -> "Elektro"
            fUp.contains("CNG") -> "G-TEC / CNG"
            fUp.contains("LPG") -> "LPG"
            else -> ""
        }

        val base = listOf(litersStr, fuelLabel).filter { it.isNotBlank() }.joinToString(" ")
        return when {
            base.isNotBlank() && ccm != null -> "$base ($ccm ccm)"
            ccm != null -> "$engineCode ($ccm ccm)"
            else -> engineCode
        }
    }

    private fun decodeMakeFromVin(vin: String): String {
        return when {
            vin.startsWith("TMB") -> "Škoda"
            vin.startsWith("WAU") || vin.startsWith("WUA") || vin.startsWith("TRU") -> "Audi"
            vin.startsWith("WVW") || vin.startsWith("WVG") || vin.startsWith("WV1") || vin.startsWith("WV2") || vin.startsWith("3VW") -> "Volkswagen"
            vin.startsWith("VSS") -> "Seat"
            else -> "Vozidlo"
        }
    }

    private fun decodeModelAndEngineFromVin(vin: String, make: String, year: Int): Triple<String, String, Int> {
        val code68 = if (vin.length >= 8) vin.substring(6, 8) else ""
        val bodyPart = if (vin.length >= 9) vin.substring(3, 9) else ""

        return when (make) {
            "Audi" -> {
                val audiModel = when {
                    code68 in listOf("8P", "8V", "8Y") || bodyPart.contains("8V") || bodyPart.contains("8P") -> "A3"
                    code68 in listOf("8E", "8K", "8W", "F4") || bodyPart.contains("8K") || bodyPart.contains("8W") || bodyPart.contains("F4") -> "A4"
                    code68 in listOf("8T", "8F", "F5") || bodyPart.contains("F5") || bodyPart.contains("8T") -> "A5"
                    code68 in listOf("4B", "4F", "4G", "4A", "F2") || bodyPart.contains("4G") || bodyPart.contains("4F") || bodyPart.contains("4A") || bodyPart.contains("F2") -> "A6"
                    code68 in listOf("4K") || bodyPart.contains("4K") -> "A7"
                    code68 in listOf("4E", "4H", "4N") || bodyPart.contains("4H") || bodyPart.contains("4N") -> "A8"
                    code68 in listOf("8U", "F3") || bodyPart.contains("8U") || bodyPart.contains("F3") -> "Q3"
                    code68 in listOf("8R", "FY") || bodyPart.contains("8R") || bodyPart.contains("FY") -> "Q5"
                    code68 in listOf("4L", "4M") || bodyPart.contains("4L") || bodyPart.contains("4M") -> "Q7"
                    code68 in listOf("F8") || bodyPart.contains("F8") -> "Q8"
                    code68 in listOf("8J", "FV") -> "TT"
                    else -> "A4 / A6"
                }
                Triple(audiModel, "TDI / TFSI", 140)
            }
            "Volkswagen" -> {
                val vwModel = when {
                    code68 in listOf("3C", "3B", "3G", "CB") || bodyPart.contains("3C") || bodyPart.contains("3G") -> "Passat"
                    code68 in listOf("1J", "1K", "5K", "AU", "CD", "BV") || bodyPart.contains("AU") || bodyPart.contains("1K") -> "Golf"
                    code68 in listOf("5N", "AD", "AX") || bodyPart.contains("5N") || bodyPart.contains("AD") -> "Tiguan"
                    code68 in listOf("1T", "5T") || bodyPart.contains("1T") -> "Touran"
                    code68 in listOf("7L", "7P", "CR") || bodyPart.contains("7P") || bodyPart.contains("CR") -> "Touareg"
                    code68 in listOf("3H") || bodyPart.contains("3H") -> "Arteon"
                    code68 in listOf("2K", "SA", "SB") || bodyPart.contains("2K") -> "Caddy"
                    code68 in listOf("7H", "7J", "7E", "ST") || bodyPart.contains("7H") -> "Transporter / Multivan"
                    code68 in listOf("6R", "6C", "AW") -> "Polo"
                    code68 in listOf("7M", "7N") -> "Sharan"
                    else -> "Passat / Golf"
                }
                Triple(vwModel, "2.0 TDI / TSI", 110)
            }
            "Škoda" -> {
                when (code68) {
                    "NP", "NZ" -> Triple("Superb III", "DFGA / CRLB", 110)
                    "3T" -> Triple("Superb II", "CFFB / CFGB", 103)
                    "3U" -> Triple("Superb I", "AWX / BSS", 96)
                    "NE" -> Triple("Octavia III", "CRLB / CKFC", 110)
                    "NX" -> Triple("Octavia IV", "DTTC / DSTB", 110)
                    "1Z" -> Triple("Octavia II", "CFHC / BKD", 103)
                    "1U" -> Triple("Octavia I", "ALH / ASV", 66)
                    "NS" -> Triple("Kodiaq", "DFGA / DFHA", 140)
                    "NU" -> Triple("Karoq", "DFFA / DFGA", 110)
                    "NW" -> Triple("Scala / Kamiq", "DGTA / DXDB", 85)
                    "NJ", "PJ", "5J" -> Triple("Fabia", "CHZB / CAYA", 70)
                    "5L" -> Triple("Yeti", "CFHC / CBDB", 103)
                    "NH" -> Triple("Rapid", "CAYB / CBZB", 77)
                    else -> if (year >= 2015) Triple("Superb III", "DFGA", 110) else Triple("Superb II", "CFFB", 103)
                }
            }
            "Seat" -> {
                val seatModel = when (code68) {
                    "5F", "KL", "1P" -> "Leon"
                    "KH" -> "Ateca"
                    "KN" -> "Tarraco"
                    "6J", "KJ" -> "Ibiza"
                    "7V", "7N" -> "Alhambra"
                    else -> "Leon / Ateca"
                }
                Triple(seatModel, "2.0 TDI / TSI", 110)
            }
            else -> Triple("Vozidlo", "Motor", 110)
        }
    }

    private fun parseAllTiresFromTp(rawBase: String, rawNotes: String): String {
        val uniqueItems = linkedSetOf<String>()

        fun cleanChunk(s: String): String {
            return s
                .replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\\r", "\n")
                .replace("\\t", " ")
                .replace("\\/", "/")
                .replace("\\\"", "\"")
                .trim()
        }

        val cleanedBase = cleanChunk(rawBase)
        val cleanedNotes = cleanChunk(rawNotes)

        cleanedBase.split("\n", ";").forEach { line ->
            val trimmed = line
                .replace(Regex("""^[1-4]\s*[:.-]\s*"""), "")
                .trim(' ', '/', ';', ',', '-')
                .replace(Regex("""\s+"""), " ")
            if (trimmed.length >= 6 && trimmed.any { it.isDigit() }) {
                uniqueItems.add(trimmed)
            }
        }

        val comboRegex = Regex(
            """(\d{3}\s*/\s*\d{2,3}\s*Z?R\s*\d{2}\s*[0-9]{0,3}\s*[A-Z]{0,2}(?:\s*M\+S|\s*XL)?(?:\s*(?:NA RÁFKU|/|;|,|-|\s)\s*\d+(?:[.,]\d+)?\s*J\s*[xX]\s*\d{2}\s*(?:H2\s*)?(?:ET\s*-?\d+(?:[.,]\d+)?)?)?)""",
            RegexOption.IGNORE_CASE
        )
        comboRegex.findAll(cleanedNotes).forEach { m ->
            val found = m.value
                .replace(Regex("""\s+"""), " ")
                .replace("NA RÁFKU ", "")
                .trim(' ', '/', ';', ',', '-')
            if (found.length >= 7) {
                val normFound = found.uppercase().replace(" ", "")
                val alreadyPresent = uniqueItems.any { existing ->
                    val normEx = existing.uppercase().replace(" ", "")
                    normEx.contains(normFound) || normFound.contains(normEx)
                }
                if (!alreadyPresent) {
                    uniqueItems.add(found)
                } else {
                    val shorter = uniqueItems.find { existing ->
                        val normEx = existing.uppercase().replace(" ", "")
                        normFound.contains(normEx) && found.length > existing.length
                    }
                    if (shorter != null) {
                        uniqueItems.remove(shorter)
                        uniqueItems.add(found)
                    }
                }
            }
        }

        return uniqueItems.joinToString(" • ")
    }

    suspend fun fetchCatalogPartsForSubfolder(subfolder: SubfolderDef): List<String> {
        val resultSet = linkedSetOf<String>()
        GarageCatalog.extraCatalogPartsBySubfolder[subfolder.id]?.forEach { resultSet.add(it) }

        val encodedQuery = subfolder.catalogQuery.replace(" ", "+")
        val url = "https://www.skoda-dily.cz/hledani.html?q=$encodedQuery"
        val httpRes = GaragePlatform.httpGet(url)
        if (httpRes.isSuccess) {
            val html = httpRes.getOrNull().orEmpty()
            val regex = Regex("""<h2[^>]*>\s*<a[^>]*>([^<]{4,75})</a>\s*</h2>""", RegexOption.IGNORE_CASE)
            regex.findAll(html).take(12).forEach { match ->
                val rawTitle = match.groupValues[1]
                    .replace("&nbsp;", " ")
                    .replace("&amp;", "&")
                    .replace("&quot;", "\"")
                    .trim()
                if (rawTitle.isNotBlank()) {
                    resultSet.add(rawTitle)
                }
            }
        }
        return resultSet.toList()
    }

    private fun decodeYearFromVinChar(vin: String): Int {
        if (vin.length < 10) return 2018
        return when (vin[9]) {
            'A' -> 2010
            'B' -> 2011
            'C' -> 2012
            'D' -> 2013
            'E' -> 2014
            'F' -> 2015
            'G' -> 2016
            'H' -> 2017
            'J' -> 2018
            'K' -> 2019
            'L' -> 2020
            'M' -> 2021
            'N' -> 2022
            'P' -> 2023
            'R' -> 2024
            'S' -> 2025
            'T' -> 2026
            else -> 2018
        }
    }

    private fun extractJsonValue(json: String, key: String): String {
        val regex = Regex(""""$key"\s*:\s*(?:"((?:\\.|[^"\\])*)"|([0-9.]+))""", RegexOption.IGNORE_CASE)
        val match = regex.find(json) ?: return ""
        val raw = (match.groupValues[1].ifBlank { match.groupValues[2] }).trim()
        return raw
            .replace("\\r\\n", "\n")
            .replace("\\n", "\n")
            .replace("\\r", "\n")
            .replace("\\\"", "\"")
            .replace("\\/", "/")
            .trim()
    }
}

object GarageStorage {
    private const val KEY_VEHICLES = "garaz_vehicles_v1"
    private const val KEY_MDCR_API = "garaz_mdcr_api_key_v1"
    private const val KEY_ONEDRIVE_URI = "garaz_onedrive_uri_v1"
    private const val KEY_WEEKLY_ENABLED = "garaz_weekly_backup_enabled_v1"
    private const val KEY_LAST_BACKUP_MS = "garaz_last_backup_ms_v1"
    private const val KEY_LAST_BACKUP_DATE = "garaz_last_backup_date_v1"
    private const val SEVEN_DAYS_MS = 7L * 24L * 60L * 60L * 1000L

    fun loadApiKey(): String = GaragePlatform.loadString(KEY_MDCR_API, "")
    fun saveApiKey(key: String) = GaragePlatform.saveString(KEY_MDCR_API, key.trim())

    fun loadOneDriveUri(): String = GaragePlatform.loadString(KEY_ONEDRIVE_URI, "")
    fun saveOneDriveUri(uri: String) = GaragePlatform.saveString(KEY_ONEDRIVE_URI, uri)

    fun isWeeklyBackupEnabled(): Boolean = GaragePlatform.loadString(KEY_WEEKLY_ENABLED, "true") == "true"
    fun setWeeklyBackupEnabled(enabled: Boolean) = GaragePlatform.saveString(KEY_WEEKLY_ENABLED, if (enabled) "true" else "false")

    fun loadLastBackupDate(): String = GaragePlatform.loadString(KEY_LAST_BACKUP_DATE, "Zatím neprovedena")

    private fun recordBackupSuccess() {
        GaragePlatform.saveString(KEY_LAST_BACKUP_MS, GaragePlatform.currentTimeMillis().toString())
        GaragePlatform.saveString(KEY_LAST_BACKUP_DATE, GaragePlatform.currentDateString())
    }

    fun checkAndRunWeeklyBackupIfDue(vehicles: List<Vehicle>): Boolean {
        if (!isWeeklyBackupEnabled()) return false
        val uri = loadOneDriveUri()
        if (uri.isBlank()) return false
        val lastMs = GaragePlatform.loadString(KEY_LAST_BACKUP_MS, "0").toLongOrNull() ?: 0L
        val nowMs = GaragePlatform.currentTimeMillis()
        if (nowMs - lastMs >= SEVEN_DAYS_MS) {
            val content = serializeVehicles(vehicles)
            if (GaragePlatform.writeBackupFileUri(uri, content)) {
                recordBackupSuccess()
                return true
            }
        }
        return false
    }

    fun triggerImmediateOneDriveBackup(vehicles: List<Vehicle>): Boolean {
        val uri = loadOneDriveUri()
        if (uri.isBlank()) return false
        val content = serializeVehicles(vehicles)
        val ok = GaragePlatform.writeBackupFileUri(uri, content)
        if (ok) recordBackupSuccess()
        return ok
    }

    fun loadVehicles(): List<Vehicle> {
        val raw = GaragePlatform.loadString(KEY_VEHICLES, "")
        if (raw.isBlank()) return GarageCatalog.defaultVehicles()
        return deserializeVehicles(raw).ifEmpty { GarageCatalog.defaultVehicles() }
    }

    fun saveVehicles(vehicles: List<Vehicle>) {
        GaragePlatform.saveString(KEY_VEHICLES, serializeVehicles(vehicles))
    }

    private fun esc(s: String): String = s
        .replace("\\", "\\\\")
        .replace("|", "\\p")
        .replace("~", "\\t")
        .replace("^", "\\c")
        .replace("\n", "\\n")
        .replace("\r", "")

    private fun unesc(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            if (s[i] == '\\' && i + 1 < s.length) {
                when (s[i + 1]) {
                    '\\' -> sb.append('\\')
                    'p' -> sb.append('|')
                    't' -> sb.append('~')
                    'c' -> sb.append('^')
                    'n' -> sb.append('\n')
                    else -> sb.append(s[i + 1])
                }
                i += 2
            } else {
                sb.append(s[i])
                i++
            }
        }
        return sb.toString()
    }

    fun serializeVehicles(vehicles: List<Vehicle>): String {
        return vehicles.joinToString("\n") { v ->
            val recs = v.records.joinToString("~") { r ->
                listOf(esc(r.id), esc(r.partId), esc(r.date), r.mileageKm.toString(), esc(r.brandOrSpec), esc(r.note)).joinToString("^")
            }
            val cust = v.customParts.joinToString("~") { p ->
                listOf(
                    esc(p.id), esc(p.categoryId), esc(p.categoryName),
                    esc(p.subfolderId), esc(p.subfolderName), esc(p.name),
                    esc(p.positionTag), p.defaultIntervalKm.toString(), esc(p.specHint)
                ).joinToString("^")
            }
            val intervals = v.customIntervals.entries.joinToString("~") { (k, km) ->
                "${esc(k)}^$km"
            }
            listOf(
                esc(v.id), esc(v.name), esc(v.vin), esc(v.plate),
                v.year.toString(), esc(v.engineCode), esc(v.engineDesc),
                v.powerKw.toString(), esc(v.gearbox), esc(v.color),
                esc(v.stkUntil), esc(v.tiresInfo), v.currentMileageKm.toString(),
                recs, cust, intervals
            ).joinToString("|")
        }
    }

    fun deserializeVehicles(data: String): List<Vehicle> {
        return data.lines().filter { it.isNotBlank() }.mapNotNull { line ->
            val cols = line.split("|")
            if (cols.size < 13) return@mapNotNull null
            val recs = if (cols.size >= 14 && cols[13].isNotBlank()) {
                cols[13].split("~").mapNotNull { rStr ->
                    val rc = rStr.split("^")
                    if (rc.size >= 6) {
                        ReplacementRecord(
                            id = unesc(rc[0]),
                            partId = unesc(rc[1]),
                            date = unesc(rc[2]),
                            mileageKm = rc[3].toIntOrNull() ?: 0,
                            brandOrSpec = unesc(rc[4]),
                            note = unesc(rc[5])
                        )
                    } else null
                }
            } else emptyList()

            val cust = if (cols.size >= 15 && cols[14].isNotBlank()) {
                cols[14].split("~").mapNotNull { pStr ->
                    val pc = pStr.split("^")
                    if (pc.size >= 9) {
                        PartItem(
                            id = unesc(pc[0]),
                            categoryId = unesc(pc[1]),
                            categoryName = unesc(pc[2]),
                            subfolderId = unesc(pc[3]),
                            subfolderName = unesc(pc[4]),
                            name = unesc(pc[5]),
                            positionTag = unesc(pc[6]),
                            defaultIntervalKm = pc[7].toIntOrNull() ?: 0,
                            specHint = unesc(pc[8]),
                            isCustom = true
                        )
                    } else null
                }
            } else emptyList()

            val intervals = if (cols.size >= 16 && cols[15].isNotBlank()) {
                cols[15].split("~").mapNotNull { iStr ->
                    val ic = iStr.split("^")
                    if (ic.size >= 2) {
                        val km = ic[1].toIntOrNull()
                        if (km != null) unesc(ic[0]) to km else null
                    } else null
                }.toMap()
            } else emptyMap()

            Vehicle(
                id = unesc(cols[0]),
                name = unesc(cols[1]),
                vin = unesc(cols[2]),
                plate = unesc(cols[3]),
                year = cols[4].toIntOrNull() ?: 2018,
                engineCode = unesc(cols[5]),
                engineDesc = unesc(cols[6]),
                powerKw = cols[7].toIntOrNull() ?: 110,
                gearbox = unesc(cols[8]),
                color = unesc(cols[9]),
                stkUntil = unesc(cols[10]),
                tiresInfo = unesc(cols[11]),
                currentMileageKm = cols[12].toIntOrNull() ?: 0,
                records = recs,
                customParts = cust,
                customIntervals = intervals
            )
        }
    }
}
