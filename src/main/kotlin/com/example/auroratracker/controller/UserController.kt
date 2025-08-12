package com.example.auroratracker.controller

import com.example.auroratracker.dto.UserDto
import com.example.auroratracker.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController(
      private val userService: UserService
) {

      @GetMapping("all")
      fun getAllUsers(): ResponseEntity<List<UserDto>> = ResponseEntity.ok(userService.getAllUsers())

      @GetMapping("{id}")
      fun getUserById(@PathVariable id: Long): ResponseEntity<UserDto> {
            return userService.getUserById(id).map { ResponseEntity.ok(it) }
                  .orElse(ResponseEntity.notFound().build())
      }

      @PostMapping("save")
      fun saveUser(userDto: UserDto): ResponseEntity<UserDto> {
            val savedUser = userService.saveUser(userDto)
            return ResponseEntity.ok(savedUser)
      }

      @PostMapping("delete/{id}")
      fun deleteUserById(@PathVariable id: Long): ResponseEntity<String> {
            return try {
                  userService.deleteUserById(id)
                  ResponseEntity.ok("User with ID $id deleted successfully.")
            } catch (e: Exception) {
                  ResponseEntity.notFound().build()
            }
      }


}