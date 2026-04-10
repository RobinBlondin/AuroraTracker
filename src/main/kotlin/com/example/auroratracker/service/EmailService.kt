package com.example.auroratracker.service

import com.example.auroratracker.config.EnvConfig
import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.Response
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Content
import com.sendgrid.helpers.mail.objects.Email
import org.springframework.stereotype.Service

@Service
class EmailService(
      private val envConfig: EnvConfig
) {
      val sg = SendGrid(envConfig.twilio.sendgridApiKey)

      fun sendEmail(to: String, from: String, subject: String, content: String): Response {
            val to = Email(to)
            val from = Email(from)
            val content = Content("text/plain", content)

            val mail = Mail(from, subject, to, content)

            val request = Request()
            try {
                  request.method = Method.POST
                  request.endpoint = "mail/send"
                  request.body = mail.build()

                  return sg.api(request)
            } catch (e: Exception) {
                  println("Failed sending email: ${e.message}")
                  e.printStackTrace()
                  return Response()
            }
      }
}