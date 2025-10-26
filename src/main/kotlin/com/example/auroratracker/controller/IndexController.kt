package com.example.auroratracker.controller

import io.github.cdimascio.dotenv.Dotenv
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class IndexController(

) {
      private val dotenv = Dotenv.configure().ignoreIfMissing().load()
      @GetMapping("/")
      fun index(model: Model): String {
            val key = dotenv.get("VAPID_PUBLIC_KEY") ?: ""
            model.addAttribute("key", key)
            return "index"
      }
}