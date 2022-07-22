package io.github.qohat.routes

import arrow.core.*
import io.github.qohat.IncorrectInput
import io.github.qohat.service.RegisterUser

sealed interface InvalidField {
    val errors: NonEmptyList<String>
    val field: String
}

data class InvalidEmail(override val errors: NonEmptyList<String>) : InvalidField {
    override val field: String = "email"
}

data class InvalidPassword(override val errors: NonEmptyList<String>) : InvalidField {
    override val field: String = "password"
}

data class InvalidUsername(override val errors: NonEmptyList<String>) : InvalidField {
    override val field: String = "username"
}

private const val MIN_PASSWORD_LENGTH = 8
private const val MAX_PASSWORD_LENGTH = 100
private const val MAX_EMAIL_LENGTH = 350
private const val MIN_USERNAME_LENGTH = 1
private const val MAX_USERNAME_LENGTH = 25

fun RegisterUser.validate(): Validated<IncorrectInput, RegisterUser> =
    username.validUsername()
        .zip(email.validEmail(), password.validPassword(), ::RegisterUser)
        .mapLeft(:: IncorrectInput)

private fun String.validPassword(): ValidatedNel<InvalidPassword, String> =
    notBlank()
        .zip(minSize(MIN_PASSWORD_LENGTH), maxSize(MAX_PASSWORD_LENGTH)) { a, _, _ -> a }
        .mapLeft(toInvalidField(::InvalidPassword))

private fun String.validEmail(): ValidatedNel<InvalidEmail, String> {
    val trimmed = trim()
    return trimmed
        .notBlank()
        .zip(trimmed.maxSize(MAX_EMAIL_LENGTH), trimmed.looksLikeEmail()) { a, _, _ -> a }
        .mapLeft(toInvalidField(::InvalidEmail))
}

private fun String.validUsername(): ValidatedNel<InvalidUsername, String> {
    val trimmed = trim()
    return trimmed
        .notBlank()
        .zip(trimmed.minSize(MIN_USERNAME_LENGTH), trimmed.maxSize(MAX_USERNAME_LENGTH)) { a, _, _ -> a }
        .mapLeft(toInvalidField(::InvalidUsername))
}

private fun <A : InvalidField> toInvalidField(
    transform: (NonEmptyList<String>) -> A
): (NonEmptyList<String>) -> NonEmptyList<A> = { nel -> nonEmptyListOf(transform(nel)) }

private fun String.notBlank(): ValidatedNel<String, String> =
    if (isNotBlank()) validNel() else "Cannot be blank".invalidNel()

private fun String.minSize(size: Int) =
    if (length >= size) validNel() else "is too short (minimum is $size characters)".invalidNel()

private fun String.maxSize(size: Int) =
    if (length <= size) validNel() else "is too long (maximum is $size characters)".invalidNel()

private val emailPattern = ".+@.+\\..+".toRegex()

private fun String.looksLikeEmail() =
    if (emailPattern.matches(this)) validNel() else "'$this' is invalid email".invalidNel()