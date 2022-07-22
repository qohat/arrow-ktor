package io.github.qohat.service

import arrow.core.continuations.EffectScope
import io.github.qohat.DomainError
import io.github.qohat.repo.UserId
import io.github.qohat.repo.UserRepo
import io.github.qohat.routes.validate


data class RegisterUser(val username: String, val email: String, val password: String)

/*interface UserService {
    suspend fun register(user: RegisterUser): Either<DomainError, UserId>
}


fun userService(userRepo: UserRepo) = object : UserService {
    override suspend fun register(user: RegisterUser): Either<DomainError, UserId> =
        either {
            val (username, email, password) = user.validate().bind()
            userRepo.insert(username, email, password).bind()
        }
}*/

typealias DomainErrors = EffectScope<DomainError>

object UserService {
    context(UserRepo, DomainErrors)
    suspend fun register(user: RegisterUser): UserId {
        val (username, email, password) = user.validate().bind()
        return insert(username, email, password).bind()
    }
}