package com.example.auroratracker.controller

import com.example.auroratracker.dto.UserDto
import com.example.auroratracker.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

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
      fun saveUser(@RequestBody  userDto: UserDto): ResponseEntity<UserDto> {
            if(!checkValidRequestBody(userDto)) {
                  return ResponseEntity.badRequest().body(null)
            }

            if(userService.checkIfUserExists(userDto)) {
                  return ResponseEntity.status(HttpStatus.CONFLICT).body(null)
            }
            val savedUser = userService.saveUser(userDto)
            return ResponseEntity.ok(savedUser)
      }

      @DeleteMapping("delete/{id}")
      fun deleteUserById(@PathVariable id: Long): ResponseEntity<String> {

            val deleted = userService.deleteUserById(id)

            return if (deleted) {
                  ResponseEntity.ok("User with ID $id deleted successfully.")
            } else {
                  ResponseEntity.notFound().build()
            }
      }

      private fun checkValidRequestBody(userDto: UserDto): Boolean {
            return userDto.name != null && userDto.email != null && userDto.phoneNumber != null
      }
}