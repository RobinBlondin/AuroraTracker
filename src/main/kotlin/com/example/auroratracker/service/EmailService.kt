package com.example.auroratracker.service

import com.example.auroratracker.config.EnvConfig
import com.resend.*
import com.resend.core.exception.ResendException
import com.resend.services.emails.model.CreateEmailOptions
import com.resend.services.emails.model.CreateEmailResponse
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service


@Service
class EmailService(
    private val env: EnvConfig
) {
    private var resend = Resend(env.resend.apiKey)


    fun notificationEmailHtml(): String {
        val template = ClassPathResource("emails/notification-email.html")
            .inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val baseUrl = java.net.URI(env.resend.verificationUrl)
        return template
            .replace("{{siteUrl}}", escapeHtml(baseUrl.resolve("/").toASCIIString()))
            .replace("{{managementUrl}}", escapeHtml(baseUrl.resolve("/manage-alerts").toASCIIString()))
    }

    private fun escapeHtml(value: String): String = value.replace("&", "&amp;")
        .replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;")

    fun verificationEmailHtml(verificationUrl: String): String {
        val template = ClassPathResource("emails/verification-email.html")
            .inputStream.bufferedReader(Charsets.UTF_8)
            .use { it.readText() }

        return template.replace(
            "{{verificationUrl}}",
            verificationUrl.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
        )
    }

    fun sendManagementEmail(to: String, managementUrl: String) {
        val html = ClassPathResource("emails/management-email.html").inputStream
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val escapedUrl = managementUrl.replace("&", "&amp;").replace("\"", "&quot;")
            .replace("<", "&lt;").replace(">", "&gt;")
        val params = CreateEmailOptions.builder()
            .from("AuroraTracker <admin@auroratracker.se>")
            .to(to)
            .subject("Manage your Aurora Tracker alerts")
            .html(html.replace("{{managementUrl}}", escapedUrl))
            .build()
        resend.emails().send(params)
    }

    fun sendVerificationEmail(to: String, token: String) {
        val params = CreateEmailOptions.builder()
            .from("AuroraTracker <welcome@auroratracker.se>")
            .to(to)
            .subject("AuroraTracker subscription")
            .html(verificationEmailHtml(env.resend.verificationUrl + token))
            .build()

        try {
            val data: CreateEmailResponse = resend.emails().send(params)
            println(data.id)
        } catch (e: ResendException) {
            e.printStackTrace()
        }
    }

    fun sendNotificationEmail(to: String) {
        val email = notificationEmailHtml()
            .replace("{{notificationUrl}}", "/")
            .replace("{{managementUrl}}", "/manage-alerts")

        val params = CreateEmailOptions.builder()
            .from("Aurora Tracker <notifications@auroratracker.se>")
            .to(to)
            .subject("Aurora Tracker notifications")
            .html(email)
            .build()

            val data: CreateEmailResponse = resend.emails().send(params)
            println(data.id)
    }
}