package com.dibitara.app.data.export

import com.dibitara.app.domain.model.Category
import com.google.gson.*
import java.lang.reflect.Type

/** Garder les identifiants JSON historiques (chaînes), y compris dans les anciens exports. */
class CategoryJsonAdapter : JsonSerializer<Category>, JsonDeserializer<Category> {
    override fun serialize(src: Category, type: Type, context: JsonSerializationContext) = JsonPrimitive(src.name)
    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): Category = Category.valueOf(json.asString)
}
