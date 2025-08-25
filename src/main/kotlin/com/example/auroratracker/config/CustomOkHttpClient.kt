package com.example.auroratracker.config

import okhttp3.OkHttpClient
import org.springframework.context.annotation.Configuration

@Configuration
class CustomOkHttpClient: OkHttpClient()