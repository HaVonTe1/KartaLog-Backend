package com.github.havonte1.boot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TcgWatcherApplication

fun main(args: Array<String>) {
    runApplication<TcgWatcherApplication>(*args)
}
