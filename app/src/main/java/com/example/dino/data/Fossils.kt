package com.example.dino.data

import android.content.Context
import android.location.Location
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.cos
import org.json.JSONObject

data class FossilPart(val id: String, val name: String, val description: String,
                      val modelPath: String? = null, val collectible: Boolean = true)
data class Fossil(
    val id: String, val name: String, val category: String, val period: String,
    val age: String, val description: String, val habitat: String,
    val sourceName: String, val sourceUrl: String, val parts: List<FossilPart>,
    val museumDisplay: Boolean = false
) {
    val modelPath get() = "models/$id.glb"
    val fieldParts get() = parts.filter { it.collectible }
    val completeModelPath get() = parts.firstOrNull { !it.collectible && it.id == "body" }?.modelPath ?: modelPath
}

object Catalog {
    val fossils = listOf(
        Fossil("triceratops", "Tricerátops", "DINOSSAURO HERBÍVORO", "Cretáceo Superior", "68–66 milhões de anos atrás",
            "Tricerátops era um dinossauro herbívoro com três chifres e uma expansão óssea atrás do crânio. Seu bico e seus dentes ajudavam a cortar e processar vegetação.",
            "América do Norte. Os achados virtuais desta expedição não representam a distribuição real da espécie.",
            "Natural History Museum", "https://www.nhm.ac.uk/discover/dino-directory/triceratops.html",
            listOf(FossilPart("head", "Crânio", "Dois chifres acima dos olhos, um chifre nasal e a gola óssea caracterizam o crânio do tricerátops.", "models/triceratops_head.glb"),
                FossilPart("body", "Esqueleto completo", "O animal se apoiava em quatro pernas. Monte esta reconstrução no laboratório após coletar e limpar o crânio encontrado no mapa.", "models/triceratops_body.glb", collectible = false)), museumDisplay = true),
        Fossil("trex", "T. rex", "DINOSSAURO CARNÍVORO", "Cretáceo Superior", "68–66 milhões de anos atrás",
            "Tyrannosaurus rex era um grande terópode que caminhava sobre duas pernas. Tinha uma cabeça robusta, dentes grandes e braços curtos com dois dedos.",
            "Oeste da América do Norte. As expedições do jogo são educativas e não indicam sítios reais.",
            "Smithsonian", "https://naturalhistory.si.edu/explore/dinosaurs-fossils/nations-t-rex",
            listOf(FossilPart("head", "Crânio", "As mandíbulas e os dentes robustos ajudam a estudar a alimentação deste grande carnívoro.", "models/trex_head.glb"),
                FossilPart("body", "Esqueleto completo", "As pernas sustentavam o corpo, enquanto a cauda contribuía para o equilíbrio. Monte a reconstrução no laboratório após coletar e limpar o crânio.", "models/trex_body.glb", collectible = false)), museumDisplay = true),
        Fossil("carnotaurus", "Carnotauro", "DINOSSAURO CARNÍVORO", "Cretáceo Superior", "71–69 milhões de anos atrás",
            "Carnotaurus sastrei era um terópode com dois chifres acima dos olhos e braços muito reduzidos. Impressões de pele preservaram evidências de escamas.",
            "Argentina. A reconstrução de partes não preservadas do esqueleto depende de comparações com parentes próximos.",
            "Natural History Museum", "https://www.nhm.ac.uk/discover/dino-directory/carnotaurus.html",
            listOf(FossilPart("head", "Crânio", "Os chifres e o focinho curto são características marcantes. A função exata dos chifres ainda é discutida.", "models/carnotaurus_head.glb"),
                FossilPart("body", "Esqueleto completo", "Esta reconstrução ilustra o corpo de um abelissaurídeo. Colete e limpe o crânio para montar o esqueleto no laboratório; partes não preservadas são reconstruídas por comparação.", "models/carnotaurus_body.glb", collectible = false)), museumDisplay = true)
    )
    fun fossil(id: String) = fossils.first { it.id == id }
    val regions = listOf(
        Region("valley", "Vale dos dinossauros", "Sousa · PB", "Expedição virtual aos dinossauros", -6.760, -38.230, listOf("triceratops", "trex", "carnotaurus"))
    )
}

data class Region(val id: String, val name: String, val place: String, val subtitle: String,
                  val latitude: Double, val longitude: Double, val fossilIds: List<String>)
data class GeoPoint(val latitude: Double, val longitude: Double)
data class Encounter(val id: String, val fossilId: String, val partId: String, val point: GeoPoint) {
    val fossil get() = Catalog.fossil(fossilId)
    val part get() = fossil.parts.first { it.id == partId }
}

fun distance(a: GeoPoint, b: GeoPoint): Float {
    val results = FloatArray(1)
    Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, results)
    return results[0]
}

// A fixed origin keeps markers in place as the player moves. Spawn distributions are illustrative.
fun encounters(region: Region, origin: GeoPoint): List<Encounter> {
    val offsets = listOf(20.0 to 35.0, -38.0 to 25.0, 48.0 to -30.0,
        -20.0 to -52.0, 60.0 to 42.0, -62.0 to -30.0)
    return region.fossilIds.flatMap { id -> Catalog.fossil(id).fieldParts.map { id to it } }
        .mapIndexed { index, (fossilId, part) ->
            val (north, east) = offsets[index % offsets.size]
            Encounter("${region.id}:$fossilId:${part.id}", fossilId, part.id,
                GeoPoint(origin.latitude + north / 111_320,
                    origin.longitude + east / (111_320 * cos(Math.toRadians(origin.latitude)).coerceAtLeast(0.01))))
        }
}

