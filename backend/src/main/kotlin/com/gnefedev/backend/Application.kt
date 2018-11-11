package com.gnefedev.backend

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.web.bind.annotation.RestController

fun main(args: Array<String>) {
    SpringApplication.run(Application::class.java)
}

@SpringBootApplication
class Application {
}

@RestController
class PdfController(
) {
}
