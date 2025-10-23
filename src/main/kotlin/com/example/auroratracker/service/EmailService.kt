package com.example.auroratracker.service
import com.example.auroratracker.dto.UserDto
import io.github.cdimascio.dotenv.Dotenv
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets


@Service
class EmailService(
      private val mailSender: JavaMailSender,
) {
      private val emailScope = CoroutineScope(Dispatchers.IO)
      val dotenv: Dotenv? = Dotenv.configure().ignoreIfMissing().load()

      fun sendEmail(user: UserDto, subject: String, template: String, notification: Boolean = false, kp: Int? = null): Boolean {
            val message = mailSender.createMimeMessage()
            message.setFrom(dotenv?.get("MAIL_ADDRESS") ?: "")

            val html = loadTemplate(template)

            if(notification) {
                  html.replace("[LOCATION]", "$user.lon, $user.lat")
                  html.replace("[KP_VALUE]", kp.toString())
            }

            val helper = MimeMessageHelper(message, true, "UTF-8")
            helper.setTo(user.email!!)
            helper.setSubject(subject)
            helper.setText(html, true)

            return try {
                  mailSender.send(message)
                  true
            } catch (e: Exception) {
                  println("Failed to send email to ${user.email}: ${e.message}")
                  false
            }
      }

      fun sendEmailAsync(user: UserDto, subject: String, template: String, notification: Boolean, kp: Int? = null) = emailScope.async { sendEmail(user, subject, template, notification, kp) }

      fun loadTemplate(templateName: String): String {
            val inputStream = this::class.java.classLoader.getResourceAsStream("emails/$templateName")
                  ?: throw IllegalArgumentException("Template not found")

            return inputStream.readBytes().toString(StandardCharsets.UTF_8)
      }
}