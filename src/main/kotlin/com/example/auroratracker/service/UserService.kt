package com.example.auroratracker.service

import com.example.auroratracker.dto.UserDto
import com.example.auroratracker.mapper.UserMapper
import com.example.auroratracker.repository.UserRepository
import org.springframework.stereotype.Service
import java.util.Optional

@Service
class UserService{
      private lateinit var userRepository: UserRepository
      private lateinit var userMapper: UserMapper

      fun getAllUsers() = userRepository.findAll().map { userMapper.toDto(it) }

      fun getUserById(id: Long): Optional<UserDto> = userRepository.findById(id).map { userMapper.toDto(it) }

      fun saveUser(userDto: UserDto): UserDto = userMapper.toEntity(userDto).let { userMapper.toDto(userRepository.save(it)) }

      fun deleteUserById(id: Long) = userRepository.deleteById(id)

      fun checkIfUserExists(userDto: UserDto): Boolean {
            if(userDto.email.isNullOrEmpty() || userDto.phoneNumber.isNullOrEmpty()) {
                  return false
            }
            return userRepository.existsByEmailOrPhoneNumber(userDto.email, userDto.phoneNumber)
      }
}