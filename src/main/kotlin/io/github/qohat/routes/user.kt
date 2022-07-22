package io.github.qohat.routes

import arrow.core.Either
import arrow.core.continuations.either
import io.github.qohat.DomainError
import io.github.qohat.Unexpected
import io.github.qohat.repo.UserRepo
import io.github.qohat.service.RegisterUser
import io.github.qohat.service.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable

@Serializable data class UserWrapper<T : Any>(val user: T)

@Serializable
data class NewUser(val username: String, val email: String, val password: String)

@Serializable
data class User(
    val id: Long,
    val email: String,
    val token: String,
    val username: String,
    val image: String
)

@Serializable data class LoginUser(val email: String, val password: String)

context(UserRepo)
fun Application.userRoutes() = routing {
    route("/users") {
        post {
            either<DomainError, UserWrapper<User>> {
                val (username, email, password) = receiveCatching<UserWrapper<NewUser>>().bind().user
                val userId = UserService.register(RegisterUser(username, email, password))
                UserWrapper(User(userId.serial, email, "", username, ""))
            }.respond(HttpStatusCode.Created)
        }
    }
}

// TODO improve how we receive models with validation
private suspend inline fun <reified A : Any> PipelineContext<
        Unit, ApplicationCall>.receiveCatching(): Either<DomainError, A> =
    Either.catch { call.receive<A>() }.mapLeft { e ->
        Unexpected(e.message ?: "Received malformed JSON for ${A::class.simpleName}", e)
    }