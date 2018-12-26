package com.guizmaii.aecor.study.core.booking

import java.time.Instant
import java.util.concurrent.TimeUnit

import aecor.runtime.Eventsourced
import aecor.runtime.akkageneric.{GenericAkkaRuntime, GenericAkkaRuntimeSettings}
import akka.actor.ActorSystem
import boopickle.Pickler
import cats.effect.{Clock, ConcurrentEffect, Timer}
import com.guizmaii.aecor.study.core.booking.action.{EventMetadata, EventsourcedBooking}
import com.guizmaii.aecor.study.core.booking.entity.Booking
import com.guizmaii.aecor.study.core.booking.event.BookingCommandRejection
import com.guizmaii.aecor.study.core.booking.state.BookingKey
import com.guizmaii.aecor.study.core.common.effect.TimedOutBehaviour

final class EntityWirings[F[_]](val bookings: Bookings[F])

object EntityWirings {

  final def apply[F[_]: ConcurrentEffect: Timer](
      system: ActorSystem,
      clock: Clock[F],
      postgresWirings: PostgresWirings[F]
  ): F[EntityWirings[F]] = {

    import aecor.data._
    import cats.syntax.functor._
    import com.guizmaii.aecor.study.core.booking.runtime.BookingWireCodecs._

    import scala.concurrent.duration._

    implicitly[Pickler[Instant]] // Required for IntelliJ...

    val genericAkkaRuntime = GenericAkkaRuntime(system)

    val generateTimestamp: F[EventMetadata] =
      clock.realTime(TimeUnit.MILLISECONDS).map(Instant.ofEpochMilli).map(EventMetadata)

    /*_*/
    val bookingsBehavior =
      TimedOutBehaviour(
        EventsourcedBooking.behavior[F](clock).enrich[EventMetadata](generateTimestamp)
      )(2.seconds)

    val createBehavior: BookingKey => F[EitherK[Booking, BookingCommandRejection, F]] =
      Eventsourced(
        entityBehavior = bookingsBehavior,
        journal = postgresWirings.bookingsJournal,
        snapshotting = None
      )

    val bookings: F[Bookings[F]] =
      genericAkkaRuntime
        .runBehavior(
          typeName = EventsourcedBooking.entityName,
          createBehavior = createBehavior,
          settings = GenericAkkaRuntimeSettings.default(system)
        )
        .map(Eventsourced.Entities.fromEitherK(_))
    /*_*/

    bookings.map(new EntityWirings(_))
  }

}
