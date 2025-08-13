package com.example.auroratracker.service
import io.github.cdimascio.dotenv.Dotenv
import jakarta.mail.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class EmailService(
      private val mailSender: JavaMailSender,
) {
      private val emailScope = CoroutineScope(Dispatchers.IO)
      val dotenv: Dotenv? = Dotenv.configure().ignoreIfMissing().load()

      fun sendEmail(to: String): Boolean {
            val message = mailSender.createMimeMessage()
            message.setFrom(dotenv?.get("MAIL_ADDRESS") ?: "")
            message.setRecipients(Message.RecipientType.TO, to)
            message.subject = "Aurora Tracker Notification"
            message.setText("Hello,\n\nAn aurora event is happening near your location!\n\nBest regards,\nAurora Tracker Team")

            return try {
                  mailSender.send(message)
                  true
            } catch (e: Exception) {
                  println("Failed to send email to $to: ${e.message}")
                  false
            }
      }

      fun sendEmailAsync(to: String) = emailScope.async { sendEmail(to) }
}