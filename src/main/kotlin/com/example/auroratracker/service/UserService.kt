package com.example.auroratracker.service

import com.example.auroratracker.dto.UserDto
import com.example.auroratracker.dto.WeatherResponseDto
import com.example.auroratracker.mapper.UserMapper
import com.example.auroratracker.repository.UserRepository
import io.github.cdimascio.dotenv.Dotenv
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Optional

@Service
class UserService(
      private val userRepository: UserRepository,
      private val jsonService: JsonService
) {
      private lateinit var userMapper: UserMapper
      private val dotenv = Dotenv.configure().ignoreIfMissing().load()

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

      fun isAfterSunsetAndClearSky(userDto: UserDto): Boolean {
            val url = "https://api.open-meteo.com/v1/forecast?latitude=${userDto.lat}&longitude=${userDto.lon}&current=cloud_cover&daily=sunset"
            val response = jsonService.fetch(url)
            val weatherResponse = jsonService.parse<WeatherResponseDto>(response)

            val currentTime = LocalDateTime.parse(weatherResponse.current?.time ?: return false)
            val sunsetTime = LocalDateTime.parse(weatherResponse.daily?.sunset?.firstOrNull() ?: return false)


            val isAfterSunset = currentTime.isAfter(sunsetTime)
            println("isAfterSunset: $isAfterSunset")
            val isClearSky = (weatherResponse.current.cloudCover ?: Double.MAX_VALUE) < 20.0
            println("isClearSky: $isClearSky")

            return isAfterSunset && isClearSky
      }

      fun updateLastNotificationTime(userDto: UserDto): UserDto {
            val updatedUser = userMapper.toEntity(userDto).apply {
                  lastNotificationTime = LocalDateTime.now()
            }
            return userMapper.toDto(userRepository.save(updatedUser))
      }

      fun hasUserReceivedNotificationRecently(userDto: UserDto): Boolean {
            return userDto.lastNotificationTime?.let {
                  LocalDateTime.now().minusHours(12).isBefore(it)
            } ?: false
      }


}