package com.sans.finance.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import com.sans.finance.domain.model.Expense

import com.sans.finance.server.repository.InMemoryExpenseRepository
import com.sans.finance.domain.usecase.GetExpensesUseCase
import kotlinx.coroutines.flow.first

val repository = InMemoryExpenseRepository()
val getExpensesUseCase = GetExpensesUseCase(repository)

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }
    
    routing {
        get("/") {
            call.respondText("Sans Finance Multiplatform Server is running!")
        }
        
        get("/health") {
            call.respond(mapOf("status" to "UP"))
        }

        route("/api/expenses") {
            get {
                val expenses = getExpensesUseCase().first()
                call.respond(expenses)
            }
            
            post {
                val expense = call.receive<Expense>()
                val id = repository.insertExpense(expense)
                call.respond(mapOf("id" to id))
            }
        }
    }
}
