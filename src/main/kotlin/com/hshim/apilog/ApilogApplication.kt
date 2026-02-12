package com.hshim.apilog

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ApilogApplication

fun main(args: Array<String>) {
    runApplication<ApilogApplication>(*args)
}
