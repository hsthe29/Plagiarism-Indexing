package hust.thehs

import hust.thehs.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8704, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSerialization()
//    configureDatabases()
    configureTemplating()
    configureHTTP()
    configureSecurity()
    configureRouting()
}
