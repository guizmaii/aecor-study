package com.guizmaii.aecor.study.core.booking

import aecor.data.{Enriched, TagConsumer}
import aecor.journal.postgres.{Offset, PostgresEventJournal, PostgresOffsetStore}
import aecor.runtime.KeyValueStore
import cats.effect._
import cats.temp.par.Par
import com.guizmaii.aecor.study.core.booking.action.{EventMetadata, EventsourcedBooking}
import com.guizmaii.aecor.study.core.booking.event.BookingEvent
import com.guizmaii.aecor.study.core.booking.runtime.BookingEventSerializer
import com.guizmaii.aecor.study.core.booking.state.BookingKey
import com.guizmaii.aecor.study.core.common.postgres.PostgresTransactor
import com.guizmaii.aecor.study.core.config.{AppConfig, PostgresJournals}
import doobie.util.transactor.Transactor

final class PostgresWirings[F[_]: Async: Timer: Par] private (
    transactor: Transactor[F],
    journals: PostgresJournals
) {

  val offsetStoreCIO                                     = PostgresOffsetStore(tableName = "consumer_offset")
  val offsetStore: KeyValueStore[F, TagConsumer, Offset] = offsetStoreCIO.mapK(transactor.trans)

  val bookingsJournal =
    new PostgresEventJournal[F, BookingKey, Enriched[EventMetadata, BookingEvent]](
      xa = transactor,
      tableName = journals.booking.tableName,
      tagging = EventsourcedBooking.tagging,
      serializer = BookingEventSerializer
    )

}

object PostgresWirings {

  import cats.effect._
  import cats.implicits._
  import cats.temp.par._
  import doobie.implicits._

  final def apply[F[_]: Async: Timer: Par: ContextShift](settings: AppConfig): Resource[F, PostgresWirings[F]] =
    for {
      transactor <- PostgresTransactor.transactor[F](settings.postgres)
      wirings = new PostgresWirings(transactor, settings.postgresJournals)
      _ <- Resource.liftF(
        List(
          wirings.offsetStoreCIO.createTable.transact(transactor),
          wirings.bookingsJournal.createTable,
        ).parSequence
      )
    } yield wirings

}
