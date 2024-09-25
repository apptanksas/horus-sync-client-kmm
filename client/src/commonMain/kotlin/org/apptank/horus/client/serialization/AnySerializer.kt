package org.apptank.horus.client.serialization

import org.apptank.horus.client.base.DataMap
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
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
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.float
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.modules.serializersModuleOf

/**
 * Custom serializer for handling various types of data in JSON serialization and deserialization.
 *
 * This serializer supports basic data types such as `String`, `Int`, and `Boolean`, as well as `Map` types.
 * It is used to serialize and deserialize these types into JSON format using Kotlinx Serialization.
 */
internal object AnySerializer : KSerializer<Any> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Any")

    val decoderJSON = Json {
        ignoreUnknownKeys = true
        serializersModule = serializersModuleOf(Any::class, AnySerializer)
    }

    /**
     * Serializes the provided value into JSON format.
     *
     * This method encodes different types of values (e.g., `String`, `Int`, `Boolean`, `Map`) into their
     * corresponding JSON representations. It only supports JSON encoding and throws an exception for unsupported types.
     *
     * @param encoder The encoder used to serialize the value.
     * @param value The value to be serialized. It should be of type `String`, `Int`, `Boolean`, or `Map`.
     * @throws SerializationException If the encoder is not a `JsonEncoder` or if the value type is unsupported.
     */
    override fun serialize(encoder: Encoder, value: Any) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("This class can be saved only by Json")
        val jsonElement: JsonElement = when (value) {
            is String -> JsonPrimitive(value)
            is Int -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Double -> JsonPrimitive(value)
            is Float -> JsonPrimitive(value)
            is Map<*, *> -> JsonObject(value.mapKeys { it.key.toString() }
                .mapValues { decoderJSON.encodeToJsonElement(it.value) })
            else -> throw SerializationException("Unsupported type")
        }
        jsonEncoder.encodeJsonElement(jsonElement)
    }

    /**
     * Deserializes a JSON element into a corresponding Kotlin type.
     *
     * This method converts JSON elements (e.g., `JsonPrimitive`, `JsonArray`, `JsonObject`) into their
     * corresponding Kotlin types (`String`, `Int`, `Boolean`, `List<DataMap>`, `DataMap`). It only supports JSON decoding
     * and throws an exception for unsupported JSON element types.
     *
     * @param decoder The decoder used to deserialize the JSON element.
     * @return The deserialized value, which can be of type `String`, `Int`, `Boolean`, `List<DataMap>`, or `DataMap`.
     * @throws SerializationException If the decoder is not a `JsonDecoder` or if the JSON element type is unsupported.
     */
    override fun deserialize(decoder: Decoder): Any {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("This class can be loaded only by Json")
        val jsonElement = jsonDecoder.decodeJsonElement()

        return when (jsonElement) {
            is JsonPrimitive -> when {
                jsonElement.isString -> jsonElement.content
                jsonElement.intOrNull != null -> jsonElement.int
                jsonElement.booleanOrNull != null -> jsonElement.boolean
                jsonElement.floatOrNull != null -> jsonElement.float
                jsonElement.doubleOrNull != null -> jsonElement.double
                else -> throw SerializationException("Unknown primitive type")
            }

            is JsonArray -> decoderJSON.decodeFromString<List<DataMap>>(jsonElement.toString())
            is JsonObject -> decoderJSON.decodeFromString<DataMap>(jsonElement.toString())
            else -> throw SerializationException("Unsupported JsonElement type")
        }
    }
}
