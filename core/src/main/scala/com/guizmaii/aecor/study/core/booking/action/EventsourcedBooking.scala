package com.guizmaii.aecor.study.core.booking.action

import java.time.Instant
import java.util.concurrent.TimeUnit

import aecor.MonadActionLiftReject
import aecor.data.{EitherK, Enriched, EventsourcedBehavior}
import cats.Monad
import cats.data.NonEmptyList
import cats.effect.{Clock, Concurrent, Timer}
import com.guizmaii.aecor.study.core.booking.action.EventsourcedBooking.behavior
import com.guizmaii.aecor.study.core.common
import com.guizmaii.aecor.study.core.common.effect.TimedOutBehaviour
import com.guizmaii.aecor.study.core.booking.entity.Booking
import com.guizmaii.aecor.study.core.booking.event._
import com.guizmaii.aecor.study.core.booking.state.BookingStatus.{
  AwaitingConfirmation,
  Canceled,
  Confirmed,
  Denied,
  Settled
}
import com.guizmaii.aecor.study.core.booking.state._

final class EventsourcedBooking[F[_], G[_]](clock: Clock[G])(
    implicit F: MonadActionLiftReject[F, G, Option[BookingState], BookingEvent, BookingCommandRejection]
) extends Booking[F] {

  import cats.syntax.all._

  private val ignore: F[Unit] = F.unit

  override def place(client: ClientId, concert: ConcertId, seats: NonEmptyList[Seat]): F[Unit] =
    F.read.flatMap {
      case Some(_) => F.reject(BookingAlreadyExists)
      case None =>
        if (seats.distinct =!= seats) F.reject(DuplicateSeats)
        else if (seats.size > 10) F.reject(TooManySeats)
        else F.append(BookingPlaced(client, concert, seats))
    }

  override def confirm(tickets: NonEmptyList[Ticket], expiresAt: Option[Instant]): F[Unit] =
    status.flatMap {
      case AwaitingConfirmation =>
        F.append(BookingConfirmed(tickets, expiresAt)) >>
          F.whenA(tickets.foldMap(_.price).amount <= 0)(F.append(BookingSettled))

      case Confirmed | Settled => ignore
      case Denied              => F.reject(BookingIsDenied)
      case Canceled            => F.reject(BookingIsAlreadyCanceled)
    }

  override def deny(reason: String): F[Unit] =
    status.flatMap {
      case AwaitingConfirmation => F.append(BookingDenied(reason))
      case Denied               => ignore
      case Confirmed | Settled  => F.reject(BookingIsAlreadyConfirmed)
      case Canceled             => F.reject(BookingIsAlreadyCanceled)
    }

  override def cancel(reason: String): F[Unit] =
    status.flatMap {
      case AwaitingConfirmation | Confirmed => F.append(BookingCancelled(reason))
      case Canceled | Denied                => ignore
      case Settled                          => F.reject(BookingIsAlreadySettled)
    }

  override def receivePayment(paymentId: PaymentId): F[Unit] =
    status.flatMap {
      case AwaitingConfirmation        => F.reject(BookingIsNotConfirmed)
      case Canceled | Denied | Settled => F.reject(BookingIsAlreadySettled)
      case Confirmed                   => F.append(BookingPaid(paymentId)) >> F.append(BookingSettled)
    }

  override def status: F[BookingStatus] = state.map(_.status)

  override def tickets: F[Option[NonEmptyList[Ticket]]] = F.read.map(_.flatMap(_.tickets))

  override def expire: F[Unit] = {
    import cats.implicits._
    import common.syntax.instant._

    for {
      now <- F.liftF(clock.realTime(TimeUnit.MILLISECONDS)).map(Instant.ofEpochMilli)
      s   <- state
      _ <- s.status match {
        case Confirmed if now.isAfterM(s.expiresAt) => F.append(BookingExpired)
        case Confirmed                              => F.reject(TooEarlyToExpire)
        case _                                      => ignore
      }
    } yield ()
  }

  private def state: F[BookingState] = F.read.flatMap {
    case Some(s) => F.pure(s)
    case _       => F.reject(BookingNotFound)
  }
}

object EventsourcedBooking {
  final def behavior[F[_]: Monad](
      clock: Clock[F]
  ): EventsourcedBehavior[EitherK[Booking, BookingCommandRejection, ?[_]], F, Option[BookingState], BookingEvent] =
    EventsourcedBehavior
      .optionalRejectable(new EventsourcedBooking(clock), BookingState.init, _.handleEvent(_))
}

final case class EventMetadata(timestamp: Instant) extends AnyVal

abstract class Enrich[F[_]: Concurrent: Timer](clock: Clock[F]) {

  import cats.syntax.functor._

  import scala.concurrent.duration._

  val generateTimestamp: F[EventMetadata] =
    clock.realTime(TimeUnit.MILLISECONDS).map(Instant.ofEpochMilli).map(EventMetadata)

  val enrichedBehavior: EventsourcedBehavior[
    EitherK[Booking, BookingCommandRejection, ?[_]],
    F,
    Option[BookingState],
    Enriched[EventMetadata, BookingEvent]
  ] = /*_*/ behavior(clock).enrich[EventMetadata](generateTimestamp) /*_*/

  val timedOutBehavior: EventsourcedBehavior[
    EitherK[Booking, BookingCommandRejection, ?[_]],
    F,
    Option[BookingState],
    Enriched[EventMetadata, BookingEvent]
  ] = /*_*/ TimedOutBehaviour(enrichedBehavior)(2.seconds) /*_*/

}
