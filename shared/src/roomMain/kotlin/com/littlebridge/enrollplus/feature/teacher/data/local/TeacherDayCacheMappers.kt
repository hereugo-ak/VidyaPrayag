package com.littlebridge.enrollplus.feature.teacher.data.local

import com.littlebridge.enrollplus.feature.teacher.domain.model.ResolvedDayDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.ResolvedPeriodDto
import com.littlebridge.enrollplus.feature.teacher.domain.model.CalendarOverlayDto
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

private val periodSerializer = ListSerializer(ResolvedPeriodDto.serializer())
private val calendarSerializer = ListSerializer(CalendarOverlayDto.serializer())

fun TeacherDayCacheEntity.toDomain(): ResolvedDayDto = ResolvedDayDto(
    date = date,
    weekday = weekday,
    isHoliday = isHoliday,
    holidayName = holidayName,
    periods = json.decodeFromString(periodSerializer, periodsJson),
    calendar = json.decodeFromString(calendarSerializer, calendarJson),
    nowIndex = nowIndex,
    nextIndex = nextIndex,
)

fun ResolvedDayDto.toEntity(cachedAt: Long): TeacherDayCacheEntity = TeacherDayCacheEntity(
    date = date,
    weekday = weekday,
    isHoliday = isHoliday,
    holidayName = holidayName,
    periodsJson = json.encodeToString(periodSerializer, periods),
    calendarJson = json.encodeToString(calendarSerializer, calendar),
    nowIndex = nowIndex,
    nextIndex = nextIndex,
    cachedAt = cachedAt,
)