class ExpeditionStore(context: Context) {
    private val preferences = context.getSharedPreferences("expedition_v1", Context.MODE_PRIVATE)
    val isEmulator = Build.HARDWARE.lowercase() in setOf("goldfish","ranchu") || Build.PRODUCT.startsWith("sdk")
    init {
        // Switch existing installations to the new GPS experience once; later choices stay saved.
        if(!preferences.getBoolean("world_gps_v1",false)) preferences.edit()
            .putBoolean("demo",false).putBoolean("world_gps_v1",true).apply()
        // Desktop emulators have simulated GPS, so make play independent of Windows location.
        if(isEmulator && !preferences.getBoolean("pc_simulation_v1",false)) preferences.edit()
            .putBoolean("demo",true).putBoolean("pc_simulation_v1",true).apply()
    }
    var collected by mutableStateOf(preferences.getStringSet("collected", emptySet())!!.toSet())
        private set
    var assembledParts by mutableStateOf(preferences.getStringSet("assembled", emptySet())!!.toSet())
        private set
    var recoveredEncounters by mutableStateOf(preferences.getStringSet("recovered_encounters",emptySet())!!.toSet())
        private set
    var quantities by mutableStateOf(runCatching {
        val json=JSONObject(preferences.getString("quantities","{}")!!)
        collected.associateWith {json.optInt(it,1).coerceAtLeast(1)}
    }.getOrElse {collected.associateWith {1}})
        private set
    var demo by mutableStateOf(preferences.getBoolean("demo", false))
        private set
    var regionId by mutableStateOf(preferences.getString("region", "valley")!!.let { id ->
        if (Catalog.regions.any { it.id == id }) id else "valley"
    })
        private set
    var explorerId by mutableStateOf(preferences.getString("explorer", "male")!!.let { if (it == "female") it else "male" })
        private set
    fun selectExplorer(id: String) {
        if (id !in listOf("male", "female")) return
        explorerId = id
        preferences.edit().putString("explorer", id).apply()
    }
    fun partKey(fossilId: String, partId: String) = "$fossilId:$partId"
    // Retired catalog entries keep their saved progress, but don't count as current inventory.
    val collectedCount get() = Catalog.fossils.sumOf { fossil ->
        fossil.fieldParts.sumOf {quantity(fossil.id,it.id)}
    }
    val totalRecoveries get()=quantities.values.sum()
    fun quantity(fossilId:String,partId:String)=quantities[partKey(fossilId,partId)] ?: 0
    fun quantity(fossil:Fossil)=fossil.fieldParts.sumOf {quantity(fossil.id,it.id)}
    fun isRecovered(encounter:Encounter)=encounter.id in recoveredEncounters ||
        (!encounter.id.startsWith("world-v1:") && hasPart(encounter.fossilId,encounter.partId))
    fun hasPart(fossilId: String, partId: String) = partKey(fossilId, partId) in collected
    fun isAssembled(fossilId: String, partId: String) = partKey(fossilId, partId) in assembledParts
    fun count(fossil: Fossil) = fossil.fieldParts.count { hasPart(fossil.id, it.id) }
    fun canAssemble(fossil: Fossil) = fossil.fieldParts.isNotEmpty() &&
        fossil.fieldParts.all { hasPart(fossil.id, it.id) }
    fun complete(fossil: Fossil) = fossil.parts.all { isAssembled(fossil.id, it.id) }
    fun collect(encounter: Encounter): Boolean {
        if (!encounter.part.collectible) return false
        val key = partKey(encounter.fossilId, encounter.partId)
        if (isRecovered(encounter)) return false
        collected = collected + key
        recoveredEncounters=recoveredEncounters+encounter.id
        quantities=quantities+(key to ((quantities[key] ?: 0)+1))
        preferences.edit().putStringSet("collected",collected)
            .putStringSet("recovered_encounters",recoveredEncounters)
            .putString("quantities",JSONObject(quantities).toString()).apply()
        return true
    }
    fun assembleFossil(fossilId: String): Boolean {
        val fossil = Catalog.fossils.firstOrNull { it.id == fossilId } ?: return false
        if (!canAssemble(fossil) || complete(fossil)) return false
        // Persist the entire reconstruction at once, including non-collectible body models.
        assembledParts = assembledParts + fossil.parts.map { partKey(fossilId, it.id) }
        preferences.edit().putStringSet("assembled", assembledParts).apply()
        return true
    }
    fun changeDemoMode(value: Boolean) {
        demo = value
        preferences.edit().putBoolean("demo", value).apply()
    }
    fun selectRegion(id: String) {
        if (Catalog.regions.none { it.id == id }) return
        regionId = id
        preferences.edit().putString("region", id).apply()
    }
    fun reset() {
        collected = emptySet()
        assembledParts = emptySet()
        recoveredEncounters=emptySet()
        quantities=emptyMap()
        preferences.edit().remove("collected").remove("assembled").remove("recovered_encounters").remove("quantities").apply()
    }
}
