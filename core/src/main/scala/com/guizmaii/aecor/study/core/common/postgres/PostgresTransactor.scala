package com.guizmaii.aecor.study.core.common.postgres

import cats.effect.{Async, ContextShift, Resource}
import com.guizmaii.aecor.study.core.config.PostgresConfig
import com.zaxxer.hikari.HikariDataSource
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts

object PostgresTransactor {

  import cats.syntax.functor._

  final def transactor[F[_]: Async: ContextShift](config: PostgresConfig): Resource[F, HikariTransactor[F]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](32)
      te <- ExecutionContexts.cachedThreadPool[F]
      tr <- HikariTransactor.newHikariTransactor[F](
        "org.postgresql.Driver",
        s"jdbc:postgresql://${config.contactPoints}:${config.port}/${config.database}",
        config.username,
        config.password,
        ce,
        te
      )
      _ <- Resource.liftF(tr.configure((ds: HikariDataSource) => Async[F].delay(ds.setAutoCommit(false))))
    } yield tr

}
