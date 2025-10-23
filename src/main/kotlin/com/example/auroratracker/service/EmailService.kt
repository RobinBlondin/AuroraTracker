package com.example.auroratracker.service
import com.example.auroratracker.dto.UserDto
import io.github.cdimascio.dotenv.Dotenv
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.slf4j.LoggerFactory
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
      private val log = LoggerFactory.getLogger(this::class.java)

      fun sendEmail(user: UserDto, subject: String, template: String, notification: Boolean = false, kp: Int? = null): Boolean {
            val message = mailSender.createMimeMessage()


            var html = loadTemplate(template)

            if(notification) {
                 html = html.
                        replace("[LOCATION]", "$user.lon, $user.lat")
                        .replace("[KP_VALUE]", kp.toString())
            }

            val helper = MimeMessageHelper(message, true, "UTF-8")
            helper.setFrom(dotenv?.get("MAIL_ADDRESS") ?: "")
            helper.setTo(user.email!!)
            helper.setSubject(subject)
            helper.setText(html, true)

            return try {
                  mailSender.send(message)
                  log.info("Sent email to ${user.name}")
                  true
            } catch (e: Exception) {
                  log.error("Failed to send email to ${user.email}: ${e.message}")
                  false
            }
      }

      fun sendEmailAsync(user: UserDto, subject: String, template: String, notification: Boolean, kp: Int? = null) = emailScope.async(
            Dispatchers.IO) { sendEmail(user, subject, template, notification, kp) }

      fun loadTemplate(templateName: String): String {
            val inputStream = this::class.java.classLoader.getResourceAsStream("emails/$templateName")
                  ?: throw IllegalArgumentException("Template not found")

            return inputStream.readBytes().toString(StandardCharsets.UTF_8)
      }
}