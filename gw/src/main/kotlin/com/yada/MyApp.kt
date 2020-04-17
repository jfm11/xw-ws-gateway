package com.yada

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication
open class MyApp

/**
 * 程序入口
 */
fun main(args: Array<String>) {
    runApplication<MyApp>(*args)
}