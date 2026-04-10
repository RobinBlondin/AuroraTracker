package com.example.auroratracker.service

import com.example.auroratracker.config.EnvConfig
import com.twilio.Twilio
import com.twilio.rest.api.v2010.account.Message
import com.twilio.type.PhoneNumber
import org.springframework.stereotype.Service

@Service
class SmsService(
      private val env: EnvConfig
) {
      init {
            Twilio.init(
                  env.twilio.accountSid,
                  env.twilio.authToken
            )
      }

      fun sendMessage(to: PhoneNumber, from: PhoneNumber, message: String) {
            val message = Message.creator(to, from, message).create()
      }

      fun sendMessageSinch(to: PhoneNumber, message: Message) {
            TODO("Set up Sinch sms service")
      }

}