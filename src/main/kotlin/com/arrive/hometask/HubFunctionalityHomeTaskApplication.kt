package com.arrive.hometask

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class HubFunctionalityHomeTaskApplication

fun main(args: Array<String>) {
    runApplication<HubFunctionalityHomeTaskApplication>(*args)
}
