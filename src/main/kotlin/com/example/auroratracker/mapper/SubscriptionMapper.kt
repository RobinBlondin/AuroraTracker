package com.example.auroratracker.mapper

import com.example.auroratracker.dto.SubscriptionDto
import com.example.auroratracker.entity.Subscription
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface SubscriptionMapper {
      fun toDto(subscription: Subscription): SubscriptionDto
      fun toEntity(dto: SubscriptionDto): Subscription
}