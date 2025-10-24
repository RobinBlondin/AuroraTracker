package com.example.auroratracker.controller

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus

class UserControllerTest {

      private val mockUserService = mock(UserService::class.java)
      private val userController = UserController(mockUserService)
      private val user = UserDto(id= 1L, name ="John", email = "john@example.com", phoneNumber =  "1234567890", lat = 0.0, lon = 0.0)

      @Test
      fun getAllUsers_returnsListOfUsers() {
            val users = listOf(user)
            `when`(mockUserService.getAllUsers()).thenReturn(users)

            val response = userController.getAllUsers()

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(users, response.body)
      }

      @Test
      fun getUserById_returnsUserWhenFound() {
            `when`(mockUserService.getUserById(1L)).thenReturn(java.util.Optional.of(user))

            val response = userController.getUserById(1L)

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(user, response.body)
      }

      @Test
      fun getUserById_returnsNotFoundWhenUserDoesNotExist() {
            `when`(mockUserService.getUserById(1L)).thenReturn(java.util.Optional.empty())

            val response = userController.getUserById(1L)

            assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
      }

      @Test
      fun saveUser_savesUserWhenRequestBodyIsValidAndUserDoesNotExist() {
            val user = UserDto(name ="added", email = "added@example.com", phoneNumber =  "1234567890", lat = 0.0, lon = 0.0)
            `when`(mockUserService.checkIfUserExists(user)).thenReturn(false)
            `when`(mockUserService.saveUser(user)).thenReturn(user)

            val response = userController.saveUser(user)

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(user, response.body)
      }

      @Test
      fun saveUser_returnsBadRequestWhenRequestBodyIsInvalid() {
            val user = UserDto(name = null, email = null, phoneNumber = null, lat = 0.0, lon = 0.0)
            val response = userController.saveUser(user)

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
            assertNull(response.body)
      }

      @Test
      fun saveUser_returnsConflictWhenUserAlreadyExists() {
            `when`(mockUserService.checkIfUserExists(user)).thenReturn(true)

            val response = userController.saveUser(user)

            assertEquals(HttpStatus.CONFLICT, response.statusCode)
            assertNull(response.body)
      }

      @Test
      fun deleteUserById_returnsSuccessWhenUserIsDeleted() {
            `when`(mockUserService.deleteUserById(1L)).thenReturn(true)

            val response = userController.deleteUserById(1L)

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals("User with ID 1 deleted successfully.", response.body)
      }

      @Test
      fun deleteUserById_returnsNotFoundWhenUserDoesNotExist() {
            `when`(mockUserService.deleteUserById(1L)).thenReturn(false)

            val response = userController.deleteUserById(1L)

            assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
      }
}