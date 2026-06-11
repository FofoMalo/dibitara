package com.dibitara.app.data.export

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.dibitara.app.domain.model.ExportData
import java.lang.reflect.Type
import java.time.LocalDate

/**
 * Génère un fichier JSON formaté contenant toutes les données d'export.
 * Structure : objet racine avec "exportDate", "version", puis une clé par type de données.
 *
 * Utilise Gson (déjà présent via Retrofit) avec un TypeAdapter pour [LocalDate],
 * qui n'est pas nativement sérialisable par Gson.
 */
object JsonExporter {

    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, AdaptateurLocalDate())
        .setPrettyPrinting()
        .create()

    fun generer(data: ExportData, versionApp: String): String {
        // LinkedHashMap pour garantir l'ordre des clés dans le JSON final
        val enveloppe = linkedMapOf(
            "exportDate"       to LocalDate.now().toString(),
            "version"          to versionApp,
            "enfants"          to data.enfants,
            "transactions"     to data.transactions,
            "budgets"          to data.budgets,
            "epargne"          to data.epargne,
            "immobilier"       to data.immobilier,
            "scpi"             to data.scpi,
            "airbnb"           to data.airbnb,
            "dettes"           to data.dettes,
            "metaux_precieux"  to data.metaux,
            "actifs_libres"    to data.actifsLibres,
            "epargne_salariale" to data.epargneSalariale
        )
        return gson.toJson(enveloppe)
    }

    /** Sérialise/désérialise LocalDate en chaîne ISO-8601 (ex : "2026-05-22"). */
    private class AdaptateurLocalDate : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
        override fun serialize(src: LocalDate, typeOfSrc: Type, ctx: JsonSerializationContext) =
            JsonPrimitive(src.toString())

        override fun deserialize(json: JsonElement, typeOfT: Type, ctx: JsonDeserializationContext) =
            LocalDate.parse(json.asString)
    }
}
