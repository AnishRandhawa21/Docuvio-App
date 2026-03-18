package com.docuvio.app.core.network
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.gson.*
import java.lang.reflect.Type
import java.time.OffsetDateTime

class OffsetDateTimeAdapter : JsonSerializer<OffsetDateTime>,
    JsonDeserializer<OffsetDateTime> {

    override fun serialize(
        src: OffsetDateTime?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(src?.toString())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): OffsetDateTime? {
        return json?.asString?.let { OffsetDateTime.parse(it) }
    }
}
