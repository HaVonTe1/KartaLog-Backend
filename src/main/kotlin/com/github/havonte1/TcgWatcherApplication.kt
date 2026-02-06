package com.github.havonte1

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Main entry point for the TCGWatcher Spring Boot application.
 */
@SpringBootApplication
class TcgWatcherApplication

fun main(args: Array<String>) {
    runApplication<TcgWatcherApplication>(*args)
}
