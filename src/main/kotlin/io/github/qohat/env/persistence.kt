package io.github.qohat.env

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.continuations.resource
import arrow.fx.coroutines.fromCloseable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.qohat.repo.ProductId
import io.github.qohat.repo.UserId
import javax.sql.DataSource
import io.github.qohat.sqldelight.SqlDelight
import iogithubqohat.Products
import iogithubqohat.Users
import java.time.OffsetDateTime

fun hikari(env: Env.DataSource): Resource<HikariDataSource> =
    Resource.fromCloseable {
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = env.url
                username = env.username
                password = env.password
                driverClassName = env.driver
            }
        )
    }

fun sqlDelight(dataSource: DataSource): Resource<SqlDelight> = resource {
    val driver = Resource.fromCloseable(dataSource::asJdbcDriver).bind()
    SqlDelight.Schema.create(driver)
    SqlDelight(
        driver,
        Products.Adapter(productIdAdapter, userIdAdapter, offsetDateTimeAdapter, offsetDateTimeAdapter),
        Users.Adapter(userIdAdapter)
    )
}

private val productIdAdapter = columnAdapter(::ProductId, ProductId::serial)
private val userIdAdapter = columnAdapter(::UserId, UserId::serial)
private val offsetDateTimeAdapter = columnAdapter(OffsetDateTime::parse, OffsetDateTime::toString)

private inline fun <A : Any, B> columnAdapter(
    crossinline decode: (databaseValue: B) -> A,
    crossinline encode: (value: A) -> B
): ColumnAdapter<A, B> =
    object : ColumnAdapter<A, B> {
        override fun decode(databaseValue: B): A = decode(databaseValue)
        override fun encode(value: A): B = encode(value)
    }