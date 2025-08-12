package com.example.auroratracker.mapper

import com.example.auroratracker.dto.UserDto
import com.example.auroratracker.entity.User
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface UserMapper {
      fun toDto(user: User): UserDto
      fun toEntity(userDto: UserDto): User
}