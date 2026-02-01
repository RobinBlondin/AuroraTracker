package com.example.auroratracker.mapper

import com.example.auroratracker.dto.AuroraPointDto
import com.example.auroratracker.entity.AuroraPoint
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface AuroraPointMapper {
      fun toDto(point: AuroraPoint): AuroraPointDto
      fun toEntity(dto: AuroraPointDto): AuroraPoint
}