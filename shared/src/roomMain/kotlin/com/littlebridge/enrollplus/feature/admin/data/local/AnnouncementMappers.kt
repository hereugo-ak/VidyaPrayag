package com.littlebridge.enrollplus.feature.admin.data.local

import com.littlebridge.enrollplus.feature.admin.domain.model.AnnouncementDto
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.Json

fun AnnouncementEntity.toDomain(): AnnouncementDto = AnnouncementDto(
    type = type,
    eventId = eventId,
    title = title,
    subTitle = subTitle,
    description = description,
    eventImage = eventImage,
    date = date,
    audienceType = audienceType,
    audienceFilter = audienceFilterJson?.let { Json.decodeFromString<JsonElement>(it) },
)

fun AnnouncementDto.toEntity(): AnnouncementEntity = AnnouncementEntity(
    eventId = eventId,
    type = type,
    title = title,
    subTitle = subTitle,
    description = description,
    eventImage = eventImage,
    date = date,
    audienceType = audienceType,
    audienceFilterJson = audienceFilter?.let { Json.encodeToString(JsonElement.serializer(), it) },
)
