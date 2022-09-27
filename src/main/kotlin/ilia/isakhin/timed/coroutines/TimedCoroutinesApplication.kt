package ilia.isakhin.timed.coroutines

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.EnableAspectJAutoProxy

@SpringBootApplication
@EnableAspectJAutoProxy
class TimedCoroutinesApplication

fun main(args: Array<String>) {
    runApplication<TimedCoroutinesApplication>(*args)
}
