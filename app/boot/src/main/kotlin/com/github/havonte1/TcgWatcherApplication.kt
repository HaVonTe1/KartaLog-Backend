package com.github.havonte1

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.boot.SpringBootConfiguration

@SpringBootApplication
class TcgWatcherApplication

fun main(args: Array<String>) {
    runApplication<TcgWatcherApplication>(*args)
}
