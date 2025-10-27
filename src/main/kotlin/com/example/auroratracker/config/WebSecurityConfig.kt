package com.example.auroratracker.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class WebSecurityConfig {

      @Bean
      fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
            http
                  .cors { }
                  .csrf { it.disable() }
                  .authorizeHttpRequests { auth ->
                        auth
                              .requestMatchers(
                                    "/",
                                    "/push",
                                    "/api/subscriptions/subscribe",
                                    "/api/subscriptions/unsubscribe",
                                    "/css/**",
                                    "/manifest.json",
                                    "/sw,js",
                                    "/script/**",
                                    "/images/**"
                              ).permitAll()
                              .anyRequest().authenticated()
                  }
                  .httpBasic {} // enkelt skydd för allt annat, kan bytas senare
            return http.build()
      }

      @Bean
      fun corsConfigurationSource(): CorsConfigurationSource {
            val config = CorsConfiguration()
            config.allowedOrigins = listOf("https://www.auroratracker.se")
            config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE")
            config.allowedHeaders = listOf("*")

            val source = UrlBasedCorsConfigurationSource()
            source.registerCorsConfiguration("/**", config)
            return source
      }
}
