package com.example.auroratracker

import com.example.auroratracker.config.EnvConfig
import io.github.cdimascio.dotenv.Dotenv
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(EnvConfig::class)
class AuroraTrackerApplication

fun main(args: Array<String>) {
      val dotenv = Dotenv.configure().ignoreIfMissing().load()

      dotenv.entries().forEach {
            System.setProperty(it.key, it.value)
      }
      runApplication<AuroraTrackerApplication>(*args)
}
