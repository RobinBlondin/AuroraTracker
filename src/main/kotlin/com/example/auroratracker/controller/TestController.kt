package com.example.auroratracker.controller

import com.example.auroratracker.dto.UserDto
import com.example.auroratracker.service.TrackingService
import com.example.auroratracker.service.UserService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class TestController(
      val userService: UserService,
      val trackingService: TrackingService
) {
      @GetMapping("/test-weather")
      fun testWeather(@RequestBody userDto: UserDto): Boolean {
            return userService.isAfterSunsetAndClearSky(userDto)
      }

      @GetMapping("/test-tracking")
      fun testTracking() {
            val kp = trackingService.getKpIndex()
            println(kp)
      }
}