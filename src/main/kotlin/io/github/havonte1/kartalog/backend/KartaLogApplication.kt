package io.github.havonte1.kartalog.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KartaLogApplication

fun main(args: Array<String>) {
    runApplication<KartaLogApplication>(*args)
}
