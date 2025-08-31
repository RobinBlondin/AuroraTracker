package com.example.auroratracker.controller

import com.example.auroratracker.dto.UserDto
import com.example.auroratracker.service.UserService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping

@Controller
class SignUpController(
      val userService: UserService
) {
      @GetMapping("/")
      fun signUpForm(): String {
            return "sign-up"
      }

      @PostMapping("/")
      fun signUpUser(@ModelAttribute("user") user: UserDto, model: Model): String {
            if(user.email.isNullOrEmpty()) {
                  model.addAttribute("missingField", "email")
                  return "sign-up"
            }
            if(user.lat == 0.0 || user.lon == 0.0) {
                  model.addAttribute("missingField", "cord")
                  return "sign-up"
            }

            userService.saveUser(user)
            model.addAttribute("user", user)
            model.addAttribute("success", true)
            return "sign-up"
      }
}