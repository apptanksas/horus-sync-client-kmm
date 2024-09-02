package com.apptank.horus.client.serialization

import com.apptank.horus.client.base.DataMap
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.modules.serializersModuleOf

internal object AnySerializer : KSerializer<Any> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Any")

    private val decoderJSON = Json {
        ignoreUnknownKeys = true
        serializersModule = serializersModuleOf(Any::class, AnySerializer)
    }

    override fun serialize(encoder: Encoder, value: Any) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("This class can be saved only by Json")
        val jsonElement: JsonElement = when (value) {
            is String -> JsonPrimitive(value)
            is Int -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Map<*, *> -> JsonPrimitive(Json.encodeToString(value as DataMap))
            else -> throw SerializationException("Unsupported type")
        }
        jsonEncoder.encodeJsonElement(jsonElement)
    }

    override fun deserialize(decoder: Decoder): Any {
        val jsonDecoder = decoder as? JsonDecoder ?: throw SerializationException("This class can be loaded only by Json")
        val jsonElement = jsonDecoder.decodeJsonElement()

        return when (jsonElement) {
            is JsonPrimitive -> when {
                jsonElement.isString -> jsonElement.content
                jsonElement.intOrNull != null -> jsonElement.int
                jsonElement.booleanOrNull != null -> jsonElement.boolean
                else -> throw SerializationException("Unknown primitive type")
            }
            is JsonArray -> decoderJSON.decodeFromString<List<DataMap>>(jsonElement.toString())
            is JsonObject -> decoderJSON.decodeFromString<DataMap>(jsonElement.toString())
            else -> throw SerializationException("Unsupported JsonElement type")
        }
    }
}