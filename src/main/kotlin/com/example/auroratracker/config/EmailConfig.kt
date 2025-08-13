package com.example.auroratracker.config

import io.github.cdimascio.dotenv.Dotenv
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

@Configuration
class EmailConfig{
      val dotenv: Dotenv? = Dotenv.configure().ignoreIfMissing().load()

      @Bean
      fun JavaMailSender(): JavaMailSender {
            val mailSender = JavaMailSenderImpl()
            mailSender.host = dotenv?.get("MAIL_HOST") ?: ""
            mailSender.port = (dotenv?.get("MAIL_PORT") ?: "587").toInt()

            mailSender.username = dotenv?.get("MAIL_ADDRESS") ?: ""
            mailSender.password = dotenv?.get("MAIL_PASSWORD") ?: ""

            val props = mailSender.javaMailProperties
            props.put("mail.transport.protocol", "smtp")
            props.put("mail.smtp.auth", "true")
            props.put("mail.smtp.starttls.enable", "true")
            props.put("mail.debug", "true")

            return mailSender
      }
}