package com.example.auroratracker.service

import com.example.auroratracker.dto.UserDto
import com.example.auroratracker.dto.WeatherResponseDto
import com.example.auroratracker.mapper.UserMapper
import com.example.auroratracker.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

@Service
class UserService(
      private val userRepository: UserRepository,
      private val jsonService: JsonService,
      private val userMapper: UserMapper
) {
      fun getAllUsers() = userRepository.findAll().map { userMapper.toDto(it) }

      fun getUserById(id: Long): Optional<UserDto> = userRepository.findById(id).map { userMapper.toDto(it) }

      fun saveUser(userDto: UserDto): UserDto =
            userMapper.toEntity(userDto).let { userMapper.toDto(userRepository.save(it)) }

      fun deleteUserById(id: Long): Boolean {
            if (!userRepository.existsById(id)) {
                  return false
            }
            userRepository.deleteById(id)
            return true
      }

      fun checkIfUserExists(userDto: UserDto): Boolean {
            if (userDto.email.isNullOrEmpty() || userDto.phoneNumber.isNullOrEmpty()) {
                  return false
            }
            return userRepository.existsByEmailOrPhoneNumber(userDto.email, userDto.phoneNumber)
      }

      fun isAfterSunsetAndClearSky(userDto: UserDto): Boolean {
            val url =
                  "https://api.open-meteo.com/v1/forecast?latitude=${userDto.lat}&longitude=${userDto.lon}&current=cloud_cover&daily=sunset"
            val response = jsonService.fetch(url)
            val weatherResponse = jsonService.parse<WeatherResponseDto>(response)

            val currentTime = ZonedDateTime.parse(weatherResponse.current?.time ?: return false)
            val sunsetTime = ZonedDateTime.parse(weatherResponse.daily?.sunset?.firstOrNull() ?: return false)

            val isAfterSunset = currentTime.isAfter(sunsetTime)
            val isClearSky = (weatherResponse.current.cloudCover ?: 100.0) < 20.0

            return isAfterSunset && isClearSky
      }

      fun updateLastNotificationTime(userDto: UserDto): UserDto {
            val updatedUser = userMapper.toEntity(userDto).apply {
                  lastNotificationTime = ZonedDateTime.now(ZoneOffset.UTC)
            }
            return userMapper.toDto(userRepository.save(updatedUser))
      }


      fun hasUserReceivedNotificationRecently(userDto: UserDto): Boolean {
            return userDto.lastNotificationTime?.let {
                  ZonedDateTime.now(ZoneOffset.UTC).minusHours(12).isBefore(it)
            } ?: false
      }
}