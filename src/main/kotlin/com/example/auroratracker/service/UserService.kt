package com.example.auroratracker.service

import com.example.auroratracker.dto.UserDto
import com.example.auroratracker.mapper.UserMapper
import com.example.auroratracker.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(
      private val userRepository: UserRepository,
      private val userMapper: UserMapper
) {
      fun getAllUsers() = userRepository.findAll().map { userMapper.toDto(it) }

      fun getUserById(id: Long) = userRepository.findById(id).map { userMapper.toDto(it) }

      fun saveUser(userDto: UserDto): UserDto = userMapper.toEntity(userDto).let { userMapper.toDto(userRepository.save(it)) }

      fun deleteUserById(id: Long) = userRepository.deleteById(id)
}