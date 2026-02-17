package com.mysawit.payroll

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MysawitPayrollServiceApplication

fun main(args: Array<String>) {
    val context = runApplication<MysawitPayrollServiceApplication>(*args)
    if (context.environment.getProperty("app.test.close-context", Boolean::class.java, false)) {
        context.close()
    }
}
